<template>
  <div class="book-detail-page" v-if="book">
    <!-- 封面 + 基本信息 -->
    <div class="detail-header">
      <BookCover :book="book" class="detail-cover-wrap" />
      <div class="detail-info">
        <h1>{{ book.bookName }}</h1>
        <p class="detail-author">作者：{{ book.authorName }}</p>
        <div class="detail-meta">
          <el-tag size="small">{{ book.categoryName }}</el-tag>
          <span>⭐ {{ book.score || '—' }}</span>
          <span>📖 {{ book.wordCount || 0 }} 字</span>
          <span>👁 {{ book.visitCount || 0 }} 阅读</span>
          <span>❤ {{ book.likeCount || 0 }} 点赞</span>
          <span v-if="book.isVip === 1" class="vip-badge">👑 VIP</span>
        </div>
        <div class="detail-tags" v-if="book.tags && book.tags.length">
          <el-tag v-for="t in book.tags" :key="t.id || t" size="small" type="info" effect="plain" class="tag-item">
            {{ t.name || t }}
          </el-tag>
        </div>
        <p class="detail-desc">{{ book.bookDesc }}</p>
        <div class="detail-actions">
          <el-button type="primary" @click="startRead">📖 开始阅读</el-button>
          <el-button @click="toggleFavorite" :type="book.isFavorited ? 'warning' : 'default'">
            {{ book.isFavorited ? '❤ 取消收藏' : '🤍 收藏' }}
          </el-button>
          <el-button @click="scoreDialog = true">⭐ 评分</el-button>
        </div>
      </div>
    </div>

    <!-- 数据分析面板 -->
    <div class="data-panel" v-if="book.wordCount">
      <div class="data-item">
        <span class="data-label">总字数</span>
        <span class="data-value">{{ formatNumber(book.wordCount || 0) }}</span>
      </div>
      <div class="data-item">
        <span class="data-label">章节数</span>
        <span class="data-value">{{ chapters.length || book.chapterCount || '—' }}</span>
      </div>
      <div class="data-item">
        <span class="data-label">平均评分</span>
        <span class="data-value">⭐ {{ book.score || '—' }}</span>
      </div>
      <div class="data-item">
        <span class="data-label">总阅读量</span>
        <span class="data-value">{{ formatNumber(book.visitCount || 0) }}</span>
      </div>
      <div class="data-item">
        <span class="data-label">点赞数</span>
        <span class="data-value">❤ {{ formatNumber(book.likeCount || 0) }}</span>
      </div>
    </div>

    <!-- 章节目录 -->
    <section class="chapter-section">
      <h3>📑 章节目录 ({{ chapters.length }})</h3>
      <div class="chapter-grid">
        <router-link
          v-for="ch in chapters.slice(0, 20)"
          :key="ch.id"
          :to="`/read/${book.id}/${ch.id}`"
          class="chapter-link"
        >
          <span class="ch-num">{{ ch.chapterNum || ch.chapterNo || '' }}</span>
          <span class="ch-name">{{ ch.chapterName }}</span>
          <span v-if="ch.isVip === 1" class="ch-vip">👑</span>
        </router-link>
      </div>
      <p v-if="chapters.length === 0" class="empty-hint">暂无章节</p>
    </section>

    <!-- 评论区 -->
    <section class="comment-section">
      <h3>💬 评论区 ({{ commentPage.total }})</h3>
      <div class="comment-form" v-if="authStore.isLoggedIn">
        <el-input v-model="newComment" type="textarea" :rows="3" placeholder="写评论..." />
        <div class="comment-form-actions">
          <el-rate v-model="commentScore" :max="5" show-score text-color="#ff9900" />
          <el-button type="primary" size="small" @click="submitComment" :loading="submitting">发表</el-button>
        </div>
      </div>
      <div v-else class="comment-login-hint">
        <router-link to="/login">登录</router-link> 后评论
      </div>

      <div v-for="c in comments" :key="c.id" class="comment-item">
        <div class="comment-user">
          <el-avatar :size="28" :icon="UserFilled" />
          <div class="comment-user-info">
            <strong>{{ c.nickName || c.username }}</strong>
            <span class="comment-time">{{ formatTime(c.createTime) }}</span>
          </div>
          <el-rate v-if="c.score" :model-value="c.score" :max="5" size="small" disabled show-score text-color="#ff9900" />
        </div>
        <p class="comment-content">{{ c.content }}</p>
        <div class="comment-actions">
          <el-button text size="small" @click="showReplies(c)">💬 {{ c.replyCount || 0 }} 条回复</el-button>
          <el-button text size="small" type="danger" @click="deleteComment(c.id)" v-if="isCommentOwner(c)">删除</el-button>
        </div>
        <!-- 回复列表 -->
        <div v-if="c._showReplies" class="replies-area">
          <div v-for="r in c._replies" :key="r.id" class="reply-item">
            <strong>{{ r.nickName || r.username }}</strong>: {{ r.content }}
            <span class="reply-time">{{ formatTime(r.createTime) }}</span>
          </div>
          <div class="reply-form" v-if="authStore.isLoggedIn">
            <el-input v-model="c._replyText" size="small" placeholder="回复..." @keyup.enter="addReply(c)" />
          </div>
        </div>
      </div>

      <el-pagination
        v-if="commentPage.total > 0"
        layout="prev, next"
        :total="commentPage.total"
        :page-size="10"
        @current-change="loadComments"
      />
    </section>

    <!-- 评分弹窗 -->
    <el-dialog v-model="scoreDialog" title="为本书评分" width="300px" center>
      <div style="text-align:center">
        <el-rate v-model="scoreValue" :max="5" size="large" show-score text-color="#ff9900" />
      </div>
      <template #footer>
        <el-button @click="scoreDialog = false">取消</el-button>
        <el-button type="primary" @click="submitScore" :loading="scoring">发表</el-button>
      </template>
    </el-dialog>

    <!-- VIP 解锁弹窗 -->
    <el-dialog v-model="showVipDialog" title="👑 VIP 章节" width="400px" center>
      <div class="vip-dialog-body">
        <el-icon :size="48" color="#e6a23c"><StarFilled /></el-icon>
        <p>本书包含 VIP 章节。</p>
        <p class="vip-sub">升级 VIP 即可畅读全部内容！</p>
      </div>
      <template #footer>
        <el-button @click="showVipDialog = false">稍后再说</el-button>
        <el-button type="warning" @click="upgradeVipNow" :loading="vipLoading">立即升级</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, post, del } from '@/utils/api'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, StarFilled } from '@element-plus/icons-vue'
