package com.smartshuttle.business.route.kmeansPlanning;

import com.smartshuttle.business.station.entity.StationLocation;
import com.smartshuttle.business.station.mapper.StationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StationGroupPlannerTest {

    @Mock
    private StationMapper stationMapper;

    @InjectMocks
    private StationGroupPlanner stationGroupPlanner;

    // 测试数据
    private List<StationLocation> mockStations;

    @BeforeEach
    void setUp() {
        // 准备模拟数据 - 3个不同位置的站点
        mockStations = Arrays.asList(
                // 东城区
                createStation(1L, 116.4075, 39.9040),   // 王府井
                createStation(2L, 116.3956, 39.9138),   // 天安门广场
                createStation(3L, 116.4164, 39.9287),   // 故宫博物院
                createStation(4L, 116.4332, 39.9409),   // 东直门
                createStation(5L, 116.4178, 39.8719),   // 崇文门

                // 西城区
                createStation(6L, 116.3689, 39.9138),   // 西单
                createStation(7L, 116.3839, 39.9023),   // 金融街
                createStation(8L, 116.3529, 39.9336),   // 北京动物园
                createStation(9L, 116.3394, 39.8991),   // 西直门
                createStation(10L, 116.3736, 39.8788),  // 广安门

                // 朝阳区
                createStation(11L, 116.4864, 39.9040),  // 国贸CBD
                createStation(12L, 116.4551, 39.9137),  // 三里屯
                createStation(13L, 116.4330, 39.9608),  // 奥林匹克公园
                createStation(14L, 116.4708, 39.8819),  // 潘家园
                createStation(15L, 116.5064, 39.9335),  // 望京

                // 海淀区
                createStation(16L, 116.3168, 39.9606),  // 中关村
                createStation(17L, 116.3250, 39.9336),  // 五道口
                createStation(18L, 116.3384, 39.9762),  // 清华科技园
                createStation(19L, 116.2995, 39.9596),  // 北京大学
                createStation(20L, 116.2733, 39.9825),  // 颐和园

                // 丰台区
                createStation(21L, 116.2864, 39.8581),  // 北京西站
                createStation(22L, 116.2820, 39.8667),  // 六里桥
                createStation(23L, 116.3057, 39.8505),  // 丰台科技园
                createStation(24L, 116.2760, 39.8289),  // 花乡
                createStation(25L, 116.3005, 39.8358),  // 新发地

                // 石景山区
                createStation(26L, 116.2230, 39.9023),  // 石景山区中心
                createStation(27L, 116.2000, 39.9200),  // 八大处
                createStation(28L, 116.1833, 39.9000),  // 门头沟交界

                // 通州区
                createStation(29L, 116.5589, 39.9056),  // 通州中心

                // 顺义区
                createStation(30L, 116.6480, 40.1282),   // 首都机场
                // 东城区补充 (31-40)
                createStation(31L, 116.4221, 39.9372),   // 地坛公园
                createStation(32L, 116.4105, 39.9308),   // 东四
                createStation(33L, 116.4256, 39.9213),   // 东四十条
                createStation(34L, 116.4062, 39.8971),   // 北京站
                createStation(35L, 116.4283, 39.8889),   // 劲松
                createStation(36L, 116.4137, 39.8764),   // 磁器口
                createStation(37L, 116.4358, 39.9087),   // 工体北路
                createStation(38L, 116.4302, 39.8954),   // 双井
                createStation(39L, 116.4195, 39.9136),   // 灯市口
                createStation(40L, 116.4048, 39.9082),   // 前门

// 西城区补充 (41-50)
                createStation(41L, 116.3573, 39.9256),   // 西四
                createStation(42L, 116.3641, 39.9367),   // 新街口
                createStation(43L, 116.3778, 39.8883),   // 右安门
                createStation(44L, 116.3595, 39.9012),   // 宣武门
                createStation(45L, 116.3512, 39.9173),   // 阜成门
                createStation(46L, 116.3667, 39.9291),   // 西直门外
                createStation(47L, 116.3721, 39.9029),   // 长椿街
                createStation(48L, 116.3658, 39.8857),   // 陶然亭
                createStation(49L, 116.3582, 39.9105),   // 复兴门
                createStation(50L, 116.3498, 39.9086),   // 和平门

// 朝阳区补充 (51-65)
                createStation(51L, 116.4759, 39.8893),   // 大望路
                createStation(52L, 116.4632, 39.9037),   // 建国门
                createStation(53L, 116.4978, 39.9172),   // 东坝
                createStation(54L, 116.4485, 39.8986),   // 团结湖
                createStation(55L, 116.4582, 39.9283),   // 安贞桥
                createStation(56L, 116.4901, 39.8965),   // 四惠
                createStation(57L, 116.5032, 39.9067),   // 高碑店
                createStation(58L, 116.4667, 39.8904),   // 华贸中心
                createStation(59L, 116.4823, 39.9132),   // 三元桥
                createStation(60L, 116.4568, 39.8841),   // 双井桥
                createStation(61L, 116.4376, 39.9691),   // 亚运村
                createStation(62L, 116.5012, 39.9248),   // 太阳宫
                createStation(63L, 116.4889, 39.8889),   // 百子湾
                createStation(64L, 116.4701, 39.9031),   // 金台路
                createStation(65L, 116.4523, 39.9185),   // 工体

// 海淀区补充 (66-80)
                createStation(66L, 116.3109, 39.9502),   // 魏公村
                createStation(67L, 116.2987, 39.9756),   // 清华东路
                createStation(68L, 116.3056, 39.9321),   // 紫竹桥
                createStation(69L, 116.2833, 39.9456),   // 车道沟
                createStation(70L, 116.2958, 39.9678),   // 成府路
                createStation(71L, 116.3212, 39.9889),   // 上地
                createStation(72L, 116.3289, 39.9567),   // 苏州街
                createStation(73L, 116.3075, 39.9845),   // 清河
                createStation(74L, 116.2911, 39.9589),   // 中关村南
                createStation(75L, 116.3156, 39.9734),   // 清华西门
                createStation(76L, 116.2767, 39.9698),   // 香山
                createStation(77L, 116.3023, 39.9412),   // 西三环
                createStation(78L, 116.2876, 39.9378),   // 公主坟
                createStation(79L, 116.3321, 39.9678),   // 五道口西
                createStation(80L, 116.3045, 39.9267),   // 万寿路

// 丰台区补充 (81-90)
                createStation(81L, 116.2789, 39.8472),   // 草桥
                createStation(82L, 116.2917, 39.8605),   // 西客站南
                createStation(83L, 116.3156, 39.8432),   // 丰台北路
                createStation(84L, 116.2967, 39.8213),   // 花乡桥
                createStation(85L, 116.2878, 39.8356),   // 马家堡
                createStation(86L, 116.3089, 39.8567),   // 丰台体育场
                createStation(87L, 116.2767, 39.8511),   // 玉泉营
                createStation(88L, 116.3245, 39.8398),   // 丰台东大街
                createStation(89L, 116.3012, 39.8287),   // 新宫
                createStation(90L, 116.2923, 39.8456),   // 右安门外

// 石景山区补充 (91-95)
                createStation(91L, 116.2105, 39.9123),   // 古城
                createStation(92L, 116.1956, 39.9289),   // 苹果园
                createStation(93L, 116.1789, 39.9156),   // 模式口
                createStation(94L, 116.2032, 39.8967),   // 鲁谷
                createStation(95L, 116.1889, 39.9034),   // 八角

// 通州区补充 (96-100)
                createStation(96L, 116.5823, 39.8967),   // 九棵树
                createStation(97L, 116.5678, 39.8876),   // 果园
                createStation(98L, 116.5756, 39.8723),   // 临河里
                createStation(99L, 116.5889, 39.9078),   // 北苑
                createStation(100L, 116.5932, 39.8823)   // 土桥
        );

        // 模拟数据库查询
        when(stationMapper.selectStnLocations()).thenReturn(mockStations);
    }

    @Test
    void testPlanStationGroups_Success() {
        // 执行
        StationGroupPlanner.StationGroupPlanResult result = stationGroupPlanner.planStationGroups();

        // 验证
        assertNotNull(result, "规划结果不应为null");
        assertTrue(result.isSuccess(), "规划应成功");
        assertEquals("站点分组规划成功", result.getMessage(), "消息应正确");

        // 验证分组数量
        assertTrue(result.getGroupCount() > 0, "分组数应大于0");
        // 验证分组中心点
        List<StationGroupPlanner.GroupCenter> groupCenters = result.getGroupCenters();
        assertNotNull(groupCenters, "分组中心点列表不应为null");
        assertEquals(result.getGroupCount(), groupCenters.size(), "分组中心点数量应与分组数一致");

        // 验证站点到分组映射
        Map<Long, Integer> stationToGroup = result.getStationToGroup();
        assertNotNull(stationToGroup, "站点到分组映射不应为null");

        // 验证映射键值范围
        mockStations.forEach(station -> {
            assertTrue(stationToGroup.containsKey(station.getId()),
                    "映射应包含站点ID: " + station.getId());
            int groupId = stationToGroup.get(station.getId());
            assertTrue(groupId >= 0 && groupId < result.getGroupCount(),
                    "分组ID应在有效范围内: " + groupId);
        });

        // 验证分组站点数量统计
        Map<Integer, Integer> groupStationCount = result.getGroupStationCount();
        assertNotNull(groupStationCount, "分组站点数量统计不应为null");

        int totalCount = groupStationCount.values().stream().mapToInt(Integer::intValue).sum();

        // 验证距离计算
        Map<Long, Double> stationDistances = result.getStationDistances();
        assertNotNull(stationDistances, "站点距离信息不应为null");

        // 验证距离应为正数
        stationDistances.values().forEach(distance -> {
            assertTrue(distance >= 0, "站点到分组中心的距离应大于等于0");
        });

        // 验证规划时间
        assertNotNull(result.getPlanTime(), "规划时间不应为null");
    }

    @Test
    void testPlanStationGroups_EmptyData() {
        // 模拟空数据
        when(stationMapper.selectStnLocations()).thenReturn(Arrays.asList());

        // 执行
        StationGroupPlanner.StationGroupPlanResult result = stationGroupPlanner.planStationGroups();

        // 验证
        assertNotNull(result, "规划结果不应为null");
        assertFalse(result.isSuccess(), "空数据时规划应失败");
        assertEquals("站点数据为空", result.getMessage(), "错误消息应正确");
        assertEquals(0, result.getStationCount(), "站点数应为0");
        assertEquals(0, result.getGroupCount(), "分组数应为0");
    }

    @Test
    void testPlanStationGroups_NullData() {
        // 模拟null数据
        when(stationMapper.selectStnLocations()).thenReturn(null);

        // 执行
        StationGroupPlanner.StationGroupPlanResult result = stationGroupPlanner.planStationGroups();

        // 验证
        assertNotNull(result, "规划结果不应为null");
        assertFalse(result.isSuccess(), "null数据时规划应失败");
        assertEquals("站点数据为空", result.getMessage(), "错误消息应正确");
    }

    @Test
    void testPlanStationGroups_SingleStation() {
        // 模拟只有一个站点
        List<StationLocation> singleStation = Arrays.asList(
                createStation(1L, 121.474566, 31.231331)
        );
        when(stationMapper.selectStnLocations()).thenReturn(singleStation);

        // 执行
        StationGroupPlanner.StationGroupPlanResult result = stationGroupPlanner.planStationGroups();

        // 验证
        assertNotNull(result, "规划结果不应为null");
        assertTrue(result.isSuccess(), "单站点规划应成功");

        // 单个站点时，分组数应为1
        assertEquals(1, result.getGroupCount(), "单站点时分组数应为1");
        assertEquals(1, result.getStationCount(), "站点数应为1");

        // 验证分组中心点
        assertEquals(1, result.getGroupCenters().size(), "应有一个分组中心点");

        // 验证站点到分组的映射
        assertEquals(1, result.getStationToGroup().size(), "应有一个站点映射");
        assertEquals(0, result.getStationToGroup().get(1L), "站点应映射到分组0");

        // 验证距离应为0（站点自身就是中心点）
        assertEquals(0.0, result.getStationDistances().get(1L), 0.001, "距离应为0");
    }

    @Test
    void testPlanStationGroups_CloseStations() {
        // 模拟位置接近的站点（应分到同一组）
        List<StationLocation> closeStations = Arrays.asList(
                createStation(1L, 121.474566, 31.231331),  // 上海
                createStation(2L, 121.474600, 31.231350),  // 上海附近
                createStation(3L, 121.474500, 31.231300),  // 上海附近
                createStation(4L, 116.397775, 39.909676),  // 北京
                createStation(5L, 116.397800, 39.909700)   // 北京附近
        );
        when(stationMapper.selectStnLocations()).thenReturn(closeStations);

        // 执行
        StationGroupPlanner.StationGroupPlanResult result = stationGroupPlanner.planStationGroups();

        // 验证
        assertNotNull(result, "规划结果不应为null");
        assertTrue(result.isSuccess(), "规划应成功");

        // 分组数应小于站点数
        assertTrue(result.getGroupCount() < 5, "相近站点应被分到更少的分组");

        // 分组数量统计
        Map<Integer, Integer> groupStationCount = result.getGroupStationCount();
        assertTrue(groupStationCount.values().stream().anyMatch(count -> count > 1),
                "相近站点应被分到同一组");
    }

    @Test
    void testHaversineDistanceCalculation() {
        // 通过反射调用私有方法
        double distance = (double) ReflectionTestUtils.invokeMethod(
                stationGroupPlanner,
                "haversineDistance",
                31.231331, 121.474566,  // 上海
                39.909676, 116.397775   // 北京
        );

        assertNotNull(distance, "距离不应为null");
        assertTrue(distance > 0, "距离应大于0");

        // 上海到北京的直线距离约1000公里
        assertTrue(distance > 800 && distance < 1200, "上海到北京距离应在合理范围内");
    }

    @Test
    void testCalculateHeuristicK() {
        // 测试启发式K值计算
        int k1 = (int) ReflectionTestUtils.invokeMethod(stationGroupPlanner, "calculateHeuristicK", 10);
        assertEquals(2, k1, "10个站点时K值应为2");

        int k2 = (int) ReflectionTestUtils.invokeMethod(stationGroupPlanner, "calculateHeuristicK", 50);
        assertEquals(6, k2, "50个站点时K值应为6");

        int k3 = (int) ReflectionTestUtils.invokeMethod(stationGroupPlanner, "calculateHeuristicK", 200);
        assertEquals(20, k3, "200个站点时K值应为20（最大限制）");
    }

    @Test
    void testStationToGroupMapping() {
        // 准备测试数据
        long[] stationIds = {1L, 2L, 3L};
        int[] clusterLabels = {0, 1, 0};  // 站点1和3在分组0，站点2在分组1

        // 通过反射调用私有方法
        Map<Long, Integer> mapping = (Map<Long, Integer>) ReflectionTestUtils.invokeMethod(
                stationGroupPlanner,
                "createStationGroupMapping",
                stationIds,
                clusterLabels
        );

        assertNotNull(mapping, "映射不应为null");
        assertEquals(3, mapping.size(), "应有3个映射");
        assertEquals(0, mapping.get(1L), "站点1应在分组0");
        assertEquals(1, mapping.get(2L), "站点2应在分组1");
        assertEquals(0, mapping.get(3L), "站点3应在分组0");
    }

    @Test
    void testGroupStationCount() {
        // 准备测试数据
        int[] clusterLabels = {0, 1, 0, 2, 0, 1};  // 6个站点分布在3个分组
        int k = 3;

        // 通过反射调用私有方法
        Map<Integer, Integer> countMap = (Map<Integer, Integer>) ReflectionTestUtils.invokeMethod(
                stationGroupPlanner,
                "calculateGroupStationCount",
                clusterLabels,
                k
        );

        assertNotNull(countMap, "计数映射不应为null");
        assertEquals(3, countMap.size(), "应有3个分组");
        assertEquals(3, countMap.get(0), "分组0应有3个站点");
        assertEquals(2, countMap.get(1), "分组1应有2个站点");
        assertEquals(1, countMap.get(2), "分组2应有1个站点");
    }

    // 辅助方法：创建测试站点
    private StationLocation createStation(Long id, double longitude, double latitude) {
        StationLocation station = new StationLocation(id, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude));
        return station;
    }
}