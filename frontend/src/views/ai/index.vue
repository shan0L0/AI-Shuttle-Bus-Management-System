<template>
  <div class="ai-container">
    <h2 class="page-title">🤖 AI智能助手</h2>
    
    <el-card shadow="hover" class="chat-card">
      <!-- AI头部 -->
      <div class="ai-header">
        <div class="ai-avatar">🤖</div>
        <div class="ai-info">
          <div class="ai-name">智能助手</div>
          <div class="ai-status">
            <span class="status-dot"></span>
            {{ aiProvider }} 在线
          </div>
        </div>
        <el-select v-model="aiProvider" size="small" style="width: 120px">
          <el-option label="DeepSeek" value="deepseek" />
          <el-option label="阿里千问" value="qwen" />
          <el-option label="百度文心" value="wenxin" />
        </el-select>
      </div>
      
      <!-- 聊天消息区 -->
      <div class="chat-messages" ref="messagesRef">
        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['message', msg.role]"
        ><!--此处根据消息的发出者动态设置图标-->
          <div class="message-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
          <div class="message-body">
            <div class="message-content" v-html="formatMessage(msg.content)"></div>
            <div class="message-time">{{ msg.time }}</div>
          </div>
        </div>
        
        <!-- 加载动画 -->
        <div v-if="loading" class="message assistant">
          <div class="message-avatar">🤖</div>
          <div class="message-body">
            <div class="message-content">
              <div class="typing">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 快捷问题 -->
      <div class="quick-questions">
        <p>💡 快捷提问：</p>
        <div class="quick-btns">
          <el-tag
            v-for="q in quickQuestions"
            :key="q"
            class="quick-btn"
            @click="askQuestion(q)"
          >
            {{ q }}
          </el-tag>
        </div>
      </div>
      
      <!-- 输入区 -->
      <div class="chat-input-area">
        <el-input
          v-model="inputMessage"
          placeholder="输入您的问题，如：哪些线路乘坐率低于70%？"
          @keyup.enter="sendMessage"
          :disabled="loading"
        >
          <template #suffix>
            <el-button
              type="primary"
              :icon="Promotion"
              circle
              @click="sendMessage"
              :loading="loading"
            />
          </template>
        </el-input>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted } from 'vue'
import { Promotion } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { aiApi } from '@/api/index'

const messagesRef = ref(null)
const loading = ref(false)
const inputMessage = ref('')
const aiProvider = ref('deepseek')

let tableMarkdown = null

const quickQuestions = [
  '哪些线路乘坐率低于70%？',
  '查询每条线路的运营车辆',
  '公司一共有多少员工？'
]

const messages = reactive([
  {
    role: 'assistant',
    content: `您好！我是智能查询助手，请告诉我您的查询需求。有什么可以帮您的？`,
    time: formatTime(new Date())
  }
])

// AI响应模拟数据
const aiResponses = {
  '乘坐率低于70%': {
    content: `根据系统数据分析，目前有 **3条线路** 乘坐率低于70%：

| 线路 | 乘坐率 | 乘客/容量 |
|------|--------|-----------|
| 🚌 5号线-海淀方向 | 52.5% | 21/40人 |
| 🚌 7号线-北苑方向 | 58.0% | 29/50人 |
| 🚌 8号线-立水桥方向 | 65.0% | 26/40人 |

💡 **建议**：对这3条线路进行优化，可考虑合并或调整发车时间。`
  },
  '下雨': {
    content: `根据天气预报，明天将有 **中雨**，预计对班车运营有以下影响：

🌧️ 预计乘车人数增加15-20%（自驾员工转乘班车）
⏰ 路况可能拥堵，建议发车时间提前10分钟
🚌 建议1、2、3号线增派1辆备用车

💡 **建议**：系统已自动通知相关驾驶员，是否需要发送全员通知？`
  },
  '运营数据': {
    content: `以下是 **本月运营数据** 统计：

📊 总运营班次：352班，较上月 **+4.7%**
👥 总乘客人次：11,572人次，较上月 **+12.9%**
📈 平均乘坐率：82.5%，较上月 **+6.3%**
💰 运营成本：¥186,500，人均成本¥16.1/天

💡 **建议**：整体运营效率较上月有明显提升，建议继续优化低乘坐率线路。`
  },
  '优化成本': {
    content: `基于当前数据，我为您分析了 **成本优化方案**：

🔄 合并5号线和8号线 → 预计年省 **¥4.2万**
🚐 7号线更换35座小车 → 预计年省 **¥1.8万**
⏰ 优化发车时间减少空驶 → 预计年省 **¥1.5万**
📍 新增龙华站点提升乘坐率 → 预计年省 **¥1.0万**

💡 **总结**：综合以上措施，预计年度可节省运营成本约 **¥8.5万元**。`
  }
}

