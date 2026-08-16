<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <span class="logo-icon">🚌</span>
        <h1>智能车厂管理系统</h1>
        <el-tag type="warning" effect="dark">AI赋能版</el-tag>
      </div>
      
      <el-form ref="formRef" :model="loginForm" :rules="rules" class="login-form"><!--应用rules（rules="rules"）-->
        <el-form-item prop="username">
          <!--数据双向绑定，用户在输入框输入时数据自动更新、代码修改数据时输入框内容自动变化-->
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-tips">
        <p>演示账号: shanyue / admin123</p>
      </div>
    </div>
    
    <div class="login-footer">
      <p>© 2024 智能车厂管理系统 - AI赋能企业通勤</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const rules = {//定义表单规则：required：是否必填；message：提示信息；trigger：触发验证的条件，blur为虚焦，鼠标失去焦点
  //rules数组中有默认的基础验证选项，如是否必填、最小长度，同时也可以用validator实现自定义复杂验证逻辑
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  try {
    const res = await login({
      username: loginForm.username,
      password: loginForm.password
    })
    
    // 保存Token和用户信息
    localStorage.setItem('token', res.data.accessToken)
    localStorage.setItem('refreshToken', res.data.refreshToken)
    localStorage.setItem('username', res.data.userInfo.realName || res.data.userInfo.username)
    localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))
    
    window.localStorageSync = true

    ElMessage.success('登录成功')

    await new Promise(resolve => setTimeout(resolve, 50))

    router.push('/dashboard')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  
  .login-card {
    width: 400px;
    padding: 40px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    
    .login-header {
      text-align: center;
      margin-bottom: 30px;
      
      .logo-icon {
        font-size: 48px;
        display: block;
        margin-bottom: 16px;
      }
      
      h1 {
        font-size: 24px;
        color: #333;
        margin: 0 0 12px;
      }
    }
    
    .login-form {
      .login-btn {
        width: 100%;
      }
    }
    
    .login-tips {
      text-align: center;
      margin-top: 20px;
      
      p {
        color: #999;
        font-size: 13px;
        margin: 0;
      }
    }
  }
  
  .login-footer {
    margin-top: 30px;
    color: rgba(255, 255, 255, 0.7);
    font-size: 13px;
  }
}
</style>
