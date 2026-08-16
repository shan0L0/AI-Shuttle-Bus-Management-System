package com.smartshuttle.ai.routeOptimizeAdvice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件知识构建器
 * 功能：从 knowledgeResource 目录读取 txt 文件，使用 TextSplitterService 进行文本切割
 */
@Slf4j
@Service
public class FileKnowledgeBuilder {

    @Autowired
    private TextSplitterService textSplitterService;

    @Value("${knowledge.resource.path:knowledgeResource}")//在admin模块的yml配置中
    private String knowledgeResourcePath;

    // 默认分块配置
    private static final TextSplitterService.ChunkConfig DEFAULT_CONFIG =
            new TextSplitterService.ChunkConfig(
                    TextSplitterService.SplitStrategy.MIXED,
                    500, 20, 50, true, true
            );

    /**
     * 功能一：从 knowledgeResource 目录下读取所有 txt 文件，构建切割后的 String 数组
     */
    public String[] buildAllTxtKnowledge() {
        return buildAllTxtKnowledge(DEFAULT_CONFIG);
    }

    /**
     * 功能一重载：使用自定义配置
     */
    public String[] buildAllTxtKnowledge(TextSplitterService.ChunkConfig config) {
        log.info("开始从 knowledgeResource 目录读取所有 txt 文件...");

        Path resourceDir = Paths.get(knowledgeResourcePath);

        if (!Files.exists(resourceDir) || !Files.isDirectory(resourceDir)) {
            log.error("knowledgeResource 目录不存在: {}", knowledgeResourcePath);
            return new String[0];
        }

        try {
            // 递归获取所有 txt 文件
            List<Path> txtFiles;
            try (Stream<Path> walk = Files.walk(resourceDir)) {
                txtFiles = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".txt"))
                        .collect(Collectors.toList());
            }

            if (txtFiles.isEmpty()) {
                log.warn("未找到任何 txt 文件，目录: {}", knowledgeResourcePath);
                return new String[0];
            }

            log.info("找到 {} 个 txt 文件", txtFiles.size());

            List<String> allChunks = new ArrayList<>();

            for (Path filePath : txtFiles) {
                String fileName = filePath.getFileName().toString();
                String absolutePath = filePath.toAbsolutePath().toString();

                log.info("正在处理文件: {}", absolutePath);

                // 直接使用绝对路径调用 splitFromFile
                String[] chunks = textSplitterService.splitFromFile(absolutePath, config);

                if (chunks != null && chunks.length > 0) {
                    Collections.addAll(allChunks, chunks);
                    log.info("文件 {} 切割为 {} 个文本块", fileName, chunks.length);
                }
            }

