package com.smartshuttle.ai.routeOptimizeAdvice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "embedding-ai")//映射application.yml中的ai属性，从此层级开始匹配
public class EmbAiProperties {
    /**
     * AI服务提供商：
     */
    private String provider;

    /**
     * Zhipu配置
     */
    private ZhipuConfig Zhipu = new ZhipuConfig();

    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    @Data
    public static class ZhipuConfig {
        private String apiKey = "ac09714420de46a09d4890a4c7dc433d.UWKnXmVLWGeQA37h";
        private String baseUrl = "https://open.bigmodel.cn/api/paas/v4/embeddings";
        private String model = "embedding-3";
        private Integer dimensions = 512;
        private Double temperature = 0.7;
        private Integer timeout = 60;
    }

    @Data
    public static class CacheConfig {
        private Boolean enabled = true;
        private Integer ttl = 3600; // 秒
    }

    @Data
    public static class RateLimitConfig {
        private Integer requestsPerMinute = 60;
    }
}
