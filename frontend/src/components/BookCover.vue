<template>
  <router-link :to="`/book/${book.id}`" class="book-cover" :title="`${title} — ${author}`">
    <div class="cover-body" :style="coverStyle">
      <!-- 左侧书脊立体阴影 -->
      <div class="spine-shadow" />

      <!-- 纸张纹理叠加层 -->
      <div class="paper-texture" />

      <!-- 自定义封面图 -->
      <img v-if="coverUrl" :src="coverUrl" :alt="title" class="cover-image" />

      <!-- 无封面时的拟物排版 -->
      <div v-else class="cover-artwork">
        <!-- 烫金几何边框 -->
        <div class="golden-border">
          <div class="border-corner tl" />
          <div class="border-corner tr" />
          <div class="border-corner bl" />
          <div class="border-corner br" />
        </div>

        <!-- 书名（优雅衬线字体） -->
        <h3 class="book-title" :class="{ 'title-sm': title.length > 8, 'title-xs': title.length > 14 }">
          {{ title }}
        </h3>

        <!-- 分隔装饰线 -->
        <div class="ornament-divider">
          <span class="ornament-dot" />
          <span class="ornament-line" />
          <span class="ornament-diamond">◆</span>
          <span class="ornament-line" />
          <span class="ornament-dot" />
        </div>

        <!-- 作者名 -->
        <p class="book-author">{{ author }}</p>

        <!-- 分类标签 -->
        <span v-if="category" class="category-tag">{{ category }}</span>
      </div>

      <!-- 底部光泽条 -->
      <div class="bottom-sheen" />
    </div>

    <!-- 底部信息栏 -->
    <div class="cover-info">
      <p class="info-title" :title="title">{{ title }}</p>
      <p class="info-author">{{ author }}</p>
    </div>
  </router-link>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  book: {
    type: Object,
    default: () => ({})
  }
})

// 兼容两种传参方式：book 对象 或 单独 props
const title = computed(() => props.book?.bookName || props.book?.title || props.title || 'Untitled')
const author = computed(() => props.book?.authorName || props.book?.author || props.author || 'Unknown')
const category = computed(() => props.book?.categoryName || props.book?.category || props.category || '')
const coverUrl = computed(() => props.book?.picUrl || props.book?.coverUrl || props.coverUrl || '')

// 根据分类匹配高级渐变色系
const categoryPalette = {
  '玄幻': { primary: '#1a1a3e', secondary: '#2d1b69', accent: '#c9a84c', text: '#e8d5a3' },
  '奇幻': { primary: '#0f2027', secondary: '#203a43', accent: '#7b9eaa', text: '#d0e8f0' },
  '武侠': { primary: '#2d1b0e', secondary: '#5a2d1a', accent: '#d4a84b', text: '#f0d8a0' },
  '仙侠': { primary: '#1a2744', secondary: '#3a5a8c', accent: '#a0c4e8', text: '#e0ecff' },
  '都市': { primary: '#1e2f3f', secondary: '#3d566e', accent: '#6cb4c1', text: '#c8e6e8' },
  '现实': { primary: '#3d322b', secondary: '#5c4a3a', accent: '#b8956e', text: '#f0dfc8' },
  '历史': { primary: '#4a1a1a', secondary: '#7a2e2e', accent: '#d4a76a', text: '#f0d8b8' },
  '军事': { primary: '#1b2f1b', secondary: '#3a5a2a', accent: '#a0b878', text: '#e0e8d0' },
  '科幻': { primary: '#0a1a2a', secondary: '#1a4a5a', accent: '#4ac8c8', text: '#b8f0f0' },
  '悬疑': { primary: '#1a1a2a', secondary: '#3a2a4a', accent: '#8a6aaa', text: '#d0c8e8' },
  '灵异': { primary: '#0a0a1a', secondary: '#1a1a3a', accent: '#6a3a8a', text: '#c8b0e0' },
  '轻小说': { primary: '#2a2a4a', secondary: '#4a4a7a', accent: '#c8a0e8', text: '#f0d8ff' },
  '短篇': { primary: '#3a3a3a', secondary: '#5a5a5a', accent: '#aaa', text: '#f0f0f0' },
  '游戏': { primary: '#1a2a1a', secondary: '#2a4a2a', accent: '#6ac86a', text: '#d0f0d0' },
  '体育': { primary: '#2a2a1a', secondary: '#4a3a1a', accent: '#c8a86a', text: '#f0e8d0' },
  '衍生': { primary: '#2a1a2a', secondary: '#5a3a5a', accent: '#c8a0c8', text: '#f0d8f0' }
}

const palette = computed(() => {
  const cat = category.value
  if (!cat) return categoryPalette['玄幻'] // default
  for (const [key, colors] of Object.entries(categoryPalette)) {
    if (cat.includes(key) || key.includes(cat)) return colors
  }
  return categoryPalette['玄幻']
})

const coverStyle = computed(() => ({
  background: `linear-gradient(160deg, ${palette.value.primary} 0%, ${palette.value.secondary} 60%, ${palette.value.primary} 100%)`,
  '--accent': palette.value.accent,
  '--text-color': palette.value.text
}))
</script>

