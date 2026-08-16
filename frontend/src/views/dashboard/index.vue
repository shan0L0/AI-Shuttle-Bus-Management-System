<template>
  <div class="dashboard-container">
    <h2 class="page-title">📊 数据总览</h2>
    
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="4" v-for="(stat, index) in statsCards" :key="index">
        <div class="stat-card" :class="stat.colorClass">
          <div class="stat-icon">{{ stat.icon }}</div>
          <div class="stat-info">
            <h3>{{ stat.value }}</h3>
            <p>{{ stat.label }}</p>
            <span class="stat-trend" :class="stat.trendClass">{{ stat.trend }}</span>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 图表区域 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>📈 本周乘坐率趋势</span>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>🏆 站点乘车人数TOP8</span>
          </template>
          <div ref="stationChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 今日班车状态 -->
    <el-card shadow="hover" class="status-card">
      <template #header>
        <div class="card-header">
          <span>🚌 今日班车状态</span>
          <el-button type="primary" size="small" @click="refreshStatus" :loading="loading">
            🔄 刷新
          </el-button>
        </div>
      </template>
      <el-table :data="todayStatus" stripe>
        <el-table-column prop="routeName" label="线路" width="180">
          <template #default="{ row }">
            <span :class="['route-dot', getRateClass(row.occupancyRate)]"></span>
            {{ row.routeName }}
          </template>
        </el-table-column>
        <el-table-column prop="vehiclePlate" label="车辆" width="120" />
        <el-table-column prop="driverName" label="驾驶员" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
              {{ row.status === 1 ? '运行中' : '待发车' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="passengers" label="乘客数" width="100">
          <template #default="{ row }">{{ row.passengers }}人</template>
        </el-table-column>
        <el-table-column label="乘坐率" width="180">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-progress
                :percentage="row.occupancyRate"
                :color="getRateColor(row.occupancyRate)"
                :stroke-width="8"
                style="width: 100px"
              />
              <span :style="{ color: getRateColor(row.occupancyRate) }">
                {{ row.occupancyRate }}%
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="departureTime" label="发车时间" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { dashboardApi } from '@/api/index'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const trendChartRef = ref(null)
const stationChartRef = ref(null)
let trendChart = null
let stationChart = null

defineOptions({
  name: 'Dashboard'  //定义组件名
})

// 统计卡片数据
const statsCards = reactive([
  { icon: '🚌', value: '12', label: '运营车辆', trend: '↑ 运行中 10辆', colorClass: 'blue', trendClass: 'up' },
  { icon: '👥', value: '526', label: '通勤员工', trend: '↑ 本月新增 12人', colorClass: 'green', trendClass: 'up' },
  { icon: '📍', value: '15', label: '服务站点', trend: '覆盖 5 个区域', colorClass: 'orange', trendClass: '' },
  { icon: '🛤️', value: '8', label: '运营线路', trend: '↑ 平均乘坐率 82%', colorClass: 'purple', trendClass: 'up' },
  { icon: '📈', value: '82.5%', label: '整体乘坐率', trend: '↑ 较上周+3.2%', colorClass: 'red', trendClass: 'up' },
  { icon: '💰', value: '¥15.8', label: '人均成本/天', trend: '↓ 较上月-8%', colorClass: 'cyan', trendClass: 'down' }
])

// 今日状态数据
const todayStatus = ref([
  { routeName: '1号线-天通苑方向', vehiclePlate: '京A12345', driverName: '张师傅', status: 1, passengers: 35, occupancyRate: 87.5, departureTime: '07:00' },
  { routeName: '2号线-回龙观方向', vehiclePlate: '京A12346', driverName: '李师傅', status: 1, passengers: 42, occupancyRate: 84.0, departureTime: '07:15' },
  { routeName: '3号线-望京方向', vehiclePlate: '京A12347', driverName: '王师傅', status: 1, passengers: 38, occupancyRate: 95.0, departureTime: '07:00' },
  { routeName: '4号线-通州方向', vehiclePlate: '京A12349', driverName: '刘师傅', status: 1, passengers: 40, occupancyRate: 80.0, departureTime: '06:45' },
  { routeName: '5号线-海淀方向', vehiclePlate: '京A12350', driverName: '陈师傅', status: 1, passengers: 21, occupancyRate: 52.5, departureTime: '07:30' },
  { routeName: '6号线-清河方向', vehiclePlate: '京A12351', driverName: '周师傅', status: 1, passengers: 28, occupancyRate: 80.0, departureTime: '07:15' },
  { routeName: '7号线-北苑方向', vehiclePlate: '京A12352', driverName: '吴师傅', status: 1, passengers: 29, occupancyRate: 58.0, departureTime: '07:00' },
  { routeName: '8号线-立水桥方向', vehiclePlate: '京A12348', driverName: '赵师傅', status: 0, passengers: 26, occupancyRate: 65.0, departureTime: '07:30' }
])

const getRateClass = (rate) => {
  if (rate >= 80) return 'high'
  if (rate >= 60) return 'medium'
  return 'low'
}

const getRateColor = (rate) => {
  if (rate >= 80) return '#52c41a'
  if (rate >= 60) return '#faad14'
  return '#ff4d4f'
}

const initCharts = () => {
  // 乘坐率趋势图
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['乘坐率', '目标值'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
    yAxis: { type: 'value', min: 40, max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      {
        name: '乘坐率',
        type: 'line',
        smooth: true,
        data: [78, 82, 85, 79, 88, 45, 42],
        itemStyle: { color: '#2E75B6' },
        areaStyle: { color: 'rgba(46, 117, 182, 0.1)' }
      },
      {
        name: '目标值',
        type: 'line',
        data: [85, 85, 85, 85, 85, 85, 85],
        itemStyle: { color: '#52c41a' },
        lineStyle: { type: 'dashed' }
      }
    ]
  })
  
  // 站点排行图
  stationChart = echarts.init(stationChartRef.value)
  const stationData = [
    { name: '西二旗站', value: 82 },
    { name: '回龙观站', value: 72 },
    { name: '天通苑站', value: 68 },
    { name: '望京站', value: 55 },
    { name: '龙泽站', value: 52 },
    { name: '通州站', value: 48 },
    { name: '上地站', value: 45 },
    { name: '立水桥站', value: 42 }
  ]
  stationChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '10%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: stationData.map(s => s.name).reverse() },
    series: [{
      type: 'bar',
      data: stationData.map(s => s.value).reverse(),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#2E75B6' },
          { offset: 1, color: '#667eea' }
        ])
      },
      label: { show: true, position: 'right', formatter: '{c}人' }
    }]
  })
}

