# run.ps1 - No Maven needed: auto-downloads Maven, builds, runs the RAG project
# Pure ASCII on purpose (PowerShell GBK read breaks non-ASCII).
$ErrorActionPreference = "Stop"

$ProjectDir    = $PSScriptRoot
$MavenVersion = "3.9.9"
$MavenUrl     = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MavenVersion/apache-maven-$MavenVersion-bin.zip"
$LocalDir     = Join-Path $ProjectDir ".mvn-local"
$MvnCmd       = "mvn"

# ---- Java detection (robust: PATH -> JAVA_HOME -> auto probe) ----
Write-Host "==> Checking Java (JDK 17+) ..."
$jdkOk = $false
try { java -version 2>&1 | Out-Null; $jdkOk = $true } catch {}
if (-not $jdkOk -and $env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $jdkOk = $true
}
if (-not $jdkOk) {
    $pf = $env:ProgramFiles
    foreach ($loc in @((Join-Path $pf 'Java'), (Join-Path $pf 'Eclipse Adoptium'), (Join-Path $pf 'Amazon Corretto'))) {
        if (Test-Path $loc) {
            $cand = Get-ChildItem $loc -Recurse -Depth 2 -Filter 'java.exe' -ErrorAction SilentlyContinue | Where-Object { $_.FullName -match 'bin\\java\.exe$' } | Select-Object -First 1
            if ($cand) { $env:JAVA_HOME = Split-Path (Split-Path $cand.FullName); $jdkOk = $true; break }
        }
    }
}
if (-not $jdkOk) {
    Write-Error "Java not found. Install JDK 17, set JAVA_HOME/PATH, then open a NEW PowerShell and run again."
    exit 1
}
Write-Host "Java OK (JAVA_HOME=$env:JAVA_HOME)"
$JavaExe = Join-Path $env:JAVA_HOME 'bin/java.exe'

# ---- Maven (auto-download if missing) ----
Write-Host "==> Checking Maven ..."
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    $MvnExe = Join-Path $LocalDir "apache-maven-$MavenVersion\bin\mvn.cmd"
    if (-not (Test-Path $MvnExe)) {
        Write-Host "Maven not found. Downloading Apache Maven $MavenVersion ..."
        $zip = Join-Path $env:TEMP "apache-maven-$MavenVersion-bin.zip"
        try {
            Invoke-WebRequest -Uri $MavenUrl -OutFile $zip -UseBasicParsing
        } catch {
            Write-Error "Maven download failed. Check network or put Maven manually into $LocalDir. Error: $_"
            exit 1
        }
        Expand-Archive -Path $zip -DestinationPath $LocalDir -Force
        Remove-Item $zip -Force
        Write-Host "Maven downloaded to $LocalDir"
    }
    $MvnCmd = $MvnExe
}

# ---- DeepSeek key ----
# If application.yml already has the key hardcoded, just press Enter to skip.
# Or set user env var DEEPSEEK_API_KEY=sk-xxx (reopen PowerShell after) for a one-time setup.
if (-not $env:DEEPSEEK_API_KEY) {
    $key = Read-Host "Paste DeepSeek API Key (or just press Enter if hardcoded in application.yml)"
    if ($key) { $env:DEEPSEEK_API_KEY = $key.Trim() }
}

# ---- Build executable jar ----
Write-Host "==> Building project (mvn clean package) ..."
Set-Location $ProjectDir
& $MvnCmd -B clean package
if ($LASTEXITCODE -ne 0) {
    Write-Error "BUILD FAILED. See Maven output above."
    exit 1
}

# ---- Run (capture full app output incl. crash stack to build.log) ----
$jar = Get-ChildItem target -Filter "rag-demo-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $jar) {
    Write-Error "Jar not found after build."
    exit 1
}
Write-Host "==> Starting Spring Boot app at http://localhost:8080 ... (full log -> build.log)"
Write-Host "    After it starts, open: http://localhost:8080/ask?q=hello"
& $JavaExe -jar $jar.FullName *>&1 | Tee-Object -FilePath build.log
