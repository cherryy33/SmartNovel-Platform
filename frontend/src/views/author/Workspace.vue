<template>
  <div class="author-workspace">
    <div class="ws-header">
      <h2>作者工作台</h2>
      <div class="ws-header-actions">
        <el-button type="primary" @click="$router.push('/author/books')">📖 管理作品</el-button>
        <el-button @click="showAiPopup = true">🤖 AI 写作</el-button>
      </div>
    </div>

    <!-- 作者信息卡片 -->
    <div class="ws-card" v-if="authorInfo">
      <h3>个人资料</h3>
      <div class="info-grid">
        <div class="info-row"><span class="label">笔名</span><span class="val">{{ authorInfo.penName }}</span></div>
        <div class="info-row"><span class="label">状态</span>
          <el-tag :type="statusType(authorInfo.status)" size="small">
            {{ statusText(authorInfo.status) }}
          </el-tag>
        </div>
        <div class="info-row" v-if="authorInfo.reviewComment">
          <span class="label">审核意见</span><span class="val">{{ authorInfo.reviewComment }}</span>
        </div>
        <div class="info-row" v-if="authorInfo.intro">
          <span class="label">简介</span><span class="val">{{ authorInfo.intro }}</span>
        </div>
      </div>
      <el-button size="small" @click="showEditInfo = true" style="margin-top:10px">编辑资料</el-button>
    </div>

    <!-- 未申请时显示申请表单 -->
    <div class="ws-card" v-if="!authorInfo && !applying">
      <h3>成为作者</h3>
      <p class="card-desc">申请在智阅小说发布你的作品！</p>
      <el-form @submit.prevent="applyAuthor">
        <el-form-item label="笔名">
          <el-input v-model="applyForm.penName" placeholder="你的笔名" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="applyForm.intro" type="textarea" :rows="3" placeholder="简要介绍" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="applying">提交申请</el-button>
      </el-form>
    </div>

    <!-- 快速统计 -->
    <div class="stats-row" v-if="authorInfo">
      <div class="stat-card">
        <span class="stat-num">{{ myBooks.length }}</span>
        <span class="stat-label">作品</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{{ totalWords.toLocaleString() }}</span>
        <span class="stat-label">总字数</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{{ totalReads.toLocaleString() }}</span>
        <span class="stat-label">总阅读量</span>
      </div>
    </div>

    <!-- 最近评论 -->
    <div class="ws-card" v-if="recentComments.length">
      <div class="card-header-row">
        <h3>最近评论</h3>
        <el-button text size="small" @click="$router.push('/author/comments')">查看全部 →</el-button>
      </div>
      <div v-for="c in recentComments.slice(0, 5)" :key="c.id" class="comment-mini">
        <strong>{{ c.nickName || c.username }}</strong>
        <span class="comment-book">评论了 {{ c.bookName }}</span>
        <p>{{ c.content }}</p>
        <span class="comment-time">{{ formatTime(c.createTime) }}</span>
      </div>
    </div>

    <!-- 最近私信 -->
    <div class="ws-card" v-if="unreadMsgCount > 0">
      <div class="card-header-row">
        <h3>私信</h3>
        <el-badge :value="unreadMsgCount" :max="99">
          <el-button text size="small" @click="$router.push('/author/messages')">查看全部 →</el-button>
        </el-badge>
      </div>
    </div>

    <!-- AI 写作助手弹窗 -->
    <el-dialog v-model="showAiPopup" title="🤖 AI 写作助手" width="600px" top="5vh">
      <div class="ai-popup-body">
        <div class="ai-messages-area">
          <div v-for="(msg, i) in aiMessages" :key="i" class="ai-msg" :class="msg.role">
            <strong>{{ msg.role === 'user' ? '你' : 'AI' }}</strong>
            <p>{{ msg.content }}</p>
          </div>
        </div>
        <div class="ai-input-row">
          <el-input v-model="aiPrompt" placeholder="请 AI 帮你写作、构思情节、塑造角色..."
                    @keyup.enter="askAi" />
          <el-button type="primary" @click="askAi" :loading="aiLoading">发送</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 编辑信息弹窗 -->
    <el-dialog v-model="showEditInfo" title="编辑作者信息" width="400px">
      <el-form @submit.prevent="updateInfo">
        <el-form-item label="笔名"><el-input v-model="editInfoForm.penName" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="editInfoForm.intro" type="textarea" :rows="3" /></el-form-item>
        <el-button type="primary" native-type="submit" :loading="updating">保存</el-button>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { get, post, postAi, put } from '@/utils/api'
import { ElMessage } from 'element-plus'

const authorInfo = ref(null)
const myBooks = ref([])
const recentComments = ref([])
const unreadMsgCount = ref(0)

const applying = ref(false)
const applyForm = ref({ penName: '', intro: '' })

