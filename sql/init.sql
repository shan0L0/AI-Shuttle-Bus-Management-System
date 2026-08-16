-- ================================================
-- 智能车厂管理系统 数据库初始化脚本
-- 数据库: MySQL 8.0+
-- ================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS smart_shuttle DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_shuttle;

-- ================================================
-- 系统管理模块
-- ================================================

-- 系统用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(255) COMMENT '头像URL',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='系统用户表';

-- 系统角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    description VARCHAR(200) COMMENT '描述',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB COMMENT='系统角色表';

-- 系统权限表
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    permission_name VARCHAR(50) NOT NULL COMMENT '权限名称',
    permission_code VARCHAR(100) NOT NULL COMMENT '权限编码',
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    type TINYINT DEFAULT 1 COMMENT '类型：1菜单 2按钮',
    path VARCHAR(200) COMMENT '路由路径',
    icon VARCHAR(50) COMMENT '图标',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_permission_code (permission_code)
) ENGINE=InnoDB COMMENT='系统权限表';

-- 用户角色关联表
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB COMMENT='用户角色关联表';

-- 角色权限关联表
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB COMMENT='角色权限关联表';

-- ================================================
-- 业务模块
-- ================================================

-- 车辆表
DROP TABLE IF EXISTS biz_vehicle;
CREATE TABLE biz_vehicle (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    plate_number VARCHAR(20) NOT NULL COMMENT '车牌号',
    brand VARCHAR(50) COMMENT '品牌型号',
    seats INT NOT NULL COMMENT '座位数',
    status TINYINT DEFAULT 0 COMMENT '状态：0待命 1运行中 2维修中',
    fuel_consumption DECIMAL(5,2) COMMENT '百公里油耗(L)',
    route_id BIGINT COMMENT '所属线路ID',
    driver_name VARCHAR(50) COMMENT '驾驶员姓名',
    driver_phone VARCHAR(20) COMMENT '驾驶员电话',
    purchase_date DATE COMMENT '购置日期',
    last_maintenance DATE COMMENT '上次保养日期',
    next_maintenance DATE COMMENT '下次保养日期',
    mileage INT DEFAULT 0 COMMENT '总里程(km)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_plate_number (plate_number),
    KEY idx_status (status),
    KEY idx_route_id (route_id)
) ENGINE=InnoDB COMMENT='车辆表';

