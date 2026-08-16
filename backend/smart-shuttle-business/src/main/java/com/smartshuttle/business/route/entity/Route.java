package com.smartshuttle.business.route.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartshuttle.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 线路实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_route")
public class Route extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 线路名称
     */
    private String name;
    
    /**
     * 关联车辆ID
     */
    private Long vehicleId;
    
    /**
     * 站点数量
     */
    private Integer stationCount;
    
    /**
     * 总乘客数
     */
    private Integer totalPassenger;
    
    /**
     * 载客容量
     */
    private Integer capacity;
    
    /**
     * 乘坐率（百分比）
     */
    private BigDecimal occupancyRate;
    
    /**
     * 总里程（km）
     */
    private BigDecimal totalDistance;
    
    /**
     * 预计行驶时间（分钟）
     */
    private Integer estimatedTime;
    
    /**
     * 发车时间
     */
    private LocalTime departureTime;
    
    /**
     * 状态：0停运 1运营中
     */
    private Integer status;
    
    /**
     * 线路颜色（用于地图显示）
     */
    private String color;
    
    /**
     * 备注
     */
    private String remark;
}