const showAiPopup = ref(false)
const aiMessages = ref([])
const aiPrompt = ref('')
const aiLoading = ref(false)

const showEditInfo = ref(false)
const updating = ref(false)
const editInfoForm = ref({ penName: '', intro: '' })

const totalWords = computed(() => myBooks.value.reduce((s, b) => s + (b.wordCount || 0), 0))
const totalReads = computed(() => myBooks.value.reduce((s, b) => s + (b.visitCount || 0), 0))

onMounted(async () => {
  try {
    authorInfo.value = await get('/author/info')
  } catch (e) { /* not applied yet */ }

  if (authorInfo.value) {
    try {
      myBooks.value = await get('/author/books') || []
    } catch (e) { /* silent */ }
    try {
      const res = await get('/author/messages/unread-count')
      unreadMsgCount.value = res?.count || 0
    } catch (e) { /* silent */ }
    // 加载最近评论（取第一本书的评论）
    if (myBooks.value.length > 0) {
      try {
        const res = await get(`/author/comments/${myBooks.value[0].id}`, { pageNum: 1, pageSize: 5 })
        recentComments.value = res.list || []
      } catch (e) { /* silent */ }
    }
  }
})

async function applyAuthor() {
  if (!applyForm.value.penName.trim()) { ElMessage.warning('请输入笔名'); return }
  applying.value = true
  try {
    await post('/author/apply', applyForm.value)
    ElMessage.success('申请已提交！')
    authorInfo.value = await get('/author/info')
  } finally { applying.value = false }
}

async function updateInfo() {
  updating.value = true
  try {
    await put('/author/info', {
      penName: editInfoForm.value.penName,
      intro: editInfoForm.value.intro
    })
    ElMessage.success('已更新')
    showEditInfo.value = false
    authorInfo.value = await get('/author/info')
  } finally { updating.value = false }
}

function openEditInfo() {
  editInfoForm.value = {
    penName: authorInfo.value?.penName || '',
    intro: authorInfo.value?.intro || ''
  }
  showEditInfo.value = true
}

async function askAi() {
  if (!aiPrompt.value.trim()) return
  aiMessages.value.push({ role: 'user', content: aiPrompt.value })
  aiLoading.value = true
  try {
    const res = await postAi('/author/ai/ask', { question: aiPrompt.value })
    aiMessages.value.push({ role: 'assistant', content: res?.content || res?.text || JSON.stringify(res) })
  } catch (e) {
    aiMessages.value.push({ role: 'assistant', content: 'AI 服务暂不可用' })
  } finally {
    aiLoading.value = false
    aiPrompt.value = ''
  }
}

function formatTime(t) { return t ? new Date(t).toLocaleDateString() : '' }

// 后端 Jackson 把数字序列化为字符串（status 为 "0"/"1"/"2"），需归一化后再比较
function statusNum(s) {
  return s === null || s === undefined || s === '' ? null : Number(s)
}
function statusType(s) {
  const n = statusNum(s)
  return n === 1 ? 'success' : n === 0 ? 'warning' : 'danger'
}
function statusText(s) {
  const n = statusNum(s)
  return n === 1 ? '✓ 已通过' : n === 0 ? '⏳ 审核中' : '✗ 已拒绝'
}
</script>

<style scoped>
.ws-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}
.ws-header h2 { margin: 0; }
.ws-header-actions { display: flex; gap: 8px; }

.ws-card {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 16px;
}
.ws-card h3 { margin: 0 0 12px; }
.card-header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.card-header-row h3 { margin: 0; }
.card-desc { color: #909399; font-size: 14px; margin-bottom: 12px; }
.info-grid { display: flex; flex-direction: column; gap: 8px; }
.info-row { display: flex; gap: 12px; align-items: center; }
.label { color: #909399; width: 80px; flex-shrink: 0; }
.val { color: #303133; }

.stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
.stat-card {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  text-align: center;
}
.stat-num { display: block; font-size: 24px; font-weight: 700; color: #409eff; }
.stat-label { font-size: 13px; color: #909399; }

.comment-mini {
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}
.comment-mini strong { font-size: 13px; }
.comment-book { color: #909399; font-size: 12px; margin-left: 6px; }
.comment-mini p { margin: 4px 0; font-size: 13px; color: #606266; }
.comment-time { font-size: 11px; color: #c0c4cc; }

.ai-popup-body { display: flex; flex-direction: column; height: 350px; }
.ai-messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 12px;
}
.ai-msg { margin-bottom: 10px; padding: 8px 12px; border-radius: 8px; font-size: 13px; }
.ai-msg.user { background: #ecf5ff; }
.ai-msg.assistant { background: #f5f7fa; }
.ai-msg strong { display: block; font-size: 11px; color: #909399; margin-bottom: 4px; }
.ai-msg p { margin: 0; line-height: 1.5; white-space: pre-wrap; }
.ai-input-row { display: flex; gap: 8px; }
</style>