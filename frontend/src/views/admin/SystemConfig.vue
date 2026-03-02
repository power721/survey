<template>
  <div style="max-width: 680px; margin: 0 auto">
    <n-card :title="t('admin.systemConfig')">
      <n-spin :show="loading">
        <n-form label-placement="left" label-width="180" :model="form">
          <n-h3>{{ t('admin.basicSettings') }}</n-h3>
          <n-form-item :label="t('admin.siteTitle')">
            <n-input v-model:value="form.siteTitle" :placeholder="t('admin.siteTitlePlaceholder')"/>
          </n-form-item>
          <n-form-item :label="t('admin.siteDescription')">
            <n-input v-model:value="form.siteDescription" :placeholder="t('admin.siteDescriptionPlaceholder')"/>
          </n-form-item>
          <n-form-item :label="t('admin.siteLogo')">
            <n-input v-model:value="form.siteLogo" placeholder="https://..."/>
          </n-form-item>
          <n-form-item :label="t('admin.siteFooter')">
            <n-input v-model:value="form.siteFooter" :placeholder="t('admin.siteFooterPlaceholder')"/>
          </n-form-item>
          <n-form-item :label="t('admin.timezone')">
            <n-select v-model:value="form.timezone" :options="timezoneOptions" filterable/>
          </n-form-item>

          <n-h3>{{ t('admin.userSettings') }}</n-h3>
          <n-form-item :label="t('admin.registerEnabled')">
            <n-switch v-model:value="form.registerEnabled"/>
          </n-form-item>

          <n-h3>{{ t('admin.oauth2Settings') }}</n-h3>
          <n-form-item :label="t('admin.oauth2Enabled')">
            <n-switch v-model:value="form.oauth2Enabled"/>
          </n-form-item>
          <template v-if="form.oauth2Enabled">
            <n-h4 prefix="bar">GitHub</n-h4>
            <n-form-item :label="t('admin.clientId')">
              <n-input v-model:value="form.githubClientId" placeholder="GitHub Client ID"/>
            </n-form-item>
            <n-form-item :label="t('admin.clientSecret')">
              <n-input v-model:value="form.githubClientSecret" type="password" show-password-on="click"
                       placeholder="GitHub Client Secret"/>
            </n-form-item>
            <n-h4 prefix="bar">Google</n-h4>
            <n-form-item :label="t('admin.clientId')">
              <n-input v-model:value="form.googleClientId" placeholder="Google Client ID"/>
            </n-form-item>
            <n-form-item :label="t('admin.clientSecret')">
              <n-input v-model:value="form.googleClientSecret" type="password" show-password-on="click"
                       placeholder="Google Client Secret"/>
            </n-form-item>
          </template>

          <n-h3>{{ t('admin.uploadSettings') }}</n-h3>
          <n-form-item :label="t('admin.uploadMaxSize')">
            <n-input-number v-model:value="form.uploadMaxSize" :min="1" style="width: 100%">
              <template #suffix>MB</template>
            </n-input-number>
          </n-form-item>
          <n-form-item :label="t('admin.uploadAllowedExtensions')">
            <n-input v-model:value="form.uploadAllowedExtensions"
                     :placeholder="t('admin.uploadAllowedExtensionsPlaceholder')" type="textarea" :rows="2"/>
          </n-form-item>

          <n-h3>{{ t('admin.securitySettings') }}</n-h3>
          <n-form-item :label="t('admin.loginMaxAttempts')">
            <n-input-number v-model:value="form.loginMaxAttempts" :min="1" :max="100" style="width: 100%"/>
          </n-form-item>
          <n-form-item :label="t('admin.jwtExpiration')">
            <n-input-number v-model:value="form.jwtExpirationHours" :min="1" :max="720" style="width: 100%">
              <template #suffix>{{ t('admin.hours') }}</template>
            </n-input-number>
          </n-form-item>

          <n-form-item>
            <n-button type="primary" :loading="saving" @click="handleSave">
              {{ t('common.save') }}
            </n-button>
          </n-form-item>
        </n-form>
      </n-spin>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {useMessage} from 'naive-ui'
