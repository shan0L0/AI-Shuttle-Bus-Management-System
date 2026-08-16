package com.smartshuttle.ai.routeOptimizeAdvice.promptManager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Prompt 管理器
 * 负责构建发送给大模型的 Prompt
 */
@Slf4j
@Component
public class OptimizePromptManager {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 系统角色定义（基础角色）
     */
    private static final String SYSTEM_ROLE = "你是一个专业的公交运营优化助手，擅长线路规划、车辆调度、客流分析等工作。";

    /**
     * 构建优化建议Prompt
     *
     * @param userQuestion 用户问题
     * @param knowledgeList 知识数组
     * @param currentData 当前运营数据
     * @return 优化建议 Prompt
     */
    public String buildOptimizationPromptWithHistory(String userQuestion, String[] knowledgeList, String[] currentData, List<HistoryMessage> historyMessages) {
        StringBuilder prompt = new StringBuilder();

        // 3. 历史对话
        if (historyMessages != null && !historyMessages.isEmpty()) {
            prompt.append("【历史对话】\n");
            for (HistoryMessage msg : historyMessages) {
                prompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
            prompt.append("\n");
        }
        prompt.append(SYSTEM_ROLE + "请根据以下信息做出回答。\n");

        prompt.append("【用户需求】\n");
        prompt.append(userQuestion).append("\n\n");

        prompt.append("【当前运营数据】\n");
        if (currentData != null && currentData.length > 0) {
            for (int i = 0; i < currentData.length; i++) {
                prompt.append(i + 1).append(". ").append(currentData[i]).append("\n");
            }
        }

        prompt.append("【理论知识参考】\n");
        if (knowledgeList != null && knowledgeList.length > 0) {
            for (int i = 0; i < knowledgeList.length; i++) {
                prompt.append(i + 1).append(". ").append(knowledgeList[i]).append("\n");
            }
        }
        prompt.append("\n");

        prompt.append("【输出要求】\n");
        prompt.append("1. 分析当前数据中存在的问题\n");
        prompt.append("2. 给出具体的优化方案\n");
        prompt.append("3. 预估优化后的效果\n");
        prompt.append("4. 建议用表格或分点列出\n\n");

        prompt.append("【优化建议】\n");

        return prompt.toString();
    }

    /**
     * 历史消息内部类
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class HistoryMessage {
        private String role;    // "user" 或 "assistant"
        private String content;
        private String timestamp;

        public HistoryMessage(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        }
    }
}