<style scoped>
.book-cover {
  display: block;
  width: 150px;
  cursor: pointer;
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1),
              box-shadow 0.3s ease;
  border-radius: 6px;
  overflow: visible;
  text-decoration: none;
}

.book-cover:hover {
  transform: translateY(-6px) rotateY(-3deg) scale(1.03);
}

.book-cover:hover .cover-body {
  box-shadow:
    6px 8px 24px rgba(0,0,0,0.35),
    12px 16px 40px rgba(0,0,0,0.15),
    inset 0 1px 0 rgba(255,255,255,0.1);
}

/* ===== 封面主体 ===== */
.cover-body {
  position: relative;
  width: 150px;
  height: 210px;
  border-radius: 5px 8px 8px 5px;
  overflow: hidden;
  box-shadow:
    4px 6px 16px rgba(0,0,0,0.25),
    8px 12px 28px rgba(0,0,0,0.1),
    inset 0 1px 0 rgba(255,255,255,0.08);
  transition: box-shadow 0.3s ease;
  font-family: 'Georgia', 'Times New Roman', 'Songti SC', 'Noto Serif SC', serif;
}

/* 书脊立体阴影 */
.spine-shadow {
  position: absolute;
  left: 0;
  top: 0;
  width: 8px;
  height: 100%;
  background: linear-gradient(
    90deg,
    rgba(255,255,255,0.12) 0%,
    rgba(0,0,0,0.25) 30%,
    rgba(0,0,0,0.35) 60%,
    rgba(0,0,0,0.15) 100%
  );
  z-index: 3;
  pointer-events: none;
  border-radius: 2px 0 0 2px;
}

/* 纸张纹理 */
.paper-texture {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  opacity: 0.06;
  background:
    repeating-linear-gradient(
      0deg,
      transparent,
      transparent 2px,
      rgba(255,255,255,0.08) 2px,
      rgba(255,255,255,0.08) 3px
    ),
    repeating-linear-gradient(
      90deg,
      transparent,
      transparent 1px,
      rgba(0,0,0,0.04) 1px,
      rgba(0,0,0,0.04) 2px
    ),
    radial-gradient(
      ellipse at 30% 20%,
      rgba(255,255,255,0.08) 0%,
      transparent 60%
    ),
    radial-gradient(
      ellipse at 70% 80%,
      rgba(0,0,0,0.06) 0%,
      transparent 60%
    );
}

/* 自定义封面图 */
.cover-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
  border-radius: 5px 8px 8px 5px;
}

/* ===== 无封面时的艺术排版 ===== */
.cover-artwork {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 18px 14px;
  text-align: center;
}

/* 烫金几何边框 */
.golden-border {
  position: absolute;
  inset: 10px;
  pointer-events: none;
  z-index: 0;
}

.border-corner {
  position: absolute;
  width: 16px;
  height: 16px;
  border-color: var(--accent);
  border-style: solid;
  opacity: 0.5;
}

.tl { top: 0; left: 0; border-width: 1.5px 0 0 1.5px; border-radius: 2px 0 0 0; }
.tr { top: 0; right: 0; border-width: 1.5px 1.5px 0 0; border-radius: 0 2px 0 0; }
.bl { bottom: 0; left: 0; border-width: 0 0 1.5px 1.5px; border-radius: 0 0 0 2px; }
.br { bottom: 0; right: 0; border-width: 0 1.5px 1.5px 0; border-radius: 0 0 2px 0; }

/* 书名 */
.book-title {
  position: relative;
  z-index: 1;
  margin: 0 0 10px;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-color);
  line-height: 1.3;
  letter-spacing: 0.05em;
  text-shadow: 0 1px 3px rgba(0,0,0,0.5), 0 0 8px rgba(255,255,255,0.05);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.title-sm { font-size: 14px; }
.title-xs { font-size: 12px; }

/* 装饰分隔线 */
.ornament-divider {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
  opacity: 0.6;
}

.ornament-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--accent);
}

.ornament-line {
  width: 20px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--accent), transparent);
}

.ornament-diamond {
  font-size: 6px;
  color: var(--accent);
  line-height: 1;
}

/* 作者名 */
.book-author {
  position: relative;
  z-index: 1;
  margin: 0;
  font-size: 11px;
  color: var(--text-color);
  opacity: 0.7;
  font-style: italic;
  letter-spacing: 0.03em;
  text-shadow: 0 1px 2px rgba(0,0,0,0.4);
}

/* 分类标签 */
.category-tag {
  position: absolute;
  bottom: 12px;
  right: 12px;
  font-size: 10px;
  color: var(--accent);
  opacity: 0.7;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  z-index: 1;
}

/* 底部光泽条 */
.bottom-sheen {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg,
    transparent 0%,
    var(--accent) 20%,
    var(--accent) 80%,
    transparent 100%
  );
  opacity: 0.4;
  z-index: 3;
  border-radius: 0 0 8px 8px;
}

/* ===== 底部信息 ===== */
.cover-info {
  padding: 8px 4px 0;
  text-align: center;
}

.info-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0 0 3px;
  line-height: 1.3;
}

.info-author {
  font-size: 11px;
  color: #909399;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
