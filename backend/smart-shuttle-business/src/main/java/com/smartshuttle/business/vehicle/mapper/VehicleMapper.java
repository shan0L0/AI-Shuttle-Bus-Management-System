package com.smartshuttle.business.vehicle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartshuttle.business.vehicle.dto.VehicleQueryDTO;
import com.smartshuttle.business.vehicle.entity.Vehicle;
import com.smartshuttle.business.vehicle.vo.VehicleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 车辆Mapper
 */
@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {
    
    /**
     * 分页查询车辆列表
     */
    IPage<VehicleVO> selectVehiclePage(Page<VehicleVO> page, @Param("query") VehicleQueryDTO query);
    
    /**
     * 根据车牌号查询
     */
    @Select("SELECT * FROM biz_vehicle WHERE plate_number = #{plateNumber} AND deleted = 0")
    Vehicle selectByPlateNumber(@Param("plateNumber") String plateNumber);
    
    /**
     * 统计各状态车辆数量
     */
    @Select("SELECT status, COUNT(*) as count FROM biz_vehicle WHERE deleted = 0 GROUP BY status")
    java.util.List<java.util.Map<String, Object>> countByStatus();
}
