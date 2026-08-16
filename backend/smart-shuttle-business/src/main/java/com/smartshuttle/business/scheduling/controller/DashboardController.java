package com.smartshuttle.business.scheduling.controller;

import com.smartshuttle.business.route.mapper.RouteMapper;
import com.smartshuttle.business.station.mapper.StationMapper;
import com.smartshuttle.business.vehicle.mapper.VehicleMapper;
import com.smartshuttle.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据总览控制器
 */
@Tag(name = "数据总览")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final VehicleMapper vehicleMapper;
    private final StationMapper stationMapper;
    private final RouteMapper routeMapper;
    
    @Operation(summary = "获取统计卡片数据")
    @GetMapping("/stats")
    public Result<DashboardStats> getDashboardStats() {
        // 车辆统计
        List<Map<String, Object>> vehicleStats = vehicleMapper.countByStatus();
        int totalVehicles = 0;
        int runningVehicles = 0;
        for (Map<String, Object> stat : vehicleStats) {
            int count = ((Number) stat.get("count")).intValue();
            totalVehicles += count;
            if ((Integer) stat.get("status") == 1) {
                runningVehicles = count;
            }
        }
        
        // 站点统计
        Map<String, Object> stationStats = stationMapper.selectStats();
        int totalStations = ((Number) stationStats.get("total")).intValue();
        int totalEmployees = ((Number) stationStats.get("totalPassengers")).intValue();
        int districts = ((Number) stationStats.get("districts")).intValue();
        
        // 线路统计
        Map<String, Object> routeStats = routeMapper.selectRouteStats();
        int totalRoutes = ((Number) routeStats.get("total")).intValue();
        BigDecimal avgOccupancyRate = new BigDecimal(((Number) routeStats.get("avgOccupancyRate")).doubleValue())
                .setScale(1, RoundingMode.HALF_UP);
        
        return Result.success(DashboardStats.builder()
                .totalVehicles(totalVehicles)
                .runningVehicles(runningVehicles)
                .totalEmployees(totalEmployees)
                .newEmployeesThisMonth(12) // 模拟数据
                .totalStations(totalStations)
                .districts(districts)
                .totalRoutes(totalRoutes)
                .avgOccupancyRate(avgOccupancyRate)
                .costPerPerson(new BigDecimal("15.8"))
                .costChangePercent(new BigDecimal("-8"))
                .build());
    }
    
    @Operation(summary = "获取今日班车状态")
    @GetMapping("/today-status")
    public Result<List<TodayStatus>> getTodayStatus() {
        List<Map<String, Object>> routes = routeMapper.selectRouteMapData();
        List<TodayStatus> statusList = new ArrayList<>();
        
        // 模拟驾驶员和发车时间数据
        String[] drivers = {"张师傅", "李师傅", "王师傅", "赵师傅", "刘师傅", "陈师傅", "周师傅", "吴师傅"};
        String[] plates = {"京A12345", "京A12346", "京A12347", "京A12348", "京A12349", "京A12350", "京A12351", "京A12352"};
        LocalTime[] times = {
                LocalTime.of(7, 0), LocalTime.of(7, 15), LocalTime.of(7, 0),
                LocalTime.of(6, 45), LocalTime.of(7, 30), LocalTime.of(7, 15),
                LocalTime.of(7, 0), LocalTime.of(7, 30)
        };
        
        int i = 0;
        for (Map<String, Object> route : routes) {
            BigDecimal rate = new BigDecimal(((Number) route.get("occupancyRate")).doubleValue());
            statusList.add(TodayStatus.builder()
                    .routeName((String) route.get("name"))
                    .vehiclePlate(plates[i % plates.length])
                    .driverName(drivers[i % drivers.length])
                    .status(i < 7 ? 1 : 0) // 最后一条待发车
                    .passengers(((Number) route.get("passengers")).intValue())
                    .occupancyRate(rate)
                    .departureTime(times[i % times.length])
                    .build());
            i++;
        }
        
        return Result.success(statusList);
    }
    
    @Operation(summary = "获取站点排行数据")
    @GetMapping("/station-ranking")
    public Result<List<Map<String, Object>>> getStationRanking() {
        List<Map<String, Object>> stations = stationMapper.selectMapData();
        // 按乘车人数排序取前8
        stations.sort((a, b) -> ((Number) b.get("passengerCount")).intValue() - ((Number) a.get("passengerCount")).intValue());
        return Result.success(stations.subList(0, Math.min(8, stations.size())));
    }
    
    @Operation(summary = "获取本周乘坐率趋势")
    @GetMapping("/weekly-trend")
    public Result<List<Map<String, Object>>> getWeeklyTrend() {
        List<Map<String, Object>> data = new ArrayList<>();
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        double[] rates = {78, 82, 85, 79, 88, 45, 42};
        
        for (int i = 0; i < days.length; i++) {
            data.add(Map.of(
                    "day", days[i],
                    "rate", rates[i],
                    "target", 85
            ));
        }
        return Result.success(data);
    }
    
    @Data
    @Builder
    public static class DashboardStats {
        private Integer totalVehicles;
        private Integer runningVehicles;
        private Integer totalEmployees;
        private Integer newEmployeesThisMonth;
        private Integer totalStations;
        private Integer districts;
        private Integer totalRoutes;
        private BigDecimal avgOccupancyRate;
        private BigDecimal costPerPerson;
        private BigDecimal costChangePercent;
    }
    
    @Data
    @Builder
    public static class TodayStatus {
        private String routeName;
        private String vehiclePlate;
        private String driverName;
        private Integer status; // 0待发车 1运行中
        private Integer passengers;
        private BigDecimal occupancyRate;
        private LocalTime departureTime;
    }
}
