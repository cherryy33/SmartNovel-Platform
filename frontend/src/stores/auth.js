import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { post, get } from '@/utils/api'

/** 角色归一化：兼容 ADMIN / ROLE_ADMIN / admin / USER 等写法 */
function normalizeRole(raw) {
  if (!raw) return 'READER'
  let r = String(raw).trim().toUpperCase()
  if (r.startsWith('ROLE_')) r = r.slice(5)
  if (r === 'ADMIN') return 'ADMIN'
  if (r === 'AUTHOR' || r === 'WRITER') return 'AUTHOR'
  return 'READER'
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => normalizeRole(user.value?.role))
  const isVip = computed(() => Number(user.value?.isVip) === 1)
  const isAuthor = computed(() => role.value === 'AUTHOR')
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isReader = computed(() => role.value === 'READER')

  function setAuth(t, u) {
    token.value = t
    const normalized = u ? { ...u, role: normalizeRole(u.role) } : u
    user.value = normalized
    localStorage.setItem('token', t)
    localStorage.setItem('user', JSON.stringify(normalized))
  }

  function updateUser(patch) {
    if (!user.value) return
    user.value = { ...user.value, ...patch }
    if (patch.role) user.value.role = normalizeRole(patch.role)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  async function login(username, password, loginRole = 'READER') {
    const data = await post('/front/auth/login', {
      username, password, role: loginRole
    })
    setAuth(data.token, data.user || data)
    return data
  }

  async function register(username, password, regRole = 'READER', extra = {}) {
    const data = await post('/front/auth/register', {
      username, password, role: regRole, ...extra
    })
    if (data && data.token) {
      setAuth(data.token, data.user || data)
    }
    return data
  }

  /** 静默刷新当前用户资料（用于审核通过后同步角色） */
  async function refreshProfile() {
    try {
      const profile = await get('/front/user/profile')
      if (profile) {
        updateUser({
          ...profile,
          role: normalizeRole(profile.role)
        })
      }
      return profile
    } catch (e) {
      return null
    }
  }

  async function resetPassword(email, newPassword, code) {
    return await post('/front/auth/reset-password', {
      email, newPassword, code
    })
  }

  async function oauthLogin(oauthType) {
    const data = await post('/front/auth/oauth', {
      oauthType,
      oauthId: 'simulate_' + Date.now(),
      nickName: oauthType + '_user'
    })
    setAuth(data.token, data.user || data)
    return data
  }

  async function upgradeVip() {
    const data = await post('/front/user/upgrade-vip')
    updateUser({ isVip: 1 })
    return data
  }

  return {
    token, user, role, isLoggedIn, isVip, isAuthor, isAdmin, isReader,
    setAuth, updateUser, logout, login, register, refreshProfile,
    resetPassword, oauthLogin, upgradeVip, normalizeRole
  }
})
