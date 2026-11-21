<template>
  <div class="author-books">
    <div class="page-header">
      <h2>我的作品</h2>
      <el-button type="primary" @click="showAddDialog = true">+ 创建作品</el-button>
    </div>

    <div v-if="books.length === 0" class="empty-hint">
      <p>还没有作品，开始创作你的第一部小说吧！</p>
      <el-button type="primary" @click="showAddDialog = true">创建第一部作品</el-button>
    </div>

    <div v-for="book in books" :key="book.id" class="book-item">
      <BookCover :book="book" class="book-cover-small" />
      <div class="book-info">
        <h4>{{ book.bookName }}</h4>
        <div class="book-meta">
          <span>{{ book.categoryName || '未分类' }}</span>
          <span>·</span>
          <span>{{ book.wordCount || 0 }} 字</span>
          <span>·</span>
          <el-tag :type="bookStatusType(book.bookStatus)" size="small">
            {{ bookStatusText(book.bookStatus) }}
          </el-tag>
          <span>·</span>
          <el-tag :type="auditStatusType(book.auditStatus)" size="small">
            {{ auditStatusText(book.auditStatus) }}
          </el-tag>
        </div>
        <div class="book-stats">
          <span>👁 {{ book.visitCount || 0 }} 阅读</span>
          <span>❤ {{ book.likeCount || 0 }} 点赞</span>
          <span>⭐ {{ book.score || '—' }}</span>
        </div>
      </div>
      <div class="book-actions">
        <el-button size="small" type="primary" @click="$router.push(`/author/write/${book.id}`)">
          ✏️ 章节管理
        </el-button>
        <el-button size="small" @click="openAiAssist(book)">🤖 AI 辅助</el-button>
        <el-button size="small" @click="editBook(book)">编辑</el-button>
        <el-button size="small" type="danger" @click="deleteBook(book.id)">删除</el-button>
      </div>
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="showAddDialog" :title="editingBook ? '编辑作品' : '创建作品'" width="520px">
      <el-form @submit.prevent="saveBook">
        <el-form-item label="作品名称">
          <el-input v-model="form.bookName" placeholder="请输入作品名称" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="创作方向">
          <el-radio-group v-model="form.workDirection">
            <el-radio :value="0">男频</el-radio>
            <el-radio :value="1">女频</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.bookDesc" type="textarea" :rows="3" placeholder="作品简介" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.bookStatus">
            <el-radio :value="1">连载中</el-radio>
            <el-radio :value="2">已完结</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tagIds" multiple placeholder="选择标签" style="width:100%">
            <el-option v-for="t in allTags" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="saving">保存</el-button>
      </el-form>
    </el-dialog>

    <!-- AI 辅助弹窗 -->
    <el-dialog v-model="showAiDialog" title="🤖 AI 写作助手" width="600px" top="5vh">
      <p class="ai-hint">获取 AI 写作帮助："{{ aiTargetBook?.bookName }}"</p>
      <div class="ai-chat-area">
        <div v-for="(msg, i) in aiChat" :key="i" class="ai-msg" :class="msg.role">
          <strong>{{ msg.role === 'user' ? '你' : 'AI' }}</strong>
          <p>{{ msg.content }}</p>
        </div>
      </div>
      <div class="ai-input-row">
        <el-input v-model="aiPrompt" placeholder="向 AI 寻求情节构思、角色塑造建议..."
                  @keyup.enter="askAiForBook" />
        <el-button type="primary" @click="askAiForBook" :loading="aiLoading">发送</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, postAi, put, del } from '@/utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import BookCover from '@/components/BookCover.vue'

const books = ref([])
const categories = ref([])
const allTags = ref([])
const showAddDialog = ref(false)
const editingBook = ref(null)
const saving = ref(false)
const form = ref({ bookName: '', categoryId: null, bookDesc: '', bookStatus: 1, workDirection: 0, tagIds: [] })

const showAiDialog = ref(false)
const aiTargetBook = ref(null)
const aiChat = ref([])
const aiPrompt = ref('')
const aiLoading = ref(false)

onMounted(async () => {
  try { books.value = await get('/author/books') || [] } catch (e) { /* silent */ }
  try { categories.value = await get('/front/categories') || [] } catch (e) { /* silent */ }
  try { allTags.value = await get('/front/tags') || [] } catch (e) { /* silent */ }
})

