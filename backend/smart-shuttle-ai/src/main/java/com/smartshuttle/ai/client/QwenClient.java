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
 * 阿里千问客户端实现
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "qwen")
public class QwenClient implements LlmClient {
    
    private final AiProperties.QwenConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public QwenClient(AiProperties properties) {
        this.config = properties.getQwen();
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .readTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .writeTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .build();
        log.info("阿里千问客户端初始化完成，模型: {}", config.getModel());
    }
    
    @Override
    public String getProvider() {
        return "qwen";
    }
    
    @Override
    public String chat(String prompt) {
        return chat(prompt, new ArrayList<>());
    }
    
    @Override
    public String chat(String prompt, List<ChatMessage> history) {
        try {
            // 构建请求体 - 阿里千问格式
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            
            // input对象
            ObjectNode input = requestBody.putObject("input");
            ArrayNode messages = input.putArray("messages");
            
            // 添加系统消息
            ObjectNode sysMsg = messages.addObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", "你是一个智能车厂管理助手，帮助用户分析班车运营数据、优化线路和提供调度建议。");
            
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
            
            // parameters对象
            ObjectNode parameters = requestBody.putObject("parameters");
            parameters.put("max_tokens", config.getMaxTokens());
            parameters.put("temperature", config.getTemperature());
            parameters.put("result_format", "message");
            
            // 发送请求
            Request request = new Request.Builder()
                    .url(config.getBaseUrl() + "/services/aigc/text-generation/generation")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(requestBody),
                            MediaType.parse("application/json")
                    ))
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("千问API请求失败: {}", response.code());
                    throw BusinessException.of(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI服务请求失败: " + response.code());
                }
                
                String responseBody = response.body().string();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                // 解析阿里千问响应格式
                JsonNode output = jsonResponse.get("output");
                if (output != null) {
                    JsonNode choices = output.get("choices");
                    if (choices != null && choices.isArray() && choices.size() > 0) {
                        return choices.get(0).get("message").get("content").asText();
                    }
                    // 兼容旧格式
                    JsonNode text = output.get("text");
                    if (text != null) {
                        return text.asText();
                    }
                }
                
                throw BusinessException.of(ErrorCode.AI_RESPONSE_ERROR, "AI响应解析失败");
            }
        } catch (IOException e) {
            log.error("千问API调用异常", e);
            throw BusinessException.of(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI服务暂时不可用");
        }
    }
    
    @Override
    public CompletableFuture<String> chatAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> chat(prompt));
    }
}