function formatTime(date) {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function formatMessage(content) {
  return marked.parse(content)
}

//数组转 Markdown 表格函数
function arrayToMarkdownTable(data, columns) {
  if (!data || data.length === 0) return '';

  let header = '| ' + columns.map(c => c.displayName).join(' | ') + ' |';
  let separator = '| ' + columns.map(() => '---').join(' | ') + ' |';
  let rows = data.map(row => {
    return '| ' + columns.map(col => row[col.name]).join(' | ') + ' |';
  }).join('\n');

  return header + '\n' + separator + '\n' + rows;
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function askQuestion(question) {
  inputMessage.value = question
  sendMessage()
}

async function sendMessage() {
  const text = inputMessage.value.trim()
  if (!text || loading.value) return
  
  // 添加用户消息
  messages.push({
    role: 'user',
    content: text,
    time: formatTime(new Date())
  })
  inputMessage.value = ''
  scrollToBottom()
  
  // 显示加载状态
  loading.value = true
  
  //调用API
  try {
    const res = await aiApi.selectChat({
      userId: 1,
      message: text,
      sessionId: 'session-1'
    })
    
    if (res.data.data && res.data.data.length > 0) {
    // 将查询结果转换为 Markdown 表格字符串
    tableMarkdown = arrayToMarkdownTable(res.data.data, res.data.columns);
    console.log(tableMarkdown)
    }

    messages.push({
      role: 'assistant',
      content: tableMarkdown,
      time: formatTime(new Date())
    })
  } catch (error) {
    console.log(error)
    messages.push({
      role: 'assistant',
      content: '抱歉，AI服务暂时不可用，请稍后重试。',
      time: formatTime(new Date())
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style lang="scss" scoped>
.ai-container {
  .page-title {
    margin: 0 0 20px;
    font-size: 20px;
    color: #333;
  }
  
  .chat-card {
    :deep(.el-card__body) {
      padding: 0;
      display: flex;
      flex-direction: column;
      height: calc(100vh - 200px);
      max-height: 700px;
    }
  }
  
  .ai-header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 20px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: white;
    
    .ai-avatar {
      width: 48px;
      height: 48px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
    }
    
    .ai-info {
      flex: 1;
      
      .ai-name {
        font-weight: bold;
        font-size: 16px;
      }
      
      .ai-status {
        font-size: 12px;
        opacity: 0.9;
        display: flex;
        align-items: center;
        gap: 6px;
        
        .status-dot {
          width: 8px;
          height: 8px;
          background: #52c41a;
          border-radius: 50%;
        }
      }
    }
  }
  
  .chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    background: #f5f7fa;
    
    .message {
      display: flex;
      gap: 12px;
      margin-bottom: 20px;
      max-width: 80%;
      
      &.user {
        flex-direction: row-reverse;
        margin-left: auto;
        
        .message-avatar {
          background: #2E75B6;
        }
        
        .message-content {
          background: #2E75B6;
          color: black;
          
          :deep(a) {
            color: #fff;
          }
        }
      }
      
      &.assistant {
        .message-avatar {
          background: linear-gradient(135deg, #667eea, #764ba2);
        }
      }
      
      .message-avatar {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
        flex-shrink: 0;
      }
      
      .message-body {
        .message-content {
          background: white;
          padding: 12px 16px;
          border-radius: 12px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
          line-height: 1.6;
          
          :deep(p) {
            margin: 0 0 8px;
            &:last-child {
              margin: 0;
            }
          }
          
          :deep(table) {
            width: 100%;
            border-collapse: collapse;
            margin: 10px 0;
            
            th, td {
              border: 1px solid #eee;
              padding: 8px;
              text-align: left;
            }
            
            th {
              background: #f5f7fa;
            }
          }
          
          :deep(strong) {
            color: #2E75B6;
          }
        }
        
        .message-time {
          font-size: 11px;
          color: #999;
          margin-top: 4px;
        }
      }
    }
  }
  
  .quick-questions {
    padding: 12px 16px;
    background: #fafafa;
    border-top: 1px solid #f0f0f0;
    
    p {
      font-size: 12px;
      color: #999;
      margin: 0 0 8px;
    }
    
    .quick-btns {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      
      .quick-btn {
        cursor: pointer;
        transition: all 0.3s;
        
        &:hover {
          color: #2E75B6;
          border-color: #2E75B6;
        }
      }
    }
  }
  
  .chat-input-area {
    padding: 16px;
    background: white;
    border-top: 1px solid #f0f0f0;
  }
  
  .typing {
    display: flex;
    gap: 4px;
    padding: 8px 0;
    
    span {
      width: 8px;
      height: 8px;
      background: #999;
      border-radius: 50%;
      animation: typing 1.4s infinite both;
      
      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.4s; }
    }
  }
  
  @keyframes typing {
    0%, 60%, 100% { transform: translateY(0); }
    30% { transform: translateY(-10px); }
  }
}
</style>
