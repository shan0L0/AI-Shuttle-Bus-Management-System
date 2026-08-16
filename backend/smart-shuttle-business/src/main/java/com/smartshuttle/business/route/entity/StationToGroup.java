package com.smartshuttle.business.route.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartshuttle.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_route_station")
public class StationToGroup extends BaseEntity {
    private Long routeId;
    private Long stationId;
}
