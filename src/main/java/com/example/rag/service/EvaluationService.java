package com.example.rag.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * RAG 评测：用可量化指标验证检索与生成质量，而非只做「问答外壳」。
 * - Recall@K：检索结果前 K 条是否包含最相关文档（依据 source 标注判定）
 * - Faithfulness（忠实度）：答案是否完全由上下文支持（LLM-as-Judge）
 * - AnswerRelevance（答案相关性）：答案对问题的相关程度 1-5（LLM-as-Judge）
 */
@Service
public class EvaluationService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public EvaluationService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
    }

    public Metrics evaluate(List<EvaluationCase> cases, int k) {
        int hits = 0;
        double faithSum = 0.0;
        double relSum = 0.0;

        for (EvaluationCase c : cases) {
            List<Document> retrieved = vectorStore.similaritySearch(
                    SearchRequest.builder().query(c.question()).topK(k).build());

            boolean hit = retrieved.stream()
                    .anyMatch(d -> c.relevantSource().equals(d.getMetadata().get("source")));
            if (hit) {
                hits++;
            }

            String context = retrieved.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n---\n"));

            if (judgeFaithful(context, c.expectedAnswer())) {
                faithSum += 1.0;
            }
            relSum += judgeRelevance(c.question(), c.expectedAnswer());
        }

        int n = cases.size();
        return new Metrics(hits / (double) n, faithSum / n, relSum / n);
    }

    private boolean judgeFaithful(String context, String answer) {
        String r = chatClient.prompt().user(u -> u.text("""
                判断下面的【回答】是否完全由【上下文】支持（不得凭空捏造）。
                只回复 SUPPORTED 或 NOT_SUPPORTED 两个词之一。
                上下文：
                {context}
                回答：
                {answer}
                """).param("context", context).param("answer", answer)).call().content();
        return r != null && r.toUpperCase().contains("SUPPORTED");
    }

    private double judgeRelevance(String question, String answer) {
        String r = chatClient.prompt().user(u -> u.text("""
                评估【回答】对【问题】的相关程度，只回复 1 到 5 的整数（5 表示最相关）。
                问题：{question}
                回答：{answer}
                """).param("question", question).param("answer", answer)).call().content();
        try {
            return Double.parseDouble(r.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    public record Metrics(double recallAtK, double faithfulness, double answerRelevance) {
        @Override
        public String toString() {
            return String.format("Recall@K=%.2f | Faithfulness=%.2f | AnswerRelevance=%.2f",
                    recallAtK, faithfulness, answerRelevance);
        }
    }
}