import BookCover from '@/components/BookCover.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const book = ref(null)
const chapters = ref([])
const comments = ref([])
const newComment = ref('')
const commentScore = ref(0)
const commentPage = ref({ total: 0, current: 1 })
const submitting = ref(false)
const scoreDialog = ref(false)
const scoreValue = ref(0)
const scoring = ref(false)
const showVipDialog = ref(false)
const vipLoading = ref(false)

onMounted(async () => {
  const id = route.params.id
  try {
    book.value = await get(`/front/novels/${id}`)
    // 加载目录
    const chs = await get(`/front/novels/${id}/chapters`)
    chapters.value = chs || []
  } catch (e) { /* silent */ }
  loadComments(1)
})

async function loadComments(pageNum) {
  const id = route.params.id
  try {
    const res = await get(`/front/novels/${id}/comments`, { pageNum, pageSize: 10 })
    comments.value = (res.list || []).map(c => ({ ...c, _showReplies: false, _replies: [], _replyText: '' }))
    commentPage.value = { total: res.total || 0, current: res.current || pageNum }
  } catch (e) { /* silent */ }
}

async function submitComment() {
  if (!newComment.value.trim()) return
  submitting.value = true
  try {
    await post(`/front/novels/${route.params.id}/comments`, {
      content: newComment.value, score: commentScore.value
    })
    ElMessage.success('评论已发表')
    newComment.value = ''
    commentScore.value = 0
    loadComments(1)
  } finally { submitting.value = false }
}

async function showReplies(c) {
  if (c._showReplies) { c._showReplies = false; return }
  try {
    const res = await get(`/front/comments/${c.id}/replies`, { pageNum: 1, pageSize: 20 })
    c._replies = res.list || []
    c._showReplies = true
  } catch (e) { /* silent */ }
}

async function addReply(c) {
  if (!c._replyText?.trim()) return
  try {
    await post(`/front/comments/${c.id}/replies`, { content: c._replyText })
    ElMessage.success('已回复')
    c._replyText = ''
    showReplies(c)
  } catch (e) { /* silent */ }
}

async function deleteComment(commentId) {
  try {
    await ElMessageBox.confirm('Delete this comment?', 'Confirm', { type: 'warning' })
    await del(`/front/comments/${commentId}`)
    ElMessage.success('已删除')
    loadComments(1)
  } catch (e) { /* silent */ }
}

function isCommentOwner(c) {
  return authStore.user && (c.userId === authStore.user.id || authStore.isAdmin)
}

