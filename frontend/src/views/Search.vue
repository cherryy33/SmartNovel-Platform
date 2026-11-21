<template>
  <div class="search-page">
    <div class="search-hero">
      <el-input v-model="keyword" size="large" placeholder="按书名或作者搜索..." clearable @keyup.enter="doSearch" prefix-icon="Search" />
    </div>

    <div class="search-filters">
      <el-select v-model="categoryId" placeholder="分类" clearable>
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="workDirection" placeholder="创作方向" clearable>
        <el-option label="男频" :value="0" />
        <el-option label="女频" :value="1" />
      </el-select>
      <el-select v-model="isVip" placeholder="全部 / 免费 / VIP" clearable>
        <el-option label="免费" :value="0" />
        <el-option label="VIP" :value="1" />
      </el-select>
      <el-select v-model="sortField" placeholder="排序方式">
        <el-option label="更新时间" value="update_time" />
        <el-option label="评分" value="score" />
        <el-option label="阅读量" value="visit_count" />
        <el-option label="最新" value="create_time" />
      </el-select>
      <el-button type="primary" @click="doSearch">搜索</el-button>
    </div>

    <div class="search-results">
      <div class="results-header" v-if="searched">
        <span>找到 {{ total }} 个结果</span>
      </div>
      <div v-if="books.length === 0 && searched" class="empty-hint">未找到结果，请尝试其他关键词！</div>
      <div class="book-grid">
        <BookCover v-for="book in books" :key="book.id" :book="book" />
      </div>
      <el-pagination
        v-if="total > 0"
        layout="prev, next, total"
        :total="total"
        :page-size="pageSize"
        @current-change="doSearch"
        style="margin-top: 20px; justify-content: center;"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get } from '@/utils/api'
import BookCover from '@/components/BookCover.vue'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const categoryId = ref(null)
const workDirection = ref(null)
const isVip = ref(null)
const sortField = ref('update_time')
const books = ref([])
const total = ref(0)
const pageSize = 20
const searched = ref(false)
const categories = ref([])

onMounted(async () => {
  try {
    categories.value = await get('/front/categories') || []
  } catch (e) { /* silent */ }
  if (route.query.q) {
    keyword.value = route.query.q
    doSearch()
  }
  if (route.query.categoryId) {
    categoryId.value = Number(route.query.categoryId)
    doSearch()
  }
})

async function doSearch(pageNum = 1) {
  searched.value = true
  try {
    const res = await get('/front/search', {
      keyword: keyword.value || undefined,
      categoryId: categoryId.value || undefined,
      workDirection: workDirection.value !== null ? workDirection.value : undefined,
      isVip: isVip.value !== null ? isVip.value : undefined,
      sortField: sortField.value,
      pageNum,
      pageSize
    })
    books.value = res.list || []
    total.value = res.total || 0
  } catch (e) { /* silent */ }
}
</script>

<style scoped>
.search-hero { margin-bottom: 16px; }
.search-filters {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.results-header { margin-bottom: 12px; font-size: 14px; color: #909399; }
.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
}
.empty-hint { text-align: center; padding: 60px 0; color: #909399; font-size: 16px; }
</style>