const refreshStatus = async () => {
  loading.value = true
  try {
    // 实际项目中调用API
    // const res = await dashboardApi.getTodayStatus()
    // todayStatus.value = res.data
    ElMessage.success('数据已刷新')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initCharts()
  window.addEventListener('resize', () => {
    trendChart?.resize()
    stationChart?.resize()
  })
})

onUnmounted(() => {
  trendChart?.dispose()
  stationChart?.dispose()
})
</script>

<style lang="scss" scoped>
.dashboard-container {
  .page-title {
    margin: 0 0 20px;
    font-size: 20px;
    color: #333;
  }
  
  .stats-row {
    margin-bottom: 20px;
    
    .stat-card {
      background: white;
      border-radius: 12px;
      padding: 20px;
      display: flex;
      align-items: center;
      gap: 16px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      transition: all 0.3s;
      
      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
      }
      
      .stat-icon {
        width: 56px;
        height: 56px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
      }
      
      &.blue .stat-icon { background: linear-gradient(135deg, #667eea, #764ba2); }
      &.green .stat-icon { background: linear-gradient(135deg, #11998e, #38ef7d); }
      &.orange .stat-icon { background: linear-gradient(135deg, #f093fb, #f5576c); }
      &.purple .stat-icon { background: linear-gradient(135deg, #4facfe, #00f2fe); }
      &.red .stat-icon { background: linear-gradient(135deg, #fa709a, #fee140); }
      &.cyan .stat-icon { background: linear-gradient(135deg, #a8edea, #fed6e3); }
      
      .stat-info {
        h3 {
          font-size: 24px;
          margin: 0;
          color: #333;
        }
        p {
          margin: 4px 0;
          color: #666;
          font-size: 13px;
        }
        .stat-trend {
          font-size: 12px;
          &.up { color: #52c41a; }
          &.down { color: #ff4d4f; }
        }
      }
    }
  }
  
  .chart-container {
    height: 300px;
  }
  
  .status-card {
    margin-top: 20px;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
  
  .route-dot {
    display: inline-block;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    margin-right: 8px;
    
    &.high { background: #52c41a; }
    &.medium { background: #faad14; }
    &.low { background: #ff4d4f; }
  }
  
  .rate-cell {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}
</style>
