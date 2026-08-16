package com.smartshuttle.business.station.kmeans_planning;

import com.smartshuttle.business.employee.entity.EmployeeLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import smile.clustering.CentroidClustering;
import smile.clustering.KMeans;
import com.smartshuttle.business.employee.mapper.EmployeeMapper;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class Planner {

    private final EmployeeMapper employeeMapper;

    // ==================== 业务约束参数 ====================
    private static final int MIN_PEOPLE_PER_STATION = 3;      // 每个站点最少服务人数
    private static final int MAX_PEOPLE_PER_STATION = 6;      // 每个站点最多服务人数

    public StationPlanResult planStations() {
        List<EmployeeLocation> empLocations = employeeMapper.selectEmpLocations();

        if (empLocations == null || empLocations.isEmpty()) {
            log.warn("员工位置数据为空，无法规划站点");
            return StationPlanResult.failure("员工位置数据为空");
        }

        // 1. 单独存储id用于后续映射簇编号
        long[] empIds = empLocations.stream()
                .mapToLong(EmployeeLocation::getId)
                .toArray();

        // 2. 构建二维坐标数组用于聚类
        double[][] coordinates = empLocations.stream()
                .map(emp -> lngLatToWebMercator(
                        emp.getLongitude().doubleValue(),
                        emp.getLatitude().doubleValue()
                ))
                .toArray(double[][]::new);

        // 3. 【修改】使用手肘法确定最佳站点数
        int optimalK = determineOptimalKWithElbowMethod(coordinates, empLocations.size());
        log.info("手肘法推荐站点数: {}", optimalK);

        // 4. K-Means++聚类（多次运行取最优）
        CentroidClustering<double[], double[]> clusteringResult = performMultipleKMeansRuns(coordinates, optimalK);

        // 5. 将员工id与簇中心编号对应
        Map<Long, Integer> employeeToStationMap = createEmployeeStationMapping(empIds, clusteringResult.y);

        // 6. 获取站点中心点坐标
        List<StationCenter> stationCenters = extractStationCenters(clusteringResult.centroids);

        // 7. 统计每个站点的员工数量
        Map<Integer, Integer> stationEmployeeCount = calculateStationEmployeeCount(clusteringResult.y, optimalK);

        // 8. 计算每个员工到所属站点的距离
        Map<Long, Double> employeeDistances = calculateDistancesToStation(
                empLocations, batchXYToLngLat(clusteringResult.centroids), clusteringResult.y
        );

        return StationPlanResult.success(
                stationCenters,
                employeeToStationMap,
                stationEmployeeCount,
                employeeDistances,
                optimalK,
                empLocations.size()
        );
    }

    // ==================== 手肘法核心实现 ====================

    /**
     * 根据业务约束计算 K 值范围
     * @param totalPeople 员工总数
     * @return [minK, maxK]
     */
    private int[] calculateKRange(int totalPeople) {
        if(MAX_PEOPLE_PER_STATION < MIN_PEOPLE_PER_STATION){
            log.warn("业务约束有误，最大站点人数小于最小站点人数");
            return null;
        }
        // 基于业务约束计算最小和最大 K 值
        int minKByPeople = (int) Math.ceil((double) totalPeople / MAX_PEOPLE_PER_STATION);
        int maxKByPeople = (int) Math.floor((double) totalPeople / MIN_PEOPLE_PER_STATION);

        log.info("业务约束: 员工数={}, 每站点服务人数范围=[{}, {}], K值范围=[{}, {}]",
                totalPeople, MIN_PEOPLE_PER_STATION, MAX_PEOPLE_PER_STATION, minKByPeople, maxKByPeople);

        return new int[]{minKByPeople, maxKByPeople};
    }

    /**
     * 使用手肘法确定最优 K 值
     * @param data 坐标数据（Web Mercator投影）
     * @param totalPeople 员工总数
     * @return 最优 K 值
     */
    private int determineOptimalKWithElbowMethod(double[][] data, int totalPeople) {
        if (data == null || data.length == 0) {
            log.warn("数据为空，使用默认K=3");
            return 3;
        }

        // 1. 根据业务约束计算 K 值范围
        int[] kRange = calculateKRange(totalPeople);
        if(kRange == null){
            log.warn("站点人数约束有误，使用默认K=3");
            return 3;
        }
        int minK = kRange[0];
        int maxK = kRange[1];

        // 数据量很小时的特殊处理
        if (data.length <= MIN_PEOPLE_PER_STATION) {
            log.info("员工数较少({})，直接使用1个站点", data.length);
            return 1;
        }

        if (maxK <= minK) {
            log.info("业务约束下K值范围过小，直接使用K={}", minK);
            return minK;
        }

        log.info("开始手肘法评估，K值范围: {} ~ {}", minK, maxK);

        // 2. 对每个 K 值，运行多次 K-Means 取最优 SSE
        Map<Integer, Double> bestSSEForK = new HashMap<>();

        for (int k = minK; k <= maxK; k++) {
            double bestSSE = Double.MAX_VALUE;

            // 对每个 K 运行多次 K-Means
            for (int run = 0; run < calculateOptimalRuns(data.length); run++) {
                try {
                    CentroidClustering<double[], double[]> result = KMeans.fit(data, k);
                    if (result.distortion < bestSSE) {
                        bestSSE = result.distortion;//记录最佳失真度
                    }
                } catch (Exception e) {
                    log.warn("K={}, 第{}次运行失败: {}", k, run + 1, e.getMessage());
                }
            }

            if (bestSSE == Double.MAX_VALUE) {
                log.warn("K={} 所有运行均失败，使用默认值", k);
                bestSSE = 0;
            }

            bestSSEForK.put(k, bestSSE);//失真度map
            log.info("K={}, 最佳SSE={}", k, bestSSE);
        }

        // 3. 检测拐点（手肘点）
        int optimalK = detectElbow(bestSSEForK);

        log.info("手肘法确定最优K值: {}", optimalK);
        return optimalK;
    }

    /**
     * 检测手肘拐点（基于肘点定义：点前斜率为负，且点后斜率 - 点前斜率 > 0）
     * 从所有符合条件的肘点中，选择（点后斜率 - 点前斜率）最大的点作为最终结果
     * @param sseMap K -> SSE 的映射
     * @return 最优K值
     */
    private int detectElbow(Map<Integer, Double> sseMap) {
        List<Integer> ks = new ArrayList<>(sseMap.keySet());
        Collections.sort(ks);

        if (ks.size() < 3) {
            // 数据点太少，返回中位数
            return ks.get(ks.size() / 2);
        }

        double maxSlopeChange = -Double.MAX_VALUE;
        int optimalK = ks.get(0);

        for (int i = 1; i < ks.size() - 1; i++) {
            int kPrev = ks.get(i - 1);
            int kCurr = ks.get(i);
            int kNext = ks.get(i + 1);

            double ssePrev = sseMap.get(kPrev);
            double sseCurr = sseMap.get(kCurr);
            double sseNext = sseMap.get(kNext);

            // 避免除零
            if (kCurr - kPrev == 0 || kNext - kCurr == 0) continue;

            // 计算前一段和后一段的斜率
            double slopePrev = (sseCurr - ssePrev) / (kCurr - kPrev);
            double slopeNext = (sseNext - sseCurr) / (kNext - kCurr);

            // 肘点条件：点前斜率为负，且点后斜率 - 点前斜率 > 0
            if (slopePrev < 0 && (slopeNext - slopePrev) > 0) {
                double slopeChange = slopeNext - slopePrev;

                if (slopeChange > maxSlopeChange) {
                    maxSlopeChange = slopeChange;
                    optimalK = kCurr;
                }
            }
        }

        return optimalK;
    }

    // ==================== 原有方法（保持不变） ====================

    /**
     * 多次运行K-Means算法，选择失真度最小的结果
     */
    private CentroidClustering<double[], double[]> performMultipleKMeansRuns(double[][] data, int k) {
        if (data == null || data.length == 0 || k <= 0) {
            throw new IllegalArgumentException("无效的输入参数");
        }

        int numRuns = calculateOptimalRuns(data.length);
        log.info("将进行 {} 次K-Means运行，选择最优结果", numRuns);

        CentroidClustering<double[], double[]> bestResult = null;
        double bestDistortion = Double.MAX_VALUE;

        for (int run = 0; run < numRuns; run++) {
            try {
                CentroidClustering<double[], double[]> clusteringResult = KMeans.fit(data, k);
                log.info("K-Means运行 {}: 失真度 = {}", run + 1, clusteringResult.distortion);

                if (clusteringResult.distortion < bestDistortion) {
                    bestDistortion = clusteringResult.distortion;
                    bestResult = clusteringResult;
                    log.info("发现新的最佳结果，失真度: {}", bestDistortion);
                }
            } catch (Exception e) {
                log.warn("K-Means运行 {} 失败: {}", run + 1, e.getMessage());
            }
        }

        if (bestResult == null) {
            log.warn("所有K-Means运行都失败，回退到单次运行");
            CentroidClustering<double[], double[]> fallbackResult = KMeans.fit(data, k);
            // 打印回退结果的平均距离
            printAverageDistance(fallbackResult.distortion, data.length);
            return fallbackResult;
        }

        // 打印最佳结果的平均距离（Web Mercator坐标，单位：米）
        printAverageDistance(bestDistortion, data.length);

        log.info("最终选择的K-Means结果，失真度: {}", bestDistortion);
        return bestResult;
    }

    /**
     * 根据失真度计算并打印平均距离（适用于Web Mercator坐标）
     * @param distortion 失真度（SSE），单位：平方米
     * @param dataSize 数据点数量
     */
    private void printAverageDistance(double distortion, int dataSize) {
        if (dataSize <= 0) {
            log.warn("数据点数量无效，无法计算平均距离");
            return;
        }

        double averageDistanceMeters = Math.sqrt(distortion / dataSize);
        double averageDistanceKm = averageDistanceMeters / 1000;

        // SLF4J 占位符是 {}，不是 {:.2f}
        log.info("平均距离 ≈ {} 米 ({} 公里)",
                String.format("%.2f", averageDistanceMeters),
                String.format("%.2f", averageDistanceKm));
    }

    private int calculateOptimalRuns(int dataSize) {
        if (dataSize <= 10) return 3;
        else if (dataSize <= 50) return 5;
        else if (dataSize <= 200) return 7;
        else return 10;
    }

    private double[] lngLatToWebMercator(double longitude, double latitude) {
        double x = longitude * 20037508.34 / 180;
        double rad = Math.toRadians(latitude);
        double y = Math.log(Math.tan(Math.PI/4 + rad/2)) * 20037508.34 / Math.PI;
        return new double[]{x, y};
    }

    private double webMercatorToLng(double x) {
        return x * 180 / 20037508.34;
    }

    private double webMercatorToLat(double y) {
        double yRad = y * Math.PI / 20037508.34;
        return Math.atan(Math.exp(yRad)) * 360 / Math.PI - 90;
    }

    public double[][] batchXYToLngLat(double[][] xyArray) {
        if (xyArray == null || xyArray.length == 0) {
            return new double[0][0];
        }
        double[][] lngLatArray = new double[xyArray.length][2];
        for (int i = 0; i < xyArray.length; i++) {
            if (xyArray[i] == null || xyArray[i].length < 2) {
                lngLatArray[i] = new double[]{0.0, 0.0};
                continue;
            }
            lngLatArray[i] = new double[]{webMercatorToLng(xyArray[i][0]), webMercatorToLat(xyArray[i][1])};
        }
        return lngLatArray;
    }

    private Map<Long, Integer> createEmployeeStationMapping(long[] empIds, int[] clusterLabels) {
        Map<Long, Integer> mapping = new HashMap<>();
        for (int i = 0; i < empIds.length; i++) {
            mapping.put(empIds[i], clusterLabels[i]);
        }
        return mapping;
    }

    private List<StationCenter> extractStationCenters(double[][] centroids) {
        List<StationCenter> centers = new ArrayList<>();
        for (int i = 0; i < centroids.length; i++) {
            centers.add(new StationCenter(i, webMercatorToLng(centroids[i][0]), webMercatorToLat(centroids[i][1])));
        }
        return centers;
    }

    //计算每个站点有多少人
    private Map<Integer, Integer> calculateStationEmployeeCount(int[] clusterLabels, int k) {
        Map<Integer, Integer> countMap = new HashMap<>();
        for (int i = 0; i < k; i++) countMap.put(i, 0);
        for (int label : clusterLabels) countMap.put(label, countMap.get(label) + 1);
        return countMap;
    }

    private Map<Long, Double> calculateDistancesToStation(
            List<EmployeeLocation> employees,
            double[][] centroids,
            int[] clusterLabels
    ) {
        Map<Long, Double> distances = new HashMap<>();
        for (int i = 0; i < employees.size(); i++) {
            EmployeeLocation emp = employees.get(i);
            int stationId = clusterLabels[i];
            double[] stationCoord = centroids[stationId];
            double distance = haversineDistance(
                    emp.getLatitude().doubleValue(),
                    emp.getLongitude().doubleValue(),
                    stationCoord[1], stationCoord[0]
            );
            distances.put(emp.getId(), distance);
        }
        return distances;
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ==================== 内部数据类（保持不变） ====================

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class StationPlanResult {
        private boolean success;
        private String message;
        private List<StationCenter> stationCenters;
        private Map<Long, Integer> employeeToStation;
        private Map<Integer, Integer> stationEmployeeCount;
        private Map<Long, Double> employeeDistances;
        private int stationCount;
        private int employeeCount;
        private Date planTime;

        public static StationPlanResult success(
                List<StationCenter> stationCenters,
                Map<Long, Integer> employeeToStation,
                Map<Integer, Integer> stationEmployeeCount,
                Map<Long, Double> employeeDistances,
                int stationCount,
                int employeeCount
        ) {
            return new StationPlanResult(
                    true, "站点规划成功",
                    stationCenters, employeeToStation, stationEmployeeCount,
                    employeeDistances, stationCount, employeeCount, new Date()
            );
        }

        public static StationPlanResult failure(String message) {
            return new StationPlanResult(
                    false, message,
                    Collections.emptyList(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    0, 0, new Date()
            );
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class StationCenter {
        private int stationId;
        private double longitude;
        private double latitude;
        private String address;

        public StationCenter(int stationId, double longitude, double latitude) {
            this.stationId = stationId;
            this.longitude = longitude;
            this.latitude = latitude;
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class EmployeeAssignment {
        private Long employeeId;
        private Integer stationId;
        private double distanceToStation;

        public static EmployeeAssignment from(EmployeeLocation emp, Integer stationId, Double distance) {
            return new EmployeeAssignment(emp.getId(), stationId, distance != null ? distance : 0.0);
        }
    }
}