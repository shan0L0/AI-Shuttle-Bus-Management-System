package com.smartshuttle.business.vehicle.service;

import com.smartshuttle.business.vehicle.dto.VehicleDTO;
import com.smartshuttle.business.vehicle.dto.VehicleQueryDTO;
import com.smartshuttle.business.vehicle.vo.VehicleVO;
import com.smartshuttle.common.result.PageResult;

import java.util.List;

/**
 * 车辆服务接口
 */
public interface VehicleService {
    
    /**
     * 分页查询车辆列表
     */
    PageResult<VehicleVO> getVehiclePage(VehicleQueryDTO query);
    
    /**
     * 获取车辆详情
     */
    VehicleVO getVehicleById(Long id);
    
    /**
     * 新增车辆
     */
    Long addVehicle(VehicleDTO dto);
    
    /**
     * 更新车辆
     */
    void updateVehicle(VehicleDTO dto);
    
    /**
     * 删除车辆
     */
    void deleteVehicle(Long id);
    
    /**
     * 批量删除车辆
     */
    void deleteVehicleBatch(List<Long> ids);
    
    /**
     * 更新车辆状态
     */
    void updateVehicleStatus(Long id, Integer status);
    
    /**
     * 获取所有车辆（下拉选择用）
     */
    List<VehicleVO> getAllVehicles();
    
    /**
     * 统计各状态车辆数量
     */
    java.util.Map<String, Long> countByStatus();
}
