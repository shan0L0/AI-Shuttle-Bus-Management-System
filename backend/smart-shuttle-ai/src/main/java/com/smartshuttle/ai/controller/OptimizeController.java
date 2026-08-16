package com.smartshuttle.ai.controller;

import com.smartshuttle.ai.service.RouteOptimizeService;
import com.smartshuttle.ai.service.RouteOptimizeService.LowOccupancyAnalysis;
import com.smartshuttle.ai.service.RouteOptimizeService.OptimizationResult;
import com.smartshuttle.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 智能优化控制器
 */
@Tag(name = "智能优化")
@RestController
@RequestMapping("/api/v1/optimize")
@RequiredArgsConstructor
public class OptimizeController {
    
    private final RouteOptimizeService routeOptimizeService;
    
    @Operation(summary = "获取低乘坐率线路分析")
    @GetMapping("/low-occupancy")
    public Result<List<LowOccupancyAnalysis>> getLowOccupancyAnalysis() {
        List<LowOccupancyAnalysis> analyses = routeOptimizeService.analyzeLowOccupancyRoutes();
        return Result.success(analyses);
    }
    
    @Operation(summary = "AI智能优化分析")
    @PostMapping("/ai-analyze")
    public Result<OptimizationResult> runAiOptimization() {
        OptimizationResult result = routeOptimizeService.runAiOptimization();
        return Result.success(result);
    }
    
    @Operation(summary = "获取优化统计概览")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getOptimizeStats() {
        Map<String, Object> stats = Map.of(
                "lowOccupancyCount", 3,
                "estimatedAnnualSaving", new BigDecimal("85000"),
                "estimatedOccupancyImprovement", new BigDecimal("12")
        );
        return Result.success(stats);
    }
}
