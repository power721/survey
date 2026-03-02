<template>
  <div class="card-container">
    <n-card :title="t('common.editProfile')">
      <n-spin :show="loading">
        <div style="display: flex; justify-content: center; margin-bottom: 24px">
          <n-avatar :src="form.avatar || undefined" :size="80" round>
            <template v-if="!form.avatar" #default>
              <span style="font-size: 32px">{{ (form.nickname || form.username || '?').charAt(0) }}</span>
            </template>
          </n-avatar>
        </div>
        <n-form ref="formRef" :model="form" label-placement="top">
          <n-form-item :label="t('auth.username')">
            <n-space align="center" style="width: 100%">
              <n-input :value="form.username" disabled style="flex: 1" />
              <n-tag v-if="authStore.isAdmin" class="admin-badge" size="small" :bordered="false">{{ t('common.admin') }}</n-tag>
            </n-space>
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
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {useMessage} from 'naive-ui'
import {authApi} from '@/api/auth'
import {useAuthStore} from '@/stores/auth'

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
  avatar: '' as string | null,
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
    form.value.avatar = profile.avatar || null
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
    authStore.avatar = profile.avatar || null
    localStorage.setItem('nickname', profile.nickname || '')
    if (profile.avatar) localStorage.setItem('avatar', profile.avatar)
    else localStorage.removeItem('avatar')
    form.value.avatar = profile.avatar || null
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

<style scoped>
.admin-badge {
  border: 2px solid #d4a017 !important;
  color: #d4a017 !important;
  background: transparent !important;
  font-weight: bold;
}
</style>
