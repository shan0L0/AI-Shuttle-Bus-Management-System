package com.smartshuttle.business.station.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartshuttle.business.employee.mapper.EmployeeMapper;
import com.smartshuttle.business.station.entity.Station;
import com.smartshuttle.business.station.entity.StationLocation;
import com.smartshuttle.business.station.mapper.StationMapper;
import com.smartshuttle.business.station.mapper.TerminalMapper;
import com.smartshuttle.common.constant.ErrorCode;
import com.smartshuttle.common.exception.BusinessException;
import com.smartshuttle.common.result.PageResult;
import com.smartshuttle.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.smartshuttle.business.station.kmeans_planning.Planner;
import java.util.List;
import java.util.Map;

/**
 * 站点管理控制器
 */
@Tag(name = "站点管理")
@RestController
@RequestMapping("/api/v1/stations")
@RequiredArgsConstructor
public class StationController {
    
    private final StationMapper stationMapper;
    private final TerminalMapper terminalMapper;
    private final Planner planner;
    
    @Operation(summary = "分页查询站点列表")
    @GetMapping
    //@PreAuthorize("hasAuthority('station:list')")
    public Result<PageResult<Station>> getStationPage(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "status", required = false) Integer status) {
        
        Page<Station> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(name)) {
            wrapper.like(Station::getName, name);
        }
        if (StringUtils.hasText(district)) {
            wrapper.eq(Station::getDistrict, district);
        }
        if (status != null) {
            wrapper.eq(Station::getStatus, status);
        }
        wrapper.orderByDesc(Station::getPassengerCount);
        IPage<Station> result = stationMapper.selectPage(page, wrapper);
        return Result.success(PageResult.of(result));
    }
    
    @Operation(summary = "获取站点详情")
    @GetMapping("/{id}")
    //@PreAuthorize("hasAuthority('station:list')")
    public Result<Station> getStationById(@Parameter(description = "id") @PathVariable(name = "id") Long id) {
        Station station = stationMapper.selectById(id);
        if (station == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "站点不存在");
        }
        return Result.success(station);
    }

    @Operation(summary = "获取终点站详情")
    @GetMapping("/terminal/{id}")
    //@PreAuthorize("hasAuthority('station:list')")
    public Result<StationLocation> getTerminalById(@Parameter(description = "id") @PathVariable(name = "id") Long id) {
        StationLocation station = terminalMapper.selectStnLocationById(id).getFirst();
        if (station == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "站点不存在");
        }
        return Result.success(station);
    }
    
    @Operation(summary = "新增站点")
    @PostMapping
    //@PreAuthorize("hasAuthority('station:add')")
    public Result<Long> addStation(@RequestBody Station station) {
        station.setStatus(1); // 默认启用
        if (station.getPassengerCount() == null) {
            station.setPassengerCount(0);
        }
        stationMapper.insert(station);
        return Result.success("新增成功", station.getId());
    }
    
    @Operation(summary = "更新站点")
    @PutMapping("/{id}")
    //@PreAuthorize("hasAuthority('station:edit')")
    public Result<Void> updateStation(
            @Parameter(description = "站点ID") @PathVariable("id") Long id,
            @RequestBody Station station) {
        station.setId(id);
        stationMapper.updateById(station);
        return Result.success("更新成功", null);
    }
    
    @Operation(summary = "删除站点")
    @DeleteMapping("/{id}")
    //@PreAuthorize("hasAuthority('station:delete')")
    public Result<Void> deleteStation(@Parameter(description = "站点ID") @PathVariable("id") Long id) {
        stationMapper.deleteById(id);
        return Result.success("删除成功", null);
    }
    
    @Operation(summary = "获取所有站点（地图显示）")
    @GetMapping("/all")
    public Result<List<Station>> getAllStations() {
        List<Station> stations = stationMapper.selectList(
                new LambdaQueryWrapper<Station>()
                        .eq(Station::getStatus, 1)
                        .orderByDesc(Station::getPassengerCount)
        );
        return Result.success(stations);
    }
    
    @Operation(summary = "获取地图标注数据")
    @GetMapping("/map-data")
    public Result<List<Map<String, Object>>> getMapData() {
        List<Map<String, Object>> mapData = stationMapper.selectMapData();
        return Result.success(mapData);
    }
    
    @Operation(summary = "统计站点数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStationStats() {
        Map<String, Object> stats = stationMapper.selectStats();
        return Result.success(stats);
    }


    @Operation(summary = "规划站点")
    @GetMapping("/planStations")
    public Result<List<Planner.StationCenter>> planStations() {//这里planStation之后返回plan结果到前端，显示在地图上（新地图，不是已有站点分布图）
        List<Planner.StationCenter> planResult = planner.planStations().getStationCenters();
        return Result.success(planResult);
    }
}
