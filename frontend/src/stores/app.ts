import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const darkMode = ref(localStorage.getItem('darkMode') === 'true')
  const siderCollapsed = ref(false)

  function toggleDarkMode() {
    darkMode.value = !darkMode.value
    localStorage.setItem('darkMode', String(darkMode.value))
  }

  function toggleSider() {
    siderCollapsed.value = !siderCollapsed.value
  }

  return { darkMode, siderCollapsed, toggleDarkMode, toggleSider }
})
