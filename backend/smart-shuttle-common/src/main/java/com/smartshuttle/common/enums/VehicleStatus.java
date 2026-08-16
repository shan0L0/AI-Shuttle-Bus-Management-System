package com.smartshuttle.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 车辆状态枚举
 */
@Getter
@AllArgsConstructor
public enum VehicleStatus {
    
    STANDBY(0, "待命"),
    RUNNING(1, "运行中"),
    MAINTENANCE(2, "维修中");
    
    private final Integer code;
    private final String desc;
    
    public static VehicleStatus of(Integer code) {
        for (VehicleStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    public static String getDesc(Integer code) {
        VehicleStatus status = of(code);
        return status != null ? status.getDesc() : "未知";
    }
}
