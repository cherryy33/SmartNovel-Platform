<template>
  <div class="admin-dashboard">
    <div class="page-header">
      <h2>📊 管理后台</h2>
      <span class="refresh-time" v-if="refreshTime">最近更新: {{ refreshTime }}</span>
    </div>

    <!-- 核心指标卡片 -->
    <div class="stat-cards">
      <div class="stat-card" v-for="card in statCards" :key="card.label" :style="{ borderTopColor: card.color }">
        <div class="stat-icon" :style="{ background: card.color }">{{ card.icon }}</div>
        <div class="stat-body">
          <span class="stat-value">{{ card.value }}</span>
          <span class="stat-label">{{ card.label }}</span>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-row">
      <!-- 分类数量分布饼状图 -->
      <div class="chart-card">
        <h3>📂 分类分布</h3>
        <div ref="pieChartRef" class="chart-container"></div>
      </div>

      <!-- 近7日阅读量柱状图 -->
      <div class="chart-card">
        <h3>📖 近7日阅读趋势</h3>
        <div ref="readChartRef" class="chart-container"></div>
      </div>
    </div>

    <div class="chart-row">
      <!-- 近7日新增用户柱状图 -->
      <div class="chart-card">
        <h3>👥 近7日新增用户趋势</h3>
        <div ref="userChartRef" class="chart-container"></div>
      </div>

      <!-- 合并趋势对比 -->
      <div class="chart-card">
        <h3>📈 阅读量 vs 新增用户（近7日）</h3>
        <div ref="combinedChartRef" class="chart-container"></div>
      </div>
    </div>

    <!-- AI 数据分析问答助手 -->
    <div class="ai-section">
      <div class="ai-header">
        <h3>🤖 AI 数据分析助手</h3>
        <span class="ai-hint">询问平台数据、趋势和深度分析</span>
      </div>
      <div class="ai-chat-area" ref="aiChatRef">
        <div v-if="aiMessages.length === 0" class="ai-welcome">
          <p>💡 试试问这些：</p>
          <ul>
            <li @click="quickAsk('Which category has the most books?')">"哪个分类下作品最多？"</li>
            <li @click="quickAsk('How is the reading trend this week?')">"本周阅读趋势如何？"</li>
            <li @click="quickAsk('Summarize the platform statistics')">"总结平台统计概况"</li>
            <li @click="quickAsk('What recommendations do you have for growth?')">"对平台增长有什么建议？"</li>
          </ul>
        </div>
        <div v-for="(msg, i) in aiMessages" :key="i" class="ai-msg" :class="msg.role">
          <div class="ai-msg-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
          <div class="ai-msg-body">
            <div class="ai-msg-text">{{ msg.content }}</div>
            <div class="ai-msg-time">{{ msg.time }}</div>
          </div>
        </div>
      </div>
      <div class="ai-input-row">
        <el-input
          v-model="aiQuestion"
          placeholder="询问平台数据、用户趋势、作品统计..."
          @keyup.enter="askAi"
          :disabled="aiLoading"
        />
        <el-button type="primary" @click="askAi" :loading="aiLoading" :disabled="!aiQuestion.trim()">
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { get, post } from '@/utils/api'
import { ElMessage } from 'element-plus'

const dashboard = ref({
  totalUsers: 0, totalAuthors: 0, totalBooks: 0, totalReads: 0, totalVipUsers: 0,
  categoryDistribution: [], dailyReadTrend: [], dailyNewUserTrend: []
})

const refreshTime = ref('')

// Chart refs
const pieChartRef = ref(null)
const readChartRef = ref(null)
const userChartRef = ref(null)
const combinedChartRef = ref(null)
const aiChatRef = ref(null)

let pieChart = null, readChart = null, userChart = null, combinedChart = null

// AI chat
const aiQuestion = ref('')
const aiMessages = ref([])
const aiLoading = ref(false)

const statCards = ref([
  { label: '用户总数', value: 0, icon: '👤', color: '#409eff' },
  { label: '作者数', value: 0, icon: '✍️', color: '#67c23a' },
  { label: '作品数', value: 0, icon: '📚', color: '#e6a23c' },
  { label: 'VIP 用户', value: 0, icon: '👑', color: '#f56c6c' },
  { label: '总阅读量', value: 0, icon: '👁', color: '#909399' }
])

onMounted(async () => {
  await loadDashboard()
  await nextTick()
  initCharts()
  window.addEventListener('resize', handleResize)
})

async function loadDashboard() {
  try {
    dashboard.value = await get('/admin/dashboard')
    statCards.value[0].value = dashboard.value.totalUsers || 0
    statCards.value[1].value = dashboard.value.totalAuthors || 0
    statCards.value[2].value = dashboard.value.totalBooks || 0
    statCards.value[3].value = dashboard.value.totalVipUsers || 0
    statCards.value[4].value = dashboard.value.totalReads || 0
    refreshTime.value = new Date().toLocaleTimeString()
  } catch (e) {
    ElMessage.error('加载后台数据失败')
  }
}

function initCharts() {
  initPieChart()
  initReadChart()
  initUserChart()
  initCombinedChart()
}

// ==================== 饼状图：分类分布 ====================
function initPieChart() {
  if (!pieChartRef.value) return
  if (pieChart) pieChart.dispose()
  pieChart = echarts.init(pieChartRef.value)

  const data = (dashboard.value.categoryDistribution || []).map(d => ({
    name: d.name,
    value: Number(d.value)
  }))

  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll', textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      emphasis: {
        label: { fontSize: 16, fontWeight: 'bold' },
        scaleSize: 10
      },
      data
    }]
  })
}