import {adminApi} from '@/api/admin'

const {t} = useI18n()
const message = useMessage()

const loading = ref(true)
const saving = ref(false)

const form = ref({
  siteTitle: '',
  siteDescription: '',
  siteLogo: '',
  siteFooter: '',
  timezone: 'Asia/Shanghai',
  registerEnabled: true,
  oauth2Enabled: false,
  githubClientId: '',
  githubClientSecret: '',
  googleClientId: '',
  googleClientSecret: '',
  uploadMaxSize: 10,
  uploadAllowedExtensions: '.jpg,.jpeg,.png,.gif,.bmp,.webp,.svg,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.csv,.zip,.rar,.7z,.mp3,.mp4,.wav,.avi,.mov',
  loginMaxAttempts: 10,
  jwtExpirationHours: 24,
})

const timezoneOptions = [
  'Asia/Shanghai', 'Asia/Tokyo', 'Asia/Seoul', 'Asia/Taipei',
  'Asia/Hong_Kong', 'Asia/Singapore', 'Asia/Kolkata',
  'America/New_York', 'America/Chicago', 'America/Denver', 'America/Los_Angeles',
  'Europe/London', 'Europe/Paris', 'Europe/Berlin', 'Europe/Moscow',
  'Australia/Sydney', 'Pacific/Auckland', 'UTC',
].map(tz => ({label: tz, value: tz}))

onMounted(async () => {
  try {
    const res = await adminApi.getConfig()
    const data = res.data.data
    form.value.siteTitle = data['site.title'] || ''
    form.value.siteDescription = data['site.description'] || ''
    form.value.siteLogo = data['site.logo'] || ''
    form.value.siteFooter = data['site.footer'] || ''
    form.value.timezone = data['timezone'] || 'Asia/Shanghai'
    form.value.registerEnabled = data['register.enabled'] !== 'false'
    form.value.oauth2Enabled = data['oauth2.enabled'] === 'true'
    form.value.githubClientId = data['oauth2.github.client-id'] || ''
    form.value.githubClientSecret = data['oauth2.github.client-secret'] || ''
    form.value.googleClientId = data['oauth2.google.client-id'] || ''
    form.value.googleClientSecret = data['oauth2.google.client-secret'] || ''
    const maxSizeBytes = parseInt(data['upload.max-size'] || '0')
    form.value.uploadMaxSize = maxSizeBytes > 0 ? Math.round(maxSizeBytes / 1048576) : 10
    form.value.uploadAllowedExtensions = data['upload.allowed-extensions'] || form.value.uploadAllowedExtensions
    form.value.loginMaxAttempts = parseInt(data['login.max-attempts'] || '10') || 10
    const jwtMs = parseInt(data['jwt.expiration-ms'] || '0')
    form.value.jwtExpirationHours = jwtMs > 0 ? Math.round(jwtMs / 3600000) : 24
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Failed to load config')
  } finally {
    loading.value = false
  }
})

async function handleSave() {
  try {
    saving.value = true
    const data: Record<string, string> = {
      'site.title': form.value.siteTitle,
      'site.description': form.value.siteDescription,
      'site.logo': form.value.siteLogo,
      'site.footer': form.value.siteFooter,
      'timezone': form.value.timezone,
      'register.enabled': String(form.value.registerEnabled),
      'oauth2.enabled': String(form.value.oauth2Enabled),
      'oauth2.github.client-id': form.value.githubClientId,
      'oauth2.github.client-secret': form.value.githubClientSecret,
      'oauth2.google.client-id': form.value.googleClientId,
      'oauth2.google.client-secret': form.value.googleClientSecret,
      'upload.max-size': String(form.value.uploadMaxSize * 1048576),
      'upload.allowed-extensions': form.value.uploadAllowedExtensions,
      'login.max-attempts': String(form.value.loginMaxAttempts),
      'jwt.expiration-ms': String(form.value.jwtExpirationHours * 3600000),
    }
    await adminApi.updateConfig(data)
    message.success(t('common.saveSuccess'))
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Failed to save config')
  } finally {
    saving.value = false
  }
}
</script>
