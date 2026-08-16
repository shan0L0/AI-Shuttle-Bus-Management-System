package com.smartshuttle.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")//映射application.yml中的ai属性，从此层级开始匹配
public class AiProperties {
    
    /**
     * AI服务提供商：deepseek / qwen / wenxin
     */
    private String provider;// = "deepseek";//映射yml中ai.provider
    
    /**
     * DeepSeek配置
     */
    private DeepSeekConfig deepseek = new DeepSeekConfig();//映射yml中ai.deepseek，与provider处于同一层级
    
    /**
     * 阿里千问配置
     */
    private QwenConfig qwen = new QwenConfig();
    
    /**
     * 百度文心配置
     */
    private WenxinConfig wenxin = new WenxinConfig();

    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();
    
    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();
    
    @Data
    public static class DeepSeekConfig {
        private String apiKey;
        private String baseUrl;
        private String model;
        private Integer maxTokens = 2000;
        private Double temperature = 0.7;
        private Integer timeout = 60;
    }
    
    @Data
    public static class QwenConfig {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        private String model = "qwen-turbo";
        private Integer maxTokens = 2000;
        private Double temperature = 0.7;
        private Integer timeout = 60;
    }
    
    @Data
    public static class WenxinConfig {
        private String apiKey;
        private String secretKey;
        private String baseUrl = "https://aip.baidubce.com";
        private String model = "ernie-bot-4";
        private Integer maxTokens = 2000;
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
