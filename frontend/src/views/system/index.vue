<template>
  <div class="system-container">
    <h2 class="page-title">⚙️ 系统设置</h2>
    
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 数据备份 -->
      <el-tab-pane label="📦 数据备份" name="backup">
        <div class="backup-section">
          <div class="backup-actions">
            <el-button type="primary" @click="createBackup" :loading="backupLoading">
              <el-icon><Upload /></el-icon> 立即备份
            </el-button>
            <el-button type="success" @click="scheduleBackup">
              <el-icon><Clock /></el-icon> 定时备份设置
            </el-button>
          </div>
          
          <el-table :data="backupList" stripe>
            <el-table-column prop="fileName" label="备份文件" width="280" />
            <el-table-column prop="fileSize" label="文件大小" width="100" />
            <el-table-column label="备份类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.type === 1 ? 'success' : 'info'" size="small">
                  {{ row.type === 1 ? '自动' : '手动' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="备份时间" width="180" />
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="downloadBackup(row)">下载</el-button>
                <el-button type="warning" link size="small" @click="restoreBackup(row)">恢复</el-button>
                <el-button type="danger" link size="small" @click="deleteBackup(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
      
      <!-- AI配置 -->
      <el-tab-pane label="🤖 AI配置" name="ai">
        <el-form :model="aiConfig" label-width="140px" class="ai-config-form">
          <el-divider content-position="left">AI服务提供商</el-divider>
          
          <el-form-item label="当前服务商">
            <el-radio-group v-model="aiConfig.provider">
              <el-radio-button value="deepseek">
                <span class="provider-option">
                  <img src="https://www.deepseek.com/favicon.ico" class="provider-icon" />
                  DeepSeek
                </span>
              </el-radio-button>
              <el-radio-button value="qwen">
                <span class="provider-option">
                  <span class="provider-icon-text">🧠</span>
                  阿里千问
                </span>
              </el-radio-button>
              <el-radio-button value="wenxin">
                <span class="provider-option">
                  <span class="provider-icon-text">🔮</span>
                  百度文心
                </span>
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          
          <!-- DeepSeek配置 -->
          <template v-if="aiConfig.provider === 'deepseek'">
            <el-divider content-position="left">DeepSeek 配置</el-divider>
            <el-form-item label="API Key">
              <el-input v-model="aiConfig.deepseek.apiKey" type="password" show-password placeholder="sk-xxxxxxxx" style="width: 400px" />
            </el-form-item>
            <el-form-item label="模型">
              <el-select v-model="aiConfig.deepseek.model" style="width: 200px">
                <el-option label="deepseek-chat" value="deepseek-chat" />
                <el-option label="deepseek-coder" value="deepseek-coder" />
              </el-select>
            </el-form-item>
            <el-form-item label="API地址">
              <el-input v-model="aiConfig.deepseek.baseUrl" style="width: 400px" />
            </el-form-item>
          </template>
          
          <!-- 阿里千问配置 -->
          <template v-if="aiConfig.provider === 'qwen'">
            <el-divider content-position="left">阿里千问 配置</el-divider>
            <el-form-item label="API Key">
              <el-input v-model="aiConfig.qwen.apiKey" type="password" show-password placeholder="sk-xxxxxxxx" style="width: 400px" />
            </el-form-item>
            <el-form-item label="模型">
              <el-select v-model="aiConfig.qwen.model" style="width: 200px">
                <el-option label="qwen-turbo" value="qwen-turbo" />
                <el-option label="qwen-plus" value="qwen-plus" />
                <el-option label="qwen-max" value="qwen-max" />
              </el-select>
            </el-form-item>
            <el-form-item label="API地址">
              <el-input v-model="aiConfig.qwen.baseUrl" style="width: 400px" />
            </el-form-item>
          </template>
          
          <!-- 百度文心配置 -->
          <template v-if="aiConfig.provider === 'wenxin'">
            <el-divider content-position="left">百度文心 配置</el-divider>
            <el-form-item label="API Key">
              <el-input v-model="aiConfig.wenxin.apiKey" type="password" show-password placeholder="API Key" style="width: 400px" />
            </el-form-item>
            <el-form-item label="Secret Key">
              <el-input v-model="aiConfig.wenxin.secretKey" type="password" show-password placeholder="Secret Key" style="width: 400px" />
            </el-form-item>
            <el-form-item label="模型">
              <el-select v-model="aiConfig.wenxin.model" style="width: 200px">
                <el-option label="ERNIE-Bot 4.0" value="ernie-bot-4" />
                <el-option label="ERNIE-Bot-turbo" value="ernie-bot-turbo" />
                <el-option label="ERNIE-Bot" value="ernie-bot" />
              </el-select>
            </el-form-item>
          </template>
          
          <el-divider content-position="left">通用配置</el-divider>
          
          <el-form-item label="请求限流">
            <el-input-number v-model="aiConfig.rateLimit" :min="1" :max="100" /> 次/分钟
          </el-form-item>
          
          <el-form-item label="缓存时间">
            <el-input-number v-model="aiConfig.cacheTtl" :min="0" :max="86400" :step="60" /> 秒
          </el-form-item>
          
          <el-form-item label="启用缓存">
            <el-switch v-model="aiConfig.cacheEnabled" />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="saveAiConfig" :loading="saving">保存配置</el-button>
            <el-button @click="testAiConnection" :loading="testing">测试连接</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <!-- 系统参数 -->
      <el-tab-pane label="🔧 系统参数" name="params">
        <el-form :model="sysParams" label-width="140px">
          <el-form-item label="系统名称">
            <el-input v-model="sysParams.systemName" style="width: 300px" />
          </el-form-item>
          <el-form-item label="乘坐率预警阈值">
            <el-slider v-model="sysParams.occupancyWarning" :min="50" :max="100" :format-tooltip="val => val + '%'" style="width: 300px" />
          </el-form-item>
          <el-form-item label="自动同步HR数据">
            <el-switch v-model="sysParams.autoSyncHR" />
          </el-form-item>
          <el-form-item label="同步频率">
            <el-select v-model="sysParams.syncFrequency" style="width: 200px">
              <el-option label="每小时" value="hourly" />
              <el-option label="每天" value="daily" />
              <el-option label="每周" value="weekly" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveParams">保存参数</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('ai')
const backupLoading = ref(false)
const saving = ref(false)
const testing = ref(false)

const backupList = ref([
  { id: 1, fileName: 'backup_20241213_auto.sql', fileSize: '2.3MB', type: 1, createTime: '2024-12-13 02:00:00' },
  { id: 2, fileName: 'backup_20241212_auto.sql', fileSize: '2.2MB', type: 1, createTime: '2024-12-12 02:00:00' },
  { id: 3, fileName: 'backup_20241210_manual.sql', fileSize: '2.1MB', type: 2, createTime: '2024-12-10 15:30:00' }
])

const aiConfig = reactive({
  provider: 'deepseek',
  deepseek: {
    apiKey: '',
    model: 'deepseek-chat',
    baseUrl: 'https://api.deepseek.com/v1'
  },
  qwen: {
    apiKey: '',
    model: 'qwen-turbo',
    baseUrl: 'https://dashscope.aliyuncs.com/api/v1'
  },
  wenxin: {
    apiKey: '',
    secretKey: '',
    model: 'ernie-bot-4'
  },
  rateLimit: 60,
  cacheTtl: 3600,
  cacheEnabled: true
})

const sysParams = reactive({
  systemName: '智能车厂管理系统',
  occupancyWarning: 70,
  autoSyncHR: true,
  syncFrequency: 'daily'
})

const createBackup = async () => {
  backupLoading.value = true
  setTimeout(() => {
    backupLoading.value = false
    ElMessage.success('数据备份成功')
    backupList.value.unshift({
      id: Date.now(),
      fileName: `backup_${new Date().toISOString().slice(0, 10).replace(/-/g, '')}_manual.sql`,
      fileSize: '2.4MB',
      type: 2,
      createTime: new Date().toLocaleString()
    })
  }, 1500)
}

const scheduleBackup = () => {
  ElMessage.info('定时备份设置功能开发中')
}

const downloadBackup = (row) => {
  ElMessage.success(`开始下载 ${row.fileName}`)
}

const restoreBackup = (row) => {
  ElMessageBox.confirm(`确定要恢复到 ${row.createTime} 的备份吗？此操作将覆盖当前数据！`, '警告', {
    confirmButtonText: '确定恢复',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('数据恢复成功')
  })
}

const deleteBackup = (row) => {
  ElMessageBox.confirm(`确定删除备份文件 ${row.fileName} 吗？`).then(() => {
    const index = backupList.value.findIndex(b => b.id === row.id)
    if (index > -1) backupList.value.splice(index, 1)
    ElMessage.success('删除成功')
  })
}

const saveAiConfig = () => {
  saving.value = true
  setTimeout(() => {
    saving.value = false
    ElMessage.success('AI配置保存成功')
  }, 1000)
}

const testAiConnection = () => {
  testing.value = true
  setTimeout(() => {
    testing.value = false
    ElMessage.success(`${aiConfig.provider} 连接测试成功！`)
  }, 2000)
}

const saveParams = () => {
  ElMessage.success('系统参数保存成功')
}
</script>

<style lang="scss" scoped>
.system-container {
  .page-title { margin: 0 0 20px; font-size: 20px; color: #333; }
  
  .backup-section {
    .backup-actions { margin-bottom: 20px; }
  }
  
  .ai-config-form {
    max-width: 700px;
    
    .provider-option {
      display: flex; align-items: center; gap: 6px;
      
      .provider-icon {
        width: 16px; height: 16px; border-radius: 4px;
      }
      
      .provider-icon-text {
        font-size: 14px;
      }
    }
  }
}
</style>
