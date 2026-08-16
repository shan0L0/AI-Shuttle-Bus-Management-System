package com.smartshuttle.ai.routeOptimizeAdvice.mapper.mysqlMapper;
import com.smartshuttle.ai.routeOptimizeAdvice.entity.mysqlEntity.OperationRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OperationRecordMapper {

    @Select("SELECT * FROM operation_record WHERE id = #{id}")
    OperationRecord selectById(@Param("id") Long id);

    @Select("SELECT * FROM operation_record")
    List<OperationRecord> selectAll();

    /**
     * 查询过去 N 天的运营记录
     * @param days 天数（如：14 表示过去14天）
     */
    @Select("SELECT * FROM operation_record WHERE departure_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    List<OperationRecord> selectLastNDays(@Param("days") int days);

    @Select("SELECT * FROM operation_record WHERE schedule_id = #{scheduleId}")
    List<OperationRecord> selectByScheduleId(@Param("scheduleId") Long scheduleId);

    @Insert("INSERT INTO operation_record(schedule_id, occupation_rate, arrival_time, " +
            "departure_time, one_point_rate, two_point_rate, three_point_rate, " +
            "four_point_rate, five_point_rate) " +
            "VALUES(#{scheduleId}, #{occupationRate}, #{arrivalTime}, #{departureTime}, " +
            "#{onePointRate}, #{twoPointRate}, #{threePointRate}, #{fourPointRate}, #{fivePointRate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationRecord record);

    @Update("UPDATE operation_record SET schedule_id = #{scheduleId}, " +
            "occupation_rate = #{occupationRate}, arrival_time = #{arrivalTime}, " +
            "departure_time = #{departureTime}, one_point_rate = #{onePointRate}, " +
            "two_point_rate = #{twoPointRate}, three_point_rate = #{threePointRate}, " +
            "four_point_rate = #{fourPointRate}, five_point_rate = #{fivePointRate} " +
            "WHERE id = #{id}")
    int update(OperationRecord record);

    @Delete("DELETE FROM operation_record WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}