package com.example.rag.service;

/**
 * 一次问答的结果：问题、答案、拼接的检索上下文。
 */
public record AnswerResult(String question, String answer, String context) {
}
