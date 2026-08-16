package com.smartshuttle.ai.controller;

import com.smartshuttle.ai.handler.NLSelectHandler.NLSelectResult;//内部类也可以导入
import com.smartshuttle.ai.service.NLSelectService;
import com.smartshuttle.ai.service.NLSelectService.NLSelectRequest;
import com.smartshuttle.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI智能助手控制器
 */
@Tag(name = "AI智能助手")
@RestController
@RequestMapping("/api/v1/aiSelect")
@RequiredArgsConstructor
public class NLSelectController {
    
    private final NLSelectService NLSelectService;
    
    @Operation(summary = "AI对话")
    @PostMapping("/chat")
    public Result<NLSelectResult> chat(@RequestBody ChatRequestDTO request) {
        NLSelectRequest aiRequest = NLSelectRequest.builder()
                .userId(request.getUserId())
                .message(request.getMessage())
                .sessionId(request.getSessionId())
                .build();

        NLSelectResult response = NLSelectService.chat(aiRequest);
        return Result.success(response);
    }
    
    @Operation(summary = "快捷问题列表")
    @GetMapping("/quick-questions")
    public Result<List<String>> getQuickQuestions() {
        List<String> questions = List.of(
                "哪些线路乘坐率低于70%？",
                "明天下雨，班车需要调整吗？",
                "统计本月各线路运营数据",
                "如何优化成本？",
                "目前有多少车辆在运行？",
                "哪个站点乘车人数最多？"
        );
        return Result.success(questions);
    }
    
    @Operation(summary = "获取AI配置信息")
    @GetMapping("/config")
    public Result<Map<String, Object>> getAiConfig() {
        Map<String, Object> config = Map.of(
                "providers", List.of(
                        Map.of("value", "deepseek", "label", "DeepSeek"),
                        Map.of("value", "qwen", "label", "阿里千问"),
                        Map.of("value", "wenxin", "label", "百度文心")
                ),
                "currentProvider", "deepseek",
                "maxTokens", 2000,
                "temperature", 0.7
        );
        return Result.success(config);
    }
    
    @Data
    public static class ChatRequestDTO {
        private Long userId;
        private String message;
        private String sessionId;
    }
}
