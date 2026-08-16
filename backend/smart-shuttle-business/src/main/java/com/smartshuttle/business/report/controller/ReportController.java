package com.smartshuttle.business.report.controller;

import com.smartshuttle.business.report.service.ReportService;
import com.smartshuttle.business.report.service.ReportService.*;
import com.smartshuttle.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据报表控制器
 */
@Tag(name = "数据报表")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    
    private final ReportService reportService;
    
    @Operation(summary = "生成综合运营报表")
    @GetMapping("/comprehensive")
    public Result<ComprehensiveReport> getComprehensiveReport(
            @RequestParam(defaultValue = "month") String period) {
        ComprehensiveReport report = reportService.generateComprehensiveReport(period);
        return Result.success(report);
    }
    
    @Operation(summary = "生成线路分析报表")
    @GetMapping("/route-analysis")
    public Result<List<RouteAnalysisReport>> getRouteAnalysisReport() {
        List<RouteAnalysisReport> reports = reportService.generateRouteAnalysisReport();
        return Result.success(reports);
    }
    
    @Operation(summary = "获取月度统计数据")
    @GetMapping("/monthly-stats")
    public Result<List<MonthlyStats>> getMonthlyStats(
            @RequestParam(defaultValue = "6") int months) {
        List<MonthlyStats> stats = reportService.getMonthlyStats(months);
        return Result.success(stats);
    }
    
    @Operation(summary = "AI智能报表分析")
    @PostMapping("/ai-analysis")
    public Result<AiReportAnalysis> generateAiReport(
            @RequestParam(defaultValue = "comprehensive") String reportType,
            @RequestParam(defaultValue = "month") String period) {
        AiReportAnalysis analysis = reportService.generateAiReport(reportType, period);
        return Result.success(analysis);
    }
    
    @Operation(summary = "获取乘坐率趋势图数据")
    @GetMapping("/charts/occupancy-trend")
    public Result<List<Map<String, Object>>> getOccupancyTrendData() {
        List<Map<String, Object>> data = reportService.getOccupancyTrendData();
        return Result.success(data);
    }
    
    @Operation(summary = "获取线路对比图数据")
    @GetMapping("/charts/route-compare")
    public Result<List<Map<String, Object>>> getRouteCompareData() {
        List<Map<String, Object>> data = reportService.getRouteCompareData();
        return Result.success(data);
    }
    
    @Operation(summary = "获取成本趋势图数据")
    @GetMapping("/charts/cost-trend")
    public Result<List<Map<String, Object>>> getCostTrendData() {
        List<Map<String, Object>> data = reportService.getCostTrendData();
        return Result.success(data);
    }
}
