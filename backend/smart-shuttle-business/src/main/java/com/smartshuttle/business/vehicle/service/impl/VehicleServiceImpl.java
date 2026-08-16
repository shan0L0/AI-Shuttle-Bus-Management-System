package com.smartshuttle.business.vehicle.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartshuttle.business.vehicle.dto.VehicleDTO;
import com.smartshuttle.business.vehicle.dto.VehicleQueryDTO;
import com.smartshuttle.business.vehicle.entity.Vehicle;
import com.smartshuttle.business.vehicle.mapper.VehicleMapper;
import com.smartshuttle.business.vehicle.service.VehicleService;
import com.smartshuttle.business.vehicle.vo.VehicleVO;
import com.smartshuttle.common.constant.ErrorCode;
import com.smartshuttle.common.exception.BusinessException;
import com.smartshuttle.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {
    
    private final VehicleMapper vehicleMapper;
    
    @Override
    public PageResult<VehicleVO> getVehiclePage(VehicleQueryDTO query) {
        Page<VehicleVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<VehicleVO> result = vehicleMapper.selectVehiclePage(page, query);
        return PageResult.of(result);
    }
    
    @Override
    public VehicleVO getVehicleById(Long id) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "车辆不存在");
        }
        return BeanUtil.copyProperties(vehicle, VehicleVO.class);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addVehicle(VehicleDTO dto) {
        // 检查车牌号是否已存在
        Vehicle existing = vehicleMapper.selectByPlateNumber(dto.getPlateNumber());
        if (existing != null) {
            throw BusinessException.of(ErrorCode.DATA_EXISTS, "车牌号已存在");
        }
        
        // 新增车辆
        Vehicle vehicle = BeanUtil.copyProperties(dto, Vehicle.class);
        vehicle.setStatus(0); // 默认待命状态
        vehicleMapper.insert(vehicle);
        
        log.info("新增车辆成功: {}", vehicle.getPlateNumber());
        return vehicle.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVehicle(VehicleDTO dto) {
        if (dto.getId() == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "车辆ID不能为空");
        }
        
        // 检查车辆是否存在
        Vehicle vehicle = vehicleMapper.selectById(dto.getId());
        if (vehicle == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "车辆不存在");
        }
        
        // 检查车牌号是否重复
        if (!vehicle.getPlateNumber().equals(dto.getPlateNumber())) {
            Vehicle existing = vehicleMapper.selectByPlateNumber(dto.getPlateNumber());
            if (existing != null) {
                throw BusinessException.of(ErrorCode.DATA_EXISTS, "车牌号已被使用");
            }
        }
        
        // 更新车辆信息
        BeanUtil.copyProperties(dto, vehicle, "id", "status", "createTime", "createBy", "deleted");
        vehicleMapper.updateById(vehicle);
        
        log.info("更新车辆成功: {}", vehicle.getPlateNumber());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "车辆不存在");
        }
        
        // 检查是否有关联线路（实际项目中需要检查）
        if (vehicle.getRouteId() != null) {
            throw BusinessException.of(ErrorCode.DATA_REFERENCED, "车辆已分配线路，无法删除");
        }
        
        vehicleMapper.deleteById(id);
        log.info("删除车辆成功: {}", vehicle.getPlateNumber());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVehicleBatch(List<Long> ids) {
        for (Long id : ids) {
            deleteVehicle(id);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVehicleStatus(Long id, Integer status) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "车辆不存在");
        }
        
        vehicle.setStatus(status);
        vehicleMapper.updateById(vehicle);
        
        log.info("更新车辆状态: {} -> {}", vehicle.getPlateNumber(), status);
    }
    
    @Override
    public List<VehicleVO> getAllVehicles() {
        List<Vehicle> vehicles = vehicleMapper.selectList(
                new LambdaQueryWrapper<Vehicle>()
                        .orderByAsc(Vehicle::getPlateNumber)
        );
        return BeanUtil.copyToList(vehicles, VehicleVO.class);
    }
    
    @Override
    public Map<String, Long> countByStatus() {
        List<Map<String, Object>> result = vehicleMapper.countByStatus();
        Map<String, Long> countMap = new HashMap<>();
        countMap.put("total", 0L);
        countMap.put("standby", 0L);
        countMap.put("running", 0L);
        countMap.put("maintenance", 0L);
        
        for (Map<String, Object> row : result) {
            Integer status = (Integer) row.get("status");
            Long count = ((Number) row.get("count")).longValue();
            countMap.put("total", countMap.get("total") + count);
            
            switch (status) {
                case 0 -> countMap.put("standby", count);
                case 1 -> countMap.put("running", count);
                case 2 -> countMap.put("maintenance", count);
            }
        }
        
        return countMap;
    }
}
