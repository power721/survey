import {createI18n} from 'vue-i18n'
import en from './en'
import zhCN from './zh-CN'
import zhTW from './zh-TW'
import ja from './ja'
import ko from './ko'

const i18n = createI18n({
    legacy: false,
    locale: localStorage.getItem('locale') || 'zh-CN',
    fallbackLocale: 'en',
    messages: {
        'en': en,
        'zh-CN': zhCN,
        'zh-TW': zhTW,
        'ja': ja,
        'ko': ko,
    },
})

export default i18n
