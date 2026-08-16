package com.smartshuttle.ai.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NLSelectHandler {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // SQL注入关键字检测
    private static final Pattern SQL_INJECTION_PATTERN =
            Pattern.compile("(?i)(\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT( +INTO)?|MERGE|SELECT|UPDATE|UNION( +ALL)?)\\b)");

    // 允许的操作
    private static final Set<String> ALLOWED_KEYWORDS =
            Set.of("SELECT", "WITH", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY", "LIMIT");

    /**
     * 执行SQL查询（主方法）
     */
    public NLSelectResult executeQuery(String sql) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("执行SQL: {}", sql);
            // 1. 参数验证
            validateSQL(sql);
            // 2. SQL安全检查
            checkSQLSafety(sql);
            // 3. 添加分页限制
            String safeSQL = addSafetyLimits(sql);
            // 4. 执行查询
            List<Map<String, Object>> rawData = jdbcTemplate.queryForList(safeSQL);
            // 5. 转换结果
            List<Map<String, Object>> formattedData = formatResultForJSON(rawData);

            long endTime = System.currentTimeMillis();

            // 6. 返回结果
            return NLSelectResult.builder()
                    .success(true)
                    .sql(safeSQL)
                    .originalSql(sql)
                    .data(formattedData)
                    .columns(extractColumnMeta(rawData))
                    .meta(QueryMeta.builder()
                            .rowCount(formattedData.size())
                            .executionTime(endTime - startTime)
                            .timestamp(LocalDateTime.now())
                            .hasMore(formattedData.size() >= 1000)  // 判断是否被截断
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("SQL执行失败: {}", sql, e);
            return NLSelectResult.error(e.getMessage(), sql);
        }
    }

    /**
     * 参数验证
     */
    private void validateSQL(String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new IllegalArgumentException("SQL语句不能为空");
        }

        if (sql.length() > 10000) {
            throw new IllegalArgumentException("SQL语句过长");
        }
    }

    /**
     * SQL安全检查
     */
    private void checkSQLSafety(String sql) {
        String upperSQL = sql.toUpperCase().trim();

        // 1. 检查是否以SELECT开头
        if (!upperSQL.startsWith("SELECT")) {
            throw new SecurityException("只允许SELECT查询");
        }

        // 2. 检查危险操作
        String[] dangerous = {"DELETE", "UPDATE", "INSERT", "DROP", "ALTER", "TRUNCATE", "EXEC", "CALL"};
        for (String keyword : dangerous) {
            if (upperSQL.contains(" " + keyword + " ") || upperSQL.contains("\n" + keyword + " ")) {
                throw new SecurityException("检测到危险操作: " + keyword);
            }
        }

        // 3. 检查系统表
        if (upperSQL.contains("INFORMATION_SCHEMA") ||
                upperSQL.contains("SYS.") ||
                upperSQL.contains("MYSQL.")) {
            throw new SecurityException("禁止访问系统表");
        }

        // 4. 检查注释（防止SQL注入）
        if (upperSQL.contains("--") || upperSQL.contains("/*") || upperSQL.contains("*/")) {
            throw new SecurityException("SQL包含注释，可能存在注入风险");
        }
    }

    /**
     * 添加安全限制
     */
    /**
     * 添加安全限制
     */
    private String addSafetyLimits(String sql) {
        // 1. 先去掉末尾的分号（如果有）
        sql = sql.trim();
        boolean endsWithSemicolon = sql.endsWith(";");
        if (endsWithSemicolon) {
            sql = sql.substring(0, sql.length() - 1);
            log.debug("移除SQL末尾的分号");
        }

        String upperSQL = sql.toUpperCase();

        // 2. 添加或修正 LIMIT
        if (!upperSQL.contains(" LIMIT ")) {
            // 没有LIMIT，直接添加
            sql = sql + " LIMIT 1000";
        } else {
            // 有LIMIT，确保不超过最大值
            sql = enforceMaxLimit(sql, 1000);
        }

        // 3. 最后加上分号
        sql = sql + ";";

        return sql;
    }

    /**
     * 确保LIMIT不超过最大值
     */
    private String enforceMaxLimit(String sql, int maxLimit) {
        String upperSQL = sql.toUpperCase();
        int limitIndex = upperSQL.lastIndexOf(" LIMIT ");
        if (limitIndex == -1) return sql;

        String beforeLimit = sql.substring(0, limitIndex + 7);
        String afterLimit = sql.substring(limitIndex + 7);

        // 解析LIMIT值（可能后面有分号，暂时去掉）
        String afterLimitClean = afterLimit;
        if (afterLimitClean.endsWith(";")) {
            afterLimitClean = afterLimitClean.substring(0, afterLimitClean.length() - 1);
        }

        String[] parts = afterLimitClean.trim().split("[,\\s]+");
        if (parts.length > 0) {
            try {
                int limitValue = Integer.parseInt(parts[0].trim());
                if (limitValue > maxLimit) {
                    // 替换为最大值
                    return beforeLimit + maxLimit;
                }
            } catch (NumberFormatException e) {
                // 解析失败，保持原样
            }
        }

        return beforeLimit + afterLimit;
    }

    private boolean hasSubquery(String sql) {
        // 简单的子查询检测
        int selectCount = 0;
        int fromIndex = 0;
        while ((fromIndex = sql.indexOf("SELECT", fromIndex)) != -1) {
            selectCount++;
            fromIndex += 6;
        }
        return selectCount > 1;
    }


    /**
     * 转换结果格式
     */
    private List<Map<String, Object>> formatResultForJSON(List<Map<String, Object>> rawData) {
        return rawData.stream()
                .map(this::convertRow)
                .collect(Collectors.toList());
    }

    private Map<String, Object> convertRow(Map<String, Object> row) {
        Map<String, Object> converted = new LinkedHashMap<>();
        row.forEach((key, value) -> {
            converted.put(key, convertValue(value));
        });
        return converted;
    }

    private Object convertValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate().toString();
        }

        if (value instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) value;
            // 尝试保持精度
            try {
                if (bd.scale() == 0) {
                    return bd.longValueExact();
                } else {
                    return bd.doubleValue();
                }
            } catch (ArithmeticException e) {
                return bd.doubleValue();
            }
        }

        if (value instanceof byte[]) {
            return Base64.getEncoder().encodeToString((byte[]) value);
        }

        return value;
    }

    /**
     * 提取列元数据
     */
    private List<ColumnInfo> extractColumnMeta(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> firstRow = data.get(0);
        List<ColumnInfo> columns = new ArrayList<>();

        firstRow.forEach((key, value) -> {
            ColumnInfo column = ColumnInfo.builder()
                    .name(key)
                    .type(detectColumnType(value))
                    .displayName(convertToDisplayName(key))
                    .build();
            columns.add(column);
        });

        return columns;
    }

    private String detectColumnType(Object value) {
        if (value == null) return "string";
        if (value instanceof Number) return "number";
        if (value instanceof java.util.Date) return "datetime";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof byte[]) return "binary";
        return "string";
    }

    private String convertToDisplayName(String columnName) {
        // user_name -> User Name
        return Arrays.stream(columnName.split("[_\\s]"))
                .map(word -> {
                    if (word.isEmpty()) return "";
                    return Character.toUpperCase(word.charAt(0)) +
                            word.substring(1).toLowerCase();
                })
                .collect(Collectors.joining(" "));
    }

    /**
     * 结果类定义
     */
    @lombok.Data
    @lombok.Builder
    public static class NLSelectResult {
        private boolean success;
        private String message;
        private String sql;
        private String originalSql;
        private List<ColumnInfo> columns;
        private List<Map<String, Object>> data;
        private QueryMeta meta;

        public static NLSelectResult error(String message, String sql) {
            return NLSelectResult.builder()
                    .success(false)
                    .message(message)
                    .sql(sql)
                    .originalSql(sql)
                    .data(Collections.emptyList())
                    .columns(Collections.emptyList())
                    .meta(QueryMeta.builder()
                            .timestamp(LocalDateTime.now())
                            .build())
                    .build();
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class ColumnInfo {
        private String name;
        private String displayName;
        private String type;
        private Integer suggestedWidth;
    }

    @lombok.Data
    @lombok.Builder
    public static class QueryMeta {
        private int rowCount;
        private long executionTime;
        private LocalDateTime timestamp;
        private boolean hasMore;
    }
}