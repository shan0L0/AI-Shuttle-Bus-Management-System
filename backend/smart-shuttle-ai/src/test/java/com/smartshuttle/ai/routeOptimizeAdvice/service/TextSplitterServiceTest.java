package com.smartshuttle.ai.routeOptimizeAdvice.service;

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
 * TextSplitterService 测试类
 */
class TextSplitterServiceTest {

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

    // ------------------ 核心功能测试 ------------------

    @Test
    void testSplitFromFile_WithMixedStrategy() throws Exception {
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

        // 执行测试
        String[] chunks = textSplitterService.splitFromFile(filePath, config);
        System.out.println(Arrays.toString(chunks));


        // 验证结果
        assertNotNull(chunks, "返回的数组不能为 null");
        assertTrue(chunks.length > 0, "应该返回至少一个分块");

        // 验证每个分块
        for (int i = 0; i < chunks.length; i++) {
            assertNotNull(chunks[i], "分块 " + i + " 不能为 null");
            assertFalse(chunks[i].isEmpty(), "分块 " + i + " 不能为空");
            assertTrue(chunks[i].length() >= config.getMinChars(),
                    "分块 " + i + " 长度应 >= " + config.getMinChars());
            assertTrue(chunks[i].length() <= config.getMaxChars(),
                    "分块 " + i + " 长度应 <= " + config.getMaxChars());
        }

        // 统计验证
        TextSplitterService.ChunkStats stats = textSplitterService.analyzeChunks(chunks);
        System.out.printf("测试结果: %d 个分块, 平均长度: %d%n",
                stats.getChunkCount(), stats.getAvgChars());
    }

    @Test
    void testSplitFromFile_WithSentenceStrategy() throws Exception {
        String content = "第一条句子。第二条句子？第三条句子！第四条句子。";
        String filePath = createTestFile(content);

        TextSplitterService.ChunkConfig config = new TextSplitterService.ChunkConfig(
                TextSplitterService.SplitStrategy.BY_SENTENCE, 50, 5, 0, true, true
        );

        String[] chunks = textSplitterService.splitFromFile(filePath, config);

        // 应该分成4个句子
        assertEquals(4, chunks.length, "应该分成4个句子");
        assertTrue(chunks[0].contains("第一条句子"));
        assertTrue(chunks[1].contains("第二条句子"));
    }

    @Test
    void testSplitFromFile_WithParagraphStrategy() throws Exception {
        String content = """
            第一段落内容。
            
            第二段落内容较长，包含更多信息。
            
            第三段落。
            """;

        String filePath = createTestFile(content);
        TextSplitterService.ChunkConfig config = new TextSplitterService.ChunkConfig(
                TextSplitterService.SplitStrategy.BY_PARAGRAPH, 200, 1, 0, true, true
        );

        String[] chunks = textSplitterService.splitFromFile(filePath, config);

        assertEquals(3, chunks.length, "应该分成3个段落");
        assertEquals("第一段落内容。", chunks[0].trim());
    }

