package com.smartshuttle.business.employee.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;
/**
 * 员工VO
 */
@Data
public class EmployeeVO {
    
    private Long id;
    
    /**
     * 工号
     */
    private String employeeNo;
    
    /**
     * 姓名
     */
    private String name;
    
    /**
     * 部门
     */
    private String department;
    
    /**
     * 手机号（脱敏）
     */
    private String phone;
    
    /**
     * 乘车站点ID
     */
    private Long stationId;
    
    /**
     * 乘车站点名称
     */
    private String stationName;
    
    /**
     * 所属线路ID
     */
    private Long routeId;
    
    /**
     * 所属线路名称
     */
    private String routeName;
    
    /**
     * 状态码
     */
    private Integer status;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 状态名称
     */
    private String statusName;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    public void setStatus(Integer status) {
        this.status = status;
        this.statusName = switch (status) {
            case 0 -> "离职";
            case 1 -> "正常通勤";
            case 2 -> "请假";
            case 3 -> "出差";
            default -> "未知";
        };
    }
    
    /**
     * 手机号脱敏
     */
    public void setPhone(String phone) {
        if (phone != null && phone.length() == 11) {
            this.phone = phone.substring(0, 3) + "****" + phone.substring(7);
        } else {
            this.phone = phone;
        }
    }
}
