<template>
  <div class="knowledge-container">
    <h2 class="page-title">📚 知识库管理</h2>
    
    <!-- 功能卡片 -->
    <el-row :gutter="20" class="function-row">
      <el-col :span="6">
        <div class="function-card" @click="refreshAllKnowledge">
          <div class="card-icon all">🔄</div>
          <div class="card-info">
            <h3>刷新全部知识库</h3>
            <p>清空所有知识向量，重新从文件 + MySQL运营记录构建</p>
            <el-tag type="danger" size="small">全量刷新</el-tag>
          </div>
          <div v-if="refreshingAll" class="loading-mask">
            <el-icon class="is-loading"><Loading /></el-icon>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="function-card" @click="refreshFileKnowledge">
          <div class="card-icon file">📄</div>
          <div class="card-info">
            <h3>刷新文件知识库</h3>
            <p>只删除并重新构建来源为文件知识库（txt文件）的知识向量</p>
            <el-tag type="warning" size="small">文件知识库</el-tag>
          </div>
          <div v-if="refreshingFile" class="loading-mask">
            <el-icon class="is-loading"><Loading /></el-icon>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="function-card" @click="refreshRecordKnowledge">
          <div class="card-icon record">📊</div>
          <div class="card-info">
            <h3>刷新运营记录知识库</h3>
            <p>只删除并重新构建来源为数据库运营记录的知识向量</p>
            <el-tag type="success" size="small">数据库运营记录</el-tag>
          </div>
          <div v-if="refreshingRecord" class="loading-mask">
            <el-icon class="is-loading"><Loading /></el-icon>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="function-card" @click="getKnowledgeStats">
          <div class="card-icon stats">📈</div>
          <div class="card-info">
            <h3>获取统计信息</h3>
            <p>查看当前知识库的统计信息，包括各来源数量、总字符数等</p>
            <el-tag type="info" size="small">查看统计</el-tag>
          </div>
          <div v-if="loadingStats" class="loading-mask">
            <el-icon class="is-loading"><Loading /></el-icon>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <!-- AI 对话区域 -->
    <el-card shadow="hover" class="chat-card">
      <template #header>
        <div class="card-header">
          <span>🤖 AI 智能助手</span>
          <div class="chat-actions">
            <el-button type="danger" link @click="clearChatHistory" :loading="clearingHistory">
              <el-icon><Delete /></el-icon>
              清空历史
            </el-button>
          </div>
        </div>
      </template>
      
      <!-- 对话消息列表 -->
      <div class="chat-messages" ref="messagesContainer">
        <div v-for="(msg, index) in chatMessages" :key="index" :class="['message', msg.role]">
          <div class="message-avatar">
            {{ msg.role === 'user' ? '👤' : '🤖' }}
          </div>
          <div class="message-content">
            <div class="message-text">{{ msg.content }}</div>
            <div class="message-time">{{ msg.timestamp }}</div>
          </div>
        </div>
        
        <!-- 加载状态 -->
        <div v-if="sending" class="message assistant">
          <div class="message-avatar">🤖</div>
          <div class="message-content">
            <div class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 输入区域 -->
      <div class="chat-input-area">
        <el-input
          v-model="userInput"
          type="textarea"
          :rows="3"
          placeholder="输入您的问题，例如：5号线上座率低，有什么优化建议？"
          @keydown.ctrl.enter="sendMessage"
          :disabled="sending"
        />
        <div class="input-actions">
          <span class="input-hint">Ctrl + Enter 发送</span>
          <el-button type="primary" @click="sendMessage" :loading="sending" :disabled="!userInput.trim()">
            发送
          </el-button>
        </div>
      </div>
    </el-card>
    
    <!-- 统计信息展示区 -->
    <el-card shadow="hover" class="stats-card">
      <template #header>
        <div class="card-header">
          <span>📊 知识库统计信息</span>
          <el-button type="primary" link @click="getKnowledgeStats" :loading="loadingStats">
            <el-icon><Refresh /></el-icon>
            刷新统计
          </el-button>
        </div>
      </template>
      
      <div v-if="statsInfo" class="stats-content">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ statsInfo.totalCount || 0 }}</div>
              <div class="stat-label">总知识条数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ statsInfo.fileSourceCount || 0 }}</div>
              <div class="stat-label">文件来源</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ statsInfo.recordSourceCount || 0 }}</div>
              <div class="stat-label">运营记录来源</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item">
              <div class="stat-value">{{ formatNumber(statsInfo.totalChars) || 0 }}</div>
              <div class="stat-label">总字符数</div>
            </div>
          </el-col>
        </el-row>
        
        <el-row :gutter="20" class="stats-sub">
          <el-col :span="12">
            <div class="stat-sub-item">
              <span class="label">平均每条知识：</span>
              <span class="value">{{ statsInfo.avgChars || 0 }} 字符</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="stat-sub-item">
              <span class="label">最后更新时间：</span>
              <span class="value">{{ getCurrentTime() }}</span>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <div v-else class="stats-placeholder">
        <el-empty description="暂无统计信息，点击上方卡片或刷新统计按钮获取" :image-size="100" />
      </div>
    </el-card>
    
    <!-- 操作日志 -->
    <el-card shadow="hover" class="log-card">
      <template #header>
        <div class="card-header">
          <span>📝 操作日志</span>
          <el-button type="primary" link @click="clearLogs">
            <el-icon><Delete /></el-icon>
            清空日志
          </el-button>
        </div>
      </template>
      
      <div class="log-list">
        <div v-for="(log, index) in logs" :key="index" class="log-item">
          <span class="log-time">{{ log.time }}</span>
          <span :class="['log-type', log.type]">{{ log.typeText }}</span>
          <span class="log-message">{{ log.message }}</span>
        </div>
        <div v-if="logs.length === 0" class="log-empty">
          <el-empty description="暂无操作日志" :image-size="60" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Refresh, Delete } from '@element-plus/icons-vue'
