package com.smartshuttle.business.route.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartshuttle.business.route.entity.Route;
import com.smartshuttle.business.route.vo.RouteVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 线路Mapper
 */
@Mapper
public interface RouteMapper extends BaseMapper<Route> {
    
    /**
     * 分页查询线路（含车辆信息）
     */
    IPage<RouteVO> selectRoutePage(Page<RouteVO> page, @Param("name") String name, @Param("status") Integer status);
    
    /**
     * 查询线路详情（含车辆信息）
     */
    RouteVO selectRouteById(@Param("id") Long id);
    
    /**
     * 查询低乘坐率线路
     */
    @Select("SELECT r.*, v.plate_number as vehiclePlate " +
            "FROM biz_route r " +
            "LEFT JOIN biz_vehicle v ON r.vehicle_id = v.id " +
            "WHERE r.status = 1 AND r.deleted = 0 AND r.occupancy_rate < #{threshold} " +
            "ORDER BY r.occupancy_rate ASC")
    List<RouteVO> selectLowOccupancyRoutes(@Param("threshold") BigDecimal threshold);
    
    /**
     * 线路统计
     */
    @Select("SELECT COUNT(*) as total, " +
            "SUM(total_passenger) as totalPassengers, " +
            "SUM(capacity) as totalCapacity, " +
            "AVG(occupancy_rate) as avgOccupancyRate " +
            "FROM biz_route WHERE status = 1 AND deleted = 0")
    Map<String, Object> selectRouteStats();
    
    /**
     * 获取线路地图数据
     */
    @Select("SELECT r.id, r.name, r.color, r.occupancy_rate as occupancyRate, " +
            "r.total_passenger as passengers, r.capacity " +
            "FROM biz_route r WHERE r.status = 1 AND r.deleted = 0")
    List<Map<String, Object>> selectRouteMapData();
}
