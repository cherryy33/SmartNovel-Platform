import { createRouter, createWebHistory } from 'vue-router'

// 阶段 2 页面对应的 Vue SFC 文件将在后续逐步创建。
// 当前使用动态 import 约定路径，构建时不会报错（Vite 会警告，但不影响开发）。
// 在正式创建页面文件后，import 将正常工作。

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  },
  {
    path: '/book/:id',
    name: 'BookDetail',
    component: () => import('@/views/BookDetail.vue')
  },
  {
    path: '/read/:bookId/:chapterId',
    name: 'ChapterReader',
    component: () => import('@/views/ChapterReader.vue')
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/Search.vue')
  },
  {
    path: '/community',
    name: 'Community',
    component: () => import('@/views/Community.vue')
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('@/views/Messages.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue')
  },
  {
    path: '/bookshelf',
    name: 'Bookshelf',
    component: () => import('@/views/Bookshelf.vue')
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('@/views/History.vue')
  },
  {
    path: '/categories',
    name: 'Categories',
    component: () => import('@/views/Categories.vue')
  },
  // === 作者端 ===
  {
    path: '/author',
    name: 'AuthorWorkspace',
    component: () => import('@/views/author/Workspace.vue'),
    meta: { requiresAuth: true, role: 'author' }
  },
  {
    path: '/author/books',
    name: 'AuthorBooks',
    component: () => import('@/views/author/Books.vue'),
    meta: { requiresAuth: true, role: 'author' }
  },
  {
    path: '/author/write/:bookId',
    name: 'AuthorChapterManager',
    component: () => import('@/views/author/ChapterManager.vue'),
    meta: { requiresAuth: true, role: 'author' }
  },
  {
    path: '/author/comments',
    name: 'AuthorComments',
    component: () => import('@/views/author/Comments.vue'),
    meta: { requiresAuth: true, role: 'author' }
  },
  {
    path: '/author/messages',
    name: 'AuthorMessages',
    component: () => import('@/views/author/Messages.vue'),
    meta: { requiresAuth: true, role: 'author' }
  },
  // === 管理员端：统一左侧边栏布局 ===
  {
    path: '/admin',
    component: () => import('@/components/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      { path: '', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'authors', name: 'AdminAuthors', component: () => import('@/views/admin/Authors.vue') },
      { path: 'books', name: 'AdminBooks', component: () => import('@/views/admin/Books.vue') },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/Users.vue') },
      { path: 'notices', name: 'AdminNotices', component: () => import('@/views/admin/Notices.vue') },
      { path: 'comments', name: 'AdminComments', component: () => import('@/views/admin/Comments.vue') },
      { path: 'community', name: 'AdminCommunity', component: () => import('@/views/admin/Community.vue') },
      { path: 'tags', name: 'AdminTags', component: () => import('@/views/admin/Tags.vue') },
      { path: 'categories', name: 'AdminCategories', component: () => import('@/views/admin/Categories.vue') },
      { path: 'messages', name: 'AdminMessages', component: () => import('@/views/admin/Messages.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 全局路由守卫：鉴权拦截
router.beforeEach((to, from, next) => {
  const { requiresAuth, role } = to.meta

  if (requiresAuth) {
    // 动态 import auth store 避免循环依赖
    import('@/stores/auth').then(({ useAuthStore }) => {
      const authStore = useAuthStore()

      if (!authStore.isLoggedIn) {
        next({ path: '/login', query: { redirect: to.fullPath } })
        return
      }

      if (role) {
        // meta.role 为小写（admin / author），统一归一化后比较
        const actual = authStore.role
        const expected = String(role).toUpperCase()
        const allowed = actual === expected
          // 管理员可访问作者端
          || (expected === 'AUTHOR' && actual === 'ADMIN')
        if (!allowed) {
          next({ path: '/' })
          return
        }
      }

      next()
    })
  } else {
    next()
  }
})

export default router
