<template>
  <div class="home-page">
    <!-- Banner轮播 -->
    <el-carousel v-if="banners.length" height="360px" indicator-position="outside" class="banner-carousel">
      <el-carousel-item v-for="b in banners" :key="b.id">
        <div class="banner-item" :style="b.imageUrl ? { backgroundImage: `url(${b.imageUrl})` } : {}">
          <div class="banner-mask">
            <h3>{{ b.title }}</h3>
            <p v-if="b.description">{{ b.description }}</p>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>

    <!-- 公告栏 -->
    <div v-if="notices.length" class="notice-bar">
      <el-icon><Bell /></el-icon>
      <el-carousel direction="vertical" height="20px" :autoplay="true" :interval="4000" indicator-position="none">
        <el-carousel-item v-for="n in notices" :key="n.id">
          <span class="notice-text">{{ n.title }}：{{ n.content }}</span>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- 个性化推荐（登录后展示） -->
    <section v-if="authStore.isLoggedIn && personalRecs.length" class="book-section">
      <div class="section-header">
        <h2>🎯 为你推荐</h2>
        <router-link to="/search" class="more-link">更多 →</router-link>
      </div>
      <div class="book-grid">
        <BookCover v-for="book in personalRecs" :key="book.id" :book="book" />
      </div>
    </section>

    <!-- 猜你喜欢 -->
    <section v-if="authStore.isLoggedIn && guessLike.length" class="book-section">
      <div class="section-header">
        <h2>💡 猜你喜欢</h2>
      </div>
      <div class="book-grid">
        <BookCover v-for="book in guessLike" :key="book.id" :book="book" />
      </div>
    </section>

    <!-- 热门作品 -->
    <section class="book-section">
      <div class="section-header">
        <h2>🔥 热门小说</h2>
        <router-link to="/search?sortField=visit_count" class="more-link">更多 →</router-link>
      </div>
      <div class="book-grid">
        <BookCover v-for="book in hotBooks" :key="book.id" :book="book" />
      </div>
    </section>

    <!-- 最新作品 -->
    <section class="book-section">
      <div class="section-header">
        <h2>✨ 新书速递</h2>
        <router-link to="/search?sortField=create_time" class="more-link">更多 →</router-link>
      </div>
      <div class="book-grid">
        <BookCover v-for="book in newBooks" :key="book.id" :book="book" />
      </div>
    </section>

    <!-- 高分作品 -->
    <section class="book-section">
      <div class="section-header">
        <h2>⭐ 高分榜</h2>
        <router-link to="/search?sortField=score" class="more-link">更多 →</router-link>
      </div>
      <div class="book-grid">
        <BookCover v-for="book in scoreBooks" :key="book.id" :book="book" />
      </div>
    </section>

    <!-- 收藏最多 -->
    <section class="book-section">
      <div class="section-header">
        <h2>💖 收藏榜</h2>
      </div>
      <div class="book-grid">
        <BookCover v-for="book in favoredBooks" :key="book.id" :book="book" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { get } from '@/utils/api'
import { useAuthStore } from '@/stores/auth'
import BookCover from '@/components/BookCover.vue'

const authStore = useAuthStore()

const banners = ref([])
const notices = ref([])
const hotBooks = ref([])
const newBooks = ref([])
const scoreBooks = ref([])
const favoredBooks = ref([])
const personalRecs = ref([])
const guessLike = ref([])

// 单个接口失败不影响其余板块渲染
async function safeGet(url, params, target) {
  try {
    const data = await get(url, params)
    target.value = data || []
  } catch (e) { /* silent */ }
}

onMounted(async () => {
  await Promise.all([
    safeGet('/front/banners', undefined, banners),
    safeGet('/front/notices', undefined, notices),
    safeGet('/front/recommend/hot', { limit: 10 }, hotBooks),
    safeGet('/front/recommend/new', { limit: 10 }, newBooks),
    safeGet('/front/recommend/score', { limit: 10 }, scoreBooks),
    safeGet('/front/recommend/favorited', { limit: 10 }, favoredBooks)
  ])

  // 登录后加载个性化推荐
  if (authStore.isLoggedIn) {
    try {
      const [personal, guess] = await Promise.all([
        get('/front/recommend/personal'),
        get('/front/recommend/guess')
      ])
      personalRecs.value = personal || []
      guessLike.value = guess || []
    } catch (e) { /* silent */ }
  }
})
</script>

<style scoped>
.banner-carousel { margin-bottom: 20px; border-radius: 8px; overflow: hidden; }
.banner-item {
  height: 360px;
  background-size: cover;
  background-position: center;
  background-color: #2c3e50;
}
.banner-mask {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 40px;
  background: linear-gradient(transparent, rgba(0,0,0,0.55));
  color: #fff;
}
.banner-mask h3 { font-size: 24px; margin: 0; }
.banner-mask p { font-size: 14px; margin: 6px 0 0; opacity: 0.85; }

.notice-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #ecf5ff;
  border-radius: 8px;
  margin-bottom: 24px;
  font-size: 14px;
  color: #409eff;
  overflow: hidden;
}
.notice-text { white-space: nowrap; }

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.section-header h2 { font-size: 20px; font-weight: 600; margin: 0; }
.more-link { font-size: 14px; color: #409eff; }

.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
}
.book-section { margin-bottom: 32px; }
</style>