    @Test
    void testSplitFromFile_WithCharCountStrategy() throws Exception {
        String content = "这是一段较长的文本内容，需要按字符数进行分割。";
        String filePath = createTestFile(content);

        TextSplitterService.ChunkConfig config = new TextSplitterService.ChunkConfig(
                TextSplitterService.SplitStrategy.BY_CHAR_COUNT, 10, 5, 2, true, true
        );

        String[] chunks = textSplitterService.splitFromFile(filePath, config);

        // 验证分块数量（考虑重叠）
        assertTrue(chunks.length >= 4, "应该至少有4个分块");

        // 验证每个分块长度
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 10, "每个分块长度应 <= 10");
            assertTrue(chunk.length() >= 5, "每个分块长度应 >= 5");
        }
    }

    // ------------------ 边界条件测试 ------------------

    @Test
    void testSplitFromFile_EmptyFile() throws Exception {
        String filePath = createTestFile("");
        TextSplitterService.ChunkConfig config = createTestConfig(
                TextSplitterService.SplitStrategy.MIXED
        );

        String[] chunks = textSplitterService.splitFromFile(filePath, config);

        assertNotNull(chunks, "返回的数组不能为 null");
        assertEquals(0, chunks.length, "空文件应该返回空数组");
    }

    @Test
    void testSplitFromFile_OnlyWhitespace() throws Exception {
        String filePath = createTestFile("   \n\n  \t  \n  ");
        TextSplitterService.ChunkConfig config = createTestConfig(
                TextSplitterService.SplitStrategy.MIXED
        );

        String[] chunks = textSplitterService.splitFromFile(filePath, config);

        assertEquals(0, chunks.length, "纯空白文件应该返回空数组");
    }

    @Test
    void testSplitFromFile_TextBelowMinChars() throws Exception {
        String filePath = createTestFile("短文本");
        TextSplitterService.ChunkConfig config = new TextSplitterService.ChunkConfig(
                TextSplitterService.SplitStrategy.MIXED, 100, 10, 0, true, true
        );

        String[] chunks = textSplitterService.splitFromFile(filePath, config);

        assertEquals(0, chunks.length, "文本长度小于最小值时应返回空数组");
    }

    @Test
    void testSplitFromFile_VeryLongParagraph() throws Exception {
        // 创建超长段落（超过 maxChars）
        StringBuilder longText = new StringBuilder("长段落开始。");
        for (int i = 0; i < 100; i++) {
            longText.append("这是第").append(i).append("句。");
        }

        String filePath = createTestFile(longText.toString());
        TextSplitterService.ChunkConfig config = new TextSplitterService.ChunkConfig(
                TextSplitterService.SplitStrategy.MIXED, 200, 20, 20, true, true
        );

        String[] chunks = textSplitterService.splitFromFile(filePath, config);

        assertTrue(chunks.length > 1, "超长段落应该被分割成多个块");
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 200, "每个分块长度应 <= 200");
        }
    }

    @Test
    void testSplitFromFile_FileNotExists() {
        String nonExistentPath = "/tmp/nonexistent/file.txt";
        TextSplitterService.ChunkConfig config = createTestConfig(
                TextSplitterService.SplitStrategy.MIXED
        );

        String[] chunks = textSplitterService.splitFromFile(nonExistentPath, config);

        assertNotNull(chunks, "即使文件不存在也不应返回 null");
        assertEquals(0, chunks.length, "文件不存在时应返回空数组");
    }

    // ------------------ 配置测试 ------------------

    @Test
    void testSplitFromFile_DifferentConfigs() throws Exception {
        String content = "句子一。句子二。句子三。";
        String filePath = createTestFile(content);

        // 测试1：不修剪空白
        TextSplitterService.ChunkConfig config1 = new TextSplitterService.ChunkConfig(
                TextSplitterService.SplitStrategy.BY_SENTENCE, 100, 5, 0, false, true
        );
        String[] chunks1 = textSplitterService.splitFromFile(filePath, config1);

        // 测试2：不移除空块
        TextSplitterService.ChunkConfig config2 = new TextSplitterService.ChunkConfig(
                TextSplitterService.SplitStrategy.BY_SENTENCE, 100, 5, 0, true, false
        );
        String[] chunks2 = textSplitterService.splitFromFile("   ", config2);

        // 验证配置生效
        assertTrue(chunks1.length > 0);
    }

    // ------------------ 性能测试 ------------------

    @Test
    void testSplitFromFile_Performance() throws Exception {
        // 创建大文件测试性能
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("段落").append(i).append(": 这是测试内容。\n\n");
        }

        String filePath = createTestFile(largeContent.toString());
        TextSplitterService.ChunkConfig config = new TextSplitterService.ChunkConfig(
                TextSplitterService.SplitStrategy.MIXED, 500, 20, 50, true, true
        );

        long startTime = System.currentTimeMillis();
        String[] chunks = textSplitterService.splitFromFile(filePath, config);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        System.out.printf("处理 1000 个段落耗时: %d ms, 生成 %d 个分块%n",
                duration, chunks.length);

        // 性能断言（可根据实际情况调整）
        assertTrue(duration < 5000, "处理 1000 个段落应在 5 秒内完成");
        assertTrue(chunks.length > 0, "应该生成分块");
    }

    // ------------------ 辅助测试 ------------------

    @Test
    void testAnalyzeChunks() {
        String[] testChunks = {
                "第一个分块内容",
                "第二个分块稍微长一些",
                "短",
                "这个分块长度中等"
        };

        TextSplitterService.ChunkStats stats = textSplitterService.analyzeChunks(testChunks);

        assertEquals(4, stats.getChunkCount());
        assertTrue(stats.getAvgChars() > 0);
        assertEquals(1, stats.getMinChars());  // "短" 的长度
        assertEquals(9, stats.getMaxChars());  // "第二个分块稍微长一些" 的长度
    }

    @Test
    void testAnalyzeChunks_EmptyArray() {
        String[] emptyChunks = {};
        TextSplitterService.ChunkStats stats = textSplitterService.analyzeChunks(emptyChunks);

        assertEquals(0, stats.getChunkCount());
        assertEquals(0, stats.getAvgChars());
        assertEquals(0, stats.getMinChars());
        assertEquals(0, stats.getMaxChars());
    }

    @Test
    void testAnalyzeChunks_NullInput() {
        TextSplitterService.ChunkStats stats = textSplitterService.analyzeChunks(null);

        assertEquals(0, stats.getChunkCount());
        assertEquals(0, stats.getAvgChars());
        assertEquals(0, stats.getMinChars());
        assertEquals(0, stats.getMaxChars());
    }
}