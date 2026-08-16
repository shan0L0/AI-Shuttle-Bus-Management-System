package com.smartshuttle.business.vehicle.dto;

import lombok.Data;

/**
 * 车辆查询DTO
 */
@Data
public class VehicleQueryDTO {
    
    /**
     * 页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页条数
     */
    private Integer pageSize = 10;
    
    /**
     * 车牌号（模糊查询）
     */
    private String plateNumber;
    
    /**
     * 状态筛选
     */
    private Integer status;
    
    /**
     * 所属线路ID
     */
    private Long routeId;
    
    /**
     * 品牌（模糊查询）
     */
    private String brand;
}
