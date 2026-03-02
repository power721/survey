<template>
  <n-config-provider :theme="theme" :locale="naiveLocale" :date-locale="naiveDateLocale">
    <n-message-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <n-layout class="app-layout" has-sider position="absolute">
            <AppLayout/>
          </n-layout>
        </n-notification-provider>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import {computed, onMounted} from 'vue'
import {darkTheme, dateEnUS, dateJaJP, dateKoKR, dateZhCN, dateZhTW, enUS, jaJP, koKR, zhCN, zhTW} from 'naive-ui'
import {useI18n} from 'vue-i18n'
import AppLayout from './layouts/AppLayout.vue'
import {useAppStore} from './stores/app'

const {locale} = useI18n()
const appStore = useAppStore()

onMounted(() => {
  appStore.fetchPublicConfig()
})

const naiveLocaleMap: Record<string, any> = {
  'zh-CN': zhCN, 'zh-TW': zhTW, 'ja': jaJP, 'ko': koKR, 'en': enUS,
}
const naiveDateLocaleMap: Record<string, any> = {
  'zh-CN': dateZhCN, 'zh-TW': dateZhTW, 'ja': dateJaJP, 'ko': dateKoKR, 'en': dateEnUS,
}

const theme = computed(() => appStore.darkMode ? darkTheme : null)
const naiveLocale = computed(() => naiveLocaleMap[locale.value] || enUS)
const naiveDateLocale = computed(() => naiveDateLocaleMap[locale.value] || dateEnUS)
</script>

<style>
.app-layout {
  height: 100vh;
}
</style>
