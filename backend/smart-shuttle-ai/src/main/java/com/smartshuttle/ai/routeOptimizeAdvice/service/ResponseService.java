package com.smartshuttle.ai.routeOptimizeAdvice.service;

import com.smartshuttle.ai.routeOptimizeAdvice.entity.postgresqlEntity.KnowledgeVector;
import com.smartshuttle.ai.routeOptimizeAdvice.llmClient.ZhipuClient;
import com.smartshuttle.common.ai.LlmClient;
import com.smartshuttle.ai.routeOptimizeAdvice.mapper.postgresqlMapper.KnowledgeVectorMapper;
import com.smartshuttle.ai.routeOptimizeAdvice.promptManager.OptimizePromptManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 响应服务
 * 接收用户问题 -> 转向量 -> 转string格式向量 -> 查询相关知识 -> 构建prompt -> 调用大模型
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseService {

    private final ZhipuClient zhipuClient;        // Embedding 客户端（智谱）
    private final LlmClient llmclient;            // 大模型客户端（DeepSeek）
    private final KnowledgeVectorMapper knowledgeVectorMapper;
    private final OptimizePromptManager optimizePromptManager;
    private final FloatStringConverter floatStringConverter;

    // 默认查询相关知识的数量
    private static final int DEFAULT_TOP_K = 5;

    // 全局历史消息列表（所有对话共享）
    private final List<OptimizePromptManager.HistoryMessage> conversationHistory = new ArrayList<>();

    // 日期时间格式化器
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     *
     */
    public String processQuestionWithHistory(String userQuestion){//传入conversationHistory
        return processQuestionWithHistory(userQuestion, conversationHistory);
    }

    /**
     * 多轮对话（带历史记录）- 外部传入历史消息，同时更新内部 conversationHistory
     *
     * @param userQuestion 用户问题
     * @param historyMessages 历史对话记录
     * @return AI响应内容
     */
    public String processQuestionWithHistory(String userQuestion, List<OptimizePromptManager.HistoryMessage> historyMessages) {
        log.info("========== 开始处理多轮对话 ==========");
        log.info("用户问题: {}", userQuestion);
        log.info("历史消息数: {}", historyMessages == null ? 0 : historyMessages.size());

        // 1. 将用户消息加入历史
        if (historyMessages != null) {
            historyMessages.add(new OptimizePromptManager.HistoryMessage("user", userQuestion, LocalDateTime.now().format(DATE_TIME_FORMATTER)));
            log.info("添加用户消息到历史，当前历史消息数: {}", historyMessages.size());
        }

        try {
            // 2. 用户问题转向量
            float[][] queryEmbedding = zhipuClient.chat(new String[]{userQuestion});
            if (queryEmbedding == null || queryEmbedding.length == 0) {
                log.error("用户问题向量化失败");
                return "抱歉，处理您的问题时出现了错误，请稍后重试。";
            }

            float[] embedding = queryEmbedding[0];
            String queryVectorStr = floatStringConverter.floatArrayToString(embedding);

            // 3. 查询相关知识库（分别查询 file 和 operation_record 来源）
            log.info("查询 file 来源知识库，TopK={}...", DEFAULT_TOP_K);
            List<KnowledgeVector> fileKnowledge = knowledgeVectorMapper.searchByCosineSimilarityForFile(queryVectorStr, DEFAULT_TOP_K);
            log.info("查询到 {} 条 file 来源相关知识", fileKnowledge.size());

            log.info("查询 operation_record 来源知识库，TopK={}...", DEFAULT_TOP_K);
            List<KnowledgeVector> operationRecord = knowledgeVectorMapper.searchByCosineSimilarityForRecord(queryVectorStr, DEFAULT_TOP_K);
            log.info("查询到 {} 条 operation_record 来源相关知识", operationRecord.size());

            // 4. allKnowledge 只放来源为 file 的知识
            String[] knowledgeArray = fileKnowledge.stream()
                    .map(KnowledgeVector::getContent)
                    .toArray(String[]::new);

            // 5. currentData 改为 operationRecord，存放运营数据
            String[] operationRecordArray = operationRecord.stream()
                    .map(KnowledgeVector::getContent)
                    .toArray(String[]::new);

            // 6. 构建Prompt（使用优化建议方法）
            String prompt = optimizePromptManager.buildOptimizationPromptWithHistory(
                    userQuestion,
                    knowledgeArray,      // 理论知识参考（来自 file）
                    operationRecordArray, // 当前运营数据（来自 operation_record）
                    historyMessages
            );
            System.out.println("prompt:" + prompt);

            // 7. 调用 DeepSeek 大模型
            String response = llmclient.chat(prompt);

            // 8. 将AI回复加入历史
            if (historyMessages != null && response != null && !response.isEmpty()) {
                historyMessages.add(new OptimizePromptManager.HistoryMessage("assistant", response, LocalDateTime.now().format(DATE_TIME_FORMATTER)));
                log.info("添加AI回复到历史，当前历史消息数: {}", historyMessages.size());
            }

            log.info("对话处理完成，响应长度: {}", response != null ? response.length() : 0);
            return response;

        } catch (Exception e) {
            log.error("处理多轮对话失败", e);
            return "抱歉，处理您的问题时出现了系统错误，请稍后重试。";
        }
    }

    /**
     * 处理用户问题（使用内部 conversationHistory 自动管理）
     *
     * @param userQuestion 用户问题
     * @return AI响应内容
     */
    public String processQuestion(String userQuestion) {
        log.info("========== 开始处理用户问题（自动历史） ==========");
        log.info("用户问题: {}", userQuestion);
        log.info("当前历史消息数: {}", conversationHistory.size());

        String response = processQuestionWithHistory(userQuestion, conversationHistory);

        log.info("处理完成，当前历史消息数: {}", conversationHistory.size());
        return response;
    }

    /**
     * 获取当前会话历史
     */
    public List<OptimizePromptManager.HistoryMessage> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }

    /**
     * 清空历史消息
     */
    public void clearConversationHistory() {
        conversationHistory.clear();
        log.info("历史消息已清空");
    }

    // ==================== 历史消息管理方法 ====================

    /**
     * 获取所有历史消息
     *
     * @return 历史消息列表
     */
    public List<OptimizePromptManager.HistoryMessage> getHistory() {
        return new ArrayList<>(conversationHistory);
    }

    /**
     * 获取历史消息数量
     */
    public int getHistorySize() {
        return conversationHistory.size();
    }

    /**
     * 添加历史消息
     *
     * @param role 角色（user/assistant）
     * @param content 消息内容
     */
    public void addHistoryMessage(String role, String content) {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        conversationHistory.add(new OptimizePromptManager.HistoryMessage(role, content, timestamp));
        log.debug("添加历史消息 - 角色: {}, 当前总数: {}", role, conversationHistory.size());
    }

    /**
     * 更新指定索引的历史消息
     *
     * @param index 消息索引（从0开始）
     * @param newContent 新内容
     * @return 是否更新成功
     */
    public boolean updateHistoryMessage(int index, String newContent) {
        if (index >= 0 && index < conversationHistory.size()) {
            OptimizePromptManager.HistoryMessage oldMsg = conversationHistory.get(index);
            String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            OptimizePromptManager.HistoryMessage newMsg = new OptimizePromptManager.HistoryMessage(
                    oldMsg.getRole(),
                    newContent,
                    timestamp + " (已编辑)"
            );
            conversationHistory.set(index, newMsg);
            log.info("更新历史消息 - 索引: {}, 角色: {}, 原内容预览: {}, 新内容预览: {}",
                    index, oldMsg.getRole(),
                    oldMsg.getContent().length() > 50 ? oldMsg.getContent().substring(0, 50) + "..." : oldMsg.getContent(),
                    newContent.length() > 50 ? newContent.substring(0, 50) + "..." : newContent);
            return true;
        }
        log.warn("更新历史消息失败 - 索引越界: {}, 当前总数: {}", index, conversationHistory.size());
        return false;
    }

    /**
     * 更新最后一条消息（通常是AI的最后一条回复）
     *
     * @param newContent 新内容
     * @return 是否更新成功
     */
    public boolean updateLastMessage(String newContent) {
        if (!conversationHistory.isEmpty()) {
            return updateHistoryMessage(conversationHistory.size() - 1, newContent);
        }
        return false;
    }

    /**
     * 删除指定索引的历史消息
     *
     * @param index 消息索引（从0开始）
     * @return 是否删除成功
     */
    public boolean deleteHistoryMessage(int index) {
        if (index >= 0 && index < conversationHistory.size()) {
            OptimizePromptManager.HistoryMessage removed = conversationHistory.remove(index);
            log.info("删除历史消息 - 索引: {}, 角色: {}, 内容预览: {}",
                    index, removed.getRole(),
                    removed.getContent().length() > 50 ? removed.getContent().substring(0, 50) + "..." : removed.getContent());
            return true;
        }
        log.warn("删除历史消息失败 - 索引越界: {}, 当前总数: {}", index, conversationHistory.size());
        return false;
    }

    /**
     * 删除多条历史消息（批量删除）
     *
     * @param indices 要删除的索引列表
     * @return 删除的数量
     */
    public int deleteHistoryMessages(List<Integer> indices) {
        // 从大到小排序，避免索引变化问题
        List<Integer> sortedIndices = indices.stream()
                .sorted((a, b) -> b - a)
                .distinct()
                .collect(Collectors.toList());

        int deletedCount = 0;
        for (int index : sortedIndices) {
            if (deleteHistoryMessage(index)) {
                deletedCount++;
            }
        }
        log.info("批量删除历史消息 - 请求删除: {}, 实际删除: {}", indices.size(), deletedCount);
        return deletedCount;
    }

    /**
     * 删除最后一条消息
     *
     * @return 是否删除成功
     */
    public boolean deleteLastMessage() {
        if (!conversationHistory.isEmpty()) {
            return deleteHistoryMessage(conversationHistory.size() - 1);
        }
        return false;
    }

    /**
     * 删除最后N条消息
     *
     * @param count 要删除的数量
     * @return 实际删除的数量
     */
    public int deleteLastNMessages(int count) {
        int deleted = 0;
        for (int i = 0; i < count && !conversationHistory.isEmpty(); i++) {
            if (deleteLastMessage()) {
                deleted++;
            }
        }
        log.info("删除最后 {} 条消息，实际删除: {} 条", count, deleted);
        return deleted;
    }

    /**
     * 删除所有用户消息（保留AI回复）
     */
    public int deleteAllUserMessages() {
        List<Integer> userIndices = new ArrayList<>();
        for (int i = 0; i < conversationHistory.size(); i++) {
            if ("user".equals(conversationHistory.get(i).getRole())) {
                userIndices.add(i);
            }
        }
        return deleteHistoryMessages(userIndices);
    }

    /**
     * 删除所有AI回复消息（保留用户消息）
     */
    public int deleteAllAssistantMessages() {
        List<Integer> assistantIndices = new ArrayList<>();
        for (int i = 0; i < conversationHistory.size(); i++) {
            if ("assistant".equals(conversationHistory.get(i).getRole())) {
                assistantIndices.add(i);
            }
        }
        return deleteHistoryMessages(assistantIndices);
    }

    /**
     * 清空所有历史消息（开始新会话）
     */
    public void clearHistory() {
        int size = conversationHistory.size();
        conversationHistory.clear();
        log.info("清空所有历史消息，共清除 {} 条", size);
    }

    // ==================== 统计方法 ====================

    /**
     * 获取会话统计信息
     */
    public ConversationStats getConversationStats() {
        long userCount = conversationHistory.stream()
                .filter(msg -> "user".equals(msg.getRole()))
                .count();
        long assistantCount = conversationHistory.stream()
                .filter(msg -> "assistant".equals(msg.getRole()))
                .count();

        return ConversationStats.builder()
                .totalMessages(conversationHistory.size())
                .userMessages((int) userCount)
                .assistantMessages((int) assistantCount)
                .build();
    }

    // ==================== 私有方法 ====================

    /**
     * 搜索相关知识（余弦相似度）
     *
     * @param queryVectorStr 查询向量字符串
     * @param topK 返回数量
     * @return 相关知识列表
     */
    private List<KnowledgeVector> searchRelevantKnowledge(String queryVectorStr, int topK) {
        try {
            List<KnowledgeVector> results = knowledgeVectorMapper.searchByCosineSimilarity(queryVectorStr, topK);
            if (results == null) {
                return List.of();
            }
            return results;
        } catch (Exception e) {
            log.error("查询知识库失败", e);
            return List.of();
        }
    }

    // ==================== 内部类 ====================

    @lombok.Builder
    @lombok.Data
    public static class ConversationStats {
        private int totalMessages;
        private int userMessages;
        private int assistantMessages;
    }
}