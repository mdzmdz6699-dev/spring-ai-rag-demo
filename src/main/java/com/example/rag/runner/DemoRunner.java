package com.example.rag.runner;

import java.util.List;

import com.example.rag.service.EvaluationCase;
import com.example.rag.service.EvaluationService;
import com.example.rag.service.IngestService;
import com.example.rag.service.RagService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 启动即演示：入库 -> 问答示例 -> 跑一组评测并打印指标。
 * 想用接口方式访问可改用 RagController（已提供 /ask）。
 */
@Configuration
public class DemoRunner {

    @Bean
    CommandLineRunner demo(IngestService ingest, RagService rag, EvaluationService eval) {
        return args -> {
            try {
                ingest.ingest();

                var result = rag.ask("如何提交一个工单？", 4);
                System.out.println("=== 问答示例 ===");
                System.out.println(result.answer());

                var cases = List.of(
                        new EvaluationCase("如何提交一个工单？", "guide", "通过系统新建工单并提交"),
                        new EvaluationCase("工单数据用的是什么格式？", "spec", "JSON，统一定义 Ticket 字段"),
                        new EvaluationCase("常见问题怎么查？", "faq", "FAQ 中列出了问题与解答"));

                System.out.println("=== 评测结果 ===");
                System.out.println(eval.evaluate(cases, 4));
            } catch (Exception e) {
                // 即使 Demo 调用 DeepSeek 失败，也保证 Web 服务正常启动（/ask 仍可用）
                System.err.println("!!! Demo 自动演示失败（应用仍正常启动，可访问 /ask 接口）：");
                e.printStackTrace();
            }
        };
    }
}
