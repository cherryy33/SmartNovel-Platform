<template>
  <div class="history-page">
    <h2>阅读历史</h2>
    <div v-if="history.length === 0" class="empty-hint">暂无阅读记录</div>
    <div v-for="h in history" :key="h.bookId" class="history-item">
      <router-link :to="`/read/${h.bookId}/${h.lastChapterId}`" class="history-link">
        <span class="history-book-name">{{ h.bookName }}</span>
        <span class="history-chapter">{{ chapterLabel(h) }}</span>
        <span class="history-time">{{ formatTime(h.lastReadTime) }}</span>
      </router-link>
    </div>
    <el-pagination
      v-if="total > 0"
      layout="prev, next"
      :total="total"
      :page-size="pageSize"
      @current-change="load"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '@/utils/api'

const history = ref([])
const total = ref(0)
const pageSize = 20

onMounted(() => load(1))

async function load(pageNum = 1) {
  try {
    const res = await get('/front/user/read-history', { pageNum, pageSize })
    // 后端返回的 Long ID 为字符串，这里只保留有有效 bookId 的记录
    history.value = (res.list || []).filter(h => h.bookId)
    total.value = Number(res.total || 0)
  } catch (e) {
    history.value = []
    total.value = 0
  }
}

// 章节标签：优先用完整章节名，其次用章节序号，均无则显示「继续阅读」
function chapterLabel(h) {
  if (h.lastChapterName) return h.lastChapterName
  if (h.lastChapterNum !== null && h.lastChapterNum !== undefined) {
    return `第 ${Number(h.lastChapterNum) + 1} 章`
  }
  return '继续阅读'
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString()
}
</script>

<style scoped>
.history-item {
  background: #fff;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 8px;
}
.history-link {
  display: flex;
  align-items: center;
  gap: 16px;
}
.history-book-name { font-weight: 600; flex: 1; }
.history-chapter { color: #606266; font-size: 14px; }
.history-time { color: #c0c4cc; font-size: 13px; margin-left: auto; }
.empty-hint { text-align: center; padding: 60px 0; color: #909399; font-size: 16px; }
</style>