import { aiApi } from '@/api/index'

// 刷新状态
const refreshingAll = ref(false)
const refreshingFile = ref(false)
const refreshingRecord = ref(false)
const loadingStats = ref(false)
const statsInfo = ref(null)

// 对话相关状态
const userInput = ref('')
const sending = ref(false)
const clearingHistory = ref(false)
const chatMessages = ref([])
const messagesContainer = ref(null)

// 操作日志
const logs = ref([])

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

/**
 * 添加对话消息
 */
const addChatMessage = (role, content) => {
  const now = new Date()
  const timestamp = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
  
  chatMessages.value.push({
    role,
    content,
    timestamp
  })
  scrollToBottom()
}

/**
 * 发送消息 - 使用 AI 建议对话接口
 */
const sendMessage = async () => {
  const question = userInput.value.trim()
  if (!question) return
  
  // 添加用户消息
  addChatMessage('user', question)
  userInput.value = ''
  sending.value = true
  
  try {
    // 调用 AI 建议对话接口
    const res = await aiApi.optimizeChat({ question })
    
    if (res.code === 200 && res.data) {
      addChatMessage('assistant', res.data)
      addLog('info', `AI对话: ${question.substring(0, 50)}${question.length > 50 ? '...' : ''}`)
    } else {
      ElMessage.error(res.message || 'AI响应失败')
      addChatMessage('assistant', '抱歉，处理您的问题时出现了错误，请稍后重试。')
      addLog('error', `AI对话失败: ${res.message || '未知错误'}`)
    }
  } catch (error) {
    console.error('发送消息失败', error)
    ElMessage.error(error.message || '发送消息失败，请稍后重试')
    addChatMessage('assistant', '抱歉，网络连接失败，请检查网络后重试。')
    addLog('error', `AI对话异常: ${error.message}`)
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

/**
 * 清空对话历史
 */
const clearChatHistory = async () => {
  try {
    await ElMessageBox.confirm(
      '此操作将清空所有对话历史记录，确定继续吗？',
      '确认清空对话历史',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    clearingHistory.value = true
    // 调用清空对话历史接口
    const res = await aiApi.clearOptimizeChatHistory()
    
    if (res.code === 200) {
      chatMessages.value = []
      ElMessage.success('对话历史已清空')
      addLog('success', '清空对话历史成功')
    } else {
      throw new Error(res.message || '清空失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '清空对话历史失败')
      addLog('error', error.message || '清空对话历史失败')
    }
  } finally {
    clearingHistory.value = false
  }
}

/**
 * 添加日志
 */
const addLog = (type, message) => {
  const now = new Date()
  const time = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
  
  const typeMap = {
    success: { text: '成功', class: 'success' },
    error: { text: '失败', class: 'error' },
    info: { text: '信息', class: 'info' }
  }
  
  logs.value.unshift({
    time,
    type: typeMap[type]?.class || 'info',
    typeText: typeMap[type]?.text || '信息',
    message
  })
  
  // 只保留最近50条日志
  if (logs.value.length > 50) {
    logs.value = logs.value.slice(0, 50)
  }
}

/**
 * 清空日志
 */
const clearLogs = () => {
  logs.value = []
  ElMessage.success('日志已清空')
}

/**
 * 格式化数字
 */
const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  return num.toLocaleString()
}

/**
 * 获取当前时间
 */
const getCurrentTime = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
}

/**
 * 功能1：刷新全部知识库
 */
const refreshAllKnowledge = async () => {
  try {
    await ElMessageBox.confirm(
      '此操作将清空所有知识向量，并重新从文件和数据库构建知识库，确定继续吗？\n\n⚠️ 注意：此操作可能需要较长时间，请耐心等待！',
      '确认刷新全部知识库',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    refreshingAll.value = true
    addLog('info', '开始刷新全部知识库...')
    
    const res = await aiApi.refreshAllKnowledge()
    
    if (res.code === 200) {
      ElMessage.success(res.message || '全部知识库刷新成功')
      addLog('success', res.message || '全部知识库刷新成功')
      await getKnowledgeStats()
    } else {
      throw new Error(res.message || '刷新失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '刷新全部知识库失败')
      addLog('error', error.message || '刷新全部知识库失败')
    }
  } finally {
    refreshingAll.value = false
  }
}

/**
 * 功能2：只刷新来源为 file 的知识
 */
const refreshFileKnowledge = async () => {
  try {
    await ElMessageBox.confirm(
      '此操作将删除所有文件来源的知识向量，并重新从txt文件构建，确定继续吗？',
      '确认刷新文件知识库',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    refreshingFile.value = true
    addLog('info', '开始刷新文件知识库...')
    
    const res = await aiApi.refreshFileKnowledge()
    
    if (res.code === 200) {
      ElMessage.success(res.message || '文件知识库刷新成功')
      addLog('success', res.message || '文件知识库刷新成功')
      await getKnowledgeStats()
    } else {
      throw new Error(res.message || '刷新失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '刷新文件知识库失败')
      addLog('error', error.message || '刷新文件知识库失败')
    }
  } finally {
    refreshingFile.value = false
  }
}

/**
 * 功能3：只刷新来源为 operation_record 的知识
 */
const refreshRecordKnowledge = async () => {
  try {
    await ElMessageBox.confirm(
      '此操作将删除所有运营记录来源的知识向量，并重新从数据库构建，确定继续吗？',
      '确认刷新运营记录知识库',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    refreshingRecord.value = true
    addLog('info', '开始刷新运营记录知识库...')
    
    const res = await aiApi.refreshRecordKnowledge()
    
    if (res.code === 200) {
      ElMessage.success(res.message || '运营记录知识库刷新成功')
      addLog('success', res.message || '运营记录知识库刷新成功')
      await getKnowledgeStats()
    } else {
      throw new Error(res.message || '刷新失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '刷新运营记录知识库失败')
      addLog('error', error.message || '刷新运营记录知识库失败')
    }
  } finally {
    refreshingRecord.value = false
  }
}

/**
 * 功能4：获取知识库统计信息
 */
const getKnowledgeStats = async () => {
  try {
    loadingStats.value = true
    addLog('info', '正在获取知识库统计信息...')
    
    const res = await aiApi.getKnowledgeStats()
    
    if (res.code === 200 && res.data) {
      statsInfo.value = res.data
      ElMessage.success('获取统计信息成功')
      addLog('success', `获取统计信息成功 - 总条数: ${res.data.totalCount}, 文件: ${res.data.fileSourceCount}, 运营记录: ${res.data.recordSourceCount}`)
    } else {
      throw new Error(res.message || '获取统计信息失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '获取统计信息失败')
    addLog('error', error.message || '获取统计信息失败')
    statsInfo.value = null
  } finally {
    loadingStats.value = false
  }
}
</script>

<style lang="scss" scoped>
.knowledge-container {
  padding: 20px;
  
  .page-title {
    margin: 0 0 20px;
    font-size: 20px;
    color: #333;
  }
  
  .function-row {
    margin-bottom: 20px;
    
    .function-card {
      background: white;
      border-radius: 12px;
      padding: 20px;
      display: flex;
      align-items: flex-start;
      gap: 16px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      cursor: pointer;
      transition: all 0.3s ease;
      position: relative;
      overflow: hidden;
      
      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
      }
      
      .card-icon {
        width: 56px;
        height: 56px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
        flex-shrink: 0;
        
        &.all { background: linear-gradient(135deg, #f093fb, #f5576c); }
        &.file { background: linear-gradient(135deg, #fa709a, #fee140); }
        &.record { background: linear-gradient(135deg, #11998e, #38ef7d); }
        &.stats { background: linear-gradient(135deg, #667eea, #764ba2); }
      }
      
      .card-info {
        flex: 1;
        
        h3 {
          font-size: 16px;
          margin: 0 0 8px;
          color: #333;
        }
        
        p {
          font-size: 13px;
          color: #666;
          margin: 0 0 12px;
          line-height: 1.5;
        }
      }
      
      .loading-mask {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(255, 255, 255, 0.8);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;
        color: #2E75B6;
      }
    }
  }
  
  // AI 对话区域样式
  .chat-card {
    margin-bottom: 20px;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .chat-messages {
      height: 400px;
      overflow-y: auto;
      padding: 16px;
      background: #f5f7fa;
      border-radius: 8px;
      margin-bottom: 16px;
      
      .message {
        display: flex;
        gap: 12px;
        margin-bottom: 16px;
        
        &.user {
          flex-direction: row-reverse;
          
          .message-avatar {
            background: linear-gradient(135deg, #667eea, #764ba2);
          }
          
          .message-content {
            background: linear-gradient(135deg, #667eea, #764ba2);
            color: white;
            
            .message-time {
              color: rgba(255, 255, 255, 0.7);
            }
          }
        }
        
        &.assistant {
          .message-avatar {
            background: linear-gradient(135deg, #11998e, #38ef7d);
          }
          
          .message-content {
            background: white;
            color: #333;
            
            .message-time {
              color: #999;
            }
          }
        }
        
        .message-avatar {
          width: 40px;
          height: 40px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 20px;
          flex-shrink: 0;
          color: white;
        }
        
        .message-content {
          max-width: 70%;
          padding: 12px 16px;
          border-radius: 12px;
          
          .message-text {
            line-height: 1.5;
            white-space: pre-wrap;
            word-break: break-word;
          }
          
          .message-time {
            font-size: 11px;
            margin-top: 6px;
            text-align: right;
          }
        }
      }
      
      .typing-indicator {
        display: flex;
        gap: 4px;
        padding: 8px 0;
        
        span {
          width: 8px;
          height: 8px;
          background: #999;
          border-radius: 50%;
          animation: typing 1.4s infinite ease-in-out;
          
          &:nth-child(1) { animation-delay: 0s; }
          &:nth-child(2) { animation-delay: 0.2s; }
          &:nth-child(3) { animation-delay: 0.4s; }
        }
      }
    }
    
    .chat-input-area {
      .input-actions {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 12px;
        
        .input-hint {
          font-size: 12px;
          color: #999;
        }
      }
    }
  }
  
  @keyframes typing {
    0%, 60%, 100% {
      transform: translateY(0);
      opacity: 0.4;
    }
    30% {
      transform: translateY(-6px);
      opacity: 1;
    }
  }
  
  .stats-card {
    margin-bottom: 20px;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .stats-content {
      .stat-item {
        text-align: center;
        padding: 16px;
        background: #f5f7fa;
        border-radius: 8px;
        
        .stat-value {
          font-size: 32px;
          font-weight: bold;
          color: #2E75B6;
        }
        
        .stat-label {
          font-size: 14px;
          color: #666;
          margin-top: 8px;
        }
      }
      
      .stats-sub {
        margin-top: 20px;
        
        .stat-sub-item {
          padding: 12px 16px;
          background: #f9f9f9;
          border-radius: 8px;
          
          .label {
            color: #666;
            font-size: 13px;
          }
          
          .value {
            color: #333;
            font-size: 14px;
            font-weight: bold;
            margin-left: 8px;
          }
        }
      }
    }
    
    .stats-placeholder {
      padding: 40px;
    }
  }
  
  .log-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .log-list {
      max-height: 400px;
      overflow-y: auto;
      
      .log-item {
        display: flex;
        align-items: center;
        gap: 16px;
        padding: 12px 16px;
        border-bottom: 1px solid #eee;
        font-size: 13px;
        
        &:hover {
          background: #f5f7fa;
        }
        
        .log-time {
          color: #999;
          font-family: monospace;
          width: 160px;
          flex-shrink: 0;
        }
        
        .log-type {
          padding: 2px 8px;
          border-radius: 4px;
          font-size: 12px;
          font-weight: bold;
          width: 50px;
          text-align: center;
          flex-shrink: 0;
          
          &.success {
            background: #f0f9eb;
            color: #67c23a;
          }
          
          &.error {
            background: #fef0f0;
            color: #f56c6c;
          }
          
          &.info {
            background: #e9f4ff;
            color: #409eff;
          }
        }
        
        .log-message {
          flex: 1;
          color: #555;
        }
      }
      
      .log-empty {
        padding: 40px;
      }
    }
  }
}
</style>