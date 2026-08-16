package com.smartshuttle.ai.client;

import com.smartshuttle.ai.config.AiProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 最简单的 DeepSeekClient 测试
 * 完全不依赖 Spring，直接创建对象
 */
class DeepSeekClientSimpleTest {

    /**
     * 测试真实 API 调用
     * 注意：这会真的调用 DeepSeek API，消耗额度
     */
    @Test
    void testRealApiCall() {
        // 1. 打印测试开始信息
        System.out.println("🚀 开始测试 DeepSeek 真实 API...");

        // 2. 创建配置对象
        AiProperties properties = new AiProperties();
        AiProperties.DeepSeekConfig config = new AiProperties.DeepSeekConfig();

        // 3. 设置配置（用你自己的 API Key）
        String apiKey = "sk-801561889fc2455aaa0e2bdf51d7ead2";
        config.setApiKey(apiKey);
        config.setBaseUrl("https://api.deepseek.com");
        config.setModel("deepseek-chat");
        config.setMaxTokens(100);  // 测试时用少一点 token
        config.setTemperature(0.1); // 测试时用低温度，回复更稳定
        config.setTimeout(30);

        properties.setDeepseek(config);

        // 4. 打印配置信息
        System.out.println("📋 配置信息：");
        System.out.println("  API Key 前10位: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
        System.out.println("  Base URL: " + config.getBaseUrl());
        System.out.println("  Model: " + config.getModel());
        System.out.println("  Max Tokens: " + config.getMaxTokens());

        // 5. 创建客户端
        DeepSeekClient client = new DeepSeekClient(properties);

        // 6. 测试获取提供商（这个方法不需要 API 调用）
        System.out.println("🔄 测试 getProvider()...");
        String provider = client.getProvider();
        assertEquals("deepseek", provider);
        System.out.println("  ✅ Provider: " + provider);

        // 7. 测试真实 API 调用
        try {
            System.out.println("📡 正在调用 DeepSeek API...");

            // 用一个简单的提示
            String prompt = "请用中文回复'测试成功'这四个字";
            System.out.println("  📝 提示: " + prompt);

            String response = client.chat(prompt);

            System.out.println("📨 API 响应：");
            System.out.println("  " + response);

            // 基本断言
            assertNotNull(response, "API 响应不能为 null");
            assertFalse(response.trim().isEmpty(), "API 响应不能为空字符串");
            assertTrue(response.length() > 0, "API 响应应有内容");

            // 检查是否包含预期内容
            if (response.contains("测试成功")) {
                System.out.println("🎉 完美！响应包含'测试成功'");
            } else if (response.contains("成功") || response.contains("test")) {
                System.out.println("✅ 不错！响应包含相关关键词");
            } else {
                System.out.println("⚠️  响应不包含预期关键词，但这不是错误");
            }

            System.out.println("✨ 测试完成！");

        } catch (Exception e) {
            // 捕获所有异常，打印详细信息
            System.err.println("❌ 测试失败！错误信息：");
            e.printStackTrace();

            // 如果是 API Key 问题
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                System.err.println("\n💡 可能的原因：");
                System.err.println("  1. API Key 无效或已过期");
                System.err.println("  2. API Key 格式错误");
                System.err.println("  3. 额度已用完");
            } else if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                System.err.println("\n💡 可能的原因：网络超时，请检查网络连接");
            }

            fail("API 调用失败: " + e.getMessage());
        }
    }

    /**
     * 测试异步调用
     */
    @Test
    void testAsyncChat() throws Exception {
        System.out.println("🔄 测试异步调用...");

        AiProperties properties = new AiProperties();
        AiProperties.DeepSeekConfig config = new AiProperties.DeepSeekConfig();
        config.setApiKey("sk-801561889fc2455aaa0e2bdf51d7ead2");
        config.setBaseUrl("https://api.deepseek.com");
        config.setModel("deepseek-chat");
        config.setMaxTokens(50);
        config.setTemperature(0.1);
        config.setTimeout(10);

        properties.setDeepseek(config);

        DeepSeekClient client = new DeepSeekClient(properties);

        // 异步调用
        var future = client.chatAsync("什么是人工智能？");

        // 等待结果
        String result = future.get();

        System.out.println("异步响应: " + result);
        assertNotNull(result);
    }

    /**
     * 测试不带参数的 chat 方法
     */
    @Test
    void testSimpleChat() {
        System.out.println("🧪 测试简单 chat 方法...");

        AiProperties properties = new AiProperties();
        AiProperties.DeepSeekConfig config = new AiProperties.DeepSeekConfig();
        config.setApiKey("sk-801561889fc2455aaa0e2bdf51d7ead2");
        config.setBaseUrl("https://api.deepseek.com");
        config.setModel("deepseek-chat");
        config.setMaxTokens(50);
        config.setTemperature(0.1);
        config.setTimeout(10);

        properties.setDeepseek(config);

        DeepSeekClient client = new DeepSeekClient(properties);

        String result = client.chat("Hello");
        System.out.println("简单响应: " + result);
        assertNotNull(result);
    }

    /**
     * 测试带历史记录的聊天
     */
    @Test
    void testChatWithHistory() {
        System.out.println("📚 测试带历史记录的聊天...");

        AiProperties properties = new AiProperties();
        AiProperties.DeepSeekConfig config = new AiProperties.DeepSeekConfig();
        config.setApiKey("sk-801561889fc2455aaa0e2bdf51d7ead2");
        config.setBaseUrl("https://api.deepseek.com");
        config.setModel("deepseek-chat");
        config.setMaxTokens(100);
        config.setTemperature(0.1);
        config.setTimeout(10);

        properties.setDeepseek(config);

        DeepSeekClient client = new DeepSeekClient(properties);

        // 创建历史记录
        var history = java.util.List.of(
                new com.smartshuttle.common.ai.LlmClient.ChatMessage("user", "我叫小明"),
                new com.smartshuttle.common.ai.LlmClient.ChatMessage("assistant", "你好小明！")
        );

        String result = client.chat("我刚才说我叫什么名字？", history);
        System.out.println("带历史的响应: " + result);
        assertNotNull(result);
    }
}