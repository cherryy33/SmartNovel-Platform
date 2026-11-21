<template>
  <div class="categories-page">
    <h2>分类</h2>

    <el-tabs v-model="activeDirection" @tab-change="loadCategories">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="男频" name="0" />
      <el-tab-pane label="女频" name="1" />
    </el-tabs>

    <div class="category-grid">
      <router-link
        v-for="c in categories"
        :key="c.id"
        :to="`/search?categoryId=${c.id}`"
        class="category-card"
      >
        <div class="category-icon">{{ c.name?.charAt(0) || '📚' }}</div>
        <span class="category-name">{{ c.name }}</span>
        <span class="category-count" v-if="c.bookCount">{{ c.bookCount }} 本</span>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '@/utils/api'

const categories = ref([])
const activeDirection = ref('all')

onMounted(() => loadCategories('all'))

async function loadCategories(tab) {
  activeDirection.value = tab
  try {
    if (tab === 'all') {
      categories.value = await get('/front/categories') || []
    } else {
      categories.value = await get(`/front/categories/${tab}`) || []
    }
  } catch (e) { /* silent */ }
}
</script>

<style scoped>
h2 { margin-bottom: 16px; }
.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 14px;
  margin-top: 16px;
}
.category-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  background: #fff;
  padding: 24px 16px;
  border-radius: 12px;
  text-align: center;
  text-decoration: none;
  color: #303133;
  transition: box-shadow 0.2s, transform 0.2s;
}
.category-card:hover {
  box-shadow: 0 4px 18px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}
.category-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ecf5ff, #d9ecff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #409eff;
  font-weight: 700;
}
.category-name { font-size: 15px; font-weight: 600; }
.category-count { font-size: 12px; color: #909399; }
</style>