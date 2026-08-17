package com.example.rag.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * 本地轻量嵌入模型（不依赖任何外部服务）。
 * 采用「词 + 中文字符」哈希到固定维度向量并做 L2 归一化，
 * 使内容相似的文本在向量空间更接近，足以支撑检索阶段的语义相似度排序。
 *
 * 说明：DeepSeek 当前不提供 embedding 接口（/v1/embeddings 返回 404），
 * 故向量化使用本地实现；问答生成仍由 DeepSeek 大模型完成。
 */
public class LocalEmbeddingModel implements EmbeddingModel {

    private static final int DIM = 512;

    @Override
    public float[] embed(String text) {
        float[] vec = new float[DIM];
        if (text == null || text.isBlank()) {
            return vec;
        }
        for (String tok : tokenize(text)) {
            int h = Math.floorMod(tok.hashCode(), DIM);
            vec[h] += 1.0f;
        }
        double norm = 0.0;
        for (float x : vec) {
            norm += x * x;
        }
        norm = Math.sqrt(norm);
        if (norm > 0.0) {
            for (int i = 0; i < vec.length; i++) {
                vec[i] = (float) (vec[i] / norm);
            }
        }
        return vec;
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public EmbeddingResponse embedForResponse(List<String> texts) {
        List<Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            embeddings.add(new Embedding(embed(texts.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        return embedForResponse(request.getInstructions());
    }

    private Set<String> tokenize(String text) {
        Set<String> out = new HashSet<>();
        String lower = text.toLowerCase();
        for (String w : lower.split("[^a-z0-9]+")) {
            if (!w.isEmpty()) {
                out.add(w);
            }
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                out.add("c:" + c);
            }
        }
        return out;
    }
}
