package com.smartshuttle.ai.routeOptimizeAdvice.controller;

import com.smartshuttle.ai.routeOptimizeAdvice.service.RenewVectorStorage;
import com.smartshuttle.ai.routeOptimizeAdvice.service.ResponseService;
import com.smartshuttle.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "AI运营优化")
@RestController
@RequestMapping("/api/v1/aiOptimize")
@RequiredArgsConstructor
public class routeOptimizeAdviceController {

    private final RenewVectorStorage renewVectorStorage;
    private final ResponseService responseService;

    /**
     * 功能1：刷新全部知识库
     * 清空所有知识向量，重新从文件 + MySQL 运营记录构建知识库
     */
    @Operation(summary = "刷新全部知识库", description = "清空所有知识向量，重新从文件 + MySQL运营记录构建知识库")
    @PostMapping("/refresh/all")
    public Result<String> refreshAllKnowledge() {
        log.info("接收到刷新全部知识库请求");
        try {
            long startTime = System.currentTimeMillis();
            renewVectorStorage.renewAll();
            long endTime = System.currentTimeMillis();
            String message = String.format("全部知识库刷新成功，耗时: %d ms", (endTime - startTime));
            log.info(message);
            return Result.success(message);
        } catch (Exception e) {
            log.error("刷新全部知识库失败", e);
            return Result.fail("刷新全部知识库失败: " + e.getMessage());
        }
    }

    /**
     * 功能2：只刷新来源为 file 的知识
     * 只删除并重新构建文件来源的知识向量
     */
    @Operation(summary = "刷新文件知识库", description = "只删除并重新构建文件来源（txt文件）的知识向量")
    @PostMapping("/refresh/file")
    public Result<String> refreshFileKnowledge() {
        log.info("接收到刷新文件知识库请求");
        try {
            long startTime = System.currentTimeMillis();
            renewVectorStorage.renewFileOnly();
            long endTime = System.currentTimeMillis();
            String message = String.format("文件知识库刷新成功，耗时: %d ms", (endTime - startTime));
            log.info(message);
            return Result.success(message);
        } catch (Exception e) {
            log.error("刷新文件知识库失败", e);
            return Result.fail("刷新文件知识库失败: " + e.getMessage());
        }
    }

    /**
     * 功能3：只刷新来源为 operation_record 的知识
     * 只删除并重新构建运营记录来源的知识向量
     */
    @Operation(summary = "刷新运营记录知识库", description = "只删除并重新构建运营记录来源的知识向量")
    @PostMapping("/refresh/record")
    public Result<String> refreshRecordKnowledge() {
        log.info("接收到刷新运营记录知识库请求");
        try {
            long startTime = System.currentTimeMillis();
            renewVectorStorage.renewRecordOnly();
            long endTime = System.currentTimeMillis();
            String message = String.format("运营记录知识库刷新成功，耗时: %d ms", (endTime - startTime));
            log.info(message);
            return Result.success(message);
        } catch (Exception e) {
            log.error("刷新运营记录知识库失败", e);
            return Result.fail("刷新运营记录知识库失败: " + e.getMessage());
        }
    }

    /**
     * 获取知识库统计信息（辅助功能）
     */
    @Operation(summary = "获取知识库统计信息", description = "获取当前知识库的统计信息，包括各来源数量、总字符数等")
    @GetMapping("/stats")
    public Result<RenewVectorStorage.VectorStats> getKnowledgeStats() {
        log.info("接收到获取知识库统计信息请求");
        try {
            RenewVectorStorage.VectorStats stats = renewVectorStorage.getVectorStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取知识库统计信息失败", e);
            return Result.fail("获取知识库统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 功能4：接受用户问题调用大模型进行建议
     */
    @Operation(summary = "AI智能建议", description = "接收用户问题，基于知识库进行智能分析并给出优化建议")
    @PostMapping("/chat")
    public Result<String> getAISuggestion(@RequestBody ChatRequest request) {
        log.info("接收到AI建议请求，问题: {}", request.getQuestion());

        try {
            // 调用响应服务处理问题
            String response = responseService.processQuestion(request.getQuestion());

            if (response == null || response.isEmpty()) {
                return Result.fail("AI服务响应为空，请稍后重试");
            }

            // 检查是否历史消息超限
            if (response.startsWith("HISTORY_LIMIT_EXCEEDED")) {
                String limitMsg = "历史消息过多，请先清空历史记录或开始新会话";
                return Result.fail(limitMsg);
            }

            log.info("AI建议生成成功，响应长度: {}", response.length());
            return Result.success(response);

        } catch (Exception e) {
            log.error("获取AI建议失败", e);
            return Result.fail("获取AI建议失败: " + e.getMessage());
        }
    }

    /**
     * 清空对话历史
     */
    @Operation(summary = "清空对话历史", description = "清空当前会话的所有历史消息")
    @DeleteMapping("/clearHistory")
    public Result<String> clearChatHistory() {
        log.info("接收到清空对话历史请求");

        try {
            responseService.clearConversationHistory();
            log.info("对话历史已清空");
            return Result.success("对话历史已清空");

        } catch (Exception e) {
            log.error("清空对话历史失败", e);
            return Result.fail("清空对话历史失败: " + e.getMessage());
        }
    }

    // ==================== 内部类 ====================
    @Data
    public static class ChatRequest {
        @Schema(description = "用户问题", example = "5号线上座率低，有什么优化建议？")
        private String question;
    }

}
