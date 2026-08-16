package com.smartshuttle.ai.service;

import com.mysql.cj.QueryResult;
import com.smartshuttle.common.ai.LlmClient;
import com.smartshuttle.ai.config.AiProperties;
import com.smartshuttle.ai.handler.NLSelectHandler;
import com.smartshuttle.ai.handler.NLSelectHandler.NLSelectResult;
import com.smartshuttle.ai.prompt.PromptManager;
import com.smartshuttle.common.constant.ErrorCode;
import com.smartshuttle.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI聊天服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NLSelectService {

    private final LlmClient llmClient;
    private final PromptManager promptManager;
    private final NLSelectHandler NLSelectHandler;
    private final AiProperties properties;
    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_KEY = "ai:rate_limit:";

    /**
     * 处理用户消息
     */
    public NLSelectResult chat(NLSelectRequest request) {
        //todo：写一个类专门用于查询
        String prompt = promptManager.buildDataQueryPrompt(request.getMessage());
        String result = llmClient.chat(prompt);
        String sql = extractPureSQL(result);
        return NLSelectHandler.executeQuery(sql);
    }

    //解析sql
    public String extractPureSQL(String llmResponse) {
        if (llmResponse == null) return null;

        String response = llmResponse.trim();

        // 去掉可能的代码块标记
        if (response.startsWith("```sql")) {
            response = response.substring(6);
        }
        if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }

        // 只保留第一个分号之前的内容
        int semicolonIndex = response.indexOf(';');
        if (semicolonIndex > 0) {
            response = response.substring(0, semicolonIndex + 1);
        }

        return response.trim();
    }

    /**
     * 限流检查
     */
    private void checkRateLimit(Long userId) {
        String key = RATE_LIMIT_KEY + userId;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }

        if (count > properties.getRateLimit().getRequestsPerMinute()) {
            throw BusinessException.of(ErrorCode.AI_RATE_LIMITED, "请求频率过高，请稍后再试");
        }
    }

    /**
     * AI聊天请求
     */
    @lombok.Data
    @lombok.Builder
    public static class NLSelectRequest {
        private Long userId;
        private String message;
        private String sessionId;
        private List<LlmClient.ChatMessage> history;
    }

    /**
     * AI聊天响应
     */
//    @lombok.Data
//    @lombok.Builder
//    public static class NLSelectResponse {
//        private String message;
//        private String intent;
//        private Map<String, Object> data;
//        private Long responseTime;
//        private String provider;
//    }
}
