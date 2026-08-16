package com.smartshuttle.ai.routeOptimizeAdvice.mapper.mysqlMapper;

import com.smartshuttle.ai.routeOptimizeAdvice.entity.mysqlEntity.BizVehicle;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface BizVehicleMapper {

    @Select("SELECT * FROM biz_vehicle WHERE id = #{id} AND deleted = 0")
    BizVehicle selectById(@Param("id") Long id);

    @Select("SELECT * FROM biz_vehicle WHERE deleted = 0")
    List<BizVehicle> selectAll();

    @Select("SELECT * FROM biz_vehicle WHERE plate_number = #{plateNumber} AND deleted = 0")
    BizVehicle selectByPlateNumber(@Param("plateNumber") String plateNumber);

    @Insert("INSERT INTO biz_vehicle(plate_number, brand, seats, status, fuel_consumption, " +
            "route_id, driver_name, driver_phone, purchase_date, last_maintenance, " +
            "next_maintenance, mileage, remark, create_by, update_by) " +
            "VALUES(#{plateNumber}, #{brand}, #{seats}, #{status}, #{fuelConsumption}, " +
            "#{routeId}, #{driverName}, #{driverPhone}, #{purchaseDate}, #{lastMaintenance}, " +
            "#{nextMaintenance}, #{mileage}, #{remark}, #{createBy}, #{updateBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizVehicle vehicle);

    @Update("UPDATE biz_vehicle SET plate_number = #{plateNumber}, brand = #{brand}, " +
            "seats = #{seats}, status = #{status}, fuel_consumption = #{fuelConsumption}, " +
            "route_id = #{routeId}, driver_name = #{driverName}, driver_phone = #{driverPhone}, " +
            "purchase_date = #{purchaseDate}, last_maintenance = #{lastMaintenance}, " +
            "next_maintenance = #{nextMaintenance}, mileage = #{mileage}, remark = #{remark}, " +
            "update_by = #{updateBy}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(BizVehicle vehicle);

    @Update("UPDATE biz_vehicle SET deleted = 1, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}