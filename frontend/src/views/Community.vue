<template>
  <div class="community-page">
    <h2>社区</h2>

    <!-- 发帖表单 -->
    <div class="post-form" v-if="authStore.isLoggedIn">
      <el-input v-model="newTitle" placeholder="帖子标题" style="margin-bottom:10px" />
      <el-input v-model="newContent" type="textarea" :rows="3" placeholder="分享你的想法..." />
      <el-button type="primary" size="small" @click="createPost" style="margin-top:10px" :loading="posting">
        发布
      </el-button>
    </div>
    <div v-else class="login-hint">
      <router-link to="/login">登录</router-link> 后发帖
    </div>

    <!-- 帖子列表 -->
    <div v-for="p in posts" :key="p.id" class="post-item">
      <div class="post-header" @click="openPost(p)">
        <div class="post-title-row">
          <strong class="post-title">{{ p.title }}</strong>
          <span v-if="p.isTop" class="top-tag">📌 置顶</span>
        </div>
        <span class="post-meta">
          <el-avatar :size="20" :icon="UserFilled" />
          {{ p.nickName || p.username }}
          · {{ formatTime(p.createTime) }}
          · 👍 {{ p.likeCount || 0 }}
          · 💬 {{ p.replyCount || 0 }}
        </span>
      </div>
      <p class="post-content">{{ p.content }}</p>
      <div class="post-actions">
        <el-button text size="small" @click="openPost(p)">💬 {{ p.replyCount || 0 }} 条回复</el-button>
        <el-button text size="small" type="danger" v-if="canDelete(p)" @click="deletePost(p.id)">🗑 删除</el-button>
      </div>

      <!-- 展开的回复区 -->
      <div v-if="p._showReplies" class="post-replies">
        <div v-for="r in p._replies" :key="r.id" class="reply-item">
          <strong>{{ r.nickName || r.username }}</strong>
          <span class="reply-content">{{ r.content }}</span>
          <span class="reply-time">{{ formatTime(r.createTime) }}</span>
        </div>
        <div class="reply-form" v-if="authStore.isLoggedIn">
          <el-input v-model="p._replyText" size="small" placeholder="写回复..."
                    @keyup.enter="addReply(p)" />
          <el-button size="small" type="primary" @click="addReply(p)" :loading="p._replying">回复</el-button>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="total > 0"
      layout="prev, next"
      :total="total"
      :page-size="pageSize"
      @current-change="loadPosts"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, post, del } from '@/utils/api'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'

const authStore = useAuthStore()

const posts = ref([])
const total = ref(0)
const pageSize = 10
const newTitle = ref('')
const newContent = ref('')
const posting = ref(false)

onMounted(() => loadPosts(1))

async function loadPosts(pageNum = 1) {
  try {
    const res = await get('/front/community/posts', { pageNum, pageSize })
    posts.value = (res.list || []).map(p => ({ ...p, _showReplies: false, _replies: [], _replyText: '', _replying: false }))
    total.value = res.total || 0
  } catch (e) { /* silent */ }
}

async function createPost() {
  if (!newTitle.value.trim() || !newContent.value.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  posting.value = true
  try {
    await post('/front/community/posts', { title: newTitle.value, content: newContent.value })
    ElMessage.success('帖子已发布')
    newTitle.value = ''
    newContent.value = ''
    loadPosts(1)
  } finally { posting.value = false }
}

async function openPost(p) {
  if (p._showReplies) { p._showReplies = false; return }
  try {
    const res = await get(`/front/community/posts/${p.id}/replies`, { pageNum: 1, pageSize: 50 })
    p._replies = res.list || []
    p._showReplies = true
  } catch (e) { /* silent */ }
}

async function addReply(p) {
  if (!p._replyText?.trim()) return
  p._replying = true
  try {
    await post(`/front/community/posts/${p.id}/replies`, { content: p._replyText })
    ElMessage.success('已回复')
    p._replyText = ''
    openPost(p)
  } finally { p._replying = false }
}

async function deletePost(postId) {
  await ElMessageBox.confirm('删除这条帖子？', '确认', { type: 'warning' })
  await del(`/front/community/posts/${postId}`)
  ElMessage.success('已删除')
  loadPosts(1)
}

function canDelete(p) {
  return authStore.user && (p.userId === authStore.user.id || authStore.isAdmin)
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleDateString()
}
</script>

<style scoped>
.login-hint { text-align: center; padding: 12px; color: #909399; font-size: 14px; margin-bottom: 16px; }
.login-hint a { color: #409eff; }

.post-form {
  background: #fff;
  padding: 20px;
  border-radius: 10px;
  margin-bottom: 20px;
}

.post-item {
  background: #fff;
  padding: 16px 20px;
  border-radius: 10px;
  margin-bottom: 12px;
  transition: box-shadow 0.15s;
}
.post-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }

.post-header { cursor: pointer; margin-bottom: 8px; }
.post-title-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.post-title { font-size: 16px; }
.top-tag { font-size: 12px; color: #e6a23c; }

.post-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

.post-content { color: #606266; line-height: 1.6; margin: 0 0 10px; }

.post-actions { display: flex; gap: 8px; }

.post-replies {
  margin-top: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  border-left: 3px solid #409eff;
}
.reply-item {
  padding: 6px 0;
  font-size: 13px;
  color: #606266;
  border-bottom: 1px solid #f0f0f0;
}
.reply-item strong { margin-right: 6px; }
.reply-content { margin-right: 8px; }
.reply-time { color: #c0c4cc; font-size: 11px; }
.reply-form { display: flex; gap: 8px; margin-top: 10px; }
</style>