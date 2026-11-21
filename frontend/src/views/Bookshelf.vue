<template>
  <div class="bookshelf-page">
    <h2>我的书架</h2>
    <div v-if="books.length === 0" class="empty-hint">书架空空如也</div>
    <div class="book-grid">
      <BookCover v-for="book in books" :key="book.id" :book="book" />
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
import BookCover from '@/components/BookCover.vue'

const books = ref([])
const total = ref(0)
const pageSize = 20

onMounted(() => load(1))

async function load(pageNum = 1) {
  const res = await get('/front/user/bookshelf', { pageNum, pageSize })
  books.value = res.list || []
  total.value = res.total || 0
}
</script>

<style scoped>
.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}
.empty-hint { text-align: center; padding: 60px 0; color: #909399; font-size: 16px; }
</style>