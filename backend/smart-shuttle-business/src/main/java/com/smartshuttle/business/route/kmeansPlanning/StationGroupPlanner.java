package com.smartshuttle.business.route.kmeansPlanning;

import com.smartshuttle.business.station.entity.StationLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import smile.clustering.*;
import com.smartshuttle.business.station.mapper.StationMapper;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class StationGroupPlanner {
    private final StationMapper stationMapper;
    private static final int MIN_STATION_PER_GROUP = 3;      // 每个站点组最少站点数
    private static final int MAX_STATION_PER_GROUP = 9;      // 每个站点最多服务人数

    // 终点站
    private final StationLocation terminalStation = new StationLocation(15L, BigDecimal.valueOf(116.557400), BigDecimal.valueOf(40.084200));

    public StationGroupPlanResult planStationGroups(){
        List<StationLocation> stnLocations = stationMapper.selectStnLocations();

        //1. 单独存储id用于后续映射
        long[] stnIds = stnLocations.stream()
                .mapToLong(StationLocation::getId)
                .toArray();

        //2. 构建极角聚类坐标数组 (cosθ, sinθ)
        double[][] polarCoordinates = buildUnitCircleCoordinates(stnLocations);

        //3. 计算最优分组数 K
        int optimalK = determineOptimalKWithElbowMethod(polarCoordinates, stnLocations.size());
        log.info("推荐分组数: {}", optimalK);

        //4. 使用极角坐标进行 K-Means 聚类
        CentroidClustering<double[], double[]> clusteringResult = performPolarKMeans(polarCoordinates, optimalK);

        //5. 将站点id与组中心编号对应
        Map<Long, Integer> stationToGroupMap = createStationGroupMapping(stnIds, clusteringResult.y);

        //6. 获取组中心点坐标（返回原始经纬度，用于地图显示）
        List<GroupCenter> groupCenters = extractGroupCentersFromPolarClustering(clusteringResult.centroids, stnLocations, clusteringResult.y);

        //7. 统计每个组的站点数量
        Map<Integer, Integer> groupStationCount = calculateGroupStationCount(clusteringResult.y, optimalK);

        //8. 计算每个站点到所属组的距离（单位：公里）
        Map<Long, Double> stationDistances = calculateDistancesToGroupCenter(stnLocations, groupCenters);

        return StationGroupPlanResult.success(
                groupCenters,
                stationToGroupMap,
                groupStationCount,
                stationDistances,
                optimalK,
                stnLocations.size()
        );
    }

    /**
     * 构建极角聚类坐标：将每个站点相对于终点站的角度转换为 (cosθ, sinθ)
     */
    private double[][] buildUnitCircleCoordinates(List<StationLocation> stations) {
        double[][] coordinates = new double[stations.size()][2];

        for (int i = 0; i < stations.size(); i++) {
            StationLocation stn = stations.get(i);

            // 计算站点相对于终点站的方位角（0-360°）
            double angle = calculateBearingToTerminal(stn);

            // 转换为弧度
            double rad = Math.toRadians(angle);

            // 极角聚类坐标：角度映射到单位圆上的点
            coordinates[i][0] = Math.cos(rad);
            coordinates[i][1] = Math.sin(rad);

            log.debug("站点{}: 角度={}°, cos={}, sin={}",
                    stn.getId(), String.format("%.2f", angle),
                    String.format("%.4f", coordinates[i][0]),
                    String.format("%.4f", coordinates[i][1]));
        }

        return coordinates;
    }

    /**
     * 计算站点相对于终点站的方位角（数学坐标系）
     * 0°=正东, 90°=正北, 180°=正西, 270°=正南
     * 角度逆时针增加
     */
    private double calculateBearingToTerminal(StationLocation station) {
        // 先获取地理方位角（北0°顺时针）
        double geoBearing = calculateBearing(
                terminalStation.getLatitude().doubleValue(),
                terminalStation.getLongitude().doubleValue(),
                station.getLatitude().doubleValue(),
                station.getLongitude().doubleValue()
        );

        // 转换为数学方位角（东0°逆时针）
        return convertGeoToMathBearing(geoBearing);
    }

    /**
     * 将地理方位角转换为数学方位角
     *
     * 地理：0°=北，顺时针增加
     * 数学：0°=东，逆时针增加
     *
     * 转换公式：mathAngle = 90° - geoAngle
     *
     * @param geoBearing 地理方位角（0-360°，0°=北，顺时针）
     * @return 数学方位角（0-360°，0°=东，逆时针）
     */
    private double convertGeoToMathBearing(double geoBearing) {
        double mathAngle = 90 - geoBearing;
        if (mathAngle < 0) {
            mathAngle += 360;
        }
        return mathAngle;
    }

    /**
     * 计算两个经纬度之间的方位角（正北为0度，顺时针增加）
     * lat1,lon1w为原点，lat2，lon2为站点
     */
    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double radLon1 = Math.toRadians(lon1);
        double radLon2 = Math.toRadians(lon2);

        //xy为站点在以lat1，lon1为切点的地球切平面上，以切点为原点，正东为x轴正方向，正北为y轴正方向的平面直角坐标系的投影
        double x = Math.cos(radLat2) * Math.sin(radLon2 - radLon1);
        double y = Math.cos(radLat1) * Math.sin(radLat2)
                - Math.sin(radLat1) * Math.cos(radLat2) * Math.cos(radLon2 - radLon1);

        double bearing = Math.toDegrees(Math.atan2(x, y));
        return (bearing + 360) % 360;
    }

    /**
     * 根据业务约束计算 K 值范围
     * @param totalPeople 员工总数
     * @return [minK, maxK]
     */
    private int[] calculateKRange(int totalPeople) {
        if(MAX_STATION_PER_GROUP < MIN_STATION_PER_GROUP){
            log.warn("业务约束有误，最大站点人数小于最小站点人数");
            return null;
        }
        // 基于业务约束计算最小和最大 K 值
        int minKByPeople = (int) Math.ceil((double) totalPeople / MAX_STATION_PER_GROUP);
        int maxKByPeople = (int) Math.floor((double) totalPeople / MIN_STATION_PER_GROUP);

        log.info("业务约束: 员工数={}, 每站点服务人数范围=[{}, {}], K值范围=[{}, {}]",
                totalPeople, MIN_STATION_PER_GROUP, MAX_STATION_PER_GROUP, minKByPeople, maxKByPeople);

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
        if (data.length <= MIN_STATION_PER_GROUP) {
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

    /**
     * 执行极角 K-Means 聚类
     */
    private CentroidClustering<double[], double[]> performPolarKMeans(double[][] polarCoordinates, int k) {
        log.info("开始极角K-Means聚类，K={}, 数据量={}", k, polarCoordinates.length);

        // 多次运行取最优结果（K-Means++ 本身已较好，多次运行进一步保证）
        int numRuns = calculateOptimalRuns(polarCoordinates.length);
        CentroidClustering<double[], double[]> bestResult = null;
        double bestDistortion = Double.MAX_VALUE;

        for (int run = 0; run < numRuns; run++) {
            try {
                CentroidClustering<double[], double[]> result = KMeans.fit(polarCoordinates, k);

                // 注意：此时的 distortion 是基于 (cosθ, sinθ) 坐标的欧氏距离
                // 它严格正相关于角度差，所以可以用作质量评估
                log.info("K-Means运行 {}: 失真度 = {}", run + 1, result.distortion);

                if (result.distortion < bestDistortion) {
                    bestDistortion = result.distortion;
                    bestResult = result;
                }
            } catch (Exception e) {
                log.warn("K-Means运行 {} 失败: {}", run + 1, e.getMessage());
            }
        }

        if (bestResult == null) {
            log.warn("所有K-Means运行失败，回退到单次运行");
            bestResult = KMeans.fit(polarCoordinates, k);
        }

        // 输出聚类质量：平均角度差（度）
        double avgAngleError = Math.toDegrees(Math.asin(Math.sqrt(bestDistortion / polarCoordinates.length / 4) * 2));
        log.info("极角聚类完成，平均角度偏差 ≈ {}°", String.format("%.2f", avgAngleError));

        return bestResult;
    }

    /**
     * 根据数据量计算运行次数
     */
    private int calculateOptimalRuns(int dataSize) {
        if (dataSize <= 10) return 3;
        else if (dataSize <= 50) return 5;
        else if (dataSize <= 200) return 7;
        else return 10;
    }

    /**
     * 从极角聚类结果中提取组中心点（转换为经纬度）
     *
     * 注意：聚类中心在 (cosθ, sinθ) 空间，需要：
     * 1. 计算中心点的角度 = atan2(中心y, 中心x)
     * 2. 沿着该方向，取组内站点的平均距离作为半径
     * 3. 计算中心点的经纬度
     */
    private List<GroupCenter> extractGroupCentersFromPolarClustering(
            double[][] centroids,
            List<StationLocation> stations,
            int[] labels) {

        List<GroupCenter> centers = new ArrayList<>();

        // 先按组收集站点
        Map<Integer, List<StationLocation>> groupToStations = new HashMap<>();
        for (int i = 0; i < stations.size(); i++) {
            groupToStations.computeIfAbsent(labels[i], k -> new ArrayList<>())
                    .add(stations.get(i));
        }

        // 为每个组计算中心点
        for (Map.Entry<Integer, List<StationLocation>> entry : groupToStations.entrySet()) {
            int groupId = entry.getKey();
            List<StationLocation> groupStations = entry.getValue();

            // 计算该组的平均角度（从聚类中心获取）
            double[] centroid = centroids[groupId];
            double centerAngle = Math.toDegrees(Math.atan2(centroid[1], centroid[0]));
            if (centerAngle < 0) centerAngle += 360;

            // 计算平均距离
            double avgDistance = 0;
            for (StationLocation stn : groupStations) {
                avgDistance += calculateDistanceToTerminal(stn);
            }
            avgDistance /= groupStations.size();

            // 根据角度和距离计算中心点的经纬度
            double[] centerLngLat = calculatePositionByAngleAndDistance(centerAngle, avgDistance);

            GroupCenter center = new GroupCenter(groupId, centerLngLat[0], centerLngLat[1]);
            centers.add(center);
        }

        // 按组ID排序
        centers.sort(Comparator.comparingInt(GroupCenter::getGroupId));
        return centers;
    }

    /**
     * 根据角度和距离计算点的经纬度
     * @param angle 方位角（度，0°=正北）
     * @param distance 距离（公里）
     * @return [经度, 纬度]
     */
    private double[] calculatePositionByAngleAndDistance(double angle, double distance) {
        double lat1 = Math.toRadians(terminalStation.getLatitude().doubleValue());
        double lon1 = Math.toRadians(terminalStation.getLongitude().doubleValue());

        double bearing = Math.toRadians(angle);
        double R = 6371; // 地球半径（公里）

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distance / R) +
                Math.cos(lat1) * Math.sin(distance / R) * Math.cos(bearing));

        double lon2 = lon1 + Math.atan2(Math.sin(bearing) * Math.sin(distance / R) * Math.cos(lat1),
                Math.cos(distance / R) - Math.sin(lat1) * Math.sin(lat2));

        return new double[]{Math.toDegrees(lon2), Math.toDegrees(lat2)};
    }

    /**
     * 将站点id与组中心编号对应
     */
    private Map<Long, Integer> createStationGroupMapping(long[] stnIds, int[] clusterLabels) {
        Map<Long, Integer> mapping = new HashMap<>();
        for (int i = 0; i < stnIds.length; i++) {
            mapping.put(stnIds[i], clusterLabels[i]);
        }
        log.info("站点-分组映射创建完成，共 {} 个映射", mapping.size());
        return mapping;
    }

    /**
     * 统计每个组的站点数量
     */
    private Map<Integer, Integer> calculateGroupStationCount(int[] clusterLabels, int k) {
        Map<Integer, Integer> countMap = new HashMap<>();
        for (int i = 0; i < k; i++) {
            countMap.put(i, 0);
        }
        for (int label : clusterLabels) {
            countMap.put(label, countMap.get(label) + 1);
        }
        return countMap;
    }

    /**
     * 计算每个站点到所属组中心的距离
     */
    private Map<Long, Double> calculateDistancesToGroupCenter(
            List<StationLocation> stations,
            List<GroupCenter> groupCenters) {

        Map<Long, Double> distances = new HashMap<>();

        // 构建组ID到中心点的映射
        Map<Integer, GroupCenter> centerMap = new HashMap<>();
        for (GroupCenter center : groupCenters) {
            centerMap.put(center.getGroupId(), center);
        }

        // 需要知道每个站点属于哪个组，这里通过传入的 stationToGroup 在外部构建
        // 注意：这个方法会被 StationGroupPlanResult.success 调用，需要传入 stationToGroup

        return distances;
    }

    /**
     * 计算站点到终点站的距离（公里）
     */
    private double calculateDistanceToTerminal(StationLocation station) {
        return haversineDistance(
                station.getLatitude().doubleValue(),
                station.getLongitude().doubleValue(),
                terminalStation.getLatitude().doubleValue(),
                terminalStation.getLongitude().doubleValue()
        );
    }

    /**
     * 计算两个经纬度之间的 Haversine 距离（公里）
     */
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

    // ==================== 保留原有方法（兼容性） ====================

    private List<Long> makeStationList(List<Long> stationIds) {
        double farthestDistance = 0;
        int bestI = 0;
        for (int i = 0; i < stationIds.size(); i++) {
            StationLocation station = stationMapper.selectStnLocationById(stationIds.get(i)).getFirst();
            if (calculateDistanceToTerminal(station) > farthestDistance) {
                farthestDistance = calculateDistanceToTerminal(station);
                bestI = i;
            }
        }
        stationIds.addFirst(stationIds.get(bestI));
        stationIds.remove(bestI + 1);
        stationIds.add(terminalStation.getId());
        return stationIds;
    }

    public Map<Integer, List<Long>> makeGroupMap(Map<Long, Integer> stationToGroup) {
        Map<Integer, List<Long>> groupToStations = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : stationToGroup.entrySet()) {
            Long stationId = entry.getKey();
            Integer groupId = entry.getValue();
            groupToStations.computeIfAbsent(groupId, k -> new ArrayList<>()).add(stationId);
        }
        for (Map.Entry<Integer, List<Long>> entry : groupToStations.entrySet()) {
            List<Long> list = entry.getValue();
            groupToStations.replace(entry.getKey(), makeStationList(list));
        }
        return groupToStations;
    }

    // ==================== 内部数据类 ====================

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class StationGroupPlanResult {
        private boolean success;
        private String message;
        private List<GroupCenter> groupCenters;
        private Map<Long, Integer> stationToGroup;
        private Map<Integer, Integer> groupStationCount;
        private Map<Long, Double> stationDistances;
        private int groupCount;
        private int stationCount;
        private Date planTime;

        public static StationGroupPlanResult success(
                List<GroupCenter> groupCenters,
                Map<Long, Integer> stationToGroup,
                Map<Integer, Integer> groupStationCount,
                Map<Long, Double> stationDistances,
                int groupCount,
                int stationCount
        ) {
            return new StationGroupPlanResult(
                    true,
                    "站点分组规划成功",
                    groupCenters,
                    stationToGroup,
                    groupStationCount,
                    stationDistances,
                    groupCount,
                    stationCount,
                    new Date()
            );
        }

        public static StationGroupPlanResult failure(String message) {
            return new StationGroupPlanResult(
                    false,
                    message,
                    Collections.emptyList(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    0,
                    0,
                    new Date()
            );
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class GroupCenter {
        private int groupId;
        private double longitude;
        private double latitude;
        private String address;

        public GroupCenter(int groupId, double longitude, double latitude) {
            this.groupId = groupId;
            this.longitude = longitude;
            this.latitude = latitude;
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class StationAssignment {
        private Long stationId;
        private Integer groupId;
        private double distanceToGroup;

        public static StationAssignment from(StationLocation stn, Integer groupId, Double distance) {
            return new StationAssignment(
                    stn.getId(),
                    groupId,
                    distance != null ? distance : 0.0
            );
        }
    }
}