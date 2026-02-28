<template>
  <div style="display: flex; justify-content: center; align-items: center; min-height: 60vh">
    <n-card :title="t('auth.loginTitle')" style="max-width: 420px; width: 100%">
      <n-form ref="formRef" :key="locale" :model="form" :rules="rules" label-placement="left" label-width="auto">
        <n-form-item :label="t('auth.username')" path="username">
          <n-input v-model:value="form.username" :placeholder="t('auth.username')" @keyup.enter="handleLogin" />
        </n-form-item>
        <n-form-item :label="t('auth.password')" path="password">
          <n-input v-model:value="form.password" type="password" show-password-on="click"
                   :placeholder="t('auth.password')" @keyup.enter="handleLogin" />
        </n-form-item>
        <n-space vertical size="large" style="width: 100%">
          <n-button type="primary" block :loading="loading" @click="handleLogin">
            {{ t('common.login') }}
          </n-button>
          <div style="text-align: center">
            {{ t('auth.noAccount') }}
            <n-button text type="primary" @click="router.push('/register')">{{ t('auth.goRegister') }}</n-button>
          </div>
        </n-space>
      </n-form>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage, type FormInst } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const { t, locale } = useI18n()
const message = useMessage()
const authStore = useAuthStore()

const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const form = ref({ username: '', password: '' })

const rules = {
  username: { required: true, message: t('auth.username'), trigger: 'blur' },
  password: { required: true, message: t('auth.password'), trigger: 'blur' },
}

async function handleLogin() {
  try {
    await formRef.value?.validate()
    loading.value = true
    await authStore.login(form.value.username, form.value.password)
    message.success(t('auth.loginSuccess'))
    const redirect = route.query.redirect as string
    router.push(redirect || '/')
  } catch (e: any) {
    if (e?.response?.data?.message) {
      message.error(e.response.data.message)
    }
  } finally {
    loading.value = false
  }
}
</script>
