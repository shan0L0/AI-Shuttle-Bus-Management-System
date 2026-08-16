package com.smartshuttle.business.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartshuttle.business.employee.entity.Employee;
import com.smartshuttle.business.employee.vo.EmployeeVO;
import com.smartshuttle.business.employee.entity.EmployeeLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 员工Mapper
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
    
    /**
     * 分页查询员工（含站点和线路信息）
     */
    //select中第一行的as作用为在联合查询中将不同表中相同的名字换掉，以便于正确映射到实体类中的属性名
    //如下，left join 功能是根据join的条件联合另一张表进行查询，不满足联合条件的记录联合表相关字段为null，inner join 联合逻辑一样，不满足联合条件的记录则直接不出现在结果里（不显示）
    @Select("select e.id, employee_no, e.name, department, phone, e.address, e.status, s.name as station_name, r.name as route_name " +//这里每行结尾必须空格，因为“+”是不会空格的，只是将字符串连接在一起
            "from biz_employee e " +
            "left join biz_station s on e.station_id = s.id " +
            "left join biz_route r on e.route_id = r.id " +
            "where e.status != 0 and e.deleted = 0")
    IPage<EmployeeVO> selectEmployeePage(Page<EmployeeVO> page,
                                         @Param("name") String name,
                                         @Param("stationId") Long stationId,
                                         @Param("status") Integer status);
    
    /**
     * 统计各状态员工数量
     */
    @Select("SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as normal, " +
            "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as onLeave, " +
            "SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as onTrip " +
            "FROM biz_employee WHERE deleted = 0 AND status != 0")
    Map<String, Object> selectStats();


    //统计员工住址坐标用于站点规划
    @Select("select id, longitude, latitude " +
            "from biz_employee " +
            "where status != 0")
    List<EmployeeLocation> selectEmpLocations();
}
