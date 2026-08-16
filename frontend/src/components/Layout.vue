<template>
  <el-container class="layout-container">
    <!-- 顶部导航 -->
    <!--!!!在 Vue 模板中，Vue功能标签（绿色标签）的内部是不支持直接写注释的!!!-->
    <!--蓝色标签为html原生标签，可以写注释-->
    <el-header class="layout-header">
      <div class="logo">
        <span class="logo-icon">🚌</span>
        <span class="logo-text">智能车厂管理系统</span>
        <el-tag type="warning" size="small" effect="dark" class="logo-badge">AI版</el-tag>
      </div>
      <div class="header-right">
        <span class="date-info">📅 {{ currentDate }}</span>
        <el-dropdown @command="handleCommand">
          <div class="user-info">
            <el-avatar :size="36" class="avatar">{{ userInitial }}</el-avatar>
            <span class="username">{{ username }}</span>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    
    <el-container>
      <!-- 侧边栏 -->
      <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          class="aside-menu"
        >
          <div class="menu-title" v-if="!isCollapse">核心功能</div>
          
          <el-menu-item index="/dashboard">
            <el-icon><DataBoard /></el-icon>
            <span>数据总览</span>
          </el-menu-item>
          
          <el-menu-item index="/vehicle">
            <el-icon><Van /></el-icon>
            <span>车辆管理</span>
          </el-menu-item>
          
          <el-menu-item index="/employee">
            <el-icon><User /></el-icon>
            <span>员工管理</span>
          </el-menu-item>
          
          <el-menu-item index="/station">
            <el-icon><Location /></el-icon>
            <span>站点管理</span>
          </el-menu-item>
          
          <el-menu-item index="/route">
            <el-icon><Guide /></el-icon>
            <span>线路管理</span>
          </el-menu-item>
          
          <el-divider v-if="!isCollapse" />
          <div class="menu-title" v-if="!isCollapse">智能功能</div>
          
          <el-menu-item index="/ai">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI智能助手</span>
          </el-menu-item>
          
          <el-menu-item index="/optimize">
            <el-icon><TrendCharts /></el-icon>
            <span>智能优化</span>
          </el-menu-item>
          
          <el-divider v-if="!isCollapse" />
          <div class="menu-title" v-if="!isCollapse">数据分析</div>
          
          <el-menu-item index="/report">
            <el-icon><DataAnalysis /></el-icon>
            <span>数据报表</span>
          </el-menu-item>
          
          <el-menu-item index="/system">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-menu>
        
        <!-- 折叠按钮 -->
        <div class="collapse-btn" @click="isCollapse = !isCollapse">
          <el-icon v-if="isCollapse"><Expand /></el-icon>
          <el-icon v-else><Fold /></el-icon>
        </div>
      </el-aside>
      
      <!-- 主内容区 -->
      <!--普通router-view是直接渲染，v-slot模式提供组件定义，前者直接产出组件实例，后者只提供组件定义由用户控制渲染，如下所示-->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <keep-alive :include="cachedViews">
              <component :is="Component"/>
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'

dayjs.locale('zh-cn')

const route = useRoute()
const router = useRouter()

const isCollapse = ref(false)
const cachedViews = ref(['Dashboard', 'Vehicle', 'Station', 'Route'])

const activeMenu = computed(() => route.path)
const currentDate = computed(() => dayjs().format('YYYY年MM月DD日 dddd'))
const username = computed(() => localStorage.getItem('username') || '管理员')
const userInitial = computed(() => username.value.charAt(0))

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      ElMessage.info('个人信息功能开发中')
      break
    case 'password':
      ElMessage.info('修改密码功能开发中')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        router.push('/login')
        ElMessage.success('已安全退出')
      })
      break
  }
}
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
  
  .layout-header {
    background: linear-gradient(135deg, #2E75B6, #1F4E79);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    z-index: 100;
    
    .logo {
      display: flex;
      align-items: center;
      color: white;
      
      .logo-icon {
        font-size: 28px;
        margin-right: 10px;
      }
      
      .logo-text {
        font-size: 18px;
        font-weight: bold;
      }
      
      .logo-badge {
        margin-left: 10px;
      }
    }
    
    .header-right {
      display: flex;
      align-items: center;
      gap: 20px;
      color: white;
      
      .date-info {
        font-size: 14px;
      }
      
      .user-info {
        display: flex;
        align-items: center;
        gap: 8px;
        cursor: pointer;
        
        .avatar {
          background: rgba(255, 255, 255, 0.2);
        }
        
        .username {
          font-size: 14px;
        }
      }
    }
  }
  
  .layout-aside {
    background: white;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
    transition: width 0.3s;
    display: flex;
    flex-direction: column;
    
    .aside-menu {
      flex: 1;
      border-right: none;
      
      .menu-title {
        padding: 12px 24px 8px;
        font-size: 12px;
        color: #999;
        text-transform: uppercase;
      }
      
      .el-menu-item {
        &.is-active {
          background: #e6f4ff;
          border-right: 3px solid #2E75B6;
        }
      }
    }
    
    .collapse-btn {
      padding: 12px;
      text-align: center;
      cursor: pointer;
      border-top: 1px solid #f0f0f0;
      
      &:hover {
        background: #f5f7fa;
      }
    }
  }
  
  .layout-main {
    background: #f0f2f5;
    padding: 20px;
    overflow-y: auto;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
