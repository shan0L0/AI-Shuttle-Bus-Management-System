package com.smartshuttle.ai.routeOptimizeAdvice.entity.postgresqlEntity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartshuttle.ai.routeOptimizeAdvice.handler.VectorTypeHandler;
import com.smartshuttle.ai.routeOptimizeAdvice.service.FloatStringConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 知识向量表实体类
 * 对应 PostgreSQL 表: knowledge_vectors
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName(value = "knowledge_vectors", autoResultMap = true)
public class KnowledgeVector {

    /**
     * 主键ID，使用 PostgreSQL 的 BIGSERIAL
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文本内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 内容哈希（用于去重）
     */
    @TableField(value = "content_hash")
    private String contentHash;

    /**
     * 向量嵌入（1024维）
     * 使用自定义 TypeHandler
     */
    @TableField(value = "embedding", typeHandler = VectorTypeHandler.class)
    private float[] embedding;

    /**
     * 元数据（JSONB格式）
     * 使用 JacksonTypeHandler 自动序列化/反序列化
     */
    @TableField(value = "metadata", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    /**
     * 来源类型
     */
    @TableField(value = "source_type")
    private String sourceType;

    /**
     * 创建时间
     * 数据库默认值：CURRENT_TIMESTAMP
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // ---------- 辅助字段（不存储到数据库）----------

    /**
     * 相似度分数（仅在向量搜索时使用）
     */
    @TableField(exist = false)
    private Float similarity;

    /**
     * 欧几里得距离（L2距离）
     */
    @TableField(exist = false)
    private Float l2Distance;

    /**
     * 内积相似度
     */
    @TableField(exist = false)
    private Float innerProduct;

    // ---------- 构造函数 ----------

    /**
     * 快速创建对象的便捷构造方法
     */
    public static KnowledgeVector of(String content, float[] embedding,
                                     String sourceType) {
        return KnowledgeVector.builder()
                .content(content)
                .embedding(embedding)
                .sourceType(sourceType)
                .build();
    }

    // ---------- 工具方法 ----------

    /**
     * 获取 embedding 的字符串表示（用于 SQL 查询）
     */
    public String getEmbeddingAsString() {
        return FloatStringConverter.floatArrayToString(embedding);
    }


    /**
     * 获取元数据中的特定值
     */
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * 设置元数据值
     */
    public void setMetadataValue(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * 生成内容哈希（MD5）
     */
    public void generateContentHash() {
        if (this.content != null && !this.content.isEmpty()) {
            this.contentHash = generateMD5(this.content);
        }
    }

    private String generateMD5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}