-- 站点表
DROP TABLE IF EXISTS biz_station;
CREATE TABLE biz_station (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '站点名称',
    longitude DECIMAL(10,6) NOT NULL COMMENT '经度',
    latitude DECIMAL(10,6) NOT NULL COMMENT '纬度',
    address VARCHAR(200) COMMENT '详细地址',
    passenger_count INT DEFAULT 0 COMMENT '乘车人数',
    district VARCHAR(50) COMMENT '所属区域',
    status TINYINT DEFAULT 1 COMMENT '状态：0停用 1启用',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_district (district),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='站点表';

-- 线路表
DROP TABLE IF EXISTS biz_route;
CREATE TABLE biz_route (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '线路名称',
    vehicle_id BIGINT COMMENT '关联车辆ID',
    station_count INT DEFAULT 0 COMMENT '站点数量',
    total_passenger INT DEFAULT 0 COMMENT '总乘客数',
    capacity INT DEFAULT 0 COMMENT '载客容量',
    occupancy_rate DECIMAL(5,2) DEFAULT 0 COMMENT '乘坐率(百分比)',
    total_distance DECIMAL(10,2) COMMENT '总里程(km)',
    estimated_time INT COMMENT '预计行驶时间(分钟)',
    departure_time TIME COMMENT '发车时间',
    status TINYINT DEFAULT 1 COMMENT '状态：0停运 1运营中',
    color VARCHAR(20) DEFAULT '#2E75B6' COMMENT '线路颜色',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_name (name),
    KEY idx_status (status),
    KEY idx_vehicle_id (vehicle_id)
) ENGINE=InnoDB COMMENT='线路表';

-- 线路站点关联表
DROP TABLE IF EXISTS biz_route_station;
CREATE TABLE biz_route_station (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    route_id BIGINT NOT NULL COMMENT '线路ID',
    station_id BIGINT NOT NULL COMMENT '站点ID',
    sequence INT NOT NULL COMMENT '站点顺序',
    distance_from_prev DECIMAL(10,2) COMMENT '距上一站距离(km)',
    time_from_prev INT COMMENT '距上一站时间(分钟)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_route_station (route_id, station_id),
    KEY idx_route_id (route_id),
    KEY idx_station_id (station_id)
) ENGINE=InnoDB COMMENT='线路站点关联表';

-- 员工表
DROP TABLE IF EXISTS biz_employee;
CREATE TABLE biz_employee (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    employee_no VARCHAR(50) NOT NULL COMMENT '工号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    department VARCHAR(100) COMMENT '部门',
    phone VARCHAR(20) COMMENT '手机号',
    station_id BIGINT COMMENT '乘车站点ID',
    route_id BIGINT COMMENT '所属线路ID',
    address VARCHAR(200) COMMENT '家庭住址',
    status TINYINT DEFAULT 1 COMMENT '状态：0离职 1在职 2请假 3出差',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_employee_no (employee_no),
    KEY idx_station_id (station_id),
    KEY idx_route_id (route_id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='员工表';

-- AI对话记录表
DROP TABLE IF EXISTS ai_conversation;
CREATE TABLE ai_conversation (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id VARCHAR(50) COMMENT '会话ID',
    user_message TEXT NOT NULL COMMENT '用户消息',
    ai_response TEXT COMMENT 'AI响应',
    intent VARCHAR(50) COMMENT '识别意图',
    response_time INT COMMENT '响应时间(ms)',
    provider VARCHAR(20) COMMENT 'AI提供商',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_id (user_id),
    KEY idx_session_id (session_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='AI对话记录表';

-- AI优化日志表
DROP TABLE IF EXISTS ai_optimization_log;
CREATE TABLE ai_optimization_log (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    optimization_type VARCHAR(50) COMMENT '优化类型',
    input_data TEXT COMMENT '输入数据',
    output_result TEXT COMMENT '输出结果',
    suggestions TEXT COMMENT 'AI建议',
    estimated_saving DECIMAL(12,2) COMMENT '预计节省成本',
    status TINYINT DEFAULT 0 COMMENT '状态：0待处理 1已采纳 2已拒绝',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by BIGINT COMMENT '创建人ID',
    KEY idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='AI优化日志表';

-- ================================================
-- 初始化数据
-- ================================================

-- 初始化管理员用户（密码：admin123）
INSERT INTO sys_user (id, username, password, real_name, phone, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKm6Km/mvovVqO9MgK.9X5J5kCjy', '系统管理员', '13800000000', 1);

-- 初始化角色
INSERT INTO sys_role (id, role_name, role_code, description, status) VALUES
(1, '超级管理员', 'ROLE_ADMIN', '系统超级管理员，拥有所有权限', 1),
(2, '运营管理员', 'ROLE_OPERATOR', '运营管理人员，负责日常班车调度', 1),
(3, '数据分析员', 'ROLE_ANALYST', '数据分析人员，查看报表和统计', 1);

-- 初始化权限
INSERT INTO sys_permission (id, permission_name, permission_code, parent_id, type, path, sort) VALUES
-- 仪表盘
(100, '数据总览', 'dashboard', 0, 1, '/dashboard', 1),
(101, '查看统计', 'dashboard:view', 100, 2, NULL, 1),
-- 车辆管理
(200, '车辆管理', 'vehicle', 0, 1, '/vehicle', 2),
(201, '车辆列表', 'vehicle:list', 200, 2, NULL, 1),
(202, '新增车辆', 'vehicle:add', 200, 2, NULL, 2),
(203, '编辑车辆', 'vehicle:edit', 200, 2, NULL, 3),
(204, '删除车辆', 'vehicle:delete', 200, 2, NULL, 4),
-- 员工管理
(300, '员工管理', 'employee', 0, 1, '/employee', 3),
(301, '员工列表', 'employee:list', 300, 2, NULL, 1),
(302, '新增员工', 'employee:add', 300, 2, NULL, 2),
(303, '编辑员工', 'employee:edit', 300, 2, NULL, 3),
(304, '删除员工', 'employee:delete', 300, 2, NULL, 4),
-- 站点管理
(400, '站点管理', 'station', 0, 1, '/station', 4),
(401, '站点列表', 'station:list', 400, 2, NULL, 1),
(402, '新增站点', 'station:add', 400, 2, NULL, 2),
(403, '编辑站点', 'station:edit', 400, 2, NULL, 3),
(404, '删除站点', 'station:delete', 400, 2, NULL, 4),
-- 线路管理
(500, '线路管理', 'route', 0, 1, '/route', 5),
(501, '线路列表', 'route:list', 500, 2, NULL, 1),
(502, '新增线路', 'route:add', 500, 2, NULL, 2),
(503, '编辑线路', 'route:edit', 500, 2, NULL, 3),
(504, '删除线路', 'route:delete', 500, 2, NULL, 4),
-- AI助手
(600, 'AI智能助手', 'ai', 0, 1, '/ai', 6),
(601, 'AI对话', 'ai:chat', 600, 2, NULL, 1),
(602, '智能优化', 'ai:optimize', 600, 2, NULL, 2),
-- 报表
(700, '数据报表', 'report', 0, 1, '/report', 7),
(701, '查看报表', 'report:view', 700, 2, NULL, 1),
(702, '导出报表', 'report:export', 700, 2, NULL, 2);

-- 用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 角色权限关联（管理员拥有所有权限）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- 初始化车辆数据
INSERT INTO biz_vehicle (id, plate_number, brand, seats, status, fuel_consumption, route_id, driver_name, driver_phone, mileage) VALUES
(1, '京A12345', '金龙客车XL500', 40, 1, 25.50, 1, '张师傅', '13800000001', 125000),
(2, '京A12346', '宇通客车ZK6', 50, 1, 28.00, 2, '李师傅', '13800000002', 98000),
(3, '京A12347', '金龙客车XL500', 40, 1, 24.80, 3, '王师傅', '13800000003', 156000),
(4, '京A12348', '比亚迪K9', 35, 0, 22.00, NULL, '赵师傅', '13800000004', 45000),
(5, '京A12349', '宇通客车ZK6', 50, 1, 27.50, 4, '刘师傅', '13800000005', 112000),
(6, '京A12350', '金龙客车XL500', 40, 1, 25.00, 5, '陈师傅', '13800000006', 88000),
(7, '京A12351', '比亚迪K9', 35, 1, 21.50, 6, '周师傅', '13800000007', 67000),
(8, '京A12352', '宇通客车ZK6', 50, 1, 28.50, 7, '吴师傅', '13800000008', 134000);

-- 初始化站点数据
INSERT INTO biz_station (id, name, longitude, latitude, address, passenger_count, district, status) VALUES
(1, '天通苑站', 116.4174, 40.0742, '北京市昌平区天通苑北一区', 68, '昌平区', 1),
(2, '回龙观站', 116.3274, 40.0742, '北京市昌平区回龙观东大街', 72, '昌平区', 1),
(3, '望京站', 116.4774, 39.9942, '北京市朝阳区望京西路', 55, '朝阳区', 1),
(4, '通州站', 116.6574, 39.9142, '北京市通州区新华大街', 48, '通州区', 1),
(5, '西二旗站', 116.3074, 40.0542, '北京市海淀区西二旗大街', 82, '海淀区', 1),
(6, '上地站', 116.3174, 40.0342, '北京市海淀区上地信息路', 45, '海淀区', 1),
(7, '清河站', 116.3374, 40.0242, '北京市海淀区清河中街', 38, '海淀区', 1),
(8, '龙泽站', 116.3474, 40.0642, '北京市昌平区龙泽苑东区', 52, '昌平区', 1),
(9, '霍营站', 116.3674, 40.0542, '北京市昌平区霍营地铁站', 35, '昌平区', 1),
(10, '立水桥站', 116.4074, 40.0442, '北京市朝阳区立水桥北路', 42, '朝阳区', 1),
(11, '北苑站', 116.4274, 40.0242, '北京市朝阳区北苑路', 28, '朝阳区', 1),
(12, '亚运村站', 116.4074, 39.9842, '北京市朝阳区亚运村', 32, '朝阳区', 1),
(13, '安贞门站', 116.4074, 39.9642, '北京市东城区安贞门外', 25, '东城区', 1),
(14, '惠新西街站', 116.4174, 39.9742, '北京市朝阳区惠新西街', 18, '朝阳区', 1),
(15, '工厂终点站', 116.5574, 40.0842, '北京市顺义区科技园区', 0, '顺义区', 1);

-- 初始化线路数据
INSERT INTO biz_route (id, name, vehicle_id, station_count, total_passenger, capacity, occupancy_rate, departure_time, status, color) VALUES
(1, '1号线-天通苑方向', 1, 5, 35, 40, 87.50, '07:00:00', 1, '#52c41a'),
(2, '2号线-回龙观方向', 2, 6, 42, 50, 84.00, '07:15:00', 1, '#52c41a'),
(3, '3号线-望京方向', 3, 5, 38, 40, 95.00, '07:00:00', 1, '#52c41a'),
(4, '4号线-通州方向', 5, 4, 40, 50, 80.00, '06:45:00', 1, '#52c41a'),
(5, '5号线-海淀方向', 6, 3, 21, 40, 52.50, '07:30:00', 1, '#ff4d4f'),
(6, '6号线-清河方向', 7, 4, 28, 35, 80.00, '07:15:00', 1, '#52c41a'),
(7, '7号线-北苑方向', 8, 3, 29, 50, 58.00, '07:00:00', 1, '#ff4d4f'),
(8, '8号线-立水桥方向', 4, 3, 26, 40, 65.00, '07:30:00', 0, '#faad14');

-- 初始化员工数据（示例）
INSERT INTO biz_employee (id, employee_no, name, department, phone, station_id, route_id, status) VALUES
(1, 'E001', '张三', '技术部', '13900000001', 1, 1, 1),
(2, 'E002', '李四', '产品部', '13900000002', 2, 2, 1),
(3, 'E003', '王五', '市场部', '13900000003', 3, 3, 2),
(4, 'E004', '赵六', '技术部', '13900000004', 4, 4, 1),
(5, 'E005', '钱七', '运营部', '13900000005', 5, 1, 3),
(6, 'E006', '孙八', '财务部', '13900000006', 6, 2, 1),
(7, 'E007', '周九', '人事部', '13900000007', 7, 3, 1),
(8, 'E008', '吴十', '技术部', '13900000008', 8, 2, 1);

-- 线路站点关联
INSERT INTO biz_route_station (route_id, station_id, sequence, distance_from_prev, time_from_prev) VALUES
-- 1号线
(1, 1, 1, 0, 0),
(1, 9, 2, 3.5, 8),
(1, 5, 3, 5.2, 12),
(1, 15, 4, 15.8, 25),
-- 2号线
(2, 2, 1, 0, 0),
(2, 8, 2, 2.8, 6),
(2, 6, 3, 3.2, 8),
(2, 15, 4, 18.5, 30),
-- 3号线
(3, 3, 1, 0, 0),
(3, 10, 2, 4.5, 10),
(3, 12, 3, 3.8, 8),
(3, 15, 4, 12.6, 22);

COMMIT;

-- 提示信息
SELECT '数据库初始化完成！' AS message;
SELECT '默认管理员账号: admin / admin123' AS tips;
