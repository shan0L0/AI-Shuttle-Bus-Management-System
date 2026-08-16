package com.smartshuttle.ai.routeOptimizeAdvice.mapper.mysqlMapper;


import com.smartshuttle.ai.routeOptimizeAdvice.entity.mysqlEntity.Schedule;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ScheduleMapper {

    @Select("SELECT * FROM schedule WHERE id = #{id}")
    Schedule selectById(@Param("id") Long id);

    @Select("SELECT * FROM schedule")
    List<Schedule> selectAll();

    @Select("SELECT * FROM schedule WHERE route_id = #{routeId}")
    List<Schedule> selectByRouteId(@Param("routeId") Long routeId);

    @Select("SELECT * FROM schedule WHERE vehicle_id = #{vehicleId}")
    List<Schedule> selectByVehicleId(@Param("vehicleId") Long vehicleId);

    @Insert("INSERT INTO schedule(route_id, planned_departure_time, vehicle_id, planned_arrival_time) " +
            "VALUES(#{routeId}, #{departureTime}, #{vehicleId}, #{arrivalTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Schedule schedule);

    @Update("UPDATE schedule SET route_id = #{routeId}, planned_departure_time = #{departureTime}, " +
            "vehicle_id = #{vehicleId}, planned_arrival_time = #{arrivalTime} WHERE id = #{id}")
    int update(Schedule schedule);

    @Delete("DELETE FROM schedule WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}