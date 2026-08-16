<template>
  <div class="employee-container">
    <h2 class="page-title">👥 员工管理</h2>
    
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card blue">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">通勤员工总数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card green">
          <div class="stat-value">{{ stats.normal }}</div>
          <div class="stat-label">正常乘车</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card orange">
          <div class="stat-value">{{ stats.leave }}</div>
          <div class="stat-label">今日请假</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card purple">
          <div class="stat-value">{{ stats.business }}</div>
          <div class="stat-label">出差中</div>
        </div>
      </el-col>
    </el-row>
    
    <el-card shadow="hover">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="search-area">
          <el-input v-model="searchForm.name" placeholder="搜索姓名/工号" style="width: 180px" clearable>
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="searchForm.department" placeholder="部门" clearable style="width: 140px">
            <el-option label="技术部" value="技术部" />
            <el-option label="产品部" value="产品部" />
            <el-option label="市场部" value="市场部" />
            <el-option label="运营部" value="运营部" />
            <el-option label="财务部" value="财务部" />
            <el-option label="人事部" value="人事部" />
          </el-select>
          <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="在职" :value="1" />
            <el-option label="请假" :value="2" />
            <el-option label="出差" :value="3" />
          </el-select>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </div>
        <div class="btn-group">
          <el-button type="success" @click="syncHR">
            <el-icon><Refresh /></el-icon> 同步HR数据
          </el-button>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增员工
          </el-button>
        </div>
      </div>
      
      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="employeeNo" label="工号" width="100">
          <template #default="{ row }">
            <span class="emp-no">{{ row.employeeNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="stationName" label="乘车站点" width="140" />
        <el-table-column prop="routeName" label="所属线路" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="handleLeave(row)">请假</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
      />
    </el-card>
    
    <!-- 地图模块 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card shadow="hover" class="map-card">
          <template #header>
            <div class="card-header">
              <span>🗺️ 员工分布地图</span>
              <el-button 
                type="primary" 
                size="small"
                @click="handlePlanning"
              >
              生成站点规划
              </el-button>
            </div>
          </template>
          <div id="employeeMap" class="map-container"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item label="工号" prop="employeeNo">
          <el-input v-model="formData.employeeNo" placeholder="如：E001" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" placeholder="员工姓名" />
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-select v-model="formData.department" placeholder="选择部门" style="width: 100%">
            <el-option label="技术部" value="技术部" />
            <el-option label="产品部" value="产品部" />
            <el-option label="市场部" value="市场部" />
            <el-option label="运营部" value="运营部" />
            <el-option label="财务部" value="财务部" />
            <el-option label="人事部" value="人事部" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="formData.phone" placeholder="手机号码" />
        </el-form-item>
        <el-form-item label="乘车站点">
          <el-select v-model="formData.stationId" placeholder="选择站点" style="width: 100%">
            <el-option v-for="s in stations" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属线路">
          <el-select v-model="formData.routeId" placeholder="选择线路" style="width: 100%">
            <el-option v-for="r in routes" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AMapLoader from '@amap/amap-jsapi-loader'
import { employeeApi, stationApi, routeApi } from '@/api/index'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增员工')
const formRef = ref(null)

const stats = reactive({ total: 526, normal: 498, leave: 18, business: 10 })

const searchForm = reactive({
  name: '',
  department: '',
  status: null
})

const pagination = reactive({ pageNum: 1, pageSize: 10, total: 8 })

const stations = ref([
  { id: 1, name: '天通苑站' }, { id: 2, name: '回龙观站' }, { id: 3, name: '望京站' },
  { id: 4, name: '通州站' }, { id: 5, name: '西二旗站' }, { id: 6, name: '上地站' }
])

const routes = ref([
  { id: 1, name: '1号线-天通苑方向' }, { id: 2, name: '2号线-回龙观方向' },
  { id: 3, name: '3号线-望京方向' }, { id: 4, name: '4号线-通州方向' }
])

const tableData = ref([])
const planResult = ref([])
const mapData = ref([])

let AMapInstance = null
let map = null
let employeeMarkers = []
let planningMarkers = []

const getEmployees = async () => {
  try {
    const response = await employeeApi.getList({
      pageNum: 1,
      pageSize: 10,
      name: '',
      stationId: '',
      status: '',
    })
    console.log('完整响应:', response);
    tableData.value = response.data.records;
  } catch (error) {
    console.error('加载失败:', error);
  } finally {
  }
}

const getMapData = async () => {
  try {
    const response = await employeeApi.getAll()
    console.log('完整响应1:', response);
    mapData.value = response.data;
    console.log('mapData:', mapData);
  } catch (error) {
    console.error('加载失败:', error);
  } finally {
  }
}

const getStations = async () => {
  try {
    const response = await stationApi.getList({
      pageNum: 1,
      pageSize: 10,
      name: '',
      district: '',
      status: '',
    })
    console.log('完整响应:', response);
    stations.value = response.data.records;
  } catch (error) {
    console.error('加载失败:', error);
  } finally {
  }
}

const formData = reactive({
  id: null, employeeNo: '', name: '', department: '', phone: '', stationId: null, routeId: null
})

const rules = {
  employeeNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  department: [{ required: true, message: '请选择部门', trigger: 'change' }]
}

const getStatusType = (status) => ({ 1: 'success', 2: 'warning', 3: 'info' }[status] || 'info')
const getStatusText = (status) => ({ 1: '在职', 2: '请假', 3: '出差' }[status] || '未知')

// 初始化地图
const initMap = async () => {
  try {
    const AMap = await AMapLoader.load({
      key: 'c63138579e021b08baa2a4634dc796fd',
      version: '2.0',
      plugins: ['AMap.Scale', 'AMap.ToolBar']
    })
    AMapInstance = AMap
    map = new AMap.Map('employeeMap', {
      viewMode: '2D',
      zoom: 11,
      center: [116.4074, 40.0242],
      mapStyle: 'amap://styles/light'
    })
    
    map.addControl(new AMap.Scale())
    map.addControl(new AMap.ToolBar({ position: 'RT' }))
    
    // 为所有员工添加标记
    addEmployeeMarkers(AMap)
    
    map.setFitView()
  } catch (error) {
    console.error('地图加载失败:', error)
  }
}

// 添加员工标记
const addEmployeeMarkers = (AMap) => {
  // 清除现有标记
  employeeMarkers.forEach(m => map.remove(m))
  employeeMarkers = []
  
  // 为每个员工添加标记
  mapData.value.forEach(employee => {
    const longitude = employee.longitude
    const latitude = employee.latitude
    const marker = new AMap.Marker({
      position: [longitude, latitude],
      content: `<div style="width:24px;height:24px;background:#2E75B6;border-radius:50%;border:3px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.3);display:flex;align-items:center;justify-content:center;color:white;font-size:10px;font-weight:bold;">👤</div>`,
      offset: new AMap.Pixel(-12, -12),
      extData: { ...employee, longitude, latitude }
    })
    
    marker.on('click', () => {
      const infoWindow = new AMap.InfoWindow({
        content: `<div style="padding:8px;min-width:180px;">
          <h4 style="margin:0 0 8px;color:#2E75B6;">${employee.name}</h4>
          <p style="margin:4px 0;font-size:12px;">🏢 部门: ${employee.department || '未设置'}</p>
          <p style="margin:4px 0;font-size:12px;color:#999;">📍 ${longitude.toFixed(6)}, ${latitude.toFixed(6)}</p>
        </div>`,
        offset: new AMap.Pixel(0, -30)
      })
      infoWindow.open(map, marker.getPosition())
    })
    
    employeeMarkers.push(marker)
    map.add(marker)
  })
}

// 添加规划站点标记
const addPlanningMarkers = (AMap) => {
  // 清除现有规划标记
  planningMarkers.forEach(m => map.remove(m))
  planningMarkers = []
  
  planResult.value.forEach(station => {
    console.log('站点坐标:', station.longitude, station.latitude)
    const color = '#52c41a' // 绿色表示规划站点
    const marker = new AMap.Marker({
      position: [station.longitude, station.latitude],
      content: `<div style="width:24px;height:24px;background:${color};border-radius:50%;border:3px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.3);display:flex;align-items:center;justify-content:center;color:white;font-size:10px;font-weight:bold;">🚌</div>`,
      offset: new AMap.Pixel(-12, -12),
      extData: station
    })
    
    marker.on('click', () => {
      const infoWindow = new AMap.InfoWindow({
        content: `<div style="padding:8px;min-width:180px;">
          <h4 style="margin:0 0 8px;color:#52c41a;">规划站点</h4>
          <p style="margin:4px 0;font-size:12px;">🆔 站点ID: ${station.stationId}</p>
          <p style="margin:4px 0;font-size:12px;color:#999;">📍 ${station.longitude.toFixed(6)}, ${station.latitude.toFixed(6)}</p>
        </div>`,
        offset: new AMap.Pixel(0, -30)
      })
      infoWindow.open(map, marker.getPosition())
    })
    
    planningMarkers.push(marker)
    map.add(marker)
  })
}

// 生成站点规划
const handlePlanning = async () => {
  try {
    const res = await stationApi.planStations()
    console.log('完整响应:', res)
    planResult.value = res.data
    
    if (AMapInstance && map) {
      addPlanningMarkers(AMapInstance)
      ElMessage.success('站点规划生成成功，已在地图上显示')
    }
  } catch (error) {
    console.error('规划失败', error)
    ElMessage.error('站点规划生成失败')
  }
}

// 原有功能保持不变
const handleSearch = () => { ElMessage.info('搜索功能') }
const handleAdd = () => { dialogTitle.value = '新增员工'; dialogVisible.value = true }
const handleEdit = (row) => { dialogTitle.value = '编辑员工'; Object.assign(formData, row); dialogVisible.value = true }
const handleSubmit = () => { ElMessage.success('保存成功'); dialogVisible.value = false }
const handleLeave = (row) => { ElMessageBox.confirm(`确定为${row.name}登记请假？`).then(() => { row.status = 2; ElMessage.success('已登记请假') }) }
const handleDelete = (row) => { ElMessageBox.confirm(`确定删除${row.name}？`).then(() => ElMessage.success('删除成功')) }
const syncHR = () => { ElMessage.success('HR数据同步成功') }

onMounted(async () => {
  await getEmployees()
  await getMapData()
  await getStations()
  await initMap()
})

onUnmounted(() => {
  map?.destroy()
})
</script>

<style lang="scss" scoped>
.employee-container {
  .page-title { margin: 0 0 20px; font-size: 20px; color: #333; }
  
  .stats-row {
    margin-bottom: 20px;
    .stat-card {
      background: white; border-radius: 12px; padding: 20px; text-align: center;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      .stat-value { font-size: 32px; font-weight: bold; }
      .stat-label { color: #666; margin-top: 8px; }
      &.blue .stat-value { color: #2E75B6; }
      &.green .stat-value { color: #52c41a; }
      &.orange .stat-value { color: #faad14; }
      &.purple .stat-value { color: #722ed1; }
    }
  }
  
  .map-card {
    margin-top: 20px;
    .card-header {
      display: flex; 
      justify-content: space-between; 
      align-items: center;
      .map-legend {
        display: flex; 
        gap: 16px; 
        font-size: 12px;
        .legend-item {
          display: flex; 
          align-items: center; 
          gap: 4px;
          .dot {
            width: 12px; 
            height: 12px; 
            border-radius: 50%;
            &.green { background: #52c41a; }
            &.yellow { background: #faad14; }
            &.blue { background: #1890ff; }
          }
        }
      }
    }
    .map-container { 
      height: 500px; 
      border-radius: 8px; 
      overflow: hidden; 
    }
  }
  
  .toolbar {
    display: flex; 
    justify-content: space-between; 
    margin-bottom: 16px;
    .search-area { 
      display: flex; 
      gap: 12px; 
    }
    .btn-group { 
      display: flex; 
      gap: 12px; 
    }
  }
  
  .emp-no { 
    font-weight: bold; 
    color: #2E75B6; 
  }
  
  .pagination { 
    margin-top: 16px; 
    justify-content: flex-end; 
  }
}
</style>