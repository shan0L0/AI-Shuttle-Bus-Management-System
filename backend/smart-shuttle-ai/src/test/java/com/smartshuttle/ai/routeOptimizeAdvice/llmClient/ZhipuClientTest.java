package com.smartshuttle.ai.routeOptimizeAdvice.llmClient;

import com.smartshuttle.ai.routeOptimizeAdvice.config.EmbAiProperties;
import com.smartshuttle.ai.routeOptimizeAdvice.service.TextSplitterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 最简单的 ZhipuClient 测试
 * 完全不依赖 Spring，直接创建对象
 */
class ZhipuClientSimpleTest {

    private TextSplitterService textSplitterService;

    @TempDir
    Path tempDir;  // JUnit 5 临时目录


    @BeforeEach
    void setUp() {
        textSplitterService = new TextSplitterService();
    }

    // ------------------ 测试数据准备 ------------------

    /**
     * 创建测试用文本文件
     */
    private String createTestFile(String content) throws IOException {
        File tempFile = tempDir.resolve("test.txt").toFile();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }
        return tempFile.getAbsolutePath();
    }

    /**
     * 标准测试配置
     */
    private TextSplitterService.ChunkConfig createTestConfig(
            TextSplitterService.SplitStrategy strategy) {
        return new TextSplitterService.ChunkConfig(
                strategy, 100, 10, 20, true, true
        );
    }


    /**
     * 测试真实 API 调用
     * 注意：这会真的调用 Zhipu API，消耗额度
     */
    @Test
    void testRealApiCall() throws IOException {
        // 准备测试数据
        String content = """
            公交线路优化理论知识。
            
            高峰期调度应采用区间车模式提升线路满载率。
            
            在城市公交运营中，早高峰时段的客流集中现象尤为明显。
            针对线路满载率超过120%的情况，建议采取增加发车班次、调整时刻表、优化站点设置等综合措施。
            同时，可考虑引入智能调度系统，实现动态资源调配。
            """;

        String filePath = createTestFile(content);
        TextSplitterService.ChunkConfig config = createTestConfig(
                TextSplitterService.SplitStrategy.BY_SENTENCE
        );

        String[] chunks = textSplitterService.splitFromFile(filePath, config);
        System.out.println(Arrays.toString(chunks));
        // 1. 打印测试开始信息
        System.out.println("🚀 开始测试 Zhipu 真实 API...");

        // 2. 创建配置对象
        EmbAiProperties properties = new EmbAiProperties();
        EmbAiProperties.ZhipuConfig config1 = new EmbAiProperties.ZhipuConfig();

        // 3. 设置配置（用你自己的 API Key）
        String apiKey = config1.getApiKey();

        properties.setZhipu(config1);

        // 4. 打印配置信息
        System.out.println("📋 配置信息：");
        System.out.println("  API Key 前10位: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
        System.out.println("  Base URL: " + config1.getBaseUrl());
        System.out.println("  Model: " + config1.getModel());

        // 5. 创建客户端
        ZhipuClient client = new ZhipuClient(properties);

        // 6. 测试获取提供商（这个方法不需要 API 调用）
        System.out.println("🔄 测试 getProvider()...");
        String provider = client.getProvider();
        assertEquals("zhipu", provider);
        System.out.println("  ✅ Provider: " + provider);

        // 7. 测试真实 API 调用
        try {
            System.out.println("📡 正在调用 Zhipu API...");

            float[][] response = client.chat(chunks);

            System.out.println("📨 API 响应：");
            System.out.println(" 完整向量数据：");
            for (int i = 0; i < response.length; i++) {
                System.out.print("  向量 " + i + ": [");
                if (response[i] != null) {
                    for (int j = 0; j < Math.min(5, response[i].length); j++) { // 只打印前5个值
                        System.out.print(response[i][j] + ", ");
                    }
                    if (response[i].length > 5) {
                        System.out.print("... (" + response[i].length + " 维)");
                    }
                }
                System.out.println("]");
            }

            // 基本断言
            assertNotNull(response, "API 响应不能为 null");
            assertTrue(response.length > 0, "API 响应应有内容");


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

}