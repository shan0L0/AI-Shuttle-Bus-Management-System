package com.smartshuttle.business.vehicle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartshuttle.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 车辆实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_vehicle")
public class Vehicle extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
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
     * 状态：0待命 1运行中 2维修中
     */
    private Integer status;
    
    /**
     * 百公里油耗
     */
    private BigDecimal fuelConsumption;
    
    /**
     * 所属线路ID
     */
    private Long routeId;
    
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
     * 总里程（km）
     */
    private Integer mileage;
    
    /**
     * 备注
     */
    private String remark;
}
