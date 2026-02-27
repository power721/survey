<template>
  <n-layout has-sider style="height: 100vh">
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="220"
      :collapsed="appStore.siderCollapsed"
      show-trigger
      @collapse="appStore.siderCollapsed = true"
      @expand="appStore.siderCollapsed = false"
      :native-scrollbar="false"
      style="height: 100vh"
    >
      <div style="padding: 16px; text-align: center; font-weight: bold; font-size: 18px">
        <span v-if="!appStore.siderCollapsed">{{ t('common.appName') }}</span>
        <span v-else>S</span>
      </div>
      <n-menu
        :collapsed="appStore.siderCollapsed"
        :collapsed-width="64"
        :collapsed-icon-size="22"
        :options="menuOptions"
        :value="activeKey"
        @update:value="handleMenuSelect"
      />
    </n-layout-sider>
    <n-layout>
      <n-layout-header bordered style="height: 56px; padding: 0 24px; display: flex; align-items: center; justify-content: space-between">
        <div style="display: flex; align-items: center; gap: 12px">
          <n-breadcrumb>
            <n-breadcrumb-item @click="router.push('/')">{{ t('nav.home') }}</n-breadcrumb-item>
          </n-breadcrumb>
        </div>
        <div style="display: flex; align-items: center; gap: 12px">
          <n-switch :value="appStore.darkMode" @update:value="appStore.toggleDarkMode" size="small">
            <template #checked>🌙</template>
            <template #unchecked>☀️</template>
          </n-switch>
          <n-dropdown :options="langOptions" @select="handleLangSelect" trigger="click">
            <n-button quaternary size="small">{{ currentLangLabel }}</n-button>
          </n-dropdown>
          <template v-if="authStore.isLoggedIn">
            <n-dropdown :options="userOptions" @select="handleUserSelect" trigger="click">
              <n-button quaternary>{{ authStore.nickname || authStore.username }}</n-button>
            </n-dropdown>
          </template>
          <template v-else>
            <n-button size="small" @click="router.push('/login')">{{ t('common.login') }}</n-button>
            <n-button size="small" type="primary" @click="router.push('/register')">{{ t('common.register') }}</n-button>
          </template>
        </div>
      </n-layout-header>
      <n-layout-content content-style="padding: 24px;" :native-scrollbar="false">
        <router-view />
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const { t, locale } = useI18n()
const appStore = useAppStore()
const authStore = useAuthStore()

const activeKey = computed(() => {
  const path = route.path
  if (path === '/') return 'home'
  if (path === '/surveys/create') return 'create-survey'
  if (path === '/surveys/public') return 'public-surveys'
  if (path.startsWith('/surveys')) return 'surveys'
  if (path === '/votes/create') return 'create-vote'
  if (path === '/votes/public') return 'public-votes'
  if (path.startsWith('/votes')) return 'votes'
  if (path.startsWith('/s/')) return 'public-surveys'
  if (path.startsWith('/v/')) return 'public-votes'
  return ''
})

const menuOptions = computed(() => {
  const items: any[] = [
    { label: t('nav.home'), key: 'home' },
    { label: t('survey.publicSurveys'), key: 'public-surveys' },
    { label: t('vote.publicVotes'), key: 'public-votes' },
  ]
  if (authStore.isLoggedIn) {
    items.push(
      { type: 'divider', key: 'd1' },
      { label: t('survey.mySurveys'), key: 'surveys' },
      { label: t('survey.createSurvey'), key: 'create-survey' },
      { type: 'divider', key: 'd2' },
      { label: t('vote.myVotes'), key: 'votes' },
      { label: t('vote.createVote'), key: 'create-vote' },
    )
  }
  return items
})

function handleMenuSelect(key: string) {
  const routeMap: Record<string, string> = {
    'home': '/',
    'surveys': '/surveys',
    'create-survey': '/surveys/create',
    'public-surveys': '/surveys/public',
    'votes': '/votes',
    'create-vote': '/votes/create',
    'public-votes': '/votes/public',
  }
  const target = routeMap[key]
  if (target) router.push(target)
}

const langOptions = [
  { label: '中文', key: 'zh-CN' },
  { label: 'English', key: 'en' },
]

const currentLangLabel = computed(() => locale.value === 'zh-CN' ? '中文' : 'EN')

function handleLangSelect(key: string) {
  locale.value = key
  localStorage.setItem('locale', key)
}

const userOptions = computed(() => [
  { label: t('common.profile'), key: 'profile' },
  { label: t('common.logout'), key: 'logout' },
])

function handleUserSelect(key: string) {
  if (key === 'profile') {
    router.push('/profile')
  } else if (key === 'logout') {
    authStore.logout()
    router.push('/')
  }
}
</script>
