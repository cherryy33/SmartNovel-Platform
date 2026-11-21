<template>
  <div class="admin-layout">
    <!-- 左侧边栏 -->
    <aside class="admin-sidebar">
      <div class="sidebar-title">
        <span class="shield">🛡️</span>
        <span>管理后台</span>
      </div>

      <nav class="sidebar-menu">
        <router-link
          v-for="item in menus"
          :key="item.path"
          :to="item.path"
          class="menu-item"
          :class="{ 'menu-active': isActive(item.path) }"
        >
          <span class="menu-icon">{{ item.icon }}</span>
          <span class="menu-label">{{ item.label }}</span>
          <el-badge
            v-if="item.badgeKey === 'pendingApplies'"
            :value="pendingApplies"
            :hidden="!pendingApplies"
            class="menu-badge"
          />
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <router-link to="/" class="back-link">← 返回读者首页</router-link>
      </div>
    </aside>

    <!-- 右侧内容 -->
    <section class="admin-content">
      <router-view />
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { get } from '@/utils/api'

const route = useRoute()

const menus = [
  { path: '/admin', label: '数据概览', icon: '📊' },
  { path: '/admin/authors', label: '作家入驻审核', icon: '✍️', badgeKey: 'pendingApplies' },
  { path: '/admin/books', label: '小说管理', icon: '📚' },
  { path: '/admin/users', label: '用户管理', icon: '👥' },
  { path: '/admin/notices', label: '公告管理', icon: '📢' },
  // 以下为既有管理模块，继续保留入口
  { path: '/admin/comments', label: '评论管理', icon: '💬' },
  { path: '/admin/community', label: '社区管理', icon: '🌐' },
  { path: '/admin/tags', label: '标签管理', icon: '🏷️' },
  { path: '/admin/categories', label: '分类管理', icon: '📂' },
  { path: '/admin/messages', label: '留言板', icon: '📝' }
]

// 首页仅精确匹配，其余按路径前缀匹配
function isActive(path) {
  if (path === '/admin') return route.path === '/admin'
  return route.path === path || route.path.startsWith(path + '/')
}

const pendingApplies = ref(0)
onMounted(async () => {
  try {
    const res = await get('/admin/author/applies', { pageNum: 1, pageSize: 1, status: 0 })
    pendingApplies.value = Number(res?.total || 0)
  } catch (e) { /* 静默失败，不影响菜单 */ }
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  min-height: calc(100vh - 140px);
}

/* ===== 侧边栏 ===== */
.admin-sidebar {
  width: 200px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 16px 12px;
  position: sticky;
  top: 84px;
  display: flex;
  flex-direction: column;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  padding: 4px 8px 14px;
  margin-bottom: 8px;
  border-bottom: 1px solid #f2f3f5;
}
.shield { font-size: 18px; }

.sidebar-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
  text-decoration: none;
  transition: all 0.2s;
}
.menu-item:hover {
  background: #f5f7fa;
  color: #409eff;
}
.menu-item.menu-active {
  background: #409eff;
  color: #fff;
  font-weight: 500;
}
.menu-icon { font-size: 15px; width: 18px; text-align: center; }
.menu-label { flex: 1; }
.menu-badge :deep(.el-badge__content) {
  border: none;
}

.sidebar-footer {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f2f3f5;
}
.back-link {
  font-size: 13px;
  color: #909399;
  text-decoration: none;
}
.back-link:hover { color: #409eff; }

/* ===== 内容区 ===== */
.admin-content {
  flex: 1;
  min-width: 0;
}

@media (max-width: 900px) {
  .admin-layout { flex-direction: column; }
  .admin-sidebar {
    width: 100%;
    position: static;
  }
  .sidebar-menu {
    flex-direction: row;
    flex-wrap: wrap;
  }
  .menu-item { padding: 6px 10px; font-size: 13px; }
  .sidebar-footer { display: none; }
}
</style>