            log.info("所有文件处理完成，共生成 {} 个知识片段", allChunks.size());
            return allChunks.toArray(new String[0]);

        } catch (IOException e) {
            log.error("读取 knowledgeResource 目录失败", e);
            return new String[0];
        }
    }

    /**
     * 功能二：按照传入的文件名读取 knowledgeResource 下的相应文件
     */
    public String[] buildByFileName(String fileName) {
        return buildByFileName(fileName, DEFAULT_CONFIG);
    }

    /**
     * 功能二重载：使用自定义配置
     */
    public String[] buildByFileName(String fileName, TextSplitterService.ChunkConfig config) {
        log.info("开始读取文件: {}", fileName);

        if (fileName == null || fileName.trim().isEmpty()) {
            log.error("文件名为空");
            return new String[0];
        }

        // 构建文件路径
        Path filePath = Paths.get(knowledgeResourcePath, fileName);

        // 如果文件名没有 .txt 后缀，尝试添加
        if (!Files.exists(filePath) && !fileName.endsWith(".txt")) {
            filePath = Paths.get(knowledgeResourcePath, fileName + ".txt");
        }

        if (!Files.exists(filePath)) {
            log.error("文件不存在: {}", filePath.toAbsolutePath());
            return new String[0];
        }

        if (Files.isDirectory(filePath)) {
            log.error("路径是目录而不是文件: {}", filePath.toAbsolutePath());
            return new String[0];
        }

        log.info("找到文件: {}", filePath.toAbsolutePath());

        // 直接使用绝对路径调用 splitFromFile
        String[] chunks = textSplitterService.splitFromFile(filePath.toAbsolutePath().toString(), config);

        if (chunks == null || chunks.length == 0) {
            log.warn("文件切割后无内容: {}", fileName);
            return new String[0];
        }

        // 为每个片段添加来源信息
        String[] result = new String[chunks.length];
        for (int i = 0; i < chunks.length; i++) {
            result[i] = buildChunkWithSource(filePath.getFileName().toString(), chunks[i]);
        }

        log.info("文件 {} 切割为 {} 个文本块", fileName, result.length);

        // 输出分块统计信息
        TextSplitterService.ChunkStats stats = textSplitterService.analyzeChunks(result);
        log.debug("分块统计 - 数量: {}, 平均字符数: {}, 最小: {}, 最大: {}",
                stats.getChunkCount(), stats.getAvgChars(), stats.getMinChars(), stats.getMaxChars());

        return result;
    }

    /**
     * 批量读取多个指定文件
     */
    public String[] buildByFileNames(String[] fileNames) {
        return buildByFileNames(fileNames, DEFAULT_CONFIG);
    }

    /**
     * 批量读取多个指定文件（使用自定义配置）
     */
    public String[] buildByFileNames(String[] fileNames, TextSplitterService.ChunkConfig config) {
        if (fileNames == null || fileNames.length == 0) {
            log.warn("文件名为空");
            return new String[0];
        }

        List<String> allChunks = new ArrayList<>();

        for (String fileName : fileNames) {
            String[] chunks = buildByFileName(fileName, config);
            if (chunks != null && chunks.length > 0) {
                for (String chunk : chunks) {
                    allChunks.add(chunk);
                }
            }
        }

        log.info("批量读取完成，共处理 {} 个文件，生成 {} 个知识片段", fileNames.length, allChunks.size());
        return allChunks.toArray(new String[0]);
    }

    /**
     * 为知识片段添加来源信息
     */
    private String buildChunkWithSource(String fileName, String chunk) {
        return String.format("[来源文件: %s]\n%s", fileName, chunk);
    }

    /**
     * 获取 knowledgeResource 目录下所有 txt 文件列表
     */
    public String[] getAllTxtFileNames() {
        log.info("获取所有 txt 文件列表...");

        Path resourceDir = Paths.get(knowledgeResourcePath);

        if (!Files.exists(resourceDir) || !Files.isDirectory(resourceDir)) {
            log.error("knowledgeResource 目录不存在: {}", knowledgeResourcePath);
            return new String[0];
        }

        try (Stream<Path> walk = Files.walk(resourceDir)) {
            List<String> fileNames = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());

            log.info("找到 {} 个 txt 文件", fileNames.size());
            return fileNames.toArray(new String[0]);

        } catch (IOException e) {
            log.error("获取文件列表失败", e);
            return new String[0];
        }
    }

    /**
     * 获取知识库统计信息
     */
    public KnowledgeStats getKnowledgeStats() {
        String[] allChunks = buildAllTxtKnowledge();

        if (allChunks.length == 0) {
            return new KnowledgeStats(0, 0, 0, 0, 0);
        }

        TextSplitterService.ChunkStats stats = textSplitterService.analyzeChunks(allChunks);

        return new KnowledgeStats(
                getAllTxtFileNames().length,
                stats.getChunkCount(),
                stats.getAvgChars(),
                stats.getMinChars(),
                stats.getMaxChars()
        );
    }

    /**
     * 知识库统计信息
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class KnowledgeStats {
        private int fileCount;
        private int chunkCount;
        private int avgChars;
        private int minChars;
        private int maxChars;
    }
}