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
        <template v-if="!appStore.siderCollapsed">
          <img v-if="appStore.siteLogo" :src="appStore.siteLogo" alt="logo"
               style="max-height: 32px; vertical-align: middle; margin-right: 8px"/>
          <span>{{ appStore.siteTitle || t('common.appName') }}</span>
        </template>
        <template v-else>
          <img v-if="appStore.siteLogo" :src="appStore.siteLogo" alt="logo" style="max-height: 28px"/>
          <span v-else>{{ (appStore.siteTitle || t('common.appName')).charAt(0) }}</span>
        </template>
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
      <n-layout-header bordered
                       style="height: 56px; padding: 0 24px; display: flex; align-items: center; justify-content: space-between">
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
              <n-button quaternary :class="{ 'admin-badge': authStore.isAdmin }"
                        style="display: flex; align-items: center; gap: 6px">
                <n-avatar v-if="authStore.avatar" :src="authStore.avatar" :size="24" round/>
                {{ authStore.nickname || authStore.username }}
              </n-button>
            </n-dropdown>
          </template>
          <template v-else>
            <n-button size="small" @click="router.push('/login')">{{ t('common.login') }}</n-button>
            <n-button v-if="appStore.registerEnabled" size="small" type="primary" @click="router.push('/register')">
              {{ t('common.register') }}
            </n-button>
          </template>
        </div>
      </n-layout-header>
      <n-layout-content content-style="padding: 24px;" :native-scrollbar="false">
        <router-view/>
        <n-layout-footer v-if="appStore.siteFooter" bordered
                         style="text-align: center; padding: 12px; color: #999; font-size: 13px">
          {{ appStore.siteFooter }}
        </n-layout-footer>
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
import {type Component, computed, h} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {NIcon} from 'naive-ui'
import {useAppStore} from '@/stores/app'
import {useAuthStore} from '@/stores/auth'
import {
  AddCircleOutline,
  CreateOutline,
  DocumentTextOutline,
  GlobeOutline,
  HomeOutline,
  ListOutline,
  MegaphoneOutline,
  SettingsOutline,
} from '@vicons/ionicons5'

function renderIcon(icon: Component) {
  return () => h(NIcon, null, {default: () => h(icon)})
}

const router = useRouter()
const route = useRoute()
const {t, locale} = useI18n()
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
  if (path.startsWith('/admin')) return 'admin-config'
  return ''
})

const menuOptions = computed(() => {
  const items: any[] = [
    {label: t('nav.home'), key: 'home', icon: renderIcon(HomeOutline)},
    {label: t('survey.publicSurveys'), key: 'public-surveys', icon: renderIcon(GlobeOutline)},
    {label: t('vote.publicVotes'), key: 'public-votes', icon: renderIcon(MegaphoneOutline)},
  ]
  if (authStore.isLoggedIn) {
    items.push(
        {type: 'divider', key: 'd1'},
        {label: t('survey.mySurveys'), key: 'surveys', icon: renderIcon(DocumentTextOutline)},
        {label: t('survey.createSurvey'), key: 'create-survey', icon: renderIcon(AddCircleOutline)},
        {type: 'divider', key: 'd2'},
        {label: t('vote.myVotes'), key: 'votes', icon: renderIcon(ListOutline)},
        {label: t('vote.createVote'), key: 'create-vote', icon: renderIcon(CreateOutline)},
    )
  }
  if (authStore.isAdmin) {
    items.push(
        {type: 'divider', key: 'd3'},
        {label: t('admin.systemConfig'), key: 'admin-config', icon: renderIcon(SettingsOutline)},
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
    'admin-config': '/admin/config',
  }
  const target = routeMap[key]
  if (target) router.push(target)
}

const langOptions = [
  {label: '简体中文', key: 'zh-CN'},
  {label: '繁體中文', key: 'zh-TW'},
  {label: 'English', key: 'en'},
  {label: '日本語', key: 'ja'},
  {label: '한국어', key: 'ko'},
]

const langLabelMap: Record<string, string> = {
  'zh-CN': '简中', 'zh-TW': '繁中', 'en': 'EN', 'ja': 'JA', 'ko': 'KO',
}
const currentLangLabel = computed(() => langLabelMap[locale.value] || 'EN')

function handleLangSelect(key: string) {
  locale.value = key
  localStorage.setItem('locale', key)
}

const userOptions = computed(() => [
  {label: t('common.profile'), key: 'profile'},
  {label: t('common.logout'), key: 'logout'},
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

<style scoped>
.admin-badge {
  border: 2px solid #d4a017 !important;
  border-radius: 6px;
  color: #d4a017 !important;
}
</style>
