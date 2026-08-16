package com.smartshuttle.ai.routeOptimizeAdvice.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文本切片服务
 * 功能：从 txt 文件读取文本，按语义智能分块
 */
@Slf4j
@Service
public class TextSplitterService {

    // ------------------ 配置参数 ------------------
    /**
     * 默认分块策略
     */
    public enum SplitStrategy {
        BY_SENTENCE,      // 按句子分割（句号、问号、叹号）
        BY_PARAGRAPH,     // 按段落分割（空行）
        BY_CHAR_COUNT,    // 按字符数分割
        MIXED             // 混合策略：优先段落，次之句子
    }

    /**
     * 分块配置
     */
    @Data
    @AllArgsConstructor
    public static class ChunkConfig {
        private SplitStrategy strategy;
        private int maxChars;         // 每个块的最大字符数
        private int minChars;          // 每个块的最小字符数
        private int overlapChars;      // 块间重叠字符数
        private boolean trimWhitespace;  // 是否去除空白字符
        private boolean removeEmpty;     // 是否移除空块
    }

    // ------------------ 核心方法 ------------------

    /**
     * 从文件路径读取文本并切片
     * @param filePath 文件路径
     * @param config 分块配置
     * @return 文本块数组
     */
    public String[] splitFromFile(String filePath, ChunkConfig config) {
        try {
            // 1. 读取文件
            String content = readTextFile(filePath);
            if (!StringUtils.hasText(content)) {
                log.warn("文件内容为空: {}", filePath);
                return new String[0];
            }

            // 2. 执行分块
            return splitText(content, config);

        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            return new String[0];
        }
    }

    /**
     * 从文件路径读取文本并切片（使用默认配置）
     */
    public String[] splitFromFile(String filePath) {
        ChunkConfig defaultConfig = new ChunkConfig(
                SplitStrategy.MIXED, 500, 20, 50, true, true
        );
        return splitFromFile(filePath, defaultConfig);
    }

    /**
     * 从文本内容直接切片
     * @param text 原始文本
     * @param config 分块配置
     * @return 文本块数组
     */
    public String[] splitText(String text, ChunkConfig config) {
        if (!StringUtils.hasText(text)) {
            return new String[0];
        }

        // 预处理：去除多余空白
        String processedText = preprocessText(text, config.isTrimWhitespace());

        // 根据策略分块
        List<String> chunks;
        switch (config.getStrategy()) {
            case BY_SENTENCE:
                chunks = splitBySentence(processedText, config);
                break;
            case BY_PARAGRAPH:
                chunks = splitByParagraph(processedText, config);
                break;
            case BY_CHAR_COUNT:
                chunks = splitByCharCount(processedText, config);
                break;
            case MIXED:
            default:
                chunks = splitMixed(processedText, config);
                break;
        }

        // 后处理
        return postprocessChunks(chunks, config);
    }

    // ------------------ 文件读取 ------------------

    /**
     * 读取文本文件
     */
    private String readTextFile(String filePath) throws IOException {
        // 支持 classpath 和绝对路径
        if (filePath.startsWith("classpath:")) {
            String resourcePath = filePath.substring(10);
            try (InputStream is = getClass().getClassLoader()
                    .getResourceAsStream(resourcePath)) {
                if (is == null) {
                    throw new FileNotFoundException("资源文件不存在: " + resourcePath);
                }
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } else {
            return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
        }
    }

    // ------------------ 预处理 ------------------

    /**
     * 文本预处理
     */
    private String preprocessText(String text, boolean trimWhitespace) {
        String processed = text;

        if (trimWhitespace) {
            // 去除首尾空白，合并连续空白
            processed = processed.trim()
                    .replaceAll("\\s+", " ")  // 合并多个空白
                    .replaceAll("\\n\\s*\\n", "\n\n");  // 保留段落分隔
        }

        return processed;
    }

    // ------------------ 分块策略实现 ------------------

    /**
     * 按句子分块
     */
    private List<String> splitBySentence(String text, ChunkConfig config) {
        List<String> chunks = new ArrayList<>();

        // 1. 按句子分割
        String[] sentences = text.split("(?<=[。！？!?\\n])");

        // 2. 直接添加每个句子（过滤空白）
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if(trimmed.length() > config.getMaxChars()){//如果太长需要分割
                chunks.addAll(splitSentence(trimmed, config));
            }
            else if (!trimmed.isEmpty()) {//不长直接加到list
                chunks.add(trimmed);
            }
        }
        return chunks;
    }

