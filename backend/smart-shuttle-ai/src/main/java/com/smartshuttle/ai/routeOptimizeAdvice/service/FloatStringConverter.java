package com.smartshuttle.ai.routeOptimizeAdvice.service;

import org.springframework.stereotype.Service;

@Service
public class FloatStringConverter {
    /**
     * 用户问题->embedding->float->String->查询
     * float[] → PostgreSQL vector 字符串
     * 例：[0.1, 0.2, 0.3] → "[0.1,0.2,0.3]"
     */
    public static String floatArrayToString(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * PostgreSQL vector 字符串 → float[]
     * 例："[0.1,0.2,0.3]" → [0.1, 0.2, 0.3]
     *
     * @param vectorStr PostgreSQL vector 字符串格式，如 "[0.1,0.2,0.3]"
     * @return float数组
     */
    public static float[] stringToFloatArray(String vectorStr) {
        if (vectorStr == null || vectorStr.trim().isEmpty()) {
            return new float[0];
        }

        String trimmed = vectorStr.trim();

        // 去除两端的方括号
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        // 空数组情况
        if (trimmed.isEmpty()) {
            return new float[0];
        }

        // 按逗号分割
        String[] parts = trimmed.split(",");
        float[] result = new float[parts.length];

        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无法解析向量字符串: " + vectorStr + ", 位置 " + i + " 的值无效: " + parts[i], e);
            }
        }

        return result;
    }
}
