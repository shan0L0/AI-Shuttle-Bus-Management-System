import request from '@/utils/request'

/**
 * 获取车辆分页列表
 */
export function getVehicleList(params) {
  return request({
    url: '/vehicles',
    method: 'get',
    params
  })
}

/**
 * 获取车辆详情
 */
export function getVehicleById(id) {
  return request({
    url: `/vehicles/${id}`,
    method: 'get'
  })
}

/**
 * 新增车辆
 */
export function addVehicle(data) {
  return request({
    url: '/vehicles',
    method: 'post',
    data
  })
}

/**
 * 更新车辆
 */
export function updateVehicle(id, data) {
  return request({
    url: `/vehicles/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除车辆
 */
export function deleteVehicle(id) {
  return request({
    url: `/vehicles/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除车辆
 */
export function deleteVehicleBatch(ids) {
  return request({
    url: '/vehicles/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * 更新车辆状态
 */
export function updateVehicleStatus(id, status) {
  return request({
    url: `/vehicles/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 获取所有车辆（下拉选择）
 */
export function getAllVehicles() {
  return request({
    url: '/vehicles/all',
    method: 'get'
  })
}

/**
 * 获取车辆统计数据
 */
export function getVehicleStats() {
  return request({
    url: '/vehicles/stats',
    method: 'get'
  })
}
