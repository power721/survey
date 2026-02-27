<template>
  <n-config-provider :theme="theme" :locale="naiveLocale" :date-locale="naiveDateLocale">
    <n-message-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <n-layout class="app-layout" has-sider position="absolute">
            <AppLayout />
          </n-layout>
        </n-notification-provider>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { darkTheme, zhCN, dateZhCN, enUS, dateEnUS } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import AppLayout from './layouts/AppLayout.vue'
import { useAppStore } from './stores/app'

const { locale } = useI18n()
const appStore = useAppStore()

const theme = computed(() => appStore.darkMode ? darkTheme : null)
const naiveLocale = computed(() => locale.value === 'zh-CN' ? zhCN : enUS)
const naiveDateLocale = computed(() => locale.value === 'zh-CN' ? dateZhCN : dateEnUS)
</script>

<style>
.app-layout {
  height: 100vh;
}
</style>
