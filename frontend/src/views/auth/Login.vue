<template>
  <div style="display: flex; justify-content: center; align-items: center; min-height: 60vh">
    <n-card :title="t('auth.loginTitle')" style="max-width: 420px; width: 100%">
      <n-form ref="formRef" :key="locale" :model="form" :rules="rules" label-placement="left" label-width="auto">
        <n-form-item :label="t('auth.username')" path="username">
          <n-input v-model:value="form.username" :placeholder="t('auth.username')" @keyup.enter="handleLogin"/>
        </n-form-item>
        <n-form-item :label="t('auth.password')" path="password">
          <n-input v-model:value="form.password" type="password" show-password-on="click"
                   :placeholder="t('auth.password')" @keyup.enter="handleLogin"/>
        </n-form-item>
        <n-space vertical size="large" style="width: 100%">
          <n-button type="primary" block :loading="loading" @click="handleLogin">
            {{ t('common.login') }}
          </n-button>
          <template v-if="appStore.oauth2Enabled">
            <n-divider>{{ t('auth.orLoginWith') }}</n-divider>
            <n-space justify="center" size="large">
              <n-button circle size="large" :loading="socialLoading === 'github'" @click="handleSocialLogin('github')">
                <template #icon>
                  <n-icon :component="LogoGithub"/>
                </template>
              </n-button>
              <n-button circle size="large" :loading="socialLoading === 'google'" @click="handleSocialLogin('google')">
                <template #icon>
                  <n-icon :component="LogoGoogle"/>
                </template>
              </n-button>
            </n-space>
          </template>
          <div v-if="appStore.registerEnabled" style="text-align: center">
            {{ t('auth.noAccount') }}
            <n-button text type="primary" @click="router.push('/register')">{{ t('auth.goRegister') }}</n-button>
          </div>
        </n-space>
      </n-form>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {type FormInst, NIcon, useMessage} from 'naive-ui'
import {useAuthStore} from '@/stores/auth'
import {useAppStore} from '@/stores/app'
import {authApi} from '@/api/auth'
import {LogoGithub, LogoGoogle} from '@vicons/ionicons5'

const router = useRouter()
const route = useRoute()
const {t, locale} = useI18n()
const message = useMessage()
const authStore = useAuthStore()
const appStore = useAppStore()

const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const socialLoading = ref<string | null>(null)
const form = ref({username: '', password: ''})

const rules = {
  username: {required: true, message: t('auth.username'), trigger: 'blur'},
  password: {required: true, message: t('auth.password'), trigger: 'blur'},
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

async function handleSocialLogin(provider: string) {
  try {
    socialLoading.value = provider
    const res = await authApi.getOAuth2Url(provider)
    window.location.href = res.data.data
  } catch (e: any) {
    message.error(e?.response?.data?.message || t('auth.oauth2Failed'))
    socialLoading.value = null
  }
}
</script>
