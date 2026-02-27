<template>
  <div class="card-container">
    <n-card :title="t('common.editProfile')">
      <n-spin :show="loading">
        <n-form ref="formRef" :model="form" label-placement="top">
          <n-form-item :label="t('auth.username')">
            <n-input :value="form.username" disabled />
          </n-form-item>

          <n-form-item :label="t('auth.nickname')">
            <n-input v-model:value="form.nickname" :placeholder="t('auth.nickname')" />
          </n-form-item>

          <n-form-item :label="t('auth.email')">
            <n-input v-model:value="form.email" :placeholder="t('auth.email')" />
          </n-form-item>

          <n-space justify="end" style="margin-top: 16px">
            <n-button @click="router.back()">{{ t('common.cancel') }}</n-button>
            <n-button type="primary" :loading="saving" @click="handleSave">{{ t('common.save') }}</n-button>
          </n-space>
        </n-form>

        <n-divider>{{ t('common.changePassword') }}</n-divider>

        <n-form :model="pwdForm" label-placement="top">
          <n-form-item :label="t('common.oldPassword')">
            <n-input v-model:value="pwdForm.oldPassword" type="password" show-password-on="click" :placeholder="t('common.oldPassword')" />
          </n-form-item>

          <n-form-item :label="t('common.newPassword')">
            <n-input v-model:value="pwdForm.newPassword" type="password" show-password-on="click" :placeholder="t('common.newPassword')" />
          </n-form-item>

          <n-space justify="end">
            <n-button type="warning" :loading="savingPwd" @click="handleChangePassword" :disabled="!pwdForm.oldPassword || !pwdForm.newPassword">
              {{ t('common.changePassword') }}
            </n-button>
          </n-space>
        </n-form>
      </n-spin>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const { t } = useI18n()
const message = useMessage()
const authStore = useAuthStore()

const loading = ref(true)
const saving = ref(false)
const savingPwd = ref(false)

const form = ref({
  username: '',
  nickname: '',
  email: '',
})

const pwdForm = ref({
  oldPassword: '',
  newPassword: '',
})

async function loadProfile() {
  try {
    const res = await authApi.getProfile()
    const profile = res.data.data
    form.value.username = profile.username
    form.value.nickname = profile.nickname || ''
    form.value.email = profile.email || ''
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const res = await authApi.updateProfile({
      nickname: form.value.nickname,
      email: form.value.email,
    })
    const profile = res.data.data
    authStore.nickname = profile.nickname
    localStorage.setItem('nickname', profile.nickname || '')
    message.success(t('common.saveSuccess'))
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error')
  } finally {
    saving.value = false
  }
}

async function handleChangePassword() {
  savingPwd.value = true
  try {
    await authApi.updateProfile({
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword,
    })
    message.success(t('common.saveSuccess'))
    pwdForm.value.oldPassword = ''
    pwdForm.value.newPassword = ''
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error')
  } finally {
    savingPwd.value = false
  }
}

onMounted(loadProfile)
</script>
