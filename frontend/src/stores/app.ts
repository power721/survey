import {defineStore} from 'pinia'
import {ref} from 'vue'
import {configApi} from '@/api/admin'

export const useAppStore = defineStore('app', () => {
    const darkMode = ref(localStorage.getItem('darkMode') === 'true')
    const siderCollapsed = ref(false)
    const siteTitle = ref('')
    const siteDescription = ref('')
    const siteLogo = ref('')
    const siteFooter = ref('')
    const registerEnabled = ref(true)
    const oauth2Enabled = ref(false)

    function toggleDarkMode() {
        darkMode.value = !darkMode.value
        localStorage.setItem('darkMode', String(darkMode.value))
    }

    function toggleSider() {
        siderCollapsed.value = !siderCollapsed.value
    }

    async function fetchPublicConfig() {
        try {
            const res = await configApi.getPublicConfig()
            const data = res.data.data
            siteTitle.value = data['site.title'] || ''
            siteDescription.value = data['site.description'] || ''
            siteLogo.value = data['site.logo'] || ''
            siteFooter.value = data['site.footer'] || ''
            registerEnabled.value = !!data['register.enabled']
            oauth2Enabled.value = !!data['oauth2.enabled']
        } catch {
            // ignore
        }
    }

    return {
        darkMode, siderCollapsed, siteTitle, siteDescription, siteLogo, siteFooter,
        registerEnabled, oauth2Enabled, toggleDarkMode, toggleSider, fetchPublicConfig,
    }
})
