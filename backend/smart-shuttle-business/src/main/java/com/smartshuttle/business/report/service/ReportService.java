package com.smartshuttle.business.report.service;

import com.smartshuttle.common.ai.LlmClient;
import com.smartshuttle.business.route.mapper.RouteMapper;
import com.smartshuttle.business.station.mapper.StationMapper;
import com.smartshuttle.business.vehicle.mapper.VehicleMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 报表服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {
    
    private final RouteMapper routeMapper;
    private final StationMapper stationMapper;
    private final VehicleMapper vehicleMapper;
    private final LlmClient llmClient;
    
    /**
     * 生成综合运营报表
     */
    public ComprehensiveReport generateComprehensiveReport(String period) {
        // 根据周期确定日期范围
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = switch (period) {
            case "week" -> endDate.minusWeeks(1);
            case "month" -> endDate.minusMonths(1);
            case "quarter" -> endDate.minusMonths(3);
            case "year" -> endDate.minusYears(1);
            default -> endDate.minusMonths(1);
        };
        
        // 获取统计数据
        Map<String, Object> routeStats = routeMapper.selectRouteStats();
        Map<String, Object> stationStats = stationMapper.selectStats();
        List<Map<String, Object>> vehicleStats = vehicleMapper.countByStatus();
        
        // 计算车辆统计
        int totalVehicles = 0;
        int runningVehicles = 0;
        for (Map<String, Object> stat : vehicleStats) {
            int count = ((Number) stat.get("count")).intValue();
            totalVehicles += count;
            if ((Integer) stat.get("status") == 1) {
                runningVehicles = count;
            }
        }
        
        // 构建报表
        return ComprehensiveReport.builder()
                .period(period)
                .startDate(startDate)
                .endDate(endDate)
                .totalRoutes(((Number) routeStats.get("total")).intValue())
                .totalPassengers(((Number) routeStats.get("totalPassengers")).intValue())
                .totalCapacity(((Number) routeStats.get("totalCapacity")).intValue())
                .avgOccupancyRate(new BigDecimal(((Number) routeStats.get("avgOccupancyRate")).doubleValue())
                        .setScale(2, RoundingMode.HALF_UP))
                .totalStations(((Number) stationStats.get("total")).intValue())
                .totalVehicles(totalVehicles)
                .runningVehicles(runningVehicles)
                .operatingDays(calculateOperatingDays(startDate, endDate))
                .totalTrips(calculateTotalTrips(startDate, endDate))
                .operatingCost(calculateOperatingCost(period))
                .costPerPerson(calculateCostPerPerson(period))
                .build();
    }
    
    /**
     * 生成线路分析报表
     */
    public List<RouteAnalysisReport> generateRouteAnalysisReport() {
        List<Map<String, Object>> routes = routeMapper.selectRouteMapData();
        List<RouteAnalysisReport> reports = new ArrayList<>();
        
        for (Map<String, Object> route : routes) {
            BigDecimal occupancyRate = new BigDecimal(((Number) route.get("occupancyRate")).doubleValue());
            String status = occupancyRate.compareTo(new BigDecimal("80")) >= 0 ? "优秀" :
                    occupancyRate.compareTo(new BigDecimal("60")) >= 0 ? "良好" : "需优化";
            
            reports.add(RouteAnalysisReport.builder()
                    .routeName((String) route.get("name"))
                    .passengers(((Number) route.get("passengers")).intValue())
                    .capacity(((Number) route.get("capacity")).intValue())
                    .occupancyRate(occupancyRate)
                    .status(status)
                    .build());
        }
        
        return reports;
    }
    
    /**
     * 生成月度统计数据
     */
    public List<MonthlyStats> getMonthlyStats(int months) {
        List<MonthlyStats> stats = new ArrayList<>();
        LocalDate now = LocalDate.now();
        
        // 模拟数据（实际项目中从数据库查询）
        Random random = new Random();
        for (int i = 0; i < months; i++) {
            LocalDate month = now.minusMonths(i);
            int days = month.lengthOfMonth() - 8; // 排除周末
            int trips = days * 8 * 2; // 8条线路，每天2班
            int passengers = trips * (35 + random.nextInt(15)); // 每班35-50人
            BigDecimal rate = new BigDecimal(70 + random.nextInt(20)).setScale(1, RoundingMode.HALF_UP);
            BigDecimal cost = new BigDecimal(170000 + random.nextInt(30000));
            BigDecimal perPerson = cost.divide(new BigDecimal(passengers), 1, RoundingMode.HALF_UP);
            
            stats.add(MonthlyStats.builder()
                    .month(month.format(DateTimeFormatter.ofPattern("yyyy年MM月")))
                    .operatingDays(days)
                    .totalTrips(trips)
                    .totalPassengers(passengers)
                    .avgOccupancyRate(rate)
                    .operatingCost(cost)
                    .costPerPerson(perPerson)
                    .build());
        }
        
        return stats;
    }
    
    /**
     * AI智能报表分析
     */
    public AiReportAnalysis generateAiReport(String reportType, String period) {
        // 获取数据上下文
        ComprehensiveReport report = generateComprehensiveReport(period);
        
        String context = String.format("""
                报表类型: %s
                统计周期: %s
                运营线路: %d条
                总乘客人次: %d
                平均乘坐率: %.2f%%
                运营成本: %.2f元
                人均成本: %.2f元
                """,
                reportType, period,
                report.getTotalRoutes(),
                report.getTotalPassengers(),
                report.getAvgOccupancyRate(),
                report.getOperatingCost(),
                report.getCostPerPerson()
        );
        
        String prompt = String.format("""
                作为智能车厂管理系统的数据分析师，请根据以下数据生成专业的分析报告：
                
                %s
                
                请提供：
                1. 核心指标分析
                2. 运营效率评估
                3. 成本控制建议
                4. 优化改进方向
                
                用专业但易懂的语言，适当使用emoji。
                """, context);
        
        String aiAnalysis = llmClient.chat(prompt);
        
        return AiReportAnalysis.builder()
                .reportType(reportType)
                .period(period)
                .analysis(aiAnalysis)
                .generatedAt(new Date())
                .build();
    }
    
    /**
     * 获取图表数据 - 乘坐率趋势
     */
    public List<Map<String, Object>> getOccupancyTrendData() {
        List<Map<String, Object>> data = new ArrayList<>();
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        double[] rates = {78, 82, 85, 79, 88, 45, 42};
        
        for (int i = 0; i < days.length; i++) {
            data.add(Map.of("day", days[i], "rate", rates[i], "target", 85));
        }
        return data;
    }
    
    /**
     * 获取图表数据 - 线路对比
     */
    public List<Map<String, Object>> getRouteCompareData() {
        return routeMapper.selectRouteMapData();
    }
    
    /**
     * 获取图表数据 - 成本趋势
     */
    public List<Map<String, Object>> getCostTrendData() {
        List<Map<String, Object>> data = new ArrayList<>();
        String[] months = {"7月", "8月", "9月", "10月", "11月", "12月"};
        double[] costs = {18.5, 19.2, 18.8, 19.6, 17.8, 18.6};
        double[] perPerson = {18.5, 18.2, 17.8, 17.7, 17.4, 16.1};
        
        for (int i = 0; i < months.length; i++) {
            data.add(Map.of("month", months[i], "cost", costs[i], "perPerson", perPerson[i]));
        }
        return data;
    }
    
    // 辅助方法
    private int calculateOperatingDays(LocalDate start, LocalDate end) {
        return (int) (end.toEpochDay() - start.toEpochDay()) - 8; // 简化计算
    }
    
    private int calculateTotalTrips(LocalDate start, LocalDate end) {
        int days = calculateOperatingDays(start, end);
        return days * 8 * 2; // 8条线路，每天2班
    }
    
    private BigDecimal calculateOperatingCost(String period) {
        return switch (period) {
            case "week" -> new BigDecimal("46500");
            case "month" -> new BigDecimal("186500");
            case "quarter" -> new BigDecimal("558000");
            case "year" -> new BigDecimal("2200000");
            default -> new BigDecimal("186500");
        };
    }
    
    private BigDecimal calculateCostPerPerson(String period) {
        return new BigDecimal("16.1");
    }
    
    @Data
    @Builder
    public static class ComprehensiveReport {
        private String period;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer totalRoutes;
        private Integer totalPassengers;
        private Integer totalCapacity;
        private BigDecimal avgOccupancyRate;
        private Integer totalStations;
        private Integer totalVehicles;
        private Integer runningVehicles;
        private Integer operatingDays;
        private Integer totalTrips;
        private BigDecimal operatingCost;
        private BigDecimal costPerPerson;
    }
    
    @Data
    @Builder
    public static class RouteAnalysisReport {
        private String routeName;
        private Integer passengers;
        private Integer capacity;
        private BigDecimal occupancyRate;
        private String status;
    }
    
    @Data
    @Builder
    public static class MonthlyStats {
        private String month;
        private Integer operatingDays;
        private Integer totalTrips;
        private Integer totalPassengers;
        private BigDecimal avgOccupancyRate;
        private BigDecimal operatingCost;
        private BigDecimal costPerPerson;
    }
    
    @Data
    @Builder
    public static class AiReportAnalysis {
        private String reportType;
        private String period;
        private String analysis;
        private Date generatedAt;
    }
}