async function toggleFavorite() {
  if (!authStore.isLoggedIn) { router.push('/login'); return }
  const id = route.params.id
  try {
    if (book.value.isFavorited) {
      await del(`/front/user/bookshelf/${id}`)
      book.value.isFavorited = false
      ElMessage.success('已取消收藏')
    } else {
      await post(`/front/user/bookshelf/${id}`)
      book.value.isFavorited = true
      ElMessage.success('已加入书架')
    }
  } catch (e) { /* silent */ }
}

async function submitScore() {
  if (!scoreValue.value) return
  scoring.value = true
  try {
    await post(`/front/user/score/${route.params.id}`, { score: scoreValue.value })
    ElMessage.success('评分已提交')
    scoreDialog.value = false
    book.value.score = scoreValue.value
  } finally { scoring.value = false }
}

function startRead() {
  const firstChapterId = chapters.value.length > 0 ? chapters.value[0].id : (book.value.lastChapterId || 1)
  router.push(`/read/${book.value.id}/${firstChapterId}`)
}

async function upgradeVipNow() {
  vipLoading.value = true
  try {
    await authStore.upgradeVip()
    ElMessage.success('VIP 已升级！')
    showVipDialog.value = false
  } finally { vipLoading.value = false }
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleDateString()
}

function formatNumber(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
</script>

<style scoped>
.detail-header {
  display: flex;
  gap: 24px;
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  margin-bottom: 20px;
}
.detail-cover-wrap { flex-shrink: 0; }
.detail-info { flex: 1; min-width: 0; }
.detail-info h1 { font-size: 24px; margin: 0 0 6px; }
.detail-author { color: #909399; margin: 0 0 12px; font-size: 15px; }
.detail-meta { display: flex; gap: 12px; align-items: center; margin-bottom: 10px; font-size: 13px; color: #606266; flex-wrap: wrap; }
.vip-badge { color: #e6a23c; font-weight: 700; font-size: 14px; }
.detail-tags { display: flex; gap: 6px; margin-bottom: 10px; flex-wrap: wrap; }
.tag-item { border-radius: 12px; }
.detail-desc { color: #606266; line-height: 1.7; margin-bottom: 16px; max-height: 100px; overflow-y: auto; font-size: 14px; }
.detail-actions { display: flex; gap: 12px; }

/* 数据面板 */
.data-panel {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}
.data-item {
  background: #fff;
  padding: 16px;
  border-radius: 10px;
  text-align: center;
}
.data-label { display: block; font-size: 12px; color: #909399; margin-bottom: 4px; }
.data-value { font-size: 18px; font-weight: 700; color: #303133; }

/* 章节 */
.chapter-section {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
}
.chapter-section h3 { margin-bottom: 12px; }
.chapter-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4px; }
.chapter-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 14px;
  color: #303133;
  transition: background 0.15s;
}
.chapter-link:hover { background: #f5f7fa; color: #409eff; }
.ch-num { color: #909399; font-size: 12px; min-width: 30px; }
.ch-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ch-vip { font-size: 10px; }
.empty-hint { text-align: center; padding: 30px 0; color: #909399; }

/* 评论 */
.comment-section {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
}
.comment-section h3 { margin-bottom: 16px; }
.comment-form { margin-bottom: 20px; }
.comment-form-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.comment-login-hint { text-align: center; padding: 12px; color: #909399; font-size: 14px; }
.comment-login-hint a { color: #409eff; }
.comment-item {
  padding: 14px 0;
  border-bottom: 1px solid #f2f3f5;
}
.comment-user { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.comment-user-info { display: flex; flex-direction: column; }
.comment-time { color: #c0c4cc; font-size: 12px; }
.comment-content { color: #303133; margin: 0 0 8px; line-height: 1.6; }
.comment-actions { display: flex; gap: 8px; }
.replies-area { margin-top: 10px; margin-left: 40px; padding: 10px; background: #fafafa; border-radius: 8px; }
.reply-item { padding: 4px 0; font-size: 13px; color: #606266; }
.reply-time { color: #c0c4cc; font-size: 11px; margin-left: 8px; }
.reply-form { margin-top: 8px; }

/* VIP dialog */
.vip-dialog-body { text-align: center; padding: 20px; }
.vip-dialog-body p { margin: 12px 0 0; font-size: 15px; }
.vip-sub { color: #909399; font-size: 13px; }

@media (max-width: 768px) {
  .detail-header { flex-direction: column; align-items: center; text-align: center; }
  .detail-meta { justify-content: center; }
  .detail-actions { justify-content: center; }
  .chapter-grid { grid-template-columns: 1fr; }
}
</style>