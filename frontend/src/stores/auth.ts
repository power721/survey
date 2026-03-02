import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import {authApi} from '@/api/auth'
import type {AuthResponse} from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const username = ref<string | null>(localStorage.getItem('username'))
  const nickname = ref<string | null>(localStorage.getItem('nickname'))
    const avatar = ref<string | null>(localStorage.getItem('avatar'))
  const role = ref<string | null>(localStorage.getItem('role'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  function setAuth(auth: AuthResponse) {
    token.value = auth.token
    username.value = auth.username
    nickname.value = auth.nickname
      avatar.value = auth.avatar
    role.value = auth.role
    localStorage.setItem('token', auth.token)
    localStorage.setItem('username', auth.username)
    localStorage.setItem('nickname', auth.nickname)
      if (auth.avatar) localStorage.setItem('avatar', auth.avatar)
      else localStorage.removeItem('avatar')
    localStorage.setItem('role', auth.role)
  }

  function clearAuth() {
    token.value = null
    username.value = null
    nickname.value = null
      avatar.value = null
    role.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('nickname')
      localStorage.removeItem('avatar')
    localStorage.removeItem('role')
  }

  async function login(usernameVal: string, password: string) {
    const res = await authApi.login({ username: usernameVal, password })
    setAuth(res.data.data)
    return res.data
  }

  async function register(data: { username: string; password: string; email?: string; nickname?: string }) {
    const res = await authApi.register(data)
    setAuth(res.data.data)
    return res.data
  }

  function logout() {
    clearAuth()
  }

    return {token, username, nickname, avatar, role, isLoggedIn, isAdmin, login, register, logout, setAuth, clearAuth}
})
