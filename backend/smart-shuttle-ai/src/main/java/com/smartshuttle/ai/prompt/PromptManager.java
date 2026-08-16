package com.smartshuttle.ai.prompt;

import org.springframework.stereotype.Component;

/**
 * Prompt管理器
 */
@Component
public class PromptManager {
    
    /**
     * 系统角色提示词
     */
    private static final String SYSTEM_ROLE = """
            你是一个智能车厂管理系统的AI助手，专门负责企业班车通勤管理。
            你可以帮助用户：
            1. 查询车辆、员工、站点、线路等数据
            2. 分析运营数据，提供优化建议
            3. 根据天气、请假等因素给出调度建议
            4. 生成各类统计报表
            
            回答要求：
            你的输出必须像这样：
            SELECT id, name FROM users WHERE age > 20 LIMIT 100;
            """;
    
    /**
     * 数据库表结构（用于NL2SQL）
     */
    private static final String TABLE_SCHEMA = """
            -- 车辆表 biz_vehicle
            id BIGINT 主键
            plate_number VARCHAR(20) 车牌号
            brand VARCHAR(50) 品牌型号
            seats INT 座位数
            status TINYINT 状态(0待命/1运行/2维修)
            route_id BIGINT 所属线路ID
            driver_name VARCHAR(50) 驾驶员姓名
            
            -- 线路表 biz_route
            id BIGINT 主键
            name VARCHAR(50) 线路名称
            vehicle_id BIGINT 关联车辆ID
            station_count INT 站点数量
            total_passenger INT 总乘客数
            capacity INT 载客容量
            occupancy_rate DECIMAL(5,2) 乘坐率(百分比)
            departure_time TIME 发车时间
            status TINYINT 状态(0停运/1运营)
            
            -- 站点表 biz_station
            id BIGINT 主键
            name VARCHAR(100) 站点名称
            passenger_count INT 乘车人数
            district VARCHAR(50) 所属区域
            longitude DECIMAL(10,6) 经度
            latitude DECIMAL(10,6) 纬度
            status TINYINT 状态(0停用/1启用)
            
            -- 员工表 biz_employee
            id BIGINT 主键
            employee_no VARCHAR(50) 工号
            name VARCHAR(50) 姓名
            department VARCHAR(100) 部门
            station_id BIGINT 乘车站点ID
            route_id BIGINT 所属线路ID
            status TINYINT 状态(0离职/1在职/2请假/3出差)
            """;
    
    /**
     * 构建数据查询Prompt
     */
    public String buildDataQueryPrompt(String userMessage) {
        return String.format("""
        你是一个专业的SQL生成器。请根据数据库结构和用户问题生成SQL。
        
        数据库结构：
        %s
        
        用户问题：%s
        
        必须遵守以下规则：
        1. 只输出SQL语句，不要任何解释
        2. 必须是SELECT查询
        3. 不能包含LIMIT
        4. 语句以分号;结尾
        5. 不要包含```sql```等标记
        
        你的输出必须像这样：
        SELECT id, name FROM users WHERE age > 20 LIMIT 100;
        """, TABLE_SCHEMA, userMessage);
    }
    
    /**
     * 构建调度建议Prompt
     */
    public String buildScheduleAdvicePrompt(String userMessage, String context) {
        return String.format("""
                %s
                
                ## 任务
                根据用户问题和当前数据，给出班车调度建议。
                
                ## 当前数据
                %s
                
                ## 用户问题
                %s
                
                请分析数据并给出具体的调度建议，包括：
                1. 需要调整的线路
                2. 具体调整措施
                3. 预期效果
                
                用emoji使回答更生动。
                """, SYSTEM_ROLE, context, userMessage);
    }
    
    /**
     * 构建线路优化Prompt
     */
    public String buildRouteOptimizePrompt(String userMessage, String context) {
        return String.format("""
                %s
                
                ## 任务
                分析当前线路数据，提供优化建议。
                
                ## 当前线路数据
                %s
                
                ## 优化目标
                1. 提高整体乘坐率至85%%以上
                2. 合理分配车辆资源
                3. 减少运营成本
                4. 确保员工通勤便利
                
                ## 用户问题
                %s
                
                请分析并给出优化方案，包括：
                - 现状分析
                - 具体优化建议（合并/调整/新增线路）
                - 预计效果（乘坐率提升、成本节省）
                
                用结构化的方式呈现，适当使用emoji。
                """, SYSTEM_ROLE, context, userMessage);
    }
    
    /**
     * 构建报表Prompt
     */
    public String buildReportPrompt(String userMessage, String context) {
        return String.format("""
                %s
                
                ## 任务
                根据数据生成运营分析报表内容。
                
                ## 统计数据
                %s
                
                ## 用户需求
                %s
                
                请生成报表分析内容，包括：
                1. 核心指标摘要
                2. 数据分析与解读
                3. 趋势判断
                4. 改进建议
                
                用专业但易懂的语言，数据要准确。
                """, SYSTEM_ROLE, context, userMessage);
    }
    
    /**
     * 构建通用对话Prompt
     */
    public String buildGeneralChatPrompt(String userMessage) {
        return String.format("""
                %s
                
                ## 用户消息
                %s
                
                请友好地回应用户，如果问题涉及班车管理，给出专业建议。
                如果问题超出能力范围，礼貌说明并引导用户提出相关问题。
                """, SYSTEM_ROLE, userMessage);
    }
}
