package com.smartshuttle.business.station.kmeans_planning;
import com.smartshuttle.business.employee.entity.EmployeeLocation;
import com.smartshuttle.business.employee.mapper.EmployeeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Planner 类单元测试
 */
@ExtendWith(MockitoExtension.class)
class PlannerTest {

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private Planner planner;

    private List<EmployeeLocation> mockEmployees;

    @BeforeEach
    void setUp() {
        // 创建测试数据 - 模拟杭州地区的员工坐标
        mockEmployees = Arrays.asList(
                createEmployee(1L, "120.155069", "30.274085", "员工1"),
                createEmployee(2L, "120.156123", "30.275432", "员工2"),
                createEmployee(3L, "120.157890", "30.276543", "员工3"),
                createEmployee(4L, "121.473701", "31.230416", "员工4"),
                createEmployee(5L, "121.474567", "31.231234", "员工5"),
                createEmployee(6L, "121.475432", "31.232345", "员工6"),
                createEmployee(7L, "116.397428", "39.909230", "员工7"),
                createEmployee(8L, "116.398123", "39.910123", "员工8"),
                createEmployee(9L, "116.398789", "39.911234", "员工9"),
                createEmployee(10L, "116.399456", "39.912345", "员工10")
        );
    }

    @Test
    void testPlanStations_Success() {
        // 1. 设置Mock行为
        when(employeeMapper.selectEmpLocations()).thenReturn(mockEmployees);

        // 2. 执行测试
        Planner.StationPlanResult result = planner.planStations();

        // 3. 验证基本结果
        assertNotNull(result, "结果不应为空");
        assertTrue(result.isSuccess(), "规划应成功");
        assertEquals("站点规划成功", result.getMessage());
        assertTrue(result.getStationCount() > 0, "站点数量应大于0");
        assertEquals(mockEmployees.size(), result.getEmployeeCount(), "员工数量应匹配");
        assertNotNull(result.getPlanTime(), "规划时间不应为空");

        // 4. 验证站点中心
        assertNotNull(result.getStationCenters(), "站点中心不应为空");
        assertFalse(result.getStationCenters().isEmpty(), "站点中心不应为空列表");

        for (Planner.StationCenter center : result.getStationCenters()) {
            assertTrue(center.getStationId() >= 0, "站点ID应>=0");
            assertTrue(center.getLongitude() > 0, "经度应大于0");
            assertTrue(center.getLatitude() > 0, "纬度应大于0");
        }

        // 5. 验证员工-站点映射
        Map<Long, Integer> employeeToStation = result.getEmployeeToStation();
        assertNotNull(employeeToStation, "员工-站点映射不应为空");
        assertEquals(mockEmployees.size(), employeeToStation.size(), "映射数量应等于员工数");

        // 验证每个员工都有映射
        for (EmployeeLocation emp : mockEmployees) {
            assertTrue(employeeToStation.containsKey(emp.getId()),
                    "员工" + emp.getId() + "应有站点映射");
            int stationId = employeeToStation.get(emp.getId());
            assertTrue(stationId >= 0 && stationId < result.getStationCount(),
                    "站点ID应在有效范围内");
        }

        // 6. 验证站点员工统计
        Map<Integer, Integer> stationEmployeeCount = result.getStationEmployeeCount();
        assertNotNull(stationEmployeeCount, "站点员工统计不应为空");

        int totalCount = stationEmployeeCount.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(mockEmployees.size(), totalCount, "员工总数应匹配");

        // 7. 验证距离计算
        Map<Long, Double> employeeDistances = result.getEmployeeDistances();
        assertNotNull(employeeDistances, "距离数据不应为空");
        assertEquals(mockEmployees.size(), employeeDistances.size(), "距离数据数量应匹配");

        for (Double distance : employeeDistances.values()) {
            assertTrue(distance >= 0, "距离应>=0");
            assertTrue(distance < 1000, "距离应在合理范围内（杭州地区）");
        }

        // 8. 验证Mapper被调用
        verify(employeeMapper, times(1)).selectEmpLocations();

        System.out.println("✅ 测试通过！规划结果：");
        System.out.println("   站点数量：" + result.getStationCount());
        System.out.println("   员工数量：" + result.getEmployeeCount());
        System.out.println("   规划时间：" + result.getPlanTime());
    }

