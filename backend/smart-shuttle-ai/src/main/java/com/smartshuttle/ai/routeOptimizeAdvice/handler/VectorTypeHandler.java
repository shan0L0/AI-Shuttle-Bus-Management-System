package com.smartshuttle.ai.routeOptimizeAdvice.handler;

import com.smartshuttle.ai.routeOptimizeAdvice.service.FloatStringConverter;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSQL vector 类型处理器
 * 自动处理 float[] ↔ PostgreSQL vector 的转换
 */
@MappedTypes(float[].class)
@MappedJdbcTypes(JdbcType.OTHER)
public class VectorTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    float[] parameter, JdbcType jdbcType) throws SQLException {
        // Java float[] → PostgreSQL vector 字符串
        String vectorStr = FloatStringConverter.floatArrayToString(parameter);

        // 创建 PGobject 并指定类型为 vector
        PGobject pgObject = new PGobject();
        pgObject.setType("vector");
        pgObject.setValue(vectorStr);

        ps.setObject(i, pgObject);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        // PostgreSQL vector → Java float[]
        return convertToFloatArray(rs.getObject(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return convertToFloatArray(rs.getObject(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return convertToFloatArray(cs.getObject(columnIndex));
    }

    /**
     * 将 PGobject 或 String 转换为 float[]
     */
    private float[] convertToFloatArray(Object obj) {
        if (obj == null) {
            return new float[0];
        }

        String vectorStr = null;

        if (obj instanceof PGobject) {
            // 直接从 PGobject 取值
            vectorStr = ((PGobject) obj).getValue();
        } else if (obj instanceof String) {
            // 如果是字符串（用了 ::text 的情况）
            vectorStr = (String) obj;
        } else {
            // 其他类型，尝试 toString
            vectorStr = obj.toString();
        }

        return FloatStringConverter.stringToFloatArray(vectorStr);
    }
}