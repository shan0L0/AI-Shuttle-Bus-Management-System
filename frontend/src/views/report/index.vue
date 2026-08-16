<template>
  <div class="report-container">
    <h2 class="page-title">📈 数据报表</h2>
    
    <!-- 筛选区 -->
    <el-card shadow="hover" class="filter-card">
      <el-form :inline="true">
        <el-form-item label="报表类型">
          <el-select v-model="reportType" style="width: 150px" @change="loadReport">
            <el-option label="综合报表" value="comprehensive" />
            <el-option label="线路报表" value="route" />
            <el-option label="站点报表" value="station" />
            <el-option label="成本报表" value="cost" />
          </el-select>
        </el-form-item>
        <el-form-item label="统计周期">
          <el-select v-model="period" style="width: 120px" @change="loadReport">
            <el-option label="本周" value="week" />
            <el-option label="本月" value="month" />
            <el-option label="本季度" value="quarter" />
            <el-option label="本年度" value="year" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadReport">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
          <el-button type="success" @click="exportReport">
            <el-icon><Download /></el-icon> 导出Excel
          </el-button>
          <el-button type="warning" @click="generateAIReport">
            <el-icon><ChatDotRound /></el-icon> AI分析
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 图表区 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>📊 线路乘坐率对比</span></template>
          <div ref="routeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>💰 月度成本趋势</span></template>
          <div ref="costChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 数据表格 -->
    <el-card shadow="hover" class="table-card">
      <template #header><span>📋 月度运营统计</span></template>
      <el-table :data="tableData" stripe show-summary :summary-method="getSummaries">
        <el-table-column prop="month" label="月份" width="100" />
        <el-table-column prop="operatingDays" label="运营天数" width="100" />
        <el-table-column prop="totalTrips" label="班次" width="100" />
        <el-table-column prop="totalPassengers" label="乘客人次" width="120" />
        <el-table-column label="乘坐率" width="150">
          <template #default="{ row }">
            <el-progress :percentage="row.occupancyRate" :color="getRateColor(row.occupancyRate)" />
          </template>
        </el-table-column>
        <el-table-column prop="totalCost" label="运营成本(元)" width="120">
          <template #default="{ row }">¥{{ row.totalCost.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="人均成本(元/天)" width="130">
          <template #default="{ row }">¥{{ row.costPerPerson.toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- AI分析结果 -->
    <el-card v-if="aiAnalysis" shadow="hover" class="ai-card">
      <template #header><span>🤖 AI智能分析</span></template>
      <div class="ai-content" v-html="formatMarkdown(aiAnalysis)"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'

const reportType = ref('comprehensive')
const period = ref('month')
const aiAnalysis = ref('')
const routeChartRef = ref(null)
const costChartRef = ref(null)
let routeChart = null
let costChart = null

const tableData = ref([
  { month: '2024-07', operatingDays: 22, totalTrips: 352, totalPassengers: 11572, occupancyRate: 82.5, totalCost: 186500, costPerPerson: 16.12 },
  { month: '2024-08', operatingDays: 23, totalTrips: 368, totalPassengers: 12180, occupancyRate: 84.2, totalCost: 192800, costPerPerson: 15.83 },
  { month: '2024-09', operatingDays: 21, totalTrips: 336, totalPassengers: 11235, occupancyRate: 81.8, totalCost: 178600, costPerPerson: 15.90 },
  { month: '2024-10', operatingDays: 22, totalTrips: 352, totalPassengers: 11890, occupancyRate: 85.3, totalCost: 185200, costPerPerson: 15.58 },
  { month: '2024-11', operatingDays: 22, totalTrips: 352, totalPassengers: 12450, occupancyRate: 88.1, totalCost: 188900, costPerPerson: 15.17 },
  { month: '2024-12', operatingDays: 15, totalTrips: 240, totalPassengers: 8120, occupancyRate: 85.0, totalCost: 128500, costPerPerson: 15.82 }
])

const getRateColor = (rate) => {
  if (rate >= 85) return '#52c41a'
  if (rate >= 75) return '#faad14'
  return '#ff4d4f'
}

const formatMarkdown = (text) => marked.parse(text)

