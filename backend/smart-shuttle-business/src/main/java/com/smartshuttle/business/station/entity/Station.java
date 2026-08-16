package com.smartshuttle.business.station.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartshuttle.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 站点实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_station")
public class Station extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 站点名称
     */
    private String name;
    
    /**
     * 经度
     */
    private BigDecimal longitude;
    
    /**
     * 纬度
     */
    private BigDecimal latitude;
    
    /**
     * 详细地址
     */
    private String address;
    
    /**
     * 乘车人数
     */
    private Integer passengerCount;
    
    /**
     * 所属区域
     */
    private String district;
    
    /**
     * 状态：0停用 1启用
     */
    private Integer status;
    
    /**
     * 备注
     */
    private String remark;
}
