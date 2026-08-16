package com.smartshuttle.business.employee.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartshuttle.business.employee.entity.Employee;
import com.smartshuttle.business.employee.entity.EmployeeLocation;
import com.smartshuttle.business.employee.mapper.EmployeeMapper;
import com.smartshuttle.business.employee.vo.EmployeeVO;
import com.smartshuttle.common.constant.ErrorCode;
import com.smartshuttle.common.exception.BusinessException;
import com.smartshuttle.common.result.PageResult;
import com.smartshuttle.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 员工管理控制器
 */
@Tag(name = "员工管理")
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {
    
    private final EmployeeMapper employeeMapper;
    
    @Operation(summary = "分页查询员工列表")
    @GetMapping
    //@PreAuthorize("hasAuthority('employee:list')")
    public Result<PageResult<EmployeeVO>> getEmployeePage(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "stationId", required = false) Long stationId,
            @RequestParam(value = "status", required = false) Integer status) {
        
        Page<EmployeeVO> page = new Page<>(pageNum, pageSize);
        IPage<EmployeeVO> result = employeeMapper.selectEmployeePage(page, name, stationId, status);
        return Result.success(PageResult.of(result));
    }

    @Operation(summary = "查询员工列表")
    @GetMapping("/all")
    //@PreAuthorize("hasAuthority('employee:list')")
    public Result<List<EmployeeLocation>> getEmployee() {
        List<EmployeeLocation> empList = employeeMapper.selectEmpLocations();
        return Result.success(empList);
    }

    @Operation(summary = "获取员工详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('employee:list')")
    public Result<Employee> getEmployeeById(@Parameter(description = "员工ID") @PathVariable Long id) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "员工不存在");
        }
        return Result.success(employee);
    }
    
    @Operation(summary = "新增员工")
    @PostMapping
    @PreAuthorize("hasAuthority('employee:add')")
    public Result<Long> addEmployee(@RequestBody Employee employee) {
        employee.setStatus(1); // 默认在职
        employeeMapper.insert(employee);
        return Result.success("新增成功", employee.getId());
    }
    
    @Operation(summary = "更新员工")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('employee:edit')")
    public Result<Void> updateEmployee(
            @Parameter(description = "员工ID") @PathVariable Long id,
            @RequestBody Employee employee) {
        employee.setId(id);
        employeeMapper.updateById(employee);
        return Result.success("更新成功", null);
    }
    
    @Operation(summary = "删除员工")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('employee:delete')")
    public Result<Void> deleteEmployee(@Parameter(description = "员工ID") @PathVariable Long id) {
        employeeMapper.deleteById(id);
        return Result.success("删除成功", null);
    }
    
    @Operation(summary = "更新员工状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('employee:edit')")
    public Result<Void> updateEmployeeStatus(
            @Parameter(description = "员工ID") @PathVariable Long id,
            @Parameter(description = "状态：0离职 1在职 2请假 3出差") @RequestParam Integer status) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setStatus(status);
        employeeMapper.updateById(employee);
        return Result.success("状态更新成功", null);
    }
    
    @Operation(summary = "员工统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getEmployeeStats() {
        Map<String, Object> stats = employeeMapper.selectStats();
        return Result.success(stats);
    }
    
    @Operation(summary = "同步HR数据")
    @PostMapping("/sync-hr")
    @PreAuthorize("hasAuthority('employee:edit')")
    public Result<Map<String, Object>> syncHRData() {
        // 模拟同步HR数据
        // 实际项目中这里会调用HR系统接口
        Map<String, Object> result = Map.of(
                "synced", 15,
                "added", 3,
                "updated", 12,
                "deleted", 0
        );
        return Result.success("HR数据同步完成", result);
    }

}
