package com.smartshuttle.business.vehicle.controller;

import com.smartshuttle.business.vehicle.dto.VehicleDTO;
import com.smartshuttle.business.vehicle.dto.VehicleQueryDTO;
import com.smartshuttle.business.vehicle.service.VehicleService;
import com.smartshuttle.business.vehicle.vo.VehicleVO;
import com.smartshuttle.common.result.PageResult;
import com.smartshuttle.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 车辆管理控制器
 */
@Tag(name = "车辆管理")
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    
    private final VehicleService vehicleService;
    
    @Operation(summary = "分页查询车辆列表")
    @GetMapping
    @PreAuthorize("hasAuthority('vehicle:list')")
    public Result<PageResult<VehicleVO>> getVehiclePage(VehicleQueryDTO query) {
        PageResult<VehicleVO> result = vehicleService.getVehiclePage(query);
        return Result.success(result);
    }
    
    @Operation(summary = "获取车辆详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('vehicle:list')")
    public Result<VehicleVO> getVehicleById(@Parameter(description = "车辆ID") @PathVariable Long id) {
        VehicleVO vehicle = vehicleService.getVehicleById(id);
        return Result.success(vehicle);
    }
    
    @Operation(summary = "新增车辆")
    @PostMapping
    @PreAuthorize("hasAuthority('vehicle:add')")
    public Result<Long> addVehicle(@Valid @RequestBody VehicleDTO dto) {
        Long id = vehicleService.addVehicle(dto);
        return Result.success("新增成功", id);
    }
    
    @Operation(summary = "更新车辆")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('vehicle:edit')")
    public Result<Void> updateVehicle(
            @Parameter(description = "车辆ID") @PathVariable Long id,
            @Valid @RequestBody VehicleDTO dto) {
        dto.setId(id);
        vehicleService.updateVehicle(dto);
        return Result.success("更新成功", null);
    }
    
    @Operation(summary = "删除车辆")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('vehicle:delete')")
    public Result<Void> deleteVehicle(@Parameter(description = "车辆ID") @PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return Result.success("删除成功", null);
    }
    
    @Operation(summary = "批量删除车辆")
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('vehicle:delete')")
    public Result<Void> deleteVehicleBatch(@RequestBody List<Long> ids) {
        vehicleService.deleteVehicleBatch(ids);
        return Result.success("批量删除成功", null);
    }
    
    @Operation(summary = "更新车辆状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('vehicle:edit')")
    public Result<Void> updateVehicleStatus(
            @Parameter(description = "车辆ID") @PathVariable Long id,
            @Parameter(description = "状态：0待命 1运行 2维修") @RequestParam Integer status) {
        vehicleService.updateVehicleStatus(id, status);
        return Result.success("状态更新成功", null);
    }
    
    @Operation(summary = "获取所有车辆（下拉选择）")
    @GetMapping("/all")
    public Result<List<VehicleVO>> getAllVehicles() {
        List<VehicleVO> vehicles = vehicleService.getAllVehicles();
        return Result.success(vehicles);
    }
    
    @Operation(summary = "统计各状态车辆数量")
    @GetMapping("/stats")
    public Result<Map<String, Long>> getVehicleStats() {
        Map<String, Long> stats = vehicleService.countByStatus();
        return Result.success(stats);
    }
}