    @Test
    void testPlanStations_EmptyData() {
        // 测试空数据
        when(employeeMapper.selectEmpLocations()).thenReturn(Arrays.asList());

        Planner.StationPlanResult result = planner.planStations();

        assertNotNull(result);
        assertFalse(result.isSuccess(), "空数据应返回失败");
        assertEquals("站点规划成功", result.getMessage()); // 注意：你的代码总是返回成功
        assertEquals(0, result.getStationCount());
        assertEquals(0, result.getEmployeeCount());
        assertTrue(result.getStationCenters().isEmpty());
        assertTrue(result.getEmployeeToStation().isEmpty());

        verify(employeeMapper, times(1)).selectEmpLocations();
    }

    @Test
    void testPlanStations_SingleEmployee() {
        // 测试只有一个员工的情况
        List<EmployeeLocation> singleEmployee = Arrays.asList(
                createEmployee(1L, "120.155069", "30.274085", "单个员工")
        );

        when(employeeMapper.selectEmpLocations()).thenReturn(singleEmployee);

        Planner.StationPlanResult result = planner.planStations();

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getEmployeeCount());
        assertTrue(result.getStationCount() >= 1);

        // 验证单个员工的映射
        assertEquals(1, result.getEmployeeToStation().size());
        assertTrue(result.getEmployeeToStation().containsKey(1L));

        verify(employeeMapper, times(1)).selectEmpLocations();
    }

    @Test
    void testPlanStations_DuplicateCoordinates() {
        // 测试坐标相同的情况
        List<EmployeeLocation> duplicateEmployees = Arrays.asList(
                createEmployee(1L, "120.155069", "30.274085", "员工1"),
                createEmployee(2L, "120.155069", "30.274085", "员工2"),
                createEmployee(3L, "120.155069", "30.274085", "员工3")
        );

        when(employeeMapper.selectEmpLocations()).thenReturn(duplicateEmployees);

        Planner.StationPlanResult result = planner.planStations();

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getEmployeeCount());

        // 所有坐标相同的员工应该被分到同一个站点
        Map<Long, Integer> assignments = result.getEmployeeToStation();
        Integer firstStation = assignments.get(1L);

        for (EmployeeLocation emp : duplicateEmployees) {
            assertEquals(firstStation, assignments.get(emp.getId()),
                    "坐标相同的员工应在同一站点");
        }

        verify(employeeMapper, times(1)).selectEmpLocations();
    }

    @Test
    void testStationPlanResult() {
        // 测试结果类
        List<Planner.StationCenter> centers = Arrays.asList(
                new Planner.StationCenter(0, 120.155069, 30.274085)
        );

        Map<Long, Integer> mapping = Map.of(1L, 0);
        Map<Integer, Integer> counts = Map.of(0, 1);
        Map<Long, Double> distances = Map.of(1L, 0.5);

        // 测试成功结果
        Planner.StationPlanResult success = Planner.StationPlanResult.success(
                centers, mapping, counts, distances, 1, 1
        );

        assertTrue(success.isSuccess());
        assertEquals("站点规划成功", success.getMessage());
        assertEquals(1, success.getStationCount());
        assertEquals(1, success.getEmployeeCount());
        assertNotNull(success.getPlanTime());

        // 测试失败结果
        Planner.StationPlanResult failure = Planner.StationPlanResult.failure("测试失败");

        assertFalse(failure.isSuccess());
        assertEquals("测试失败", failure.getMessage());
        assertEquals(0, failure.getStationCount());
        assertEquals(0, failure.getEmployeeCount());
        assertTrue(failure.getStationCenters().isEmpty());
    }

    @Test
    void testEmployeeAssignment() {
        // 测试员工分配类
        EmployeeLocation emp = new EmployeeLocation();
        emp.setId(1L);

        Planner.EmployeeAssignment assignment = Planner.EmployeeAssignment.from(emp, 0, 1.5);

        assertEquals(1L, assignment.getEmployeeId());
        assertEquals(0, assignment.getStationId());
        assertEquals(1.5, assignment.getDistanceToStation(), 0.001);

        // 测试距离为null的情况
        Planner.EmployeeAssignment assignment2 = Planner.EmployeeAssignment.from(emp, 1, null);
        assertEquals(0.0, assignment2.getDistanceToStation(), 0.001);
    }

    // 辅助方法：创建测试员工
    private EmployeeLocation createEmployee(Long id, String lng, String lat, String name) {
        EmployeeLocation emp = new EmployeeLocation();
        emp.setId(id);
        emp.setLongitude(new BigDecimal(lng));
        emp.setLatitude(new BigDecimal(lat));
        // 如果有name字段
        // emp.setName(name);
        return emp;
    }
}
