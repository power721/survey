<template>
  <div style="display: flex; justify-content: center; align-items: center; min-height: 60vh">
    <n-spin v-if="loading" size="large" />
    <n-result v-else status="error" :title="t('auth.oauth2Failed')" :description="errorMsg">
      <template #footer>
        <n-button @click="router.push('/login')">{{ t('common.back') }}</n-button>
      </template>
    </n-result>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const message = useMessage()
const authStore = useAuthStore()

const loading = ref(true)
const errorMsg = ref('')

onMounted(async () => {
  const provider = route.params.provider as string
  const code = route.query.code as string

  if (!code) {
    loading.value = false
    errorMsg.value = 'No authorization code received'
    return
  }

  try {
    const res = await authApi.oauth2Callback(provider, code)
    authStore.setAuth(res.data.data)
    message.success(t('auth.loginSuccess'))
    router.replace('/')
  } catch (e: any) {
    loading.value = false
    errorMsg.value = e?.response?.data?.message || t('auth.oauth2Failed')
  }
})
</script>
