package com.smartshuttle.business.vehicle.vo;

import com.smartshuttle.common.enums.VehicleStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆VO
 */
@Data
public class VehicleVO {
    
    private Long id;
    
    /**
     * 车牌号
     */
    private String plateNumber;
    
    /**
     * 品牌型号
     */
    private String brand;
    
    /**
     * 座位数
     */
    private Integer seats;
    
    /**
     * 状态码
     */
    private Integer status;
    
    /**
     * 状态名称
     */
    private String statusName;
    
    /**
     * 百公里油耗
     */
    private BigDecimal fuelConsumption;
    
    /**
     * 所属线路ID
     */
    private Long routeId;
    
    /**
     * 所属线路名称
     */
    private String routeName;
    
    /**
     * 驾驶员姓名
     */
    private String driverName;
    
    /**
     * 驾驶员电话
     */
    private String driverPhone;
    
    /**
     * 购置日期
     */
    private LocalDate purchaseDate;
    
    /**
     * 上次保养日期
     */
    private LocalDate lastMaintenance;
    
    /**
     * 下次保养日期
     */
    private LocalDate nextMaintenance;
    
    /**
     * 总里程
     */
    private Integer mileage;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 设置状态名称
     */
    public void setStatus(Integer status) {
        this.status = status;
        this.statusName = VehicleStatus.getDesc(status);
    }
}
