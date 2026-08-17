package com.example.rag.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * 文档入库：读取 resources/data 下的文本，切块后写入向量库。
 * 每个源文件打上 source 标识，供评测时判断「检索是否命中最相关文档」。
 */
@Service
public class IngestService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public IngestService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingest() {
        for (String name : List.of("guide", "spec", "faq")) {
            TextReader reader = new TextReader(new ClassPathResource("data/" + name + ".txt"));
            List<Document> docs = reader.get();
            for (Document d : docs) {
                d.getMetadata().put("source", name);
            }
            vectorStore.add(splitter.split(docs));
        }
        System.out.println("文档入库完成：guide / spec / faq");
    }
}