    private List<String> splitSentence(String sentence, ChunkConfig config){//递归分割
        List<String> result = new ArrayList<>();

        if (sentence == null) {
            return result;
        }

        String trimmedInput = sentence.trim();
        if (trimmedInput.isEmpty()) {
            return result;
        }

        // 检查是否包含逗号或分号
        boolean hasCommaOrSemicolon = trimmedInput.contains(",") ||
                trimmedInput.contains("，") ||
                trimmedInput.contains(";") ||
                trimmedInput.contains("；");

        if (!hasCommaOrSemicolon) {
            return result;  // 返回空列表
        }

        String[] chunks = trimmedInput.split("[,;，；]");//按照分隔符分割，并且分隔符不加入结果中
        for(int i = 0; i < chunks.length; i++){
            String trimmedChunk = chunks[i].trim();
            if(trimmedChunk.length() <= config.getMaxChars()){
                result.add(trimmedChunk);
            }
            else{
                result.addAll(splitSentence(trimmedChunk,config));
            }
        }
        return result;
    }

    /**
     * 按段落分块
     */
    private List<String> splitByParagraph(String text, ChunkConfig config) {
        return Arrays.stream(text.split("\\n\\s*\\n"))
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .filter(p -> p.length() >= config.getMinChars())
                .collect(Collectors.toList());
    }

    /**
     * 按字符数分块
     */
    private List<String> splitByCharCount(String text, ChunkConfig config) {
        List<String> chunks = new ArrayList<>();
        int overlap = config.getOverlapChars();

        for (int i = 0; i < text.length(); i += config.getMaxChars() - overlap) {
            int end = Math.min(i + config.getMaxChars(), text.length());
            String chunk = text.substring(i, end);

            if (chunk.length() >= config.getMinChars()) {
                chunks.add(chunk);
            }

            if (end == text.length()) break;
        }

        return chunks;
    }

    /**
     * 混合分块策略（推荐）
     */
    private List<String> splitMixed(String text, ChunkConfig config) {
        List<String> chunks = new ArrayList<>();

        // 1. 首先按段落分割
        String[] paragraphs = text.split("\\n\\s*\\n");

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) continue;

            // 2. 如果段落太长，再按句子分割
            if (trimmed.length() <= config.getMaxChars()) {
                if (trimmed.length() >= config.getMinChars()) {
                    chunks.add(trimmed);
                }
            } else {
                // 段落太长，按句子分块
                List<String> sentenceChunks = splitBySentence(trimmed, config);
                chunks.addAll(sentenceChunks);
            }
        }

        return chunks;
    }

    // ------------------ 后处理 ------------------

    /**
     * 分块后处理
     */
    private String[] postprocessChunks(List<String> chunks, ChunkConfig config) {
        return chunks.stream()
                .map(chunk -> config.isTrimWhitespace() ? chunk.trim() : chunk)
                .filter(chunk -> !config.isRemoveEmpty() || !chunk.isEmpty())
                .filter(chunk -> chunk.length() >= config.getMinChars())
                .toArray(String[]::new);
    }

    // ------------------ 工具方法 ------------------

    /**
     * 获取分块统计信息
     */
    public ChunkStats analyzeChunks(String[] chunks) {
        if (chunks == null || chunks.length == 0) {
            return new ChunkStats(0, 0, 0, 0);
        }

        int totalChars = 0;
        int maxChars = 0;
        int minChars = Integer.MAX_VALUE;

        for (String chunk : chunks) {
            int length = chunk.length();
            totalChars += length;
            maxChars = Math.max(maxChars, length);
            minChars = Math.min(minChars, length);
        }

        int avgChars = totalChars / chunks.length;
        return new ChunkStats(chunks.length, avgChars, minChars, maxChars);
    }

    /**
     * 分块统计信息
     */
    @Data
    @AllArgsConstructor
    public static class ChunkStats {
        private int chunkCount;      // 分块数量
        private int avgChars;        // 平均字符数
        private int minChars;        // 最小字符数
        private int maxChars;        // 最大字符数
    }
}