function editBook(book) {
  editingBook.value = book
  form.value = {
    bookName: book.bookName,
    categoryId: book.categoryId,
    bookDesc: book.bookDesc || '',
    bookStatus: book.bookStatus || 1,
    workDirection: book.workDirection || 0,
    tagIds: (book.tags || []).map(t => t.id || t)
  }
  showAddDialog.value = true
}

async function saveBook() {
  if (!form.value.bookName.trim()) { ElMessage.warning('请输入作品名称'); return }
  saving.value = true
  try {
    if (editingBook.value) {
      await put(`/author/books/${editingBook.value.id}`, form.value)
      ElMessage.success('作品已更新')
    } else {
      await post('/author/books', form.value)
      ElMessage.success('作品已创建')
    }
    showAddDialog.value = false
    editingBook.value = null
    form.value = { bookName: '', categoryId: null, bookDesc: '', bookStatus: 1, workDirection: 0, tagIds: [] }
    books.value = await get('/author/books') || []
  } finally { saving.value = false }
}

async function deleteBook(bookId) {
  await ElMessageBox.confirm('删除这部作品及所有章节？', '确认', { type: 'warning' })
  await del(`/author/books/${bookId}`)
  ElMessage.success('已删除')
  books.value = await get('/author/books') || []
}

// 后端 Jackson 把数字序列化为字符串（如 "1"），需归一化后再比较
function toNum(v) {
  return v === null || v === undefined || v === '' ? null : Number(v)
}
function bookStatusType(s) {
  return toNum(s) === 1 ? '' : 'success'
}
function bookStatusText(s) {
  const n = toNum(s)
  return n === 1 ? '连载中' : n === 2 ? '已完结' : '草稿'
}
function auditStatusType(s) {
  const n = toNum(s)
  return n === 1 ? 'success' : n === 0 ? 'warning' : 'danger'
}
function auditStatusText(s) {
  const n = toNum(s)
  return n === 1 ? '已发布' : n === 0 ? '审核中' : '已拒绝'
}

function openAiAssist(book) {
  aiTargetBook.value = book
  aiChat.value = []
  aiPrompt.value = ''
  showAiDialog.value = true
}

async function askAiForBook() {
  if (!aiPrompt.value.trim() || !aiTargetBook.value) return
  aiChat.value.push({ role: 'user', content: aiPrompt.value })
  aiLoading.value = true
  try {
    const res = await postAi(`/author/ai/write/${aiTargetBook.value.id}`, {
      content: aiPrompt.value,
      actionType: 'expand',
      prompt: aiPrompt.value
    })
    aiChat.value.push({
      role: 'assistant',
      content: res?.processedContent || res?.answer || 'AI 未返回内容'
    })
  } catch (e) {
    aiChat.value.push({ role: 'assistant', content: 'AI 服务暂不可用，请稍后再试' })
  } finally {
    aiLoading.value = false
    aiPrompt.value = ''
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 { margin: 0; }

.book-item {
  display: flex;
  gap: 16px;
  align-items: center;
  background: #fff;
  padding: 16px;
  border-radius: 10px;
  margin-bottom: 12px;
}
.book-cover-small { flex-shrink: 0; }
.book-info { flex: 1; min-width: 0; }
.book-info h4 { margin: 0 0 6px; font-size: 16px; }
.book-meta { display: flex; gap: 6px; align-items: center; font-size: 13px; color: #909399; margin-bottom: 6px; flex-wrap: wrap; }
.book-stats { display: flex; gap: 12px; font-size: 13px; color: #606266; }
.book-actions { display: flex; gap: 6px; flex-wrap: wrap; }
.empty-hint { text-align: center; padding: 60px 0; color: #909399; }

.ai-hint { color: #909399; margin-bottom: 10px; }
.ai-chat-area { max-height: 280px; overflow-y: auto; padding: 10px; background: #fafafa; border-radius: 8px; margin-bottom: 10px; }
.ai-msg { margin-bottom: 8px; padding: 8px 10px; border-radius: 8px; font-size: 13px; }
.ai-msg.user { background: #ecf5ff; }
.ai-msg.assistant { background: #f5f7fa; }
.ai-msg strong { display: block; font-size: 11px; color: #909399; margin-bottom: 4px; }
.ai-msg p { margin: 0; line-height: 1.5; white-space: pre-wrap; }
.ai-input-row { display: flex; gap: 8px; }
</style>