const initCharts = () => {
  // 线路乘坐率对比
  routeChart = echarts.init(routeChartRef.value)
  routeChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['1号线', '2号线', '3号线', '4号线', '5号线', '6号线', '7号线', '8号线'] },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [{
      type: 'bar',
      data: [
        { value: 87.5, itemStyle: { color: '#52c41a' } },
        { value: 84.0, itemStyle: { color: '#52c41a' } },
        { value: 95.0, itemStyle: { color: '#52c41a' } },
        { value: 80.0, itemStyle: { color: '#52c41a' } },
        { value: 52.5, itemStyle: { color: '#ff4d4f' } },
        { value: 80.0, itemStyle: { color: '#52c41a' } },
        { value: 58.0, itemStyle: { color: '#ff4d4f' } },
        { value: 65.0, itemStyle: { color: '#faad14' } }
      ],
      label: { show: true, position: 'top', formatter: '{c}%' },
      markLine: {
        data: [{ yAxis: 85, name: '目标', lineStyle: { color: '#52c41a', type: 'dashed' } }]
      }
    }]
  })
  
  // 月度成本趋势
  costChart = echarts.init(costChartRef.value)
  costChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['运营成本', '人均成本'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['7月', '8月', '9月', '10月', '11月', '12月'] },
    yAxis: [
      { type: 'value', name: '总成本(万元)', position: 'left' },
      { type: 'value', name: '人均(元/天)', position: 'right', min: 14, max: 18 }
    ],
    series: [
      {
        name: '运营成本',
        type: 'bar',
        data: [18.65, 19.28, 17.86, 18.52, 18.89, 12.85],
        itemStyle: { color: '#2E75B6' }
      },
      {
        name: '人均成本',
        type: 'line',
        yAxisIndex: 1,
        data: [16.12, 15.83, 15.90, 15.58, 15.17, 15.82],
        itemStyle: { color: '#52c41a' },
        smooth: true
      }
    ]
  })
}

const loadReport = () => {
  ElMessage.success('报表数据已刷新')
}

const exportReport = () => {
  ElMessage.success('报表导出成功')
}

const generateAIReport = () => {
// 先显示加载状态
  aiAnalysis.value = "⏳ AI正在分析数据，请稍候..."
  
  // 模拟延时
  setTimeout(() => {
    aiAnalysis.value = `## 📊 运营数据分析报告

### 整体表现
本月运营整体表现**良好**，平均乘坐率达到 **85.0%**，较上月提升 3.1%。

### 主要发现
1. **高效线路**：3号线乘坐率达95%，表现优异
2. **待优化线路**：5号线(52.5%)和7号线(58%)需重点关注
3. **成本控制**：人均成本15.82元/天，处于合理区间

### 优化建议
- 🔄 考虑将5号线合并至相邻线路
- ⏰ 调整7号线发车时间至7:30
- 📍 在员工密集区域增设新站点

### 预期效果
实施以上优化后，预计整体乘坐率可提升至 **88%** 以上，年节省成本约 **8.5万元**。`
    
    ElMessage.success('AI分析完成')
  }, 10000)
}

const getSummaries = ({ columns, data }) => {
  const sums = ['合计']
  columns.forEach((column, index) => {
    if (index === 0) return
    const values = data.map(item => Number(item[column.property]))
    if (column.property === 'occupancyRate') {
      sums[index] = (values.reduce((a, b) => a + b, 0) / values.length).toFixed(1) + '%'
    } else if (column.property === 'totalCost') {
      sums[index] = '¥' + values.reduce((a, b) => a + b, 0).toLocaleString()
    } else if (column.property === 'costPerPerson') {
      sums[index] = '-'
    } else if (!isNaN(values[0])) {
      sums[index] = values.reduce((a, b) => a + b, 0)
    } else {
      sums[index] = ''
    }
  })
  return sums
}

onMounted(() => {
  initCharts()
  window.addEventListener('resize', () => {
    routeChart?.resize()
    costChart?.resize()
  })
})

onUnmounted(() => {
  routeChart?.dispose()
  costChart?.dispose()
})
</script>

<style lang="scss" scoped>
.report-container {
  .page-title { margin: 0 0 20px; font-size: 20px; color: #333; }
  .filter-card { margin-bottom: 20px; }
  .chart-container { height: 300px; }
  .table-card { margin-top: 20px; }
  
  .ai-card {
    margin-top: 20px;
    .ai-content {
      background: linear-gradient(135deg, #f5f7fa, #e4e8ed);
      border-radius: 12px; padding: 20px; line-height: 1.8;
      :deep(h2), :deep(h3) { color: #2E75B6; margin-top: 16px; }
      :deep(li) { margin: 8px 0; }
      :deep(strong) { color: #2E75B6; }
    }
  }
}
</style>
