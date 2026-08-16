import { createRouter, createWebHistory } from 'vue-router'

const routes = [//组件路由，根据用户访问的路由决定要渲染哪个组件，拥有路由的组件叫做路由组件，是页面级的组件；其他的属于普通组件，路由组件的包含关系通过路由器的children实现，而普通组件的包含关系通过组件内的组件标签实现。默认渲染App.vue，因为这是所有组件的父组件，routes数组中的最外层组件就是App.vue的直接子组件。
  {
    path: '/login',//相对路径
    name: 'Login',
    component: () => import('@/views/login/index.vue'),//懒加载，只有访问了该路径，才会加载组件;@指的是根目录，在vite.config.js中配置
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/components/Layout.vue'),
    redirect: '/dashboard',//用户访问该url时自动重定向
    children: [//children指定当前路由组件下的直接子组件，可以无限嵌套套。
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据总览', icon: 'DataBoard' }
      },
      {
        path: 'vehicle',
        name: 'Vehicle',
        component: () => import('@/views/vehicle/index.vue'),
        meta: { title: '车辆管理', icon: 'Van', permission: 'vehicle:list' }
      },
      {
        path: 'employee',
        name: 'Employee',
        component: () => import('@/views/employee/index.vue'),
        meta: { title: '员工管理', icon: 'User' }
      },
      {
        path: 'station',
        name: 'Station',
        component: () => import('@/views/station/index.vue'),
        meta: { title: '站点管理', icon: 'Location' }
      },
      {
        path: 'route',
        name: 'Route',
        component: () => import('@/views/route/index.vue'),
        meta: { title: '线路管理', icon: 'Guide' }
      },
      {
        path: 'ai',
        name: 'AI',
        component: () => import('@/views/ai/index.vue'),
        meta: { title: 'AI助手', icon: 'ChatDotRound' }
      },
      {
        path: 'optimize',
        name: 'Optimize',
        component: () => import('@/views/optimize/index.vue'),
        meta: { title: '智能优化', icon: 'TrendCharts' }
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/report/index.vue'),
        meta: { title: '数据报表', icon: 'DataAnalysis' }
      },
      {
        path: 'system',
        name: 'System',
        component: () => import('@/views/system/index.vue'),
        meta: { title: '系统设置', icon: 'Setting' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 智能车厂管理系统` : '智能车厂管理系统'
  
  // 公开页面直接放行
  if (to.meta.public) {
    next()
    return
  }
  
  // 检查登录状态
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') {
    next('/login')
    return
  }
  
  next()
})

export default router
