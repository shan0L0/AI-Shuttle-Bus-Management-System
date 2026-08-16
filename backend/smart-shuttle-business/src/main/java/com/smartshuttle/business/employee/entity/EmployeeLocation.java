package com.smartshuttle.business.employee.entity;
import com.smartshuttle.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeLocation extends BaseEntity{
    //id
    private Long id;
    //纬度
    private BigDecimal latitude;
    //经度
    private BigDecimal longitude;

    public BigDecimal[] toLocatArray(){
        return new BigDecimal[]{longitude, latitude};
    }
}
