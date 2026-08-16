package com.smartshuttle.business.route.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 线路VO
 */
@Data
public class RouteVO {
    
    private Long id;
    
    /**
     * 线路名称
     */
    private String name;
    
    /**
     * 关联车辆ID
     */
    private Long vehicleId;
    
    /**
     * 车牌号
     */
    private String vehiclePlate;
    
    /**
     * 车辆品牌
     */
    private String vehicleBrand;
    
    /**
     * 驾驶员
     */
    private String driverName;
    
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
     * 乘坐率
     */
    private BigDecimal occupancyRate;
    
    /**
     * 总里程
     */
    private BigDecimal totalDistance;
    
    /**
     * 预计时间
     */
    private Integer estimatedTime;
    
    /**
     * 发车时间
     */
    private LocalTime departureTime;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 状态名称
     */
    private String statusName;
    
    /**
     * 线路颜色
     */
    private String color;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    public void setStatus(Integer status) {
        this.status = status;
        this.statusName = status == 1 ? "运营中" : "停运";
    }
}
