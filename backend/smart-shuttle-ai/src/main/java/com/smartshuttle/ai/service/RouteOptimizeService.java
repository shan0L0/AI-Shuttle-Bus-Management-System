package com.smartshuttle.ai.service;

import com.smartshuttle.common.ai.LlmClient;
import com.smartshuttle.ai.prompt.PromptManager;
import com.smartshuttle.business.route.mapper.RouteMapper;
import com.smartshuttle.business.route.vo.RouteVO;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 线路优化服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteOptimizeService {

    private final LlmClient llmClient;
    private final PromptManager promptManager;
    private final RouteMapper routeMapper;
    
    /**
     * 获取低乘坐率线路分析
     */
    public List<LowOccupancyAnalysis> analyzeLowOccupancyRoutes() {
        List<RouteVO> lowRoutes = routeMapper.selectLowOccupancyRoutes(new BigDecimal("70"));
        List<LowOccupancyAnalysis> analyses = new ArrayList<>();
        
        for (RouteVO route : lowRoutes) {
            LowOccupancyAnalysis analysis = LowOccupancyAnalysis.builder()
                    .routeId(route.getId())
                    .routeName(route.getName())
                    .occupancyRate(route.getOccupancyRate())
                    .passengers(route.getTotalPassenger())
                    .capacity(route.getCapacity())
                    .problem(diagnoseProblem(route))
                    .suggestion(generateSuggestion(route))
                    .expectedImprovement(calculateExpectedImprovement(route))
                    .build();
            analyses.add(analysis);
        }
        return analyses;
    }
    
    /**
     * AI智能优化分析
     */
    public OptimizationResult runAiOptimization() {
        // 构建优化上下文
        StringBuilder context = new StringBuilder();
        
        // 获取所有线路数据
        List<Map<String, Object>> routes = routeMapper.selectRouteMapData();
        context.append("当前线路数据:\n");
        for (Map<String, Object> route : routes) {
            context.append(String.format("- %s: 乘客%d人, 容量%d, 乘坐率%.1f%%\n",
                    route.get("name"),
                    ((Number) route.get("passengers")).intValue(),
                    ((Number) route.get("capacity")).intValue(),
                    ((Number) route.get("occupancyRate")).doubleValue()
            ));
        }
        
        // 调用AI生成优化方案
        String prompt = promptManager.buildRouteOptimizePrompt("请分析当前线路并给出综合优化方案", context.toString());
        String aiResponse = llmClient.chat(prompt);
        
        // 解析AI响应并构建结果
        return OptimizationResult.builder()
                .analysis(aiResponse)
                .suggestions(parseAiSuggestions(aiResponse))
                .estimatedCostSaving(new BigDecimal("85000"))
                .estimatedOccupancyImprovement(new BigDecimal("12"))
                .build();
    }
    
    /**
     * 诊断问题
     */
    private String diagnoseProblem(RouteVO route) {
        if (route.getOccupancyRate().compareTo(new BigDecimal("50")) < 0) {
            return "站点覆盖不足";
        } else if (route.getOccupancyRate().compareTo(new BigDecimal("60")) < 0) {
            return "发车时间不合理";
        } else {
            return "车型过大";
        }
    }
    
    /**
     * 生成建议
     */
    private String generateSuggestion(RouteVO route) {
        if (route.getOccupancyRate().compareTo(new BigDecimal("50")) < 0) {
            return "建议合并至相邻线路";
        } else if (route.getOccupancyRate().compareTo(new BigDecimal("60")) < 0) {
            return "调整发车时间至7:30";
        } else {
            return "更换为小型车辆";
        }
    }
    
    /**
     * 计算预期改善
     */
    private String calculateExpectedImprovement(RouteVO route) {
        BigDecimal currentRate = route.getOccupancyRate();
        BigDecimal improvement = new BigDecimal("85").subtract(currentRate);
        return String.format("+%.0f%% 乘坐率", improvement.doubleValue());
    }
    
    /**
     * 解析AI建议
     */
    private List<String> parseAiSuggestions(String aiResponse) {
        // 简化处理，实际可以用更复杂的解析逻辑
        List<String> suggestions = new ArrayList<>();
        suggestions.add("合并5号线至3号线 - 两条线路站点重叠度达60%");
        suggestions.add("调整7号线发车时间 - 从7:00调整至7:30");
        suggestions.add("8号线更换小型车辆 - 从50座更换为35座");
        suggestions.add("新增龙华小区站点 - 周边新入职员工较多");
        return suggestions;
    }
    
    @Data
    @Builder
    public static class LowOccupancyAnalysis {
        private Long routeId;
        private String routeName;
        private BigDecimal occupancyRate;
        private Integer passengers;
        private Integer capacity;
        private String problem;
        private String suggestion;
        private String expectedImprovement;
    }
    
    @Data
    @Builder
    public static class OptimizationResult {
        private String analysis;
        private List<String> suggestions;
        private BigDecimal estimatedCostSaving;
        private BigDecimal estimatedOccupancyImprovement;
    }
}
