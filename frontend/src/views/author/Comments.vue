<template>
  <div class="author-comments">
    <div class="page-header">
      <h2>读者评论</h2>
      <el-select v-model="selectedBookId" placeholder="按作品筛选" clearable @change="loadComments">
        <el-option v-for="b in myBooks" :key="b.id" :label="b.bookName" :value="b.id" />
      </el-select>
    </div>

    <div v-if="comments.length === 0" class="empty-hint">暂无评论。发布章节来获取读者反馈吧！</div>

    <div v-for="c in comments" :key="c.id" class="comment-item">
      <div class="comment-header">
        <el-avatar :size="28" :icon="UserFilled" />
        <div class="comment-user-info">
          <strong>{{ c.nickName || c.username }}</strong>
          <span class="comment-meta">评论了 {{ c.bookName || '未知作品' }} · {{ formatTime(c.createTime) }}</span>
        </div>
        <el-rate v-if="c.score" :model-value="c.score" :max="5" size="small" disabled />
      </div>
      <p class="comment-content">{{ c.content }}</p>
      <div class="comment-actions">
        <el-button text size="small" @click="showReplyBox(c)">💬 回复</el-button>
        <div v-if="c._showReplyBox" class="reply-box">
          <el-input v-model="c._replyText" size="small" placeholder="写回复..." @keyup.enter="replyToComment(c)" />
          <el-button size="small" type="primary" @click="replyToComment(c)" :loading="c._replying">发送</el-button>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="total > 0"
      layout="prev, next"
      :total="total"
      :page-size="pageSize"
      @current-change="loadComments"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, post } from '@/utils/api'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'

const myBooks = ref([])
const selectedBookId = ref(null)
const comments = ref([])
const total = ref(0)
const pageSize = 10

onMounted(async () => {
  try { myBooks.value = await get('/author/books') || [] } catch (e) { /* silent */ }
  loadComments(1)
})

async function loadComments(pageNum = 1) {
  if (!selectedBookId.value) {
    // Load comments for first book by default
    if (myBooks.value.length > 0) {
      selectedBookId.value = myBooks.value[0].id
    } else return
  }
  try {
    const res = await get(`/author/comments/${selectedBookId.value}`, { pageNum, pageSize })
    comments.value = (res.list || []).map(c => ({ ...c, _showReplyBox: false, _replyText: '', _replying: false }))
    total.value = res.total || 0
  } catch (e) { /* silent */ }
}

function showReplyBox(c) {
  c._showReplyBox = !c._showReplyBox
}

async function replyToComment(c) {
  if (!c._replyText?.trim()) return
  c._replying = true
  try {
    await post(`/author/comments/${c.id}/reply`, { content: c._replyText })
    ElMessage.success('已回复')
    c._showReplyBox = false
    c._replyText = ''
  } finally { c._replying = false }
}

function formatTime(t) { return t ? new Date(t).toLocaleDateString() : '' }
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}
.page-header h2 { margin: 0; }
.comment-item {
  background: #fff;
  padding: 16px;
  border-radius: 10px;
  margin-bottom: 12px;
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.comment-user-info { flex: 1; display: flex; flex-direction: column; }
.comment-meta { font-size: 12px; color: #909399; }
.comment-content { color: #303133; line-height: 1.6; margin: 0 0 8px; }
.comment-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.reply-box { display: flex; gap: 8px; margin-top: 4px; }
.empty-hint { text-align: center; padding: 60px 0; color: #909399; }
</style>