// ==================== 柱状图：近7日阅读量 ====================
function initReadChart() {
  if (!readChartRef.value) return
  if (readChart) readChart.dispose()
  readChart = echarts.init(readChartRef.value)

  const trend = dashboard.value.dailyReadTrend || []
  const dates = trend.map(d => d.date?.slice(5) || d.date) // MM-DD
  const values = trend.map(d => Number(d.value))

  readChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: values,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#409eff' },
          { offset: 1, color: '#a0cfff' }
        ]),
        borderRadius: [6, 6, 0, 0]
      },
      barWidth: '50%',
      label: { show: true, position: 'top', fontSize: 11 }
    }]
  })
}

// ==================== 柱状图：近7日新增用户 ====================
function initUserChart() {
  if (!userChartRef.value) return
  if (userChart) userChart.dispose()
  userChart = echarts.init(userChartRef.value)

  const trend = dashboard.value.dailyNewUserTrend || []
  const dates = trend.map(d => d.date?.slice(5) || d.date)
  const values = trend.map(d => Number(d.value))

  userChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: values,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#67c23a' },
          { offset: 1, color: '#b3e19d' }
        ]),
        borderRadius: [6, 6, 0, 0]
      },
      barWidth: '50%',
      label: { show: true, position: 'top', fontSize: 11 }
    }]
  })
}

// ==================== 合并趋势对比 ====================
function initCombinedChart() {
  if (!combinedChartRef.value) return
  if (combinedChart) combinedChart.dispose()
  combinedChart = echarts.init(combinedChartRef.value)

  const readTrend = dashboard.value.dailyReadTrend || []
  const userTrend = dashboard.value.dailyNewUserTrend || []
  const dates = readTrend.map(d => d.date?.slice(5) || d.date)
  const reads = readTrend.map(d => Number(d.value))
  const users = userTrend.map(d => Number(d.value))

  combinedChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['阅读量', '新增用户'], bottom: 0 },
    grid: { left: 45, right: 20, top: 20, bottom: 35 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '阅读量',
        type: 'line',
        data: reads,
        smooth: true,
        lineStyle: { width: 3, color: '#409eff' },
        itemStyle: { color: '#409eff' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64,158,255,0.25)' },
          { offset: 1, color: 'rgba(64,158,255,0.02)' }
        ])}
      },
      {
        name: '新增用户',
        type: 'line',
        data: users,
        smooth: true,
        lineStyle: { width: 3, color: '#67c23a' },
        itemStyle: { color: '#67c23a' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(103,194,58,0.25)' },
          { offset: 1, color: 'rgba(103,194,58,0.02)' }
        ])}
      }
    ]
  })
}

function handleResize() {
  pieChart?.resize()
  readChart?.resize()
  userChart?.resize()
  combinedChart?.resize()
}

// ==================== AI 助手 ====================
function quickAsk(q) {
  aiQuestion.value = q
  askAi()
}

async function askAi() {
  const q = aiQuestion.value.trim()
  if (!q || aiLoading.value) return

  aiMessages.value.push({ role: 'user', content: q, time: formatTime() })
  aiQuestion.value = ''
  aiLoading.value = true

  try {
    const res = await post('/admin/dashboard/ai-ask', { question: q })
    aiMessages.value.push({
      role: 'assistant',
      content: res?.answer || res?.text || JSON.stringify(res),
      time: formatTime()
    })
  } catch (e) {
    aiMessages.value.push({
      role: 'assistant',
      content: 'AI 服务暂不可用，请稍后再试。',
      time: formatTime()
    })
  } finally {
    aiLoading.value = false
    await nextTick()
    scrollAiChat()
  }
}

function scrollAiChat() {
  if (aiChatRef.value) {
    aiChatRef.value.scrollTop = aiChatRef.value.scrollHeight
  }
}

function formatTime() {
  return new Date().toLocaleTimeString()
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.page-header h2 { margin: 0; font-size: 22px; }
.refresh-time { font-size: 12px; color: #909399; }

/* ===== 指标卡片 ===== */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-top: 3px solid #409eff;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.stat-body {
  display: flex;
  flex-direction: column;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

/* ===== 图表 ===== */
.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.chart-card h3 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #303133;
}

.chart-container {
  width: 100%;
  height: 300px;
}

/* ===== AI 助手 ===== */
.ai-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.ai-header {
  margin-bottom: 16px;
}
.ai-header h3 {
  margin: 0 0 4px;
  font-size: 16px;
}
.ai-hint {
  font-size: 13px;
  color: #909399;
}

.ai-chat-area {
  max-height: 320px;
  overflow-y: auto;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 12px;
  min-height: 100px;
}

.ai-welcome {
  text-align: center;
  padding: 20px;
  color: #909399;
}
.ai-welcome p { margin: 0 0 8px; font-size: 14px; }
.ai-welcome ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.ai-welcome li {
  padding: 8px 12px;
  margin: 4px 0;
  background: #ecf5ff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.2s;
  color: #409eff;
}
.ai-welcome li:hover { background: #d9ecff; }

.ai-msg {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
.ai-msg.user { flex-direction: row-reverse; }
.ai-msg-avatar {
  font-size: 20px;
  flex-shrink: 0;
  width: 32px;
  text-align: center;
}
.ai-msg-body { max-width: 75%; }
.ai-msg-text {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.ai-msg.user .ai-msg-text {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.ai-msg.assistant .ai-msg-text {
  background: #f0f0f0;
  color: #303133;
  border-bottom-left-radius: 4px;
}
.ai-msg-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
  padding: 0 4px;
}

.ai-input-row {
  display: flex;
  gap: 10px;
}

@media (max-width: 768px) {
  .chart-row { grid-template-columns: 1fr; }
  .stat-cards { grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); }
}
</style>
