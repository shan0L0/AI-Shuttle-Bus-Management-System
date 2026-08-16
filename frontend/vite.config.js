import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import DefineOptions from 'unplugin-vue-define-options/vite'

export default defineConfig({
  plugins: [
    vue(),
    DefineOptions(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts'
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts'
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {//在这里配置后端端口（端口是服务端在传输层的交互口，一个端口在某一时刻只能有一个监听端（服务端），但可以有多个连接端（服务端））
    //配置了后端端口，代理就知道前端的请求要送到哪里了。
    port: 3000,//前端开发服务器端口
    proxy: {//代理配置
      '/api': {//匹配所有以/api开头的请求
        target: 'http://localhost:8080',//目标端口，也就是后端服务器地址
        changeOrigin: true//修改请求头中的origin字段为localhost:8080，解决开发时跨域问题（因为前后端分离会导致跨域问题，即两个域间因安全问题，请求被浏览器拦截，无法直接通信）
      }
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/assets/styles/variables.scss" as *;`
      }
    }
  }
})
