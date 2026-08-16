package com.smartshuttle.business.station.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartshuttle.business.employee.entity.EmployeeLocation;
import com.smartshuttle.business.station.entity.Station;
import com.smartshuttle.business.station.entity.StationLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 站点Mapper
 */
@Mapper
public interface TerminalMapper extends BaseMapper<Station> {
    @Select("select id, longitude, latitude " +
            "from terminal_station " +
            "where id = #{id} and deleted = 0")
    List<StationLocation> selectStnLocationById(@Param("id") Long id);
}
