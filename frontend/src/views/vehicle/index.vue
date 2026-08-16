<template>
  <div class="vehicle-container">
    <h2 class="page-title">🚐 车辆管理</h2>
    
    <el-card shadow="hover">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="search-area">
          <el-input
            v-model="searchForm.plateNumber"
            placeholder="搜索车牌号..."
            style="width: 200px"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="运行中" :value="1" />
            <el-option label="待命" :value="0" />
            <el-option label="维修中" :value="2" />
          </el-select>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </div>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增车辆
        </el-button>
      </div>
      
      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="plateNumber" label="车牌号" width="120">
          <template #default="{ row }">
            <span class="plate-number">{{ row.plateNumber }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="brand" label="品牌型号" width="150" />
        <el-table-column prop="seats" label="座位数" width="80">
          <template #default="{ row }">{{ row.seats }}座</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="driverName" label="驾驶员" width="100" />
        <el-table-column prop="routeName" label="所属线路" width="150">
          <template #default="{ row }">{{ row.routeName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="fuelConsumption" label="油耗(L/100km)" width="120" />
        <el-table-column prop="mileage" label="总里程(km)" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="handleStatusChange(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
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
        @size-change="loadData"
        @current-change="loadData"
        class="pagination"
      />
    </el-card>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="车牌号" prop="plateNumber">
              <el-input v-model="formData.plateNumber" placeholder="如：京A12345" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌型号" prop="brand">
              <el-input v-model="formData.brand" placeholder="如：金龙客车XL500" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="座位数" prop="seats">
              <el-input-number v-model="formData.seats" :min="1" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="油耗(L/100km)">
              <el-input-number v-model="formData.fuelConsumption" :min="0" :max="100" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="驾驶员姓名">
              <el-input v-model="formData.driverName" placeholder="驾驶员姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="驾驶员电话">
              <el-input v-model="formData.driverPhone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="购置日期">
          <el-date-picker v-model="formData.purchaseDate" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getVehicleList, addVehicle, updateVehicle, deleteVehicle, updateVehicleStatus } from '@/api/vehicle'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增车辆')
const formRef = ref(null)

defineOptions({
  name: 'Vehicle'  //定义组件名
})

const searchForm = reactive({
  plateNumber: '',
  status: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([
  { id: 1, plateNumber: '京A12345', brand: '金龙客车XL500', seats: 40, status: 1, driverName: '张师傅', driverPhone: '138****8001', routeName: '1号线', fuelConsumption: 25.5, mileage: 50000 },
  { id: 2, plateNumber: '京A12346', brand: '宇通客车ZK6', seats: 50, status: 1, driverName: '李师傅', driverPhone: '138****8002', routeName: '2号线', fuelConsumption: 28.0, mileage: 45000 },
  { id: 3, plateNumber: '京A12347', brand: '金龙客车XL500', seats: 40, status: 1, driverName: '王师傅', driverPhone: '138****8003', routeName: '3号线', fuelConsumption: 24.8, mileage: 38000 },
  { id: 4, plateNumber: '京A12348', brand: '比亚迪K9', seats: 35, status: 0, driverName: '赵师傅', driverPhone: '138****8004', routeName: '-', fuelConsumption: 22.0, mileage: 30000 },
  { id: 5, plateNumber: '京A12349', brand: '宇通客车ZK6', seats: 50, status: 2, driverName: '刘师傅', driverPhone: '138****8005', routeName: '4号线', fuelConsumption: 27.5, mileage: 55000 },
  { id: 6, plateNumber: '京A12350', brand: '金龙客车XL500', seats: 40, status: 1, driverName: '陈师傅', driverPhone: '138****8006', routeName: '5号线', fuelConsumption: 25.0, mileage: 42000 },
  { id: 7, plateNumber: '京A12351', brand: '比亚迪K9', seats: 35, status: 1, driverName: '周师傅', driverPhone: '138****8007', routeName: '6号线', fuelConsumption: 21.5, mileage: 28000 },
  { id: 8, plateNumber: '京A12352', brand: '宇通客车ZK6', seats: 50, status: 1, driverName: '吴师傅', driverPhone: '138****8008', routeName: '7号线', fuelConsumption: 28.5, mileage: 48000 }
])

const formData = reactive({
  id: null,
  plateNumber: '',
  brand: '',
  seats: 40,
  fuelConsumption: null,
  driverName: '',
  driverPhone: '',
  purchaseDate: null,
  remark: ''
})

const rules = {
  plateNumber: [
    { required: true, message: '请输入车牌号', trigger: 'blur' },
    { pattern: /^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-HJ-NP-Z0-9]{4,5}[A-HJ-NP-Z0-9挂学警港澳]$/, message: '车牌号格式不正确', trigger: 'blur' }
  ],
  brand: [{ required: true, message: '请输入品牌型号', trigger: 'blur' }],
  seats: [{ required: true, message: '请输入座位数', trigger: 'blur' }]
}

const getStatusType = (status) => {
  const types = { 0: 'info', 1: 'success', 2: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '待命', 1: '运行中', 2: '维修中' }
  return texts[status] || '未知'
}

const loadData = async () => {
  // 实际项目中调用API
  // loading.value = true
  // try {
  //   const res = await getVehicleList({
  //     ...searchForm,
  //     pageNum: pagination.pageNum,
  //     pageSize: pagination.pageSize
  //   })
  //   tableData.value = res.data.records
  //   pagination.total = res.data.total
  // } finally {
  //   loading.value = false
  // }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新增车辆'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑车辆'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitLoading.value = true
  try {
    if (formData.id) {
      // await updateVehicle(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      // await addVehicle(formData)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const handleStatusChange = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    // await updateVehicleStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success('状态更新成功')
  } catch (error) {
    console.error(error)
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除车辆 ${row.plateNumber} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    // await deleteVehicle(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const resetForm = () => {
  Object.assign(formData, {
    id: null,
    plateNumber: '',
    brand: '',
    seats: 40,
    fuelConsumption: null,
    driverName: '',
    driverPhone: '',
    purchaseDate: null,
    remark: ''
  })
  formRef.value?.resetFields()
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.vehicle-container {
  .page-title {
    margin: 0 0 20px;
    font-size: 20px;
    color: #333;
  }
  
  .toolbar {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16px;
    
    .search-area {
      display: flex;
      gap: 12px;
    }
  }
  
  .plate-number {
    font-weight: bold;
    color: #2E75B6;
  }
  
  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }
}
</style>
