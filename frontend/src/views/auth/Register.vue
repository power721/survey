<template>
  <div style="display: flex; justify-content: center; align-items: center; min-height: 60vh">
    <n-card :title="t('auth.registerTitle')" style="max-width: 420px; width: 100%">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="auto">
        <n-form-item :label="t('auth.username')" path="username">
          <n-input v-model:value="form.username" :placeholder="t('auth.username')" />
        </n-form-item>
        <n-form-item :label="t('auth.password')" path="password">
          <n-input v-model:value="form.password" type="password" show-password-on="click"
                   :placeholder="t('auth.password')" />
        </n-form-item>
        <n-form-item :label="t('auth.email')" path="email">
          <n-input v-model:value="form.email" :placeholder="t('auth.email')" />
        </n-form-item>
        <n-form-item :label="t('auth.nickname')" path="nickname">
          <n-input v-model:value="form.nickname" :placeholder="t('auth.nickname')" />
        </n-form-item>
        <n-space vertical size="large" style="width: 100%">
          <n-button type="primary" block :loading="loading" @click="handleRegister">
            {{ t('common.register') }}
          </n-button>
          <div style="text-align: center">
            {{ t('auth.hasAccount') }}
            <n-button text type="primary" @click="router.push('/login')">{{ t('auth.goLogin') }}</n-button>
          </div>
        </n-space>
      </n-form>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage, type FormInst } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const { t } = useI18n()
const message = useMessage()
const authStore = useAuthStore()

const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const form = ref({ username: '', password: '', email: '', nickname: '' })

const rules = {
  username: { required: true, message: t('auth.username'), trigger: 'blur' },
  password: { required: true, min: 6, message: t('auth.password'), trigger: 'blur' },
}

async function handleRegister() {
  try {
    await formRef.value?.validate()
    loading.value = true
    await authStore.register(form.value)
    message.success(t('auth.registerSuccess'))
    router.push('/')
  } catch (e: any) {
    if (e?.response?.data?.message) {
      message.error(e.response.data.message)
    }
  } finally {
    loading.value = false
  }
}
</script>
