package com.smartshuttle.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartshuttle.ai.config.AiProperties;
import com.smartshuttle.common.ai.LlmClient;
import com.smartshuttle.common.constant.ErrorCode;
import com.smartshuttle.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 百度文心客户端实现
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "wenxin")
public class WenxinClient implements LlmClient {
    
    private final AiProperties.WenxinConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    
    private static final String ACCESS_TOKEN_KEY = "wenxin:access_token";
    
    public WenxinClient(AiProperties properties, StringRedisTemplate redisTemplate) {
        this.config = properties.getWenxin();
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .readTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .writeTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .build();
        log.info("百度文心客户端初始化完成，模型: {}", config.getModel());
    }
    
    @Override
    public String getProvider() {
        return "wenxin";
    }
    
    /**
     * 获取AccessToken
     */
    private String getAccessToken() {
        // 从Redis获取缓存的Token
        String token = redisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
        if (token != null) {
            return token;
        }
        
        // 请求新Token
        try {
            String url = String.format(
                    "%s/oauth/2.0/token?grant_type=client_credentials&client_id=%s&client_secret=%s",
                    config.getBaseUrl(), config.getApiKey(), config.getSecretKey()
            );
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", MediaType.parse("application/json")))
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw BusinessException.of(ErrorCode.AI_SERVICE_UNAVAILABLE, "获取文心Token失败");
                }
                
                String responseBody = response.body().string();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                token = jsonResponse.get("access_token").asText();
                int expiresIn = jsonResponse.get("expires_in").asInt();
                
                // 缓存Token（提前5分钟过期）
                redisTemplate.opsForValue().set(ACCESS_TOKEN_KEY, token, expiresIn - 300, TimeUnit.SECONDS);
                
                return token;
            }
        } catch (IOException e) {
            log.error("获取文心Token异常", e);
            throw BusinessException.of(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI服务暂时不可用");
        }
    }
    
    /**
     * 根据模型获取API路径
     */
    private String getModelPath() {
        return switch (config.getModel()) {
            case "ernie-bot-4" -> "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions_pro";
            case "ernie-bot-turbo" -> "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/eb-instant";
            case "ernie-bot" -> "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions";
            default -> "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions";
        };
    }
    
    @Override
    public String chat(String prompt) {
        return chat(prompt, new ArrayList<>());
    }
    
    @Override
    public String chat(String prompt, List<ChatMessage> history) {
        try {
            String accessToken = getAccessToken();
            
            // 构建请求体 - 百度文心格式
            ObjectNode requestBody = objectMapper.createObjectNode();
            
            // messages数组
            ArrayNode messages = requestBody.putArray("messages");
            
            // 添加历史消息
            for (ChatMessage msg : history) {
                ObjectNode msgNode = messages.addObject();
                msgNode.put("role", msg.role());
                msgNode.put("content", msg.content());
            }
            
            // 添加当前用户消息
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            
            // 参数设置
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_output_tokens", config.getMaxTokens());
            
            // 系统消息
            requestBody.put("system", "你是一个智能车厂管理助手，帮助用户分析班车运营数据、优化线路和提供调度建议。");
            
            // 发送请求
            String url = config.getBaseUrl() + getModelPath() + "?access_token=" + accessToken;
            
            Request request = new Request.Builder()
                    .url(url)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(requestBody),
                            MediaType.parse("application/json")
                    ))
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("文心API请求失败: {}", response.code());
                    throw BusinessException.of(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI服务请求失败: " + response.code());
                }
                
                String responseBody = response.body().string();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                // 检查错误
                if (jsonResponse.has("error_code")) {
                    String errorMsg = jsonResponse.get("error_msg").asText();
                    log.error("文心API错误: {}", errorMsg);
                    throw BusinessException.of(ErrorCode.AI_RESPONSE_ERROR, "AI响应错误: " + errorMsg);
                }
                
                // 解析响应
                JsonNode result = jsonResponse.get("result");
                if (result != null) {
                    return result.asText();
                }
                
                throw BusinessException.of(ErrorCode.AI_RESPONSE_ERROR, "AI响应解析失败");
            }
        } catch (IOException e) {
            log.error("文心API调用异常", e);
            throw BusinessException.of(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI服务暂时不可用");
        }
    }
    
    @Override
    public CompletableFuture<String> chatAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> chat(prompt));
    }
}
