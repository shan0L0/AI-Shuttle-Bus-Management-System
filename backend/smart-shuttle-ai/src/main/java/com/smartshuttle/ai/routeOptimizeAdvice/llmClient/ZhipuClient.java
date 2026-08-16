package com.smartshuttle.ai.routeOptimizeAdvice.llmClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartshuttle.ai.routeOptimizeAdvice.config.EmbAiProperties;
import com.smartshuttle.common.ai.EmbModelClient;
import com.smartshuttle.common.constant.ErrorCode;
import com.smartshuttle.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnProperty(name = "embedding-ai.provider", havingValue = "zhipu")
public class ZhipuClient implements EmbModelClient {

    private final EmbAiProperties.ZhipuConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ZhipuClient(EmbAiProperties properties) {
        this.config = properties.getZhipu();
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .readTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .writeTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .build();
        log.info("Zhipu客户端初始化完成，模型: {}", config.getModel());
    }
    @Override
    public String getProvider() {
        return "zhipu";
    }

    @Override
    public float[][] chat(String[] text) {
        try {
            // 1. 基础配置检查
            if (config == null) {
                log.error("❌ 智谱AI配置为空！请检查 application.yml");
                throw BusinessException.of(ErrorCode.AI_SERVICE_UNAVAILABLE, "智谱AI配置缺失");
            }
            log.info("✅ 调用智谱 Embedding-3 模型，输入文本数量: {}", text.length);

            // 2. 构建请求体（严格遵循你提供的链接格式）
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel()); // 固定为 embedding-3
            requestBody.put("input", objectMapper.valueToTree(text)); // 将String[]转为JSON数组
            requestBody.put("dimensions", config.getDimensions()); // 可选：根据你的配置添加维度参数
            System.out.println("input: " + requestBody);
            // 3. 构建 HTTP 请求
            Request request = new Request.Builder()
                    .url(config.getBaseUrl()) // Embedding专用端点
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(requestBody),
                            MediaType.parse("application/json")
                    ))
                    .build();

            System.out.println("请求：" + request);

            // 4. 发送请求并处理响应
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("智谱 Embedding API 请求失败，状态码:", response.code());
                    log.error("智谱 Embedding API 请求失败，请求体:", response.body().string());
                    throw BusinessException.of(ErrorCode.AI_SERVICE_UNAVAILABLE,
                            "向量化服务失败: " + response.code());
                }

                String responseBody = response.body().string();
                JsonNode rootNode = objectMapper.readTree(responseBody);

                // 5. 解析响应数据（对应链接中的 Response 结构）
                JsonNode dataArray = rootNode.get("data");
                if (dataArray == null || !dataArray.isArray()) {
                    throw BusinessException.of(ErrorCode.AI_RESPONSE_ERROR, "响应格式错误，缺失 data 数组");
                }

                // 6. 将 embedding 列表转换为 float[][]
                float[][] embeddings = new float[dataArray.size()][];
                for (int i = 0; i < dataArray.size(); i++) {
                    JsonNode item = dataArray.get(i);
                    JsonNode vecArray = item.get("embedding");

                    // 将 List<Double> 转换为 float[]
                    embeddings[i] = new float[vecArray.size()];
                    for (int j = 0; j < vecArray.size(); j++) {
                        embeddings[i][j] = vecArray.get(j).floatValue();
                    }
                }

                log.info("✅ 向量化成功，生成 {} 条向量，维度: {}", embeddings.length,
                        embeddings.length > 0 ? embeddings[0].length : 0);
                return embeddings;
            }
        } catch (IOException e) {
            log.error("智谱 Embedding API 调用异常", e);
            throw BusinessException.of(ErrorCode.AI_SERVICE_UNAVAILABLE, "向量化服务异常");
        }
    }



    @Override
    public CompletableFuture<String> chatAsync(String prompt) {
        return null;
    }
}
