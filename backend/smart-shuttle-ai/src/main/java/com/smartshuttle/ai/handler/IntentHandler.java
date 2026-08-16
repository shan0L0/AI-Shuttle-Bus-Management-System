package com.smartshuttle.ai.handler;

import com.smartshuttle.business.route.mapper.RouteMapper;
import com.smartshuttle.business.station.mapper.StationMapper;
import com.smartshuttle.business.vehicle.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 意图处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentHandler {
    
    private final VehicleMapper vehicleMapper;
    private final StationMapper stationMapper;
    private final RouteMapper routeMapper;
    
    // 意图关键词
    private static final Map<String, List<String>> INTENT_KEYWORDS = Map.of(
            "DATA_QUERY", List.of("哪些", "多少", "查询", "统计", "列表", "有几", "几条", "几个", "是什么", "查一下"),
            "SCHEDULE_ADVICE", List.of("调整", "建议", "天气", "请假", "发车", "加班", "出差", "预测", "安排"),
            "ROUTE_OPTIMIZE", List.of("优化", "规划", "合并", "线路优化", "提高乘坐率", "降低成本", "改进"),
            "REPORT_GENERATE", List.of("报表", "报告", "生成", "导出", "汇总", "分析报告", "统计报表")
    );
    
    /**
     * 识别用户意图
     */
    public String recognizeIntent(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();
        
        for (Map.Entry<String, List<String>> entry : INTENT_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerMessage.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        
        return "GENERAL_CHAT";
    }
    
    /**
     * 构建调度上下文
     */
    public String buildScheduleContext() {
        StringBuilder context = new StringBuilder();
        
        // 车辆统计
        List<Map<String, Object>> vehicleStats = vehicleMapper.countByStatus();
        context.append("## 车辆状态\n");
        for (Map<String, Object> stat : vehicleStats) {
            String status = switch ((Integer) stat.get("status")) {
                case 0 -> "待命";
                case 1 -> "运行中";
                case 2 -> "维修中";
                default -> "未知";
            };
            context.append(String.format("- %s: %d辆\n", status, ((Number) stat.get("count")).intValue()));
        }
        
        // 线路统计
        Map<String, Object> routeStats = routeMapper.selectRouteStats();
        context.append("\n## 线路概况\n");
        context.append(String.format("- 运营线路: %d条\n", ((Number) routeStats.get("total")).intValue()));
        context.append(String.format("- 总乘客: %d人\n", ((Number) routeStats.get("totalPassengers")).intValue()));
        context.append(String.format("- 平均乘坐率: %.1f%%\n", ((Number) routeStats.get("avgOccupancyRate")).doubleValue()));
        
        // 站点统计
        Map<String, Object> stationStats = stationMapper.selectStats();
        context.append("\n## 站点概况\n");
        context.append(String.format("- 启用站点: %d个\n", ((Number) stationStats.get("total")).intValue()));
        context.append(String.format("- 总乘车人数: %d人\n", ((Number) stationStats.get("totalPassengers")).intValue()));
        context.append(String.format("- 覆盖区域: %d个\n", ((Number) stationStats.get("districts")).intValue()));
        
        return context.toString();
    }
    
    /**
     * 构建线路优化上下文
     */
    public String buildRouteOptimizeContext() {
        StringBuilder context = new StringBuilder();
        
        // 获取所有线路数据
        List<Map<String, Object>> routes = routeMapper.selectRouteMapData();
        
        context.append("## 当前线路列表\n");
        context.append("| 线路名称 | 乘客数 | 容量 | 乘坐率 |\n");
        context.append("|---------|-------|------|-------|\n");
        
        for (Map<String, Object> route : routes) {
            context.append(String.format("| %s | %d | %d | %.1f%% |\n",
                    route.get("name"),
                    ((Number) route.get("passengers")).intValue(),
                    ((Number) route.get("capacity")).intValue(),
                    ((Number) route.get("occupancyRate")).doubleValue()
            ));
        }
        
        return context.toString();
    }
    
    /**
     * 构建报表上下文
     */
    public String buildReportContext() {
        StringBuilder context = new StringBuilder();
        
        // 线路统计
        Map<String, Object> routeStats = routeMapper.selectRouteStats();
        context.append("## 运营统计\n");
        context.append(String.format("- 运营线路数: %d\n", ((Number) routeStats.get("total")).intValue()));
        context.append(String.format("- 总乘客人数: %d\n", ((Number) routeStats.get("totalPassengers")).intValue()));
        context.append(String.format("- 总载客容量: %d\n", ((Number) routeStats.get("totalCapacity")).intValue()));
        context.append(String.format("- 平均乘坐率: %.2f%%\n", ((Number) routeStats.get("avgOccupancyRate")).doubleValue()));
        
        // 车辆统计
        List<Map<String, Object>> vehicleStats = vehicleMapper.countByStatus();
        context.append("\n## 车辆统计\n");
        int totalVehicles = 0;
        for (Map<String, Object> stat : vehicleStats) {
            totalVehicles += ((Number) stat.get("count")).intValue();
        }
        context.append(String.format("- 车辆总数: %d\n", totalVehicles));
        
        // 站点统计
        Map<String, Object> stationStats = stationMapper.selectStats();
        context.append("\n## 站点统计\n");
        context.append(String.format("- 站点数量: %d\n", ((Number) stationStats.get("total")).intValue()));
        context.append(String.format("- 覆盖区域: %d\n", ((Number) stationStats.get("districts")).intValue()));
        
        return context.toString();
    }
    
    /**
     * 格式化数据查询结果
     */
    public String formatDataQueryResult(String llmResponse) {
        // 提取SQL执行结果并格式化为用户友好的文本
        // 这里简化处理，实际项目需要解析JSON并执行SQL
        return llmResponse;
    }
}
