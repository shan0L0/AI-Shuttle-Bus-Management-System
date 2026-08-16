package com.smartshuttle.common.ai;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * LLM客户端接口
 * 定义在common模块，供其他模块使用
 * 具体实现在ai模块
 */
public interface LlmClient {
    
    /**
     * 获取提供商名称
     */
    String getProvider();
    
    /**
     * 同步对话
     */
    String chat(String prompt);
    
    /**
     * 带历史记录的对话
     */
    String chat(String prompt, List<ChatMessage> history);
    
    /**
     * 异步对话
     */
    CompletableFuture<String> chatAsync(String prompt);
    
    /**
     * 对话消息
     */
    record ChatMessage(String role, String content) {
        public static ChatMessage user(String content) {
            return new ChatMessage("user", content);
        }
        
        public static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content);
        }
        
        public static ChatMessage system(String content) {
            return new ChatMessage("system", content);
        }
    }
}
