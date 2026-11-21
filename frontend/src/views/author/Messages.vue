<template>
  <div class="author-messages">
    <div class="page-header">
      <h2>私信</h2>
      <span v-if="unreadCount" class="unread-badge">{{ unreadCount }} 条未读</span>
    </div>

    <div v-if="messages.length === 0" class="empty-hint">暂无消息</div>

    <div v-for="m in messages" :key="m.id" class="message-item" :class="{ unread: m.senderId !== authStore.user?.id && !m.isRead }">
      <div class="msg-header">
        <el-avatar :size="28" :icon="UserFilled" />
        <div class="msg-header-info">
          <strong>{{ m.senderNickName || m.senderUsername || `用户${m.senderId}` }}</strong>
          <span class="msg-time">{{ formatTime(m.createTime) }}</span>
        </div>
      </div>
      <p class="msg-content">{{ m.content }}</p>
      <div class="msg-actions" v-if="m.senderId !== authStore.user?.id">
        <el-button size="small" @click="showReplyBox(m)">回复</el-button>
        <div v-if="m._showReply" class="reply-inline">
          <el-input v-model="m._replyText" size="small" placeholder="写回复..." @keyup.enter="sendReply(m)" />
          <el-button size="small" type="primary" @click="sendReply(m)" :loading="m._sending">发送</el-button>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="total > 0"
      layout="prev, next"
      :total="total"
      :page-size="pageSize"
      @current-change="loadMessages"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, post } from '@/utils/api'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'

const authStore = useAuthStore()

const messages = ref([])
const total = ref(0)
const unreadCount = ref(0)
const pageSize = 10

onMounted(async () => {
  try {
    const res = await get('/author/messages/unread-count')
    unreadCount.value = res?.count || 0
  } catch (e) { /* silent */ }
  loadMessages(1)
})

async function loadMessages(pageNum = 1) {
  // 作者端没有直接的消息列表接口，使用前端的 contacts 接口
  try {
    const contacts = await get('/front/messages/contacts') || []
    // Flatten contacts into messages
    const all = []
    for (const c of contacts) {
      const uid = c.senderId === authStore.user?.id ? c.receiverId : c.senderId
      try {
        const res = await get(`/front/messages/${uid}`, { pageNum: 1, pageSize: 20 })
        const msgs = (res.list || res || []).map(m => ({
          ...m,
          _showReply: false,
          _replyText: '',
          _sending: false
        }))
        all.push(...msgs)
      } catch (e) { /* silent */ }
    }
    messages.value = all.slice(0, pageSize)
    total.value = all.length
  } catch (e) { /* silent */ }
}

function showReplyBox(m) {
  m._showReply = !m._showReply
}

async function sendReply(m) {
  if (!m._replyText?.trim()) return
  m._sending = true
  try {
    const receiverId = m.senderId
    await post(`/author/messages/reply/${receiverId}`, { content: m._replyText })
    ElMessage.success('回复已发送')
    m._showReply = false
    m._replyText = ''
  } finally { m._sending = false }
}

function formatTime(t) { return t ? new Date(t).toLocaleString() : '' }
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 { margin: 0; }
.unread-badge { background: #f56c6c; color: #fff; padding: 2px 10px; border-radius: 10px; font-size: 12px; }
.message-item {
  background: #fff;
  padding: 16px;
  border-radius: 10px;
  margin-bottom: 10px;
}
.message-item.unread { border-left: 3px solid #409eff; }
.msg-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.msg-header-info { flex: 1; display: flex; justify-content: space-between; align-items: center; }
.msg-time { font-size: 12px; color: #c0c4cc; }
.msg-content { color: #606266; line-height: 1.6; margin: 0 0 8px; }
.msg-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.reply-inline { display: flex; gap: 6px; }
.empty-hint { text-align: center; padding: 60px 0; color: #909399; }
</style>