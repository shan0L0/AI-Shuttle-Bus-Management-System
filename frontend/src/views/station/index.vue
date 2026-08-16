<template>
  <div class="station-container">
    <h2 class="page-title">📍 站点管理</h2>
    
    <el-row :gutter="20">
      <!-- 地图区域 -->
      <el-col :span="14">
        <el-card shadow="hover" class="map-card">
          <template #header>
            <div class="card-header">
              <span>🗺️ 站点分布地图</span>
              <div class="map-legend">
                <span class="legend-item"><i class="dot green"></i> &gt;50人</span>
                <span class="legend-item"><i class="dot yellow"></i> 20-50人</span>
                <span class="legend-item"><i class="dot blue"></i> &lt;20人</span>
              </div>
            </div>
          </template>
          <div id="stationMap" class="map-container"></div>
        </el-card>
      </el-col>
      
      <!-- 站点列表 -->
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>📋 站点列表 ({{ stations.length }})</span>
              <el-button type="primary" size="small" @click="handleAdd">
                <el-icon><Plus /></el-icon> 新增
              </el-button>
            </div>
          </template>
          
          <el-input v-model="searchKey" placeholder="搜索站点名称..." style="margin-bottom: 12px" clearable>
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          
          <div class="station-list">
            <div
              v-for="station in filteredStations"
              :key="station.id"
              class="station-item"
              :class="{ active: selectedStation?.id === station.id }"
              @click="selectStation(station)"
            >
              <div class="station-info">
                <div class="station-name">
                  <span :class="['passenger-dot', getPassengerClass(station.passengerCount)]"></span>
                  {{ station.name }}
                </div>
                <div class="station-meta">
                  {{ station.district }} · {{ station.passengerCount }}人
                </div>
              </div>
              <div class="station-actions">
                <el-button type="primary" link size="small" @click.stop="handleEdit(station)">编辑</el-button>
                <el-button type="danger" link size="small" @click.stop="handleDelete(station)">删除</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>


    <!-- 规划模块 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card shadow="hover" class="map-card" style="min-height: 300px;">
          <template #header>
            <div class="card-header">
              <span>🗺️ 推荐线路</span>
            </div>
          </template>

        <div v-if="planResult && Object.keys(planResult).length > 0" style="padding: 10px;">
  <div 
    v-for="([groupId, stationIds], index) in Object.entries(planResult)"
    :key="index"
    style="
      margin-bottom: 12px; 
      padding: 12px; 
      border: 1px solid #ebeef5; 
      border-radius: 4px; 
      background-color: #fafafa;
      position: relative;
    "
  >
    <!-- 组信息 -->
    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px;">
      <div>
        <strong style="font-size: 16px; color: #303133;">组 {{ parseInt(groupId) + 1 }}</strong>
        <span style="margin-left: 8px; font-size: 12px; color: #909399;">
          ({{ stationIds.length }}个站点)
        </span>
      </div>
      
      <!-- 显示路线按钮 -->
      <el-button 
        type="primary" 
        size="small" 
        @click="showRouteOnMap(groupId)"
        style="
          background: linear-gradient(135deg, #409EFF, #66b1ff);
          border: none;
          box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
        "
      >
        <el-icon style="margin-right: 4px;"><MapLocation /></el-icon>
        显示路线
      </el-button>
    </div>
  </div>
</div>

        <!-- 如果没有数据，显示提示 -->
        <div v-else style="text-align: center; color: #999; padding: 40px;">
          暂无规划数据，请点击“生成站点规划”按钮加载。
        </div>
      </el-card>
    </el-col>
  </el-row>
    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item label="站点名称" prop="name">
          <el-input v-model="formData.name" placeholder="如：天通苑站" />
        </el-form-item> 
        <el-form-item label="所属区域" prop="district">
          <el-select v-model="formData.district" placeholder="选择区域" style="width: 100%">
            <el-option label="昌平区" value="昌平区" />
            <el-option label="海淀区" value="海淀区" />
            <el-option label="朝阳区" value="朝阳区" />
            <el-option label="通州区" value="通州区" />
            <el-option label="顺义区" value="顺义区" />
            <el-option label="东城区" value="东城区" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="经度" prop="longitude">
              <el-input-number v-model="formData.longitude" :precision="6" :step="0.001" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" prop="latitude">
              <el-input-number v-model="formData.latitude" :precision="6" :step="0.001" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="乘车人数">
          <el-input-number v-model="formData.passengerCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="formData.address" type="textarea" :rows="2" placeholder="详细地址" />
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
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AMapLoader, { load } from '@amap/amap-jsapi-loader'
import { stationApi } from '@/api/index'
import { routeApi } from '../../api'

//写站点规划接口

const searchKey = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('新增站点')
const formRef = ref(null)
const selectedStation = ref(null)
const plannedStationList = null
const loading = ref(false) // 定义在 planResult 旁边


let groupCount = null
let AMapInstance = null
let map = null
let markers = []
let isEdit = false //用于区分编辑/新增站点（因为两个功能调用同一个函数发送请求）

defineOptions({
  name: 'Station'  //定义组件名
})

const stations = ref([
  // { id: 1, name: '天通苑站', longitude: 116.4174, latitude: 40.0742, passengerCount: 68, district: '昌平区', address: '北京市昌平区天通苑北一区' },
  // { id: 2, name: '回龙观站', longitude: 116.3274, latitude: 40.0742, passengerCount: 72, district: '昌平区', address: '北京市昌平区回龙观东大街' },
  // { id: 3, name: '望京站', longitude: 116.4774, latitude: 39.9942, passengerCount: 55, district: '朝阳区', address: '北京市朝阳区望京西园四区' },
  // { id: 4, name: '通州站', longitude: 116.6574, latitude: 39.9142, passengerCount: 48, district: '通州区', address: '北京市通州区新华大街' },
  // { id: 5, name: '西二旗站', longitude: 116.3074, latitude: 40.0542, passengerCount: 82, district: '海淀区', address: '北京市海淀区西二旗地铁站' },
  // { id: 6, name: '上地站', longitude: 116.3174, latitude: 40.0342, passengerCount: 45, district: '海淀区', address: '北京市海淀区上地信息路' },
  // { id: 7, name: '清河站', longitude: 116.3374, latitude: 40.0242, passengerCount: 38, district: '海淀区', address: '北京市海淀区清河小营' },
  // { id: 8, name: '龙泽站', longitude: 116.3474, latitude: 40.0642, passengerCount: 52, district: '昌平区', address: '北京市昌平区龙泽苑东区' },
  // { id: 9, name: '霍营站', longitude: 116.3674, latitude: 40.0542, passengerCount: 35, district: '昌平区', address: '北京市昌平区霍营地铁站' },
  // { id: 10, name: '立水桥站', longitude: 116.4074, latitude: 40.0442, passengerCount: 42, district: '朝阳区', address: '北京市朝阳区立水桥南' },
  // { id: 11, name: '北苑站', longitude: 116.4274, latitude: 40.0242, passengerCount: 28, district: '朝阳区', address: '北京市朝阳区北苑路' },
  // { id: 12, name: '亚运村站', longitude: 116.4074, latitude: 39.9842, passengerCount: 32, district: '朝阳区', address: '北京市朝阳区亚运村' },
  // { id: 13, name: '安贞门站', longitude: 116.4074, latitude: 39.9642, passengerCount: 25, district: '东城区', address: '北京市东城区安贞门' },
  // { id: 14, name: '惠新西街站', longitude: 116.4174, latitude: 39.9742, passengerCount: 18, district: '朝阳区', address: '北京市朝阳区惠新西街' },
  // { id: 15, name: '工厂终点站', longitude: 116.5574, latitude: 40.0842, passengerCount: 0, district: '顺义区', address: '北京市顺义区产业园区' }
])

const allStations = ref([])
const planResult = ref({})

let groupNum = null

const formData = reactive({
  id: null, name: '', district: '', longitude: 116.4, latitude: 40.0, passengerCount: 0, address: ''
})

const rules = {
  name: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
  district: [{ required: true, message: '请选择区域', trigger: 'change' }]
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

const getAllStations = async () => {
  try {
    const response = await stationApi.getAll()
    console.log('getAllStations完整响应:', response);

    allStations.value = response.data;
    console.log("allStations:", allStations)
  } catch (error) {
    console.error('加载失败:', error);
  } finally {
  }
}

const getPlannedStationList = async () => {
  try {
    const plannedStationList = await routeApi.planStationLists()
    planResult.value = plannedStationList.data
    console.log("planResult.value 类型:", typeof planResult.value) // 应该是 object
    console.log("planResult.value 内容:", planResult.value)
    groupCount = Object.keys(planResult.value).length
    console.log("goupCount:", groupCount)
  } catch (error) {
    console.error('加载失败:', error);
  } finally {
  }
}


const filteredStations = computed(() => {
  if (!searchKey.value) return stations.value
  return stations.value.filter(s => s.name.includes(searchKey.value) || s.district.includes(searchKey.value))
})

const getPassengerClass = (count) => {
  if (count > 50) return 'high'
  if (count >= 20) return 'medium'
  return 'low'
}

const getMarkerColor = (count) => {
  if (count > 50) return '#52c41a'
  if (count >= 20) return '#faad14'
  return '#1890ff'
}

window._AMapSecurityConfig = {
  securityJsCode: "b676058b87470697ece04821f0b6aec7",
};

const initMap = async () => {
  await getAllStations()
  try {
    const AMap = await AMapLoader.load({//加载api
      key: 'c63138579e021b08baa2a4634dc796fd',
      version: '2.0',
      plugins: ['AMap.Scale', 'AMap.ToolBar'],
    })
    AMapInstance = AMap
    map = new AMap.Map('stationMap', {//地图初始化（根据mapContainer的id）
      viewMode: '2D',//默认2d
      zoom: 11,
      center: [116.4074, 40.0242],
      mapStyle: 'amap://styles/light'
    })
    
    map.addControl(new AMap.Scale())
    map.addControl(new AMap.ToolBar({ position: 'RT' }))
    
    addMarkers(AMap)
    
    // 工厂位置
    const factoryMarker = new AMap.Marker({
      position: [116.5574, 40.0842],
      content: '<div style="font-size:24px">🏭</div>',
      offset: new AMap.Pixel(-12, -12)
    })
    map.add(factoryMarker)
    
    map.setFitView()
  } catch (error) {
    console.error('地图加载失败:', error)
  }
}

const addMarkers = (AMap) => {
  markers.forEach(m => map.remove(m))
  markers = []
  let count = 0
  allStations.value.forEach(station => {
    count++
    console.log("count:", count)
    const color = getMarkerColor(station.passengerCount)
    const marker = new AMap.Marker({
      position: [station.longitude, station.latitude],
      content: `<div style="width:24px;height:24px;background:${color};border-radius:50%;border:3px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.3);display:flex;align-items:center;justify-content:center;color:white;font-size:10px;font-weight:bold;">${station.passengerCount}</div>`,//定义标记样式
      offset: new AMap.Pixel(-12, -12),
      extData: station
    })
    
    marker.on('click', () => {
      selectedStation.value = station
      const infoWindow = new AMap.InfoWindow({
        content: `<div style="padding:8px;min-width:180px;">
          <h4 style="margin:0 0 8px;color:#2E75B6;">${station.name}</h4>
          <p style="margin:4px 0;font-size:12px;">📍 ${station.district}</p>
          <p style="margin:4px 0;font-size:12px;">👥 乘车人数: ${station.passengerCount}人</p>
          <p style="margin:4px 0;font-size:12px;color:#999;">${station.longitude.toFixed(4)}, ${station.latitude.toFixed(4)}</p>
        </div>`,
        offset: new AMap.Pixel(0, -30)
      })
      infoWindow.open(map, marker.getPosition())
    })
    
    markers.push(marker)
    map.add(marker)
  })
  console.log("共渲染了：", count)
}

const selectStation = (station) => {
  selectedStation.value = station
  if (map) {
    map.setCenter([station.longitude, station.latitude])
    map.setZoom(14)
  }
}

const handleAdd = () => {
  isEdit = false
  dialogTitle.value = '新增站点'
  Object.assign(formData, { id: null, name: '', district: '', longitude: 116.4, latitude: 40.0, passengerCount: 0, address: '' })
  dialogVisible.value = true
}

const handleEdit = (station) => {//普通同步函数
  isEdit = true
  dialogTitle.value = '编辑站点'
  Object.assign(formData, station)
  dialogVisible.value = true
}

const handleSubmit = async () => {//加了async成为异步函数
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if(!isEdit){
    const res = await stationApi.add({
      name: formData.name,
      longitude: formData.longitude,
      latitude: formData.latitude,
      address: formData.address,
      passengerCount: formData.passengerCount,
      district: formData.district,
      status: 1,
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
  }
  else{
    const res = await stationApi.update(formData.id,formData
    )
    ElMessage.success('编辑成功')
    dialogVisible.value = false
  }

  } catch (error) {
    console.error('保存失败:', error)
  } finally {
  }

}

const handleDelete = (station) => {
  ElMessageBox.confirm(`确定删除站点"${station.name}"吗？`).then(() => {
    try{
      const res = stationApi.delete(station.id)
      ElMessage.success('删除成功')
    } catch (error){
      console.error('删除失败')
    }
    
  })
}

const showRouteOnMap = async (groupId) => {
  try {
      clearPreviousRoute()
      const stationIds = planResult.value[groupId]
      console.log("stationIds: ", stationIds)
      const startId = stationIds[0];
      const endId = stationIds[stationIds.length - 1];
      const waypointIds = stationIds.slice(1, -1); // 获取中间的所有ID

      console.log("waypointsId:", waypointIds)
      
      console.log(`分组 ${groupId} - 起始ID: ${startId}, 终点ID: ${endId}`);
      
      try {
        const startStation = await getStationCoordinate(startId);//根据前端存储的站点id获取坐标
        const endStation = await getTerminalCoordinate(endId);
        const startCoordinate = [startStation.data.longitude, startStation.data.latitude]
        const endCoordinate = [endStation.data.longitude, endStation.data.latitude]

        if(waypointIds.length > 0){
          // 2. 批量获取所有途经点坐标
          const waypointPromises = waypointIds.map(id => getStationCoordinate(id));
          const waypointResponses = await Promise.all(waypointPromises);
          
          // 3. 提取坐标到数组
          const waypoints = waypointResponses.map(response => [
            response.data.longitude,
            response.data.latitude
          ]);
          console.log("waypoints:", waypoints)
          
          var opts = {
          waypoints: waypoints, //途经点参数，最多支持传入16个途经点
          };
          //引入和创建驾车规划插件
          AMap.plugin(["AMap.Driving"], function () {
            const driving = new AMap.Driving({
              map: map,
              policy: AMap.DrivingPolicy.LEAST_TIME,
              //panel: "stationMap", //参数值为你页面定义容器的 id 值<div id="my-panel"></div>
            });
            //获取起终点规划线路
            driving.search(startCoordinate, endCoordinate, opts, function (status, result) {
              if (status === "complete") {
                //status：complete 表示查询成功，no_data 为查询无结果，error 代表查询错误
                //查询成功时，result 即为对应的驾车导航信息
                currentDrivingInstance = driving
                console.log("有途径点线路规划完成：", result);
              } else {
                console.log("获取驾车数据失败：" + result);
              }
            });
          });
        }else{
          //引入和创建驾车规划插件
          AMap.plugin(["AMap.Driving"], function () {
            const driving = new AMap.Driving({
              map: map,
              policy: AMap.DrivingPolicy.LEAST_TIME,
              //panel: "stationMap", //参数值为你页面定义容器的 id 值<div id="my-panel"></div>
            });
            //获取起终点规划线路
            driving.search(startCoordinate, endCoordinate, function (status, result) {
              if (status === "complete") {
                //status：complete 表示查询成功，no_data 为查询无结果，error 代表查询错误
                //查询成功时，result 即为对应的驾车导航信息
                currentDrivingInstance = driving
                console.log("无途径点线路规划完成：", result);
              } else {
                console.log("获取驾车数据失败：" + result);
              }
            });
          });
        }
        
      } catch(error) {
        console.error("获取坐标失败", error);
      }
  }catch(error){
    console.error("规划失败")
  }
}

const getStationCoordinate = async(id) => {
  console.log("=== 调用 stationApi.getStationById ===")
  console.log("传入ID:", id)
  console.log("完整URL:", `/api/stations/${id}`)
  
  try {
    const response = await stationApi.getStationById(id)
    console.log("调用成功:", response)
    return response
  } catch(error) {
    console.error("调用失败，id：", id)
    console.error("错误URL:", error.config?.url)
    console.error("错误配置:", error.config)
    console.error("响应状态:", error.response?.status)
    console.error("响应数据:", error.response?.data)
    throw error
  }
}

const getTerminalCoordinate = async(id) => {
  console.log("=== 调用 stationApi.getTerminalById ===")
  console.log("传入ID:", id)
  console.log("完整URL:", `/api/stations/terminal/${id}`)
  
  try {
    const response = await stationApi.getTerminalById(id)
    console.log("调用成功:", response)
    return response
  } catch(error) {
    console.error("调用失败，id：", id)
    console.error("错误URL:", error.config?.url)
    console.error("错误配置:", error.config)
    console.error("响应状态:", error.response?.status)
    console.error("响应数据:", error.response?.data)
    throw error
  }
}

// 全局变量
let currentDrivingInstance = null
let currentRouteMarkers = []
let currentRoutePolylines = []

// 自定义添加路线标记
const addCustomRouteMarkers = (result, groupId) => {
  if (!result.routes || result.routes.length === 0) return
  
  const route = result.routes[0]
  const path = []
  
  // 提取所有路径点
  route.steps.forEach(step => {
    if (step.path && Array.isArray(step.path)) {
      step.path.forEach(point => {
        path.push([point.lng, point.lat])
      })
    }
  })
  
  if (path.length === 0) return
  
  // 1. 添加路线折线
  const polyline = new AMap.Polyline({
    path: path,
    strokeColor: getRouteColor(groupId),  // 不同组用不同颜色
    strokeWeight: 6,
    strokeOpacity: 0.8,
    strokeStyle: "solid"
  })
  
  map.add(polyline)
  currentRoutePolylines.push(polyline)
  
  // 2. 添加起点标记
  if (route.start) {
    const startMarker = new AMap.Marker({
      position: [route.start.lng, route.start.lat],
      content: `<div style="
        background: #52c41a; 
        color: white; 
        padding: 4px 8px; 
        border-radius: 4px; 
        font-size: 12px;
        font-weight: bold;
      ">起点${parseInt(groupId) + 1}</div>`,
      offset: new AMap.Pixel(0, -20)
    })
    map.add(startMarker)
    currentRouteMarkers.push(startMarker)
  }
  
  // 3. 添加终点标记
  if (route.end) {
    const endMarker = new AMap.Marker({
      position: [route.end.lng, route.end.lat],
      content: `<div style="
        background: #f5222d; 
        color: white; 
        padding: 4px 8px; 
        border-radius: 4px; 
        font-size: 12px;
        font-weight: bold;
      ">终点${parseInt(groupId) + 1}</div>`,
      offset: new AMap.Pixel(0, -20)
    })
    map.add(endMarker)
    currentRouteMarkers.push(endMarker)
  }
  
  // 4. 自动调整视野
  if (path.length > 0) {
    map.setFitView([...currentRouteMarkers, polyline])
  }
}

// 获取路线颜色
const getRouteColor = (groupId) => {
  const colors = ['#1890ff', '#52c41a', '#fa8c16', '#722ed1', '#f5222d']
  return colors[groupId % colors.length]
}

// 清空路线函数
const clearPreviousRoute = () => {
  if (currentDrivingInstance) {
    try {
      currentDrivingInstance.clear()
      currentDrivingInstance = null
    } catch (error) {
      console.log("清空 Driving 实例:", error)
    }
  }
  
  currentRouteMarkers.forEach(marker => {
    try {
      map.remove(marker)
    } catch (e) {}
  })
  currentRouteMarkers = []
  
  currentRoutePolylines.forEach(polyline => {
    try {
      map.remove(polyline)
    } catch (e) {}
  })
  currentRoutePolylines = []
  
  console.log("✅ 已清空上一条路线")
}

onMounted(async () => {  // ✅ 这里加 async
  await getStations()   // ✅ 现在可以用 await 了
  await getAllStations()
  await getPlannedStationList()
  await initMap()
})

onUnmounted(() => {
  map?.destroy()
})
</script>

<style lang="scss" scoped>
.station-container {
  .page-title { margin: 0 0 20px; font-size: 20px; color: #333; }
  
  .map-card {
    .card-header {
      display: flex; justify-content: space-between; align-items: center;
      .map-legend {
        display: flex; gap: 16px; font-size: 12px;
        .legend-item {
          display: flex; align-items: center; gap: 4px;
          .dot {
            width: 12px; height: 12px; border-radius: 50%;
            &.green { background: #52c41a; }
            &.yellow { background: #faad14; }
            &.blue { background: #1890ff; }
          }
        }
      }
    }
    .map-container { height: 500px; border-radius: 8px; overflow: hidden; }
  }
  
  .card-header { display: flex; justify-content: space-between; align-items: center; }
  
  .station-list {
    max-height: 420px; overflow-y: auto;
    
    .station-item {
      display: flex; justify-content: space-between; align-items: center;
      padding: 12px; border-radius: 8px; margin-bottom: 8px; cursor: pointer;
      background: #fafafa; transition: all 0.3s;
      
      &:hover, &.active { background: #e6f4ff; }
      
      .station-info {
        .station-name {
          font-weight: 500; display: flex; align-items: center; gap: 8px;
          .passenger-dot {
            width: 8px; height: 8px; border-radius: 50%;
            &.high { background: #52c41a; }
            &.medium { background: #faad14; }
            &.low { background: #1890ff; }
          }
        }
        .station-id {
        display: inline-block;
        font-size: 12px;
        color: #ffffff;
        background-color: #1890ff;
        padding: 2px 8px;
        border-radius: 4px;
        font-weight: 600;
        line-height: 1.2;
        white-space: nowrap;
      }

      /* 坐标文字样式 - 等宽字体，灰色文字 */
      .coordinates {
        display: inline-block;
        font-size: 12px;
        color: #666666;
        font-family: 'Consolas', 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
        font-weight: 400;
        line-height: 1.2;
        white-space: nowrap;
        margin-left: 8px;
      }
        .station-meta { font-size: 12px; color: #999; margin-top: 4px; }
      }
    }
  }
}
</style>
