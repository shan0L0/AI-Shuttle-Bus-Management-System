import request from '@/utils/request'

// ==================== 站点API ====================
export const stationApi = {
  getList: (params) => request({ url: '/stations', method: 'get', params }),
  getStationById: (id) => request({ url: `/stations/${id}`, method: 'get' }),
  getTerminalById: (id) => request({ url: `/stations/terminal/${id}`, method: 'get' }),
  add: (data) => request({ url: '/stations', method: 'post', data }),
  update: (id, data) => request({ url: `/stations/${id}`, method: 'put', data }),
  delete: (id) => request({ url: `/stations/${id}`, method: 'delete' }),
  getAll: () => request({ url: '/stations/all', method: 'get' }),
  getMapData: () => request({ url: '/stations/map-data', method: 'get' }),
  getStats: () => request({ url: '/stations/stats', method: 'get' }),
  planStations: () => request({ url: '/stations/planStations', method: 'get' })
}

// ==================== 线路API ====================
export const routeApi = {
  getList: (params) => request({ url: '/routes', method: 'get', params }),
  getById: (id) => request({ url: `/routes/${id}`, method: 'get' }),
  add: (data) => request({ url: '/routes', method: 'post', data }),
  update: (id, data) => request({ url: `/routes/${id}`, method: 'put', data }),
  delete: (id) => request({ url: `/routes/${id}`, method: 'delete' }),
  getAll: () => request({ url: '/routes/all', method: 'get' }),
  calculateRate: (id) => request({ url: `/routes/${id}/calculate-rate`, method: 'post' }),
  getLowOccupancy: (threshold = 70) => request({ url: '/routes/low-occupancy', method: 'get', params: { threshold } }),
  getStats: () => request({ url: '/routes/stats', method: 'get' }),
  getMapData: () => request({ url: '/routes/map-data', method: 'get' }),
  planStationLists: () => request({ url: '/routes/planStationLists', method: 'get' }),
  getGroupNum: () => request({ url: 'routes/getGroupNum', method: 'get'})
}

// ==================== 员工API ====================
export const employeeApi = {
  getList: (params) => request({ url: '/employees', method: 'get', params }),
  getAll: () => request({url: '/employees/all', method: 'get' }),
  getById: (id) => request({ url: `/employees/${id}`, method: 'get' }),
  add: (data) => request({ url: '/employees', method: 'post', data }),
  update: (id, data) => request({ url: `/employees/${id}`, method: 'put', data }),
  delete: (id) => request({ url: `/employees/${id}`, method: 'delete' }),
  getStats: () => request({ url: '/employees/stats', method: 'get' }),
  syncHR: () => request({ url: '/employees/sync-hr', method: 'post' })
}

// ==================== AI API ====================
export const aiApi = {
  // AI查询对话
  selectChat: (data) => request({ url: '/aiSelect/chat', method: 'post', data }),
  // AI建议对话
  optimizeChat: (data) => request({ url: '/aiOptimize/chat', method: 'post', data }),
  // 清空对话历史
  clearOptimizeChatHistory: () => request({ url: '/aiOptimize/clearHistory', method: 'delete' }),
  // 刷新所有向量知识库记录
  refreshAllKnowledge: () => request({ url: '/aiOptimize/refresh/all', method: 'post' }),
  // 刷新来源为file的向量知识库记录
  refreshFileKnowledge: () => request({ url: '/aiOptimize/refresh/file', method: 'post' }),
  // 刷新来源为operation_record的向量知识库记录
  refreshRecordKnowledge: () => request({ url: '/aiOptimize/refresh/record', method: 'post' }),
  // 获取知识库统计信息
  getKnowledgeStats: () => request({ url: '/aiOptimize/stats', method: 'get' })
}

// ==================== 数据总览API ====================
export const dashboardApi = {
  // 获取统计概览
  getOverview: () => request({ url: '/dashboard/overview', method: 'get' }),
  
  // 获取今日班车状态
  getTodayStatus: () => request({ url: '/dashboard/today-status', method: 'get' }),
  
  // 获取乘坐率趋势
  getOccupancyTrend: (params) => request({ url: '/dashboard/occupancy-trend', method: 'get', params }),
  
  // 获取站点排行
  getStationRanking: () => request({ url: '/dashboard/station-ranking', method: 'get' })
}

// ==================== 报表API ====================
export const reportApi = {
  // 获取综合统计
  getComprehensiveStats: (params) => request({ url: '/report/comprehensive', method: 'get', params }),
  
  // 获取线路统计
  getRouteStats: (params) => request({ url: '/report/route-stats', method: 'get', params }),
  
  // 获取成本分析
  getCostAnalysis: (params) => request({ url: '/report/cost-analysis', method: 'get', params }),
  
  // 导出报表
  exportReport: (params) => request({ url: '/report/export', method: 'get', params, responseType: 'blob' })
}

// ==================== 系统API ====================
export const systemApi = {
  // 获取备份列表
  getBackupList: (params) => request({ url: '/system/backups', method: 'get', params }),
  
  // 创建备份
  createBackup: () => request({ url: '/system/backups', method: 'post' }),
  
  // 恢复备份
  restoreBackup: (id) => request({ url: `/system/backups/${id}/restore`, method: 'post' }),
  
  // 获取AI配置
  getAiConfig: () => request({ url: '/system/ai-config', method: 'get' }),
  
  // 更新AI配置
  updateAiConfig: (data) => request({ url: '/system/ai-config', method: 'put', data })
}
