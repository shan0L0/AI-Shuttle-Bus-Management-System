package com.smartshuttle.business.station.entity;

import com.smartshuttle.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class StationLocation extends BaseEntity {
    private Long id;
    private BigDecimal longitude;
    private BigDecimal latitude;

    public StationLocation(Long id, BigDecimal longitude, BigDecimal latitude){
        this.id = id;
        this.longitude = longitude;
        this.latitude = latitude;
    }
}
