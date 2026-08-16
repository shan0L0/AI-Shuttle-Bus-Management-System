package com.smartshuttle.business.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartshuttle.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 员工实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_employee")
public class Employee extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
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
     * 手机号
     */
    private String phone;
    
    /**
     * 乘车站点ID
     */
    private Long stationId;
    
    /**
     * 所属线路ID
     */
    private Long routeId;
    
    /**
     * 家庭住址
     */
    private String address;
    
    /**
     * 状态：0离职 1在职 2请假 3出差
     */
    private Integer status;
    
    /**
     * 备注
     */
    private String remark;
}
