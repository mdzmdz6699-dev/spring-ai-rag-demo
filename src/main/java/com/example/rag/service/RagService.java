package com.example.rag.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * RAG 核心：向量检索 -> 拼接上下文 -> 大模型生成可溯源回答。
 */
@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    private static final String PROMPT = """
            你是一个严谨的问答助手。只能根据下面提供的【上下文】回答问题，
            如果上下文里没有相关信息，就回答“根据已知资料无法回答”。
            上下文：
            {context}
            问题：{question}
            """;

    public RagService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
    }

    public AnswerResult ask(String question, int topK) {
        List<Document> ctx = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(topK).build());
        String context = ctx.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        String answer;
        try {
            answer = chatClient.prompt()
                    .user(u -> u.text(PROMPT)
                            .param("context", context)
                            .param("question", question))
                    .call()
                    .content();
        } catch (Exception e) {
            // 降级：生成模型不可用（DeepSeek Key 失效/网络异常）时，
            // 直接返回检索到的相关上下文，保证检索链路可独立演示
            answer = "【生成模型暂不可用（DeepSeek Key 失效或网络异常），以下为检索到的相关上下文】\n" + context;
        }

        return new AnswerResult(question, answer, context);
    }
}
