package com.smartshuttle.ai.routeOptimizeAdvice.service;

import com.smartshuttle.ai.routeOptimizeAdvice.entity.mysqlEntity.BizVehicle;
import com.smartshuttle.ai.routeOptimizeAdvice.entity.mysqlEntity.OperationRecord;
import com.smartshuttle.ai.routeOptimizeAdvice.entity.mysqlEntity.Schedule;
import com.smartshuttle.ai.routeOptimizeAdvice.mapper.mysqlMapper.BizVehicleMapper;
import com.smartshuttle.ai.routeOptimizeAdvice.mapper.mysqlMapper.OperationRecordMapper;
import com.smartshuttle.ai.routeOptimizeAdvice.mapper.mysqlMapper.ScheduleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/*
运营记录知识构建器，作用为将数据库中的运营记录构建成知识String，用于做embedding
 */

@Service
public class RecordKnowledgeBuilder {

    private static final Logger logger = LoggerFactory.getLogger(RecordKnowledgeBuilder.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private OperationRecordMapper operationRecordMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private BizVehicleMapper bizVehicleMapper;

    /**
     * 构建所有运营记录的知识描述
     * @return String数组，每条记录对应一个描述字符串
     */
    public String[] buildAllKnowledge() {
        logger.info("开始构建运营记录知识库...");
        //根据天数范围查询（N天之内）
        List<OperationRecord> operationRecords = operationRecordMapper.selectLastNDays(14);
        if (operationRecords == null || operationRecords.isEmpty()) {
            logger.warn("未查询到运营记录数据");
            return new String[0];
        }

        logger.info("查询到 {} 条运营记录", operationRecords.size());

        List<String> knowledgeList = new ArrayList<>();

        for (OperationRecord record : operationRecords) {
            String knowledge = buildSingleKnowledge(record);
            if (knowledge != null && !knowledge.isEmpty()) {
                knowledgeList.add(knowledge);
            }
        }

        logger.info("成功构建 {} 条知识描述", knowledgeList.size());
        return knowledgeList.toArray(new String[0]);
    }

    /**
     * 根据条件构建运营记录的知识描述
     * @param scheduleId 班次ID，可为null
     * @param startTime 开始时间，可为null
     * @param endTime 结束时间，可为null
     * @return String数组
     */
    public String[] buildKnowledgeByCondition(Long scheduleId, String startTime, String endTime) {
        logger.info("根据条件构建知识库: scheduleId={}, startTime={}, endTime={}", scheduleId, startTime, endTime);

        // 这里需要根据实际需求在 OperationRecordMapper 中添加条件查询方法
        // 示例：List<OperationRecord> records = operationRecordMapper.selectByCondition(scheduleId, startTime, endTime);
        // 暂时使用全量查询
        List<OperationRecord> operationRecords = operationRecordMapper.selectAll();

        if (operationRecords == null || operationRecords.isEmpty()) {
            return new String[0];
        }

        // 过滤逻辑（可根据需要实现）
        List<OperationRecord> filteredRecords = operationRecords.stream()
                .filter(record -> {
                    if (scheduleId != null && !scheduleId.equals(record.getScheduleId())) {
                        return false;
                    }
                    // 添加更多过滤条件
                    return true;
                })
                .collect(Collectors.toList());

        List<String> knowledgeList = new ArrayList<>();
        for (OperationRecord record : filteredRecords) {
            String knowledge = buildSingleKnowledge(record);
            if (knowledge != null) {
                knowledgeList.add(knowledge);
            }
        }

        return knowledgeList.toArray(new String[0]);
    }

    /**
     * 构建单条运营记录的知识描述
     * @param operationRecord 运营记录
     * @return 格式化的描述字符串
     */
    private String buildSingleKnowledge(OperationRecord operationRecord) {
        if (operationRecord == null || operationRecord.getScheduleId() == null) {
            logger.warn("运营记录或班次ID为空，跳过");
            return null;
        }

        try {
            // 1. 查询 schedule 记录
            Schedule schedule = scheduleMapper.selectById(operationRecord.getScheduleId());
            if (schedule == null) {
                logger.warn("未找到班次记录, scheduleId={}", operationRecord.getScheduleId());
                return null;
            }

            // 2. 查询 vehicle 记录
            BizVehicle vehicle = null;
            if (schedule.getVehicleId() != null) {
                vehicle = bizVehicleMapper.selectById(schedule.getVehicleId());
                if (vehicle == null) {
                    logger.warn("未找到车辆记录, vehicleId={}", schedule.getVehicleId());
                }
            }

            // 3. 构建描述信息
            return buildKnowledgeString(operationRecord, schedule, vehicle);

        } catch (Exception e) {
            logger.error("构建单条知识描述失败, operationRecordId={}", operationRecord.getId(), e);
            return null;
        }
    }

    /**
     * 格式化知识描述字符串
     */
    private String buildKnowledgeString(OperationRecord record, Schedule schedule, BizVehicle vehicle) {
        StringBuilder sb = new StringBuilder();

        // 基础运营信息
        sb.append("【运营记录】\n");
        sb.append("- 记录ID：").append(record.getId()).append("\n");

        // 班次信息
        sb.append("- 班次信息：\n");
        sb.append("  - 班次ID：").append(schedule.getId()).append("\n");
        sb.append("  - 路线ID：").append(schedule.getRouteId()).append("\n");
        if (schedule.getDepartureTime() != null) {
            sb.append("  - 计划发车时间：").append(schedule.getDepartureTime().format(DATE_TIME_FORMATTER)).append("\n");
        }
        sb.append("  - 计划到达时长：").append(schedule.getArrivalTime()).append("分钟\n");

        // 车辆信息
        if (vehicle != null) {
            sb.append("- 车辆信息：\n");
            sb.append("  - 车牌号：").append(vehicle.getPlateNumber()).append("\n");
            sb.append("  - 品牌：").append(vehicle.getBrand()).append("\n");
            sb.append("  - 座位数：").append(vehicle.getSeats()).append("座\n");
            if (vehicle.getDriverName() != null) {
                sb.append("  - 司机：").append(vehicle.getDriverName()).append("\n");
            }
            if (vehicle.getDriverPhone() != null) {
                sb.append("  - 司机电话：").append(vehicle.getDriverPhone()).append("\n");
            }
            if (vehicle.getFuelConsumption() != null) {
                sb.append("  - 油耗：").append(vehicle.getFuelConsumption()).append("L/100km\n");
            }
            sb.append("  - 累计里程：").append(vehicle.getMileage()).append("公里\n");
        } else {
            sb.append("- 车辆信息：无关联车辆\n");
        }

        // 实际运营数据
        sb.append("- 实际运营数据：\n");
        if (record.getDepartureTime() != null) {
            sb.append("  - 实际发车时间：").append(record.getDepartureTime().format(DATE_TIME_FORMATTER)).append("\n");
        }
        if (record.getArrivalTime() != null) {
            sb.append("  - 实际到达时间：").append(record.getArrivalTime().format(DATE_TIME_FORMATTER)).append("\n");
        }

        // 上座率
        if (record.getOccupationRate() != null) {
            BigDecimal rate = record.getOccupationRate().multiply(BigDecimal.valueOf(100));
            sb.append("  - 上座率：").append(rate.setScale(2, BigDecimal.ROUND_HALF_UP)).append("%\n");
        }

        // 好评率数据
        sb.append("- 乘客评价：\n");
        appendRateInfo(sb, "五星好评率", record.getFivePointRate());
        appendRateInfo(sb, "四星好评率", record.getFourPointRate());
        appendRateInfo(sb, "三星好评率", record.getThreePointRate());
        appendRateInfo(sb, "两星好评率", record.getTwoPointRate());
        appendRateInfo(sb, "一星好评率", record.getOnePointRate());

        return sb.toString();
    }

    /**
     * 添加比率信息
     */
    private void appendRateInfo(StringBuilder sb, String rateName, BigDecimal rate) {
        if (rate != null) {
            BigDecimal percentage = rate.multiply(BigDecimal.valueOf(100));
            sb.append("  - ").append(rateName).append("：")
                    .append(percentage.setScale(2, BigDecimal.ROUND_HALF_UP)).append("%\n");
        }
    }

    /**
     * 构建简洁版知识描述（用于token限制场景）
     */
    public String[] buildSimpleKnowledge() {
        logger.info("开始构建简洁版运营记录知识库...");

        List<OperationRecord> operationRecords = operationRecordMapper.selectAll();
        if (operationRecords == null || operationRecords.isEmpty()) {
            return new String[0];
        }

        List<String> knowledgeList = new ArrayList<>();

        for (OperationRecord record : operationRecords) {
            Schedule schedule = scheduleMapper.selectById(record.getScheduleId());
            if (schedule == null) continue;

            BizVehicle vehicle = null;
            if (schedule.getVehicleId() != null) {
                vehicle = bizVehicleMapper.selectById(schedule.getVehicleId());
            }

            String simpleKnowledge = buildSimpleKnowledgeString(record, schedule, vehicle);
            knowledgeList.add(simpleKnowledge);
        }

        return knowledgeList.toArray(new String[0]);
    }

    /**
     * 简洁版知识描述
     */
    private String buildSimpleKnowledgeString(OperationRecord record, Schedule schedule, BizVehicle vehicle) {
        StringBuilder sb = new StringBuilder();

        sb.append("运营记录").append(record.getId());

        if (vehicle != null) {
            sb.append("：车辆").append(vehicle.getPlateNumber());
        }

        sb.append("，路线").append(schedule.getRouteId());

        if (record.getOccupationRate() != null) {
            BigDecimal rate = record.getOccupationRate().multiply(BigDecimal.valueOf(100));
            sb.append("，上座率").append(rate.setScale(1, BigDecimal.ROUND_HALF_UP)).append("%");
        }

        if (record.getFivePointRate() != null) {
            BigDecimal fiveStar = record.getFivePointRate().multiply(BigDecimal.valueOf(100));
            sb.append("，五星好评率").append(fiveStar.setScale(1, BigDecimal.ROUND_HALF_UP)).append("%");
        }

        return sb.toString();
    }
}