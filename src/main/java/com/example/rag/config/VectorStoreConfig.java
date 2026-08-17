package com.example.rag.config;

import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 显式声明内存向量库 bean（SimpleVectorStore）。
 * 向量化使用本地轻量嵌入模型（LocalEmbeddingModel，不依赖外部服务）；
 * 问答生成由 DeepSeek 大模型完成。无需任何外部数据库。
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore() {
        return SimpleVectorStore.builder(new LocalEmbeddingModel()).build();
    }
}
