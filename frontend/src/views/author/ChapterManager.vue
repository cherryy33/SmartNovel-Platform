<template>
  <div class="chapter-manager">
    <div class="page-header">
      <el-button text @click="$router.push('/author/books')">← 返回作品列表</el-button>
      <div class="header-actions">
        <el-button @click="openAiExpand">🤖 AI 扩写</el-button>
        <el-button type="primary" @click="openAdd">+ 添加章节</el-button>
      </div>
    </div>

    <div v-if="chapters.length === 0" class="empty-hint">
      <p>还没有章节，开始写第一章吧！</p>
      <el-button type="primary" @click="openAdd">添加第一章</el-button>
    </div>

    <div v-for="ch in chapters" :key="ch.id" class="chapter-item">
      <div class="chapter-info">
        <span class="chapter-num">第{{ ch.chapterNum || ch.chapterNo || '' }}章</span>
        <span class="chapter-name">{{ ch.chapterName }}</span>
        <span class="chapter-word-count">{{ ch.wordCount || 0 }} 字</span>
        <el-tag v-if="Number(ch.isVip) === 1" size="small" type="warning">👑 VIP</el-tag>
      </div>
      <div class="chapter-actions">
        <el-button size="small" @click="editChapter(ch)">✏️ 编辑</el-button>
        <el-button size="small" type="danger" @click="deleteChapter(ch.id)">🗑 删除</el-button>
      </div>
    </div>

    <!-- 添加/编辑章节对话框 -->
    <el-dialog
      v-model="showAddDialog"
      :title="editing ? '编辑章节' : '添加章节'"
      width="750px"
      top="3vh"
      :close-on-click-modal="false"
    >
      <el-form v-loading="loadingDetail" @submit.prevent="saveChapter">
        <el-form-item label="标题">
          <el-input v-model="form.chapterName" placeholder="章节标题" />
        </el-form-item>
        <el-form-item label="VIP">
          <el-switch v-model="form.isVip" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="form.content" type="textarea" :rows="16" placeholder="在此书写章节内容..." :readonly="loadingDetail" />
          <div class="editor-toolbar">
            <span class="word-count">{{ form.content?.length || 0 }} 字符</span>
            <el-button size="small" @click="openAiExpandForEditor">🤖 AI 扩写正文</el-button>
          </div>
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="saving" :disabled="loadingDetail">保存章节</el-button>
      </el-form>
    </el-dialog>

    <!-- AI 扩写弹窗 -->
    <el-dialog v-model="showAiDialog" title="🤖 AI 内容扩写" width="600px" top="5vh" @closed="stopAiExpand">
      <p class="ai-hint">{{ aiFromEditor ? '根据当前正文进行扩写，结果会实时生成。' : '请先打开某个章节的编辑框并填写正文，再使用扩写。' }}</p>
      <el-input v-model="aiExpandPrompt" type="textarea" :rows="2" placeholder="可选补充要求，例如：更详细地描写山景风光..." />
      <el-alert v-if="aiStreaming" type="info" :closable="false" show-icon title="AI 正在生成..." style="margin-top:12px" />
      <div class="ai-result" v-else-if="aiResult">
        <div class="ai-result-header">
          <strong>AI 生成结果：</strong>
          <el-button size="small" @click="insertAiResult">📋 插入到编辑器</el-button>
        </div>
        <p>{{ aiResult }}</p>
      </div>
      <template #footer>
        <el-button @click="showAiDialog = false">{{ aiStreaming ? '停止' : '关闭' }}</el-button>
        <el-button type="primary" :loading="aiStreaming" :disabled="!form.content?.trim()" @click="doAiExpand">
          {{ aiStreaming ? '生成中' : '开始生成' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { get, post, put, del, streamAi } from '@/utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const bookId = route.params.bookId

const chapters = ref([])
const showAddDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const loadingDetail = ref(false)
const form = ref({ chapterName: '', content: '', isVip: 0 })

const showAiDialog = ref(false)
const aiExpandPrompt = ref('')
const aiResult = ref('')
const aiStreaming = ref(false)
let aiAbort = null
// AI 扩写入口来源：true = 编辑器内（结果直接追加到正文），false = 顶部按钮（结果展示供插入）
const aiFromEditor = ref(false)

onMounted(async () => {
  try {
    chapters.value = await get(`/author/books/${bookId}/chapters`) || []
  } catch (e) { /* silent */ }
})

function openAdd() {
  editing.value = null
  form.value = { chapterName: '', content: '', isVip: 0 }
  showAddDialog.value = true
}

async function editChapter(ch) {
  editing.value = ch
  // 列表接口不返回正文，先展示已知信息，再拉取详情回显正文
  form.value = { chapterName: ch.chapterName || '', content: '', isVip: ch.isVip || 0 }
  showAddDialog.value = true
  loadingDetail.value = true
  try {
    const detail = await get(`/author/books/${bookId}/chapters/${ch.id}`)
    form.value = {
      chapterName: detail?.chapterName ?? ch.chapterName ?? '',
      content: detail?.content ?? '',
      isVip: detail?.isVip ?? ch.isVip ?? 0
    }
  } catch (e) {
    ElMessage.error('章节正文加载失败，请关闭后重试')
  } finally {
    loadingDetail.value = false
  }
}

async function saveChapter() {
  if (!form.value.chapterName.trim()) { ElMessage.warning('请输入标题'); return }
  if (!form.value.content.trim()) { ElMessage.warning('请输入章节正文'); return }
  saving.value = true
  try {
    // 字段名与后端 ChapterAddReqDto / ChapterUpdateReqDto 保持一致
    const payload = {
      chapterName: form.value.chapterName.trim(),
      content: form.value.content,
      isVip: form.value.isVip
    }
    if (editing.value) {
      await put(`/author/books/${bookId}/chapters/${editing.value.id}`, payload)
      ElMessage.success('保存成功')
    } else {
      await post(`/author/books/${bookId}/chapters`, payload)
      ElMessage.success('保存成功')
    }
    showAddDialog.value = false
    editing.value = null
    form.value = { chapterName: '', content: '', isVip: 0 }
    chapters.value = await get(`/author/books/${bookId}/chapters`) || []
  } finally { saving.value = false }
}

async function deleteChapter(chapterId) {
  await ElMessageBox.confirm('删除这个章节？', '确认', { type: 'warning' })
  await del(`/author/books/${bookId}/chapters/${chapterId}`)
  ElMessage.success('已删除')
  chapters.value = await get(`/author/books/${bookId}/chapters`) || []
}

function openAiExpandForEditor() {
  aiFromEditor.value = true
  aiExpandPrompt.value = ''
  aiResult.value = ''
  showAiDialog.value = true
}

function openAiExpand() {
  aiFromEditor.value = false
  aiExpandPrompt.value = ''
  aiResult.value = ''
  showAiDialog.value = true
}

async function doAiExpand() {
  const source = (form.value.content || '').trim()
  if (!source) { ElMessage.warning('请先填写章节正文再扩写'); return }
  if (aiStreaming.value) { stopAiExpand(); return }

  aiResult.value = ''
  aiStreaming.value = true
  aiAbort = new AbortController()
  await streamAi(`/author/ai/write/${bookId}/stream`, {
    content: source,
    actionType: 'expand',
    prompt: aiExpandPrompt.value.trim()
  }, chunk => {
    aiResult.value += chunk
  }, aiAbort.signal)
  aiAbort = null

  if (!aiStreaming.value) return // 已被用户停止
  aiStreaming.value = false

  if (!aiResult.value.trim()) {
    ElMessage.warning('AI 未返回内容，请稍后重试')
    return
  }
  if (aiFromEditor.value) {
    form.value.content = (form.value.content || '') + '\n\n' + aiResult.value
    aiResult.value = ''
    showAiDialog.value = false
    ElMessage.success('扩写内容已追加到正文')
  } else {
    ElMessage.success('生成完成')
  }
}

function stopAiExpand() {
  if (aiAbort) { aiAbort.abort(); aiAbort = null }
  aiStreaming.value = false
}

function insertAiResult() {
  form.value.content = (form.value.content || '') + '\n\n' + aiResult.value
  aiResult.value = ''
  ElMessage.success('内容已插入到编辑器')
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.header-actions { display: flex; gap: 8px; }

.chapter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 8px;
}
.chapter-info { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.chapter-num { color: #909399; font-size: 13px; }
.chapter-name { font-weight: 600; }
.chapter-word-count { color: #c0c4cc; font-size: 13px; }
.chapter-actions { display: flex; gap: 8px; }
.empty-hint { text-align: center; padding: 60px 0; color: #909399; }

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}
.word-count { font-size: 12px; color: #909399; }

.ai-hint { color: #909399; font-size: 13px; margin-bottom: 10px; }
.ai-result { margin-top: 12px; padding: 12px; background: #f0f9eb; border-radius: 8px; }
.ai-result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.ai-result p { margin: 0; line-height: 1.6; white-space: pre-wrap; font-size: 14px; }
</style>