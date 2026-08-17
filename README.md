# 代码 / 文档智能检索问答（RAG Demo）

基于 **Spring AI** 的私有代码 / 文档 RAG 检索问答系统。覆盖「切分 → 向量化 → 检索 → 生成 → 评测」全链路，并用可量化指标验证质量，而非仅做「问答」外壳。

## 技术栈
- Spring Boot 3.4.5 + Spring AI 1.0
- DeepSeek（`deepseek-v4-flash`）负责生成回答
- SimpleVectorStore 内存向量库 + 本地轻量嵌入（512 维哈希向量）
- 评测：Recall@K / Faithfulness / AnswerRelevance（LLM-as-Judge）

## 为什么用本地嵌入？
DeepSeek 不提供 embedding 接口（实测 `/v1/embeddings` 返回 404），因此向量化由**本地轻量嵌入模型**完成，生成回答仍走 DeepSeek。整个系统不依赖任何外部向量服务，可离线跑通检索链路。

## 运行方式
```bash
# 1. 设置 DeepSeek API Key（从环境变量读取，不落盘）
export DEEPSEEK_API_KEY=sk-xxxx        # Linux / macOS
# set DEEPSEEK_API_KEY=sk-xxxx         # Windows

# 2. 打包
mvn -B package -DskipTests

# 3. 启动（默认端口 8080）
java -jar target/rag-demo-0.0.1-SNAPSHOT.jar --server.port=8080
```
启动后浏览器访问 `http://localhost:8080` 即可对话；也可直接请求 `http://localhost:8080/ask?q=你的问题`。

## 评测结果
基于 3 条标注问答（本地向量检索 + DeepSeek 生成）：
- Recall@K = 1.00（检索命中率）
- Faithfulness = 1.00（答案忠实度）
- AnswerRelevance = 5.00（答案相关性，1–5）

## 目录结构
```
src/main/java/com/example/rag/
├── config/      VectorStoreConfig、LocalEmbeddingModel（本地嵌入）
├── service/     IngestService、RagService、EvaluationService、EvaluationCase
├── controller/  RagController（/ask 接口）
├── runner/      DemoRunner（启动即自动演示：入库→问答→评测）
└── RagDemoApplication
src/main/resources/
├── application.yml
├── static/index.html   网页对话界面
└── data/        guide.txt / spec.txt / faq.txt（示例语料）
```

## 说明
- 示例语料为「运维工单系统」文档；替换为自己的代码 / 文档（`data/` 下的 txt）即可复用全链路。
- 本地哈希嵌入语义弱于专业 embedding 模型，数据量大时建议替换为专业模型 + pgvector / Milvus。
