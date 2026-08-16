package com.smartshuttle.business.station.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartshuttle.business.employee.entity.EmployeeLocation;
import com.smartshuttle.business.station.entity.Station;
import com.smartshuttle.business.station.entity.StationLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 站点Mapper
 */
@Mapper
public interface StationMapper extends BaseMapper<Station> {
    
    /**
     * 获取地图标注数据
     */
    @Select("SELECT id, name, longitude as lng, latitude as lat, passenger_count as passengerCount, district " +
            "FROM biz_station WHERE status = 1 AND deleted = 0")
    List<Map<String, Object>> selectMapData();
    
    /**
     * 统计站点数据
     */
    @Select("SELECT COUNT(*) as total, " +
            "SUM(passenger_count) as totalPassengers, " +
            "COUNT(DISTINCT district) as districts " +
            "FROM biz_station WHERE status = 1 AND deleted = 0")
    Map<String, Object> selectStats();

    //统计站点坐标用于规划站点组
    @Select("select id, longitude, latitude " +
            "from biz_station " +
            "where deleted = 0")
    List<StationLocation> selectStnLocations();

    @Select("select id, longitude, latitude " +
            "from biz_station " +
            "where id = #{id}")
    List<StationLocation> selectStnLocationById(@Param("id") Long id);
}
