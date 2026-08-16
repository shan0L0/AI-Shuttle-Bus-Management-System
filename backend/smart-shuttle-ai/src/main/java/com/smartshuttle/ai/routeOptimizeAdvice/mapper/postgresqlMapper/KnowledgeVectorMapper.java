package com.smartshuttle.ai.routeOptimizeAdvice.mapper.postgresqlMapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.smartshuttle.ai.routeOptimizeAdvice.entity.postgresqlEntity.KnowledgeVector;
import com.smartshuttle.ai.routeOptimizeAdvice.handler.VectorTypeHandler;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 知识向量 Mapper
 */
@DS("pg")  // 指定使用 PostgreSQL 数据源
@Mapper
public interface KnowledgeVectorMapper extends BaseMapper<KnowledgeVector> {

    /**
     * 余弦相似度搜索（使用 PostgreSQL <=> 操作符）
     * 注意：这里用 ${} 拼接向量字符串，因为 #{} 会对向量字符串加引号
     */
    @Select("SELECT kv.*, 1 - (kv.embedding <=> ${queryVector}::vector) as similarity " +
            "FROM knowledge_vectors kv " +
            "WHERE kv.embedding IS NOT NULL " +
            "ORDER BY kv.embedding <=> ${queryVector}::vector " +
            "LIMIT #{topK}")
    @Results(id = "knowledgeVectorWithSimilarity", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "content", property = "content"),
            @Result(column = "content_hash", property = "contentHash"),
            @Result(column = "embedding", property = "embedding", typeHandler = VectorTypeHandler.class),
            @Result(column = "metadata", property = "metadata", typeHandler = JacksonTypeHandler.class),
            @Result(column = "source_type", property = "sourceType"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "similarity", property = "similarity")
    })
    List<KnowledgeVector> searchByCosineSimilarity(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK
    );

    /**
     * 余弦相似度搜索（只查询 file 来源）
     */
    @Select("SELECT kv.*, 1 - (kv.embedding <=> CAST(#{queryVector} AS vector)) as similarity " +
            "FROM knowledge_vectors kv " +
            "WHERE kv.embedding IS NOT NULL " +
            "AND kv.source_type = 'file' " +
            "ORDER BY kv.embedding <=> CAST(#{queryVector} AS vector) " +
            "LIMIT #{topK}")
    @ResultMap("knowledgeVectorWithSimilarity")
    List<KnowledgeVector> searchByCosineSimilarityForFile(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK
    );

    /**
     * 余弦相似度搜索（只查询 operation_record 来源）
     */
    @Select("SELECT kv.*, 1 - (kv.embedding <=> CAST(#{queryVector} AS vector)) as similarity " +
            "FROM knowledge_vectors kv " +
            "WHERE kv.embedding IS NOT NULL " +
            "AND kv.source_type = 'operation_record' " +
            "ORDER BY kv.embedding <=> CAST(#{queryVector} AS vector) " +
            "LIMIT #{topK}")
    @ResultMap("knowledgeVectorWithSimilarity")
    List<KnowledgeVector> searchByCosineSimilarityForRecord(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK
    );

    /**
     * 根据 content_hash 查询（用于去重）
     */
    @Select("SELECT * FROM knowledge_vectors WHERE content_hash = #{contentHash}")
    @ResultMap("knowledgeVectorWithSimilarity")
    KnowledgeVector selectByContentHash(@Param("contentHash") String contentHash);

    /**
     * 批量插入（使用 MyBatis Plus 的批量插入即可）
     */
    @Insert("<script>" +
            "INSERT INTO knowledge_vectors " +
            "(content, content_hash, embedding, metadata, source_type) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.content}, #{item.contentHash}, " +
            "#{item.embedding,typeHandler=com.smartshuttle.ai.routeOptimizeAdvice.handler.VectorTypeHandler}, " +
            "CAST(#{item.metadata,typeHandler=com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler} AS jsonb), " +
            "#{item.sourceType})" +
            "</foreach>" +
            " ON CONFLICT (content_hash) DO UPDATE SET " +
            "embedding = EXCLUDED.embedding, metadata = EXCLUDED.metadata" +
            "</script>")
    int batchInsert(@Param("list") List<KnowledgeVector> vectors);

    /**
     * 清空知识向量表
     */
    @Delete("DELETE FROM knowledge_vectors")
    int deleteAll();

    /**
     * 根据来源类型删除记录
     */
    @Delete("DELETE FROM knowledge_vectors WHERE source_type = #{sourceType}")
    int deleteBySourceType(@Param("sourceType") String sourceType);
}