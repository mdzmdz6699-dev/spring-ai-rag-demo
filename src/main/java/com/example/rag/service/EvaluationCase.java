package com.example.rag.service;

/**
 * 评测用例：问题 + 最相关文档的 source 标识 + 期望答案（用于忠实度/相关性判定）。
 */
public record EvaluationCase(String question, String relevantSource, String expectedAnswer) {
}
