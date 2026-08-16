package com.smartshuttle.business.vehicle.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 车辆DTO
 */
@Data
public class VehicleDTO {
    
    /**
     * ID（更新时必填）
     */
    private Long id;
    
    /**
     * 车牌号
     */
    @NotBlank(message = "车牌号不能为空")
    @Pattern(regexp = "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-HJ-NP-Z0-9]{4,5}[A-HJ-NP-Z0-9挂学警港澳]$",
            message = "车牌号格式不正确")
    private String plateNumber;
    
    /**
     * 品牌型号
     */
    @NotBlank(message = "品牌型号不能为空")
    @Size(max = 50, message = "品牌型号不能超过50字符")
    private String brand;
    
    /**
     * 座位数
     */
    @NotNull(message = "座位数不能为空")
    @Min(value = 1, message = "座位数至少为1")
    @Max(value = 100, message = "座位数不能超过100")
    private Integer seats;
    
    /**
     * 百公里油耗
     */
    @DecimalMin(value = "0", message = "油耗不能为负数")
    @DecimalMax(value = "100", message = "油耗不能超过100L/100km")
    private BigDecimal fuelConsumption;
    
    /**
     * 所属线路ID
     */
    private Long routeId;
    
    /**
     * 驾驶员姓名
     */
    @Size(max = 50, message = "驾驶员姓名不能超过50字符")
    private String driverName;
    
    /**
     * 驾驶员电话
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String driverPhone;
    
    /**
     * 购置日期
     */
    private LocalDate purchaseDate;
    
    /**
     * 备注
     */
    @Size(max = 500, message = "备注不能超过500字符")
    private String remark;
}
