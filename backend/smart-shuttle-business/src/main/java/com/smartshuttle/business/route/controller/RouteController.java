package com.smartshuttle.business.route.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartshuttle.business.route.entity.Route;
import com.smartshuttle.business.route.entity.StationToGroup;
import com.smartshuttle.business.route.kmeansPlanning.StationGroupPlanner;
import com.smartshuttle.business.route.mapper.RouteMapper;
import com.smartshuttle.business.route.vo.RouteVO;
import com.smartshuttle.business.station.kmeans_planning.Planner;
import com.smartshuttle.common.constant.ErrorCode;
import com.smartshuttle.common.exception.BusinessException;
import com.smartshuttle.common.result.PageResult;
import com.smartshuttle.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 线路管理控制器
 */
@Tag(name = "线路管理")
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {
    
    private final RouteMapper routeMapper;
    private final StationGroupPlanner stationGroupPlanner;
    
    @Operation(summary = "分页查询线路列表")
    @GetMapping
    @PreAuthorize("hasAuthority('route:list')")
    public Result<PageResult<RouteVO>> getRoutePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        
        Page<RouteVO> page = new Page<>(pageNum, pageSize);
        IPage<RouteVO> result = routeMapper.selectRoutePage(page, name, status);
        return Result.success(PageResult.of(result));
    }
    
    @Operation(summary = "获取线路详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('route:list')")
    public Result<RouteVO> getRouteById(@Parameter(description = "线路ID") @PathVariable Long id) {
        RouteVO route = routeMapper.selectRouteById(id);
        if (route == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "线路不存在");
        }
        return Result.success(route);
    }
    
    @Operation(summary = "新增线路")
    @PostMapping
    @PreAuthorize("hasAuthority('route:add')")
    public Result<Long> addRoute(@RequestBody Route route) {
        route.setStatus(1);
        route.setStationCount(0);
        route.setTotalPassenger(0);
        route.setOccupancyRate(BigDecimal.ZERO);
        routeMapper.insert(route);
        return Result.success("新增成功", route.getId());
    }
    
    @Operation(summary = "更新线路")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('route:edit')")
    public Result<Void> updateRoute(
            @Parameter(description = "线路ID") @PathVariable Long id,
            @RequestBody Route route) {
        route.setId(id);
        routeMapper.updateById(route);
        return Result.success("更新成功", null);
    }
    
    @Operation(summary = "删除线路")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('route:delete')")
    public Result<Void> deleteRoute(@Parameter(description = "线路ID") @PathVariable Long id) {
        routeMapper.deleteById(id);
        return Result.success("删除成功", null);
    }
    
    @Operation(summary = "获取所有线路（下拉选择）")
    @GetMapping("/all")
    public Result<List<Route>> getAllRoutes() {
        List<Route> routes = routeMapper.selectList(
                new LambdaQueryWrapper<Route>()
                        .eq(Route::getStatus, 1)
                        .orderByAsc(Route::getName)
        );
        return Result.success(routes);
    }
    
    @Operation(summary = "计算/刷新线路乘坐率")
    @PostMapping("/{id}/calculate-rate")
    @PreAuthorize("hasAuthority('route:edit')")
    public Result<BigDecimal> calculateOccupancyRate(@Parameter(description = "线路ID") @PathVariable Long id) {
        Route route = routeMapper.selectById(id);
        if (route == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "线路不存在");
        }
        
        // 计算乘坐率
        BigDecimal rate = BigDecimal.ZERO;
        if (route.getCapacity() != null && route.getCapacity() > 0 && route.getTotalPassenger() != null) {
            rate = new BigDecimal(route.getTotalPassenger())
                    .divide(new BigDecimal(route.getCapacity()), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        
        route.setOccupancyRate(rate);
        routeMapper.updateById(route);
        
        return Result.success("计算成功", rate);
    }
    
    @Operation(summary = "获取低乘坐率线路（用于优化分析）")
    @GetMapping("/low-occupancy")
    public Result<List<RouteVO>> getLowOccupancyRoutes(
            @RequestParam(defaultValue = "70") BigDecimal threshold) {
        List<RouteVO> routes = routeMapper.selectLowOccupancyRoutes(threshold);
        return Result.success(routes);
    }
    
    @Operation(summary = "线路统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getRouteStats() {
        Map<String, Object> stats = routeMapper.selectRouteStats();
        return Result.success(stats);
    }
    
    @Operation(summary = "获取线路地图数据")
    @GetMapping("/map-data")
    public Result<List<Map<String, Object>>> getRouteMapData() {
        List<Map<String, Object>> mapData = routeMapper.selectRouteMapData();
        return Result.success(mapData);
    }

    @Operation(summary = "规划站点组")
    @GetMapping("/planStationLists")
    public Result<Map<Integer, List<Long>>> planStationLists() {//这里planStation之后返回plan结果到前端，显示在地图上。
        Map<Integer, List<Long>> planResult = stationGroupPlanner.makeGroupMap(stationGroupPlanner.planStationGroups().getStationToGroup());
        return Result.success(planResult);
    }

    @Operation(summary = "获取总组数")
    @GetMapping("/getGroupNum")
    public Result<Integer> getGroupNum() {//这里planStation之后返回plan结果到前端，显示在地图上（新地图，不是已有站点分布图）
        Integer planResult = stationGroupPlanner.planStationGroups().getGroupCount();
        return Result.success(planResult);
    }

}
