<template>
  <div class="route-container">
    <h2 class="page-title">🛤️ 线路管理</h2>
    
    <el-row :gutter="20">
      <!-- 地图区域 -->
      <el-col :span="14">
        <el-card shadow="hover" class="map-card">
          <template #header>
            <div class="card-header">
              <span>🗺️ 线路可视化</span>
              <el-select v-model="selectedRouteId" placeholder="选择线路" style="width: 200px" @change="showRoute">
                <el-option v-for="r in routes" :key="r.id" :label="r.name" :value="r.id" />
              </el-select>
            </div>
          </template>
          <div id="routeMap" class="map-container"></div>
          <div class="map-legend">
            <span class="legend-item"><i class="line green"></i> 乘坐率≥80%</span>
            <span class="legend-item"><i class="line yellow"></i> 60%-80%</span>
            <span class="legend-item"><i class="line red"></i> &lt;60%</span>
          </div>
        </el-card>
      </el-col>
      
      <!-- 线路列表 -->
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>📋 线路列表 ({{ routes.length }})</span>
              <el-button type="primary" size="small" @click="handleAdd">
                <el-icon><Plus /></el-icon> 新增
              </el-button>
            </div>
          </template>
          
          <div class="route-list">
            <div
              v-for="route in routes"
              :key="route.id"
              class="route-item"
              :class="{ active: selectedRouteId === route.id }"
              @click="selectRoute(route)"
            >
              <div class="route-header">
                <span class="route-name">
                  <span :class="['rate-dot', getRateClass(route.occupancyRate)]"></span>
                  {{ route.name }}
                </span>
                <el-tag :type="route.status === 1 ? 'success' : 'info'" size="small">
                  {{ route.status === 1 ? '运营中' : '停运' }}
                </el-tag>
              </div>
              <div class="route-info">
                <span>🚌 {{ route.vehiclePlate || '未分配' }}</span>
                <span>👥 {{ route.passengers }}/{{ route.capacity }}</span>
                <span>⏰ {{ route.departureTime }}</span>
              </div>
              <div class="route-rate">
                <el-progress
                  :percentage="route.occupancyRate"
                  :color="getRateColor(route.occupancyRate)"
                  :stroke-width="6"
                />
              </div>
              <div class="route-actions">
                <el-button type="primary" link size="small" @click.stop="handleEdit(route)">编辑</el-button>
                <el-button type="warning" link size="small" @click.stop="handleCalculate(route)">刷新乘坐率</el-button>
                <el-button type="danger" link size="small" @click.stop="handleDelete(route)">删除</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item label="线路名称" prop="name">
          <el-input v-model="formData.name" placeholder="如：1号线-天通苑方向" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联车辆">
              <el-select v-model="formData.vehicleId" placeholder="选择车辆" style="width: 100%">
                <el-option v-for="v in vehicles" :key="v.id" :label="v.plateNumber" :value="v.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发车时间">
              <el-time-picker v-model="formData.departureTime" format="HH:mm" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="载客容量">
              <el-input-number v-model="formData.capacity" :min="1" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="线路颜色">
              <el-color-picker v-model="formData.color" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" />
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

const selectedRouteId = ref(null)
const dialogVisible = ref(false)
const dialogTitle = ref('新增线路')
const formRef = ref(null)

let map = null
let polylines = []
let routeMarkers = []

defineOptions({
  name: 'Route'  //定义组件名
})

const vehicles = ref([
  { id: 1, plateNumber: '京A12345' }, { id: 2, plateNumber: '京A12346' },
  { id: 3, plateNumber: '京A12347' }, { id: 4, plateNumber: '京A12348' }
])

const routes = ref([
  { id: 1, name: '1号线-天通苑方向', vehiclePlate: '京A12345', passengers: 35, capacity: 40, occupancyRate: 87.5, departureTime: '07:00', status: 1, color: '#52c41a',
    path: [[116.4174, 40.0742], [116.3674, 40.0542], [116.3074, 40.0542], [116.5574, 40.0842]] },
  { id: 2, name: '2号线-回龙观方向', vehiclePlate: '京A12346', passengers: 42, capacity: 50, occupancyRate: 84.0, departureTime: '07:15', status: 1, color: '#52c41a',
    path: [[116.3274, 40.0742], [116.3474, 40.0642], [116.3174, 40.0342], [116.5574, 40.0842]] },
  { id: 3, name: '3号线-望京方向', vehiclePlate: '京A12347', passengers: 38, capacity: 40, occupancyRate: 95.0, departureTime: '07:00', status: 1, color: '#52c41a',
    path: [[116.4774, 39.9942], [116.4074, 40.0442], [116.4074, 39.9842], [116.5574, 40.0842]] },
  { id: 4, name: '4号线-通州方向', vehiclePlate: '京A12349', passengers: 40, capacity: 50, occupancyRate: 80.0, departureTime: '06:45', status: 1, color: '#52c41a',
    path: [[116.6574, 39.9142], [116.5574, 40.0842]] },
  { id: 5, name: '5号线-海淀方向', vehiclePlate: '京A12350', passengers: 21, capacity: 40, occupancyRate: 52.5, departureTime: '07:30', status: 1, color: '#ff4d4f',
    path: [[116.3074, 40.0542], [116.3174, 40.0342], [116.5574, 40.0842]] },
  { id: 6, name: '6号线-清河方向', vehiclePlate: '京A12351', passengers: 28, capacity: 35, occupancyRate: 80.0, departureTime: '07:15', status: 1, color: '#52c41a',
    path: [[116.3374, 40.0242], [116.3474, 40.0642], [116.5574, 40.0842]] },
  { id: 7, name: '7号线-北苑方向', vehiclePlate: '京A12352', passengers: 29, capacity: 50, occupancyRate: 58.0, departureTime: '07:00', status: 1, color: '#ff4d4f',
    path: [[116.4274, 40.0242], [116.4074, 40.0442], [116.5574, 40.0842]] },
  { id: 8, name: '8号线-立水桥方向', vehiclePlate: '京A12348', passengers: 26, capacity: 40, occupancyRate: 65.0, departureTime: '07:30', status: 0, color: '#faad14',
    path: [[116.4074, 40.0442], [116.4174, 40.0742], [116.5574, 40.0842]] }
])

