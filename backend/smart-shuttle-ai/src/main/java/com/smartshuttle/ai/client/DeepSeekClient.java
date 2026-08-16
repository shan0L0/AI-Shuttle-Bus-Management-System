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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek客户端实现
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "deepseek", matchIfMissing = true)
public class DeepSeekClient implements LlmClient {

    private final AiProperties.DeepSeekConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(AiProperties properties) {
        this.config = properties.getDeepseek();
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .readTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .writeTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .build();
        log.info("DeepSeek客户端初始化完成，模型: {}", config.getModel());
    }
    
    @Override
    public String getProvider() {
        return "deepseek";
    }
    
    @Override
    public String chat(String prompt) {
        return chat(prompt, new ArrayList<>());//无历史对话
    }
    
    @Override
    public String chat(String prompt, List<ChatMessage> history) {
        try {
            if (config != null) {
                log.info("✅ DeepSeek 配置加载成功");
                log.info("  API Key: {}", config.getApiKey());
                log.info("  Base URL: {}", config.getBaseUrl());
                log.info("  Model: {}", config.getModel());
            } else {
                log.error("❌ DeepSeek 配置为空！请检查 application.yml");
            }
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("temperature", config.getTemperature());
            
            // 构建消息数组
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
            
            // 发送请求
            Request request = new Request.Builder()
                    .url(config.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(requestBody),
                            MediaType.parse("application/json")
                    ))
                    .build();
            System.out.println("请求：" + request.body().toString());
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("DeepSeek API请求失败: {}", response.code());
                    throw BusinessException.of(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI服务请求失败: " + response.code());
                }
                
                String responseBody = response.body().string();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                // 解析响应
                JsonNode choices = jsonResponse.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    return choices.get(0).get("message").get("content").asText();
                }
                
                throw BusinessException.of(ErrorCode.AI_RESPONSE_ERROR, "AI响应解析失败");
            }
        } catch (IOException e) {
            log.error("DeepSeek API调用异常", e);
            throw BusinessException.of(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI服务暂时不可用");
        }
    }
    
    @Override
    public CompletableFuture<String> chatAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> chat(prompt));
    }
}
