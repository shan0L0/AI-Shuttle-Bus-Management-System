package com.smartshuttle.common.ai;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface EmbModelClient {
    /**
     * 获取提供商名称
     */
    String getProvider();

    /**
     * 同步对话
     */
    float[][] chat(String[] text);

    /**
     * 异步对话
     */
    CompletableFuture<String> chatAsync(String prompt);
}
