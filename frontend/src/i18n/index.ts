import { createI18n } from 'vue-i18n'
import en from './en'
import zhCN from './zh-CN'

const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('locale') || 'zh-CN',
  fallbackLocale: 'en',
  messages: {
    'en': en,
    'zh-CN': zhCN,
  },
})

export default i18n