const formData = reactive({
  id: null, name: '', vehicleId: null, departureTime: null, capacity: 40, color: '#52c41a', remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入线路名称', trigger: 'blur' }]
}

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

const initMap = async () => {
  try {
    const AMap = await AMapLoader.load({
      key: 'c63138579e021b08baa2a4634dc796fd',
      version: '2.0',
      plugins: ['AMap.Scale']
    })
    
    map = new AMap.Map('routeMap', {
      zoom: 10,
      center: [116.4074, 40.0242],
      mapStyle: 'amap://styles/light'
    })
    
    map.addControl(new AMap.Scale())
    
    // 工厂位置
    const factoryMarker = new AMap.Marker({
      position: [116.5574, 40.0842],
      content: '<div style="font-size:24px">🏭</div>',
      offset: new AMap.Pixel(-12, -12)
    })
    map.add(factoryMarker)
    
    showAllRoutes(AMap)
  } catch (error) {
    console.error('地图加载失败:', error)
  }
}

const showAllRoutes = async (AMap) => {
  if (!AMap) {
    AMap = window.AMap
  }
  
  polylines.forEach(p => map.remove(p))
  polylines = []
  
  routes.value.forEach(route => {
    const color = getRateColor(route.occupancyRate)
    const polyline = new AMap.Polyline({
      path: route.path,
      strokeColor: color,
      strokeWeight: 4,
      strokeOpacity: 0.8,
      lineJoin: 'round'
    })
    
    polyline.on('click', () => {
      selectedRouteId.value = route.id
    })
    
    polylines.push(polyline)
    map.add(polyline)
  })
  
  map.setFitView()
}

const showRoute = async (routeId) => {
  const route = routes.value.find(r => r.id === routeId)
  if (!route || !map) return
  
  // 高亮选中线路
  polylines.forEach((p, index) => {
    const r = routes.value[index]
    if (r.id === routeId) {
      p.setOptions({ strokeWeight: 8, strokeOpacity: 1 })
    } else {
      p.setOptions({ strokeWeight: 3, strokeOpacity: 0.4 })
    }
  })
  
  map.setFitView(polylines.find((_, i) => routes.value[i].id === routeId))
}

const selectRoute = (route) => {
  selectedRouteId.value = route.id
  showRoute(route.id)
}

const handleAdd = () => {
  dialogTitle.value = '新增线路'
  Object.assign(formData, { id: null, name: '', vehicleId: null, departureTime: null, capacity: 40, color: '#52c41a', remark: '' })
  dialogVisible.value = true
}

const handleEdit = (route) => {
  dialogTitle.value = '编辑线路'
  Object.assign(formData, route)
  dialogVisible.value = true
}

const handleSubmit = () => {
  ElMessage.success('保存成功')
  dialogVisible.value = false
}

const handleCalculate = (route) => {
  ElMessage.success(`${route.name} 乘坐率已刷新`)
}

const handleDelete = (route) => {
  ElMessageBox.confirm(`确定删除线路"${route.name}"吗？`).then(() => {
    ElMessage.success('删除成功')
  })
}

onMounted(() => {
  initMap()
})

onUnmounted(() => {
  map?.destroy()
})
</script>

<style lang="scss" scoped>
.route-container {
  .page-title { margin: 0 0 20px; font-size: 20px; color: #333; }
  
  .map-card {
    .card-header { display: flex; justify-content: space-between; align-items: center; }
    .map-container { height: 450px; border-radius: 8px; overflow: hidden; }
    .map-legend {
      display: flex; gap: 20px; padding: 12px 0; justify-content: center;
      .legend-item {
        display: flex; align-items: center; gap: 6px; font-size: 12px;
        .line {
          width: 24px; height: 4px; border-radius: 2px;
          &.green { background: #52c41a; }
          &.yellow { background: #faad14; }
          &.red { background: #ff4d4f; }
        }
      }
    }
  }
  
  .card-header { display: flex; justify-content: space-between; align-items: center; }
  
  .route-list {
    max-height: 480px; overflow-y: auto;
    
    .route-item {
      padding: 12px; border-radius: 8px; margin-bottom: 10px; cursor: pointer;
      background: #fafafa; transition: all 0.3s; border-left: 3px solid transparent;
      
      &:hover, &.active { background: #e6f4ff; border-left-color: #2E75B6; }
      
      .route-header {
        display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;
        .route-name {
          font-weight: 500; display: flex; align-items: center; gap: 8px;
          .rate-dot {
            width: 8px; height: 8px; border-radius: 50%;
            &.high { background: #52c41a; }
            &.medium { background: #faad14; }
            &.low { background: #ff4d4f; }
          }
        }
      }
      
      .route-info {
        display: flex; gap: 16px; font-size: 12px; color: #666; margin-bottom: 8px;
      }
      
      .route-rate { margin-bottom: 8px; }
      
      .route-actions { text-align: right; }
    }
  }
}
</style>
