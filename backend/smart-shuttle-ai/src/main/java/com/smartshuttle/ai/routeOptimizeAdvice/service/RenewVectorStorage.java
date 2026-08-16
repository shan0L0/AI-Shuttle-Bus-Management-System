package com.smartshuttle.ai.routeOptimizeAdvice.service;

import com.smartshuttle.ai.routeOptimizeAdvice.entity.postgresqlEntity.KnowledgeVector;
import com.smartshuttle.ai.routeOptimizeAdvice.llmClient.ZhipuClient;
import com.smartshuttle.ai.routeOptimizeAdvice.mapper.postgresqlMapper.KnowledgeVectorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库重建服务
 * 负责：扫描文件 + 查询MySQL → 文本 → 向量 → 存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RenewVectorStorage {

    private final FileKnowledgeBuilder fileKnowledgeBuilder;
    private final RecordKnowledgeBuilder recordKnowledgeBuilder;
    private final ZhipuClient zhipuClient;
    private final KnowledgeVectorMapper knowledgeVectorMapper;

    // 来源类型常量
    private static final String SOURCE_TYPE_FILE = "file";
    private static final String SOURCE_TYPE_RECORD = "operation_record";

    // ==================== 三个核心功能 ====================

    /**
     * 功能1：覆盖数据库全部数据（清空后重新插入所有知识）
     */
    public void renewAll() {
        log.info("========== 开始重建全部知识向量库 ==========");
        long startTime = System.currentTimeMillis();

        try {
            // 收集所有知识
            List<KnowledgeSource> allKnowledgeSources = collectAllKnowledge();

            if (allKnowledgeSources.isEmpty()) {
                log.warn("未收集到任何知识文本，取消重建操作");
                return;
            }

            log.info("共收集到 {} 条知识文本（文件: {}, 运营记录: {}）",
                    allKnowledgeSources.size(),
                    countBySourceType(allKnowledgeSources, SOURCE_TYPE_FILE),
                    countBySourceType(allKnowledgeSources, SOURCE_TYPE_RECORD));

            // 向量化并存储
            processAndStore(allKnowledgeSources, "all");

            long endTime = System.currentTimeMillis();
            log.info("========== 全部知识向量库重建完成，耗时: {} ms ==========", (endTime - startTime));

        } catch (Exception e) {
            log.error("全部知识向量库重建失败", e);
            throw new RuntimeException("全部知识向量库重建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 功能2：只覆盖数据库中来源为 file 的记录
     * 注意：此操作会删除所有 source_type = 'file' 的记录，然后重新插入文件知识
     */
    public void renewFileOnly() {
        log.info("========== 开始重建文件知识向量库 ==========");
        long startTime = System.currentTimeMillis();

        try {
            // 只收集文件知识
            List<KnowledgeSource> fileSources = collectFileKnowledge();

            if (fileSources.isEmpty()) {
                log.warn("未收集到任何文件知识，取消重建操作");
                return;
            }

            log.info("共收集到 {} 条文件知识", fileSources.size());

            // 删除所有 source_type = 'file' 的旧数据
            log.info("删除旧的文件来源知识数据...");
            int deletedCount = knowledgeVectorMapper.deleteBySourceType(SOURCE_TYPE_FILE);
            log.info("已删除 {} 条旧数据", deletedCount);

            // 向量化并存储
            processAndStore(fileSources, SOURCE_TYPE_FILE);

            long endTime = System.currentTimeMillis();
            log.info("========== 文件知识向量库重建完成，耗时: {} ms ==========", (endTime - startTime));

        } catch (Exception e) {
            log.error("文件知识向量库重建失败", e);
            throw new RuntimeException("文件知识向量库重建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 功能3：只覆盖数据库中来源为 operation_record 的记录
     * 注意：此操作会删除所有 source_type = 'operation_record' 的记录，然后重新插入运营记录知识
     */
    public void renewRecordOnly() {
        log.info("========== 开始重建运营记录知识向量库 ==========");
        long startTime = System.currentTimeMillis();

        try {
            // 只收集运营记录知识
            List<KnowledgeSource> recordSources = collectRecordKnowledge();

            if (recordSources.isEmpty()) {
                log.warn("未收集到任何运营记录知识，取消重建操作");
                return;
            }

            log.info("共收集到 {} 条运营记录知识", recordSources.size());

            // 删除所有 source_type = 'operation_record' 的旧数据
            log.info("删除旧的运营记录来源知识数据...");
            int deletedCount = knowledgeVectorMapper.deleteBySourceType(SOURCE_TYPE_RECORD);
            log.info("已删除 {} 条旧数据", deletedCount);

            // 向量化并存储
            processAndStore(recordSources, SOURCE_TYPE_RECORD);

            long endTime = System.currentTimeMillis();
            log.info("========== 运营记录知识向量库重建完成，耗时: {} ms ==========", (endTime - startTime));

        } catch (Exception e) {
            log.error("运营记录知识向量库重建失败", e);
            throw new RuntimeException("运营记录知识向量库重建失败: " + e.getMessage(), e);
        }
    }

    // ==================== 公共处理方法 ====================

    /**
     * 向量化并存储知识
     */
    private void processAndStore(List<KnowledgeSource> sources, String sourceType) {//向量化->构建知识实体类->存储
        if (sources.isEmpty()) {
            log.warn("知识来源为空，跳过处理");
            return;
        }

        log.info("========== 开始处理知识来源 ==========");
        log.info("总数: {} 条", sources.size());

        // 智谱 API 单次最大支持 64 条
        final int BATCH_SIZE = 64;

        List<KnowledgeVector> allVectors = new ArrayList<>();
        int totalBatches = (sources.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        int currentBatch = 0;

        // 分批向量化
        for (int start = 0; start < sources.size(); start += BATCH_SIZE) {
            currentBatch++;
            int end = Math.min(start + BATCH_SIZE, sources.size());
            List<KnowledgeSource> batchSources = sources.subList(start, end);

            log.info("---------- 批次 {}/{} ----------", currentBatch, totalBatches);
            log.info("处理第 {}-{} 条，共 {} 条", start + 1, end, batchSources.size());

            // 1. 提取当前批次的文本内容
            String[] batchTexts = batchSources.stream()
                    .map(KnowledgeSource::getContent)
                    .toArray(String[]::new);

            // 2. 调用 ZhipuClient 获取 embedding 向量
            log.info("调用智谱 API 向量化...");
            long apiStartTime = System.currentTimeMillis();
            float[][] embeddings = zhipuClient.chat(batchTexts);
            long apiEndTime = System.currentTimeMillis();

            if (embeddings == null || embeddings.length != batchTexts.length) {
                log.error("向量化结果数量不匹配，预期: {}, 实际: {}",
                        batchTexts.length, embeddings == null ? 0 : embeddings.length);
                throw new RuntimeException("向量化处理失败");
            }

            log.info("向量化完成，耗时: {} ms，向量维度: {}",
                    (apiEndTime - apiStartTime), embeddings.length > 0 ? embeddings[0].length : 0);

            // 3. 构建 KnowledgeVector 实体列表
            for (int i = 0; i < batchSources.size(); i++) {
                KnowledgeSource source = batchSources.get(i);
                float[] embedding = embeddings[i];

                KnowledgeVector vector = buildKnowledgeVector(source, embedding);
                allVectors.add(vector);
            }

            log.info("批次 {}/{} 完成，累计 {} 条", currentBatch, totalBatches, allVectors.size());
        }

        log.info("========== 向量化完成 ==========");
        log.info("共构建 {} 条 KnowledgeVector 实体", allVectors.size());

        // 4. 先清空旧数据
        log.info("清空旧数据...");
        int deletedCount = 0;
        if(sourceType == "all") {
            deletedCount = knowledgeVectorMapper.deleteAll();
        }
        else if(sourceType == SOURCE_TYPE_FILE){
            deletedCount = knowledgeVectorMapper.deleteBySourceType(SOURCE_TYPE_FILE);
        }
        else {
            deletedCount = knowledgeVectorMapper.deleteBySourceType(SOURCE_TYPE_RECORD);
        }
        log.info("已删除 {} 条旧数据", deletedCount);

        // 5. 分批插入新数据（避免一次性插入太多导致内存或SQL问题）
        final int INSERT_BATCH_SIZE = 500;
        int totalInserted = 0;
        int insertBatches = (allVectors.size() + INSERT_BATCH_SIZE - 1) / INSERT_BATCH_SIZE;

        log.info("开始分批插入，共 {} 批", insertBatches);

        for (int start = 0; start < allVectors.size(); start += INSERT_BATCH_SIZE) {
            int batchNum = start / INSERT_BATCH_SIZE + 1;
            int end = Math.min(start + INSERT_BATCH_SIZE, allVectors.size());
            List<KnowledgeVector> batchVectors = allVectors.subList(start, end);

            log.info("插入批次 {}/{}: 第 {}-{} 条", batchNum, insertBatches, start + 1, end);
            int insertedCount = knowledgeVectorMapper.batchInsert(batchVectors);
            totalInserted += insertedCount;
        }

        log.info("========== 全部完成 ==========");
        log.info("共插入 {} 条数据", totalInserted);
    }

    // ==================== 知识收集方法 ====================

    /**
     * 收集所有知识来源
     */
    private List<KnowledgeSource> collectAllKnowledge() {
        List<KnowledgeSource> sources = new ArrayList<>();
        sources.addAll(collectFileKnowledge());
        sources.addAll(collectRecordKnowledge());
        return sources;
    }

    /**
     * 收集文件知识
     */
    private List<KnowledgeSource> collectFileKnowledge() {
        List<KnowledgeSource> sources = new ArrayList<>();

        try {
            String[] fileChunks = fileKnowledgeBuilder.buildAllTxtKnowledge();
            if (fileChunks != null && fileChunks.length > 0) {
                for (String chunk : fileChunks) {
                    KnowledgeSource source = KnowledgeSource.builder()
                            .content(chunk)
                            .sourceType(SOURCE_TYPE_FILE)
                            .metadata(extractFileMetadata(chunk))
                            .build();
                    sources.add(source);
                }
                log.info("从文件知识库收集到 {} 条知识", fileChunks.length);
            } else {
                log.warn("文件知识库为空");
            }
        } catch (Exception e) {
            log.error("收集文件知识失败", e);
        }

        return sources;
    }

    /**
     * 收集运营记录知识（全部）
     */
    private List<KnowledgeSource> collectRecordKnowledge() {
        List<KnowledgeSource> sources = new ArrayList<>();

        try {
            String[] recordChunks = recordKnowledgeBuilder.buildAllKnowledge();
            if (recordChunks != null && recordChunks.length > 0) {
                for (String chunk : recordChunks) {
                    KnowledgeSource source = KnowledgeSource.builder()
                            .content(chunk)
                            .sourceType(SOURCE_TYPE_RECORD)
                            .metadata(extractRecordMetadata(chunk))
                            .build();
                    sources.add(source);
                }
                log.info("从运营记录库收集到 {} 条知识", recordChunks.length);
            } else {
                log.warn("运营记录库为空");
                // 尝试使用简洁版
                String[] simpleRecords = recordKnowledgeBuilder.buildSimpleKnowledge();
                if (simpleRecords != null && simpleRecords.length > 0) {
                    for (String chunk : simpleRecords) {
                        KnowledgeSource source = KnowledgeSource.builder()
                                .content(chunk)
                                .sourceType(SOURCE_TYPE_RECORD)
                                .metadata(extractRecordMetadata(chunk))
                                .build();
                        sources.add(source);
                    }
                    log.info("从运营记录库（简洁版）收集到 {} 条知识", simpleRecords.length);
                }
            }
        } catch (Exception e) {
            log.error("收集运营记录知识失败", e);
        }

        return sources;
    }

    /**
     * 收集过去两周的运营记录知识
     */
    private List<KnowledgeSource> collectRecordKnowledgeLastTwoWeeks() {
        List<KnowledgeSource> sources = new ArrayList<>();

        try {
            // 这里需要在 RecordKnowledgeBuilder 中添加一个方法
            // 暂时使用 buildKnowledgeByCondition 并传入时间范围
            // 如果没有这个方法，可以先回退到全部
            log.warn("过去两周运营记录收集功能需要 RecordKnowledgeBuilder 支持，当前使用全部运营记录");
            return collectRecordKnowledge();

            // 当 RecordKnowledgeBuilder 添加了相应方法后，可以这样使用：
            // String[] recordChunks = recordKnowledgeBuilder.buildKnowledgeLastTwoWeeks();
            // ... 处理逻辑
        } catch (Exception e) {
            log.error("收集过去两周运营记录知识失败", e);
        }

        return sources;
    }

    // ==================== 辅助方法 ====================

    /**
     * 统计指定来源类型的数量
     */
    private int countBySourceType(List<KnowledgeSource> sources, String sourceType) {
        return (int) sources.stream()
                .filter(s -> sourceType.equals(s.getSourceType()))
                .count();
    }

    /**
     * 构建 KnowledgeVector 实体
     */
    private KnowledgeVector buildKnowledgeVector(KnowledgeSource source, float[] embedding) {
        KnowledgeVector vector = KnowledgeVector.builder()
                .content(source.getContent())
                .embedding(embedding)
                .sourceType(source.getSourceType())
                .metadata(source.getMetadata())
                .build();

        vector.generateContentHash();
        return vector;
    }

    /**
     * 从文件知识文本中提取元数据
     */
    private Map<String, Object> extractFileMetadata(String chunk) {
        Map<String, Object> metadata = new HashMap<>();

        if (chunk != null && chunk.startsWith("[来源文件: ")) {
            int endIndex = chunk.indexOf("]\n");
            if (endIndex > 0) {
                String fileName = chunk.substring(7, endIndex);
                metadata.put("fileName", fileName);
            }
        }

        metadata.put("chunkLength", chunk != null ? chunk.length() : 0);
        metadata.put("processedAt", System.currentTimeMillis());

        return metadata;
    }

    /**
     * 从运营记录文本中提取元数据
     */
    private Map<String, Object> extractRecordMetadata(String chunk) {
        Map<String, Object> metadata = new HashMap<>();

        if (chunk != null) {
            int idStart = chunk.indexOf("记录ID：");
            if (idStart > 0) {
                int idEnd = chunk.indexOf("\n", idStart);
                if (idEnd > 0) {
                    String recordId = chunk.substring(idStart + 5, idEnd).trim();
                    metadata.put("recordId", recordId);
                }
            }

            int routeStart = chunk.indexOf("路线ID：");
            if (routeStart > 0) {
                int routeEnd = chunk.indexOf("\n", routeStart);
                if (routeEnd > 0) {
                    String routeId = chunk.substring(routeStart + 5, routeEnd).trim();
                    metadata.put("routeId", routeId);
                }
            }
        }

        metadata.put("chunkLength", chunk != null ? chunk.length() : 0);
        metadata.put("processedAt", System.currentTimeMillis());

        return metadata;
    }

    /**
     * 获取当前知识库统计信息（不重建）
     */
    public VectorStats getVectorStats() {
        try {
            List<KnowledgeSource> allSources = collectAllKnowledge();

            int fileCount = (int) allSources.stream()
                    .filter(s -> SOURCE_TYPE_FILE.equals(s.getSourceType()))
                    .count();
            int recordCount = (int) allSources.stream()
                    .filter(s -> SOURCE_TYPE_RECORD.equals(s.getSourceType()))
                    .count();

            int totalChars = allSources.stream()
                    .mapToInt(s -> s.getContent() != null ? s.getContent().length() : 0)
                    .sum();

            return VectorStats.builder()
                    .totalCount(allSources.size())
                    .fileSourceCount(fileCount)
                    .recordSourceCount(recordCount)
                    .totalChars(totalChars)
                    .avgChars(allSources.isEmpty() ? 0 : totalChars / allSources.size())
                    .build();

        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return VectorStats.builder().build();
        }
    }

    /**
     * 知识来源内部类
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class KnowledgeSource {
        private String content;
        private String sourceType;
        private Map<String, Object> metadata;
    }

    /**
     * 向量库统计信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VectorStats {
        private int totalCount;
        private int fileSourceCount;
        private int recordSourceCount;
        private int totalChars;
        private int avgChars;
    }
}