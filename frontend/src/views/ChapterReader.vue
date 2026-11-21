<template>
  <div class="reader-page" v-if="chapter">
    <!-- 顶部导航栏 -->
    <div class="reader-header">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h2>{{ chapter.chapterName }}</h2>
      <div class="reader-nav">
        <el-button size="small" :disabled="!prevChapterId" @click="goChapter(prevChapterId)">上一章</el-button>
        <el-select v-model="currentChapterId" placeholder="章节" size="small" style="width:180px" @change="goChapter">
          <el-option v-for="ch in chapters" :key="ch.id" :label="ch.chapterName" :value="ch.id" />
        </el-select>
        <el-button size="small" :disabled="!nextChapterId" @click="goChapter(nextChapterId)">下一章</el-button>
      </div>
    </div>

    <div class="reader-body">
      <!-- 正文区 -->
      <div class="reader-content-area">
        <div class="reader-content" v-html="chapterHtml"></div>

        <!-- 底部操作 -->
        <div class="reader-footer-actions">
          <el-button size="small" @click="toggleLike" :type="liked ? 'danger' : 'default'" plain>
            {{ liked ? '❤️ 已点赞' : '🤍 点赞' }}
          </el-button>
          <el-button size="small" @click="showBookmarkDialog = true" plain>🔖 书签</el-button>
          <el-button size="small" @click="openAiPanel" plain>🤖 AI 助手</el-button>
        </div>
      </div>

      <!-- AI 阅读助手侧边栏 -->
      <div class="ai-panel" v-if="showAiPanel">
        <div class="ai-panel-header">
          <h4>AI 阅读助手</h4>
          <el-button text size="small" @click="showAiPanel = false">✕</el-button>
        </div>
        <div class="ai-messages">
          <div v-for="(msg, i) in aiMessages" :key="i" class="ai-msg" :class="msg.role">
            <strong>{{ msg.role === 'user' ? '你' : 'AI' }}</strong>
            <p>{{ msg.content }}</p>
          </div>
        </div>
        <div class="ai-input-area">
          <el-input v-model="aiPrompt" placeholder="询问章节内容..." size="small" @keyup.enter="askAi" />
          <el-button type="primary" size="small" @click="askAi" :loading="aiLoading">发送</el-button>
        </div>
      </div>
    </div>

    <!-- VIP 解锁弹窗 -->
    <el-dialog v-model="showVipDialog" title="VIP 章节" width="400px" center>
      <div class="vip-dialog-body">
        <el-icon :size="48" color="#e6a23c"><StarFilled /></el-icon>
        <p>本章为 VIP 章节。</p>
        <p class="vip-sub">升级 VIP 即可继续阅读！</p>
      </div>
      <template #footer>
        <el-button @click="showVipDialog = false">取消</el-button>
        <el-button type="warning" @click="upgradeVipNow" :loading="vipLoading">升级 VIP</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, postAi } from '@/utils/api'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { StarFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const chapter = ref(null)
const chapters = ref([])
const prevChapterId = ref(null)
const nextChapterId = ref(null)
const currentChapterId = ref(null)
const liked = ref(false)
const showAiPanel = ref(false)
const aiMessages = ref([])
const aiPrompt = ref('')
const aiLoading = ref(false)
const showVipDialog = ref(false)
const showBookmarkDialog = ref(false)
const vipLoading = ref(false)

// 自动保存进度计时器
let progressTimer = null

const chapterHtml = computed(() => {
  if (!chapter.value || !chapter.value.content) return ''
  return chapter.value.content.replace(/\n/g, '<br>')
})

onMounted(async () => {
  const bookId = route.params.bookId
  const chapterId = route.params.chapterId
  currentChapterId.value = String(chapterId)
  // 加载目录
  try {
    const chs = await get(`/front/novels/${bookId}/chapters`)
    chapters.value = chs || []
  } catch (e) { /* silent */ }
  await loadChapter(chapterId)
})

watch(currentChapterId, () => {
  if (progressTimer) clearTimeout(progressTimer)
  progressTimer = setTimeout(saveProgress, 5000)
})

async function loadChapter(chapterId) {
  try {
    const data = await get(`/front/novels/${route.params.bookId}/chapters/${chapterId}`)
    chapter.value = data
    prevChapterId.value = data.prevChapterId || null
    nextChapterId.value = data.nextChapterId || null
    // Check VIP
    if (data.isVip && !authStore.isVip) {
      showVipDialog.value = true
    }
    // Save progress
    setTimeout(saveProgress, 2000)
  } catch (e) {
    ElMessage.error('加载章节失败')
  }
}

async function saveProgress() {
  try {
    await post(`/front/novels/${route.params.bookId}/progress`, {
      chapterId: currentChapterId.value
    })
  } catch (e) { /* silent */ }
}

function goChapter(chapterId) {
  if (!chapterId) return
  currentChapterId.value = String(chapterId)
  router.replace(`/read/${route.params.bookId}/${chapterId}`)
  loadChapter(chapterId)
}

function toggleLike() {
  if (!authStore.isLoggedIn) { router.push('/login'); return }
  liked.value = !liked.value
}

async function openAiPanel() {
  if (!authStore.isLoggedIn) { router.push('/login'); return }
  showAiPanel.value = !showAiPanel.value
}

async function askAi() {
  if (!aiPrompt.value.trim()) return
  aiMessages.value.push({ role: 'user', content: aiPrompt.value })
  aiLoading.value = true
  try {
    const res = await postAi(`/front/ai/chat/${route.params.bookId}`, { question: aiPrompt.value })
    const answer = res?.answer || res?.content || res?.text || '(AI 未返回内容)'
    aiMessages.value.push({ role: 'assistant', content: answer })
  } catch (e) {
    aiMessages.value.push({ role: 'assistant', content: '抱歉，AI 服务暂不可用。' })
  } finally {
    aiLoading.value = false
    aiPrompt.value = ''
  }
}

async function upgradeVipNow() {
  vipLoading.value = true
  try {
    await authStore.upgradeVip()
    ElMessage.success('VIP 已升级！畅快阅读吧。')
    showVipDialog.value = false
  } finally {
    vipLoading.value = false
  }
}
</script>

<style scoped>
.reader-page {
  max-width: 900px;
  margin: 0 auto;
}

.reader-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 8px;
}
.reader-header h2 { font-size: 18px; margin: 0; }
.reader-nav { display: flex; gap: 8px; align-items: center; }

.reader-body { display: flex; gap: 20px; }
.reader-content-area { flex: 1; min-width: 0; }

.reader-content {
  line-height: 2;
  font-size: 16px;
  color: #303133;
  padding: 20px 0;
  min-height: 50vh;
}

.reader-footer-actions {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-top: 1px solid #ebeef5;
  margin-top: 16px;
}

/* AI 侧边栏 */
.ai-panel {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  display: flex;
  flex-direction: column;
  height: fit-content;
  max-height: 60vh;
  position: sticky;
  top: 72px;
}

.ai-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}
.ai-panel-header h4 { margin: 0; font-size: 15px; }

.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  max-height: 300px;
}

.ai-msg {
  margin-bottom: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 13px;
}
.ai-msg.user { background: #ecf5ff; }
.ai-msg.assistant { background: #f5f7fa; }
.ai-msg strong { display: block; font-size: 11px; color: #909399; margin-bottom: 4px; }
.ai-msg p { margin: 0; line-height: 1.5; }

.ai-input-area {
  display: flex;
  gap: 6px;
  padding: 10px 12px;
  border-top: 1px solid #ebeef5;
}

/* VIP 弹窗 */
.vip-dialog-body {
  text-align: center;
  padding: 20px;
}
.vip-dialog-body p { margin: 12px 0 0; font-size: 15px; }
.vip-sub { color: #909399; font-size: 13px; }

@media (max-width: 768px) {
  .reader-body { flex-direction: column; }
  .ai-panel { width: 100%; position: static; }
  .reader-nav { width: 100%; justify-content: center; }
}
</style>