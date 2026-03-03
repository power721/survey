<template>
  <div style="padding: 24px">
    <n-card :title="t('audit.title')">
      <n-space vertical :size="16">
        <n-space>
          <n-select
              v-model:value="filters.action"
              :options="actionOptions"
              :placeholder="t('audit.action')"
              clearable
              style="width: 200px"
              @update:value="handleSearch"
          />
          <n-select
              v-model:value="filters.entityType"
              :options="entityTypeOptions"
              :placeholder="t('audit.entityType')"
              clearable
              style="width: 200px"
              @update:value="handleSearch"
          />
          <n-input
              v-model:value="filters.username"
              :placeholder="t('audit.username')"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
          />
          <n-button type="primary" @click="handleSearch">{{ t('common.search') }}</n-button>
          <n-button @click="handleReset">{{ t('common.reset') }}</n-button>
        </n-space>

        <n-data-table
            :columns="columns"
            :data="logs"
            :loading="loading"
            :pagination="pagination"
            :remote="true"
            @update:page="handlePageChange"
        />
      </n-space>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import {h, onMounted, reactive, ref} from 'vue'
import {useI18n} from 'vue-i18n'
import {NTag} from 'naive-ui'
import {auditApi, type AuditLog} from '@/api/audit'

const {t} = useI18n()

const loading = ref(false)
const logs = ref<AuditLog[]>([])
const filters = reactive({
  action: null as string | null,
  entityType: null as string | null,
  username: '',
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100],
  onChange: (page: number) => {
    pagination.page = page
    loadLogs()
  },
  onUpdatePageSize: (pageSize: number) => {
    pagination.pageSize = pageSize
    pagination.page = 1
    loadLogs()
  },
})

const actionOptions = [
  {label: t('audit.actions.CREATE_SURVEY'), value: 'CREATE_SURVEY'},
  {label: t('audit.actions.UPDATE_SURVEY'), value: 'UPDATE_SURVEY'},
  {label: t('audit.actions.DELETE_SURVEY'), value: 'DELETE_SURVEY'},
  {label: t('audit.actions.EXPORT_SURVEY'), value: 'EXPORT_SURVEY'},
  {label: t('audit.actions.CREATE_VOTE'), value: 'CREATE_VOTE'},
  {label: t('audit.actions.UPDATE_VOTE'), value: 'UPDATE_VOTE'},
  {label: t('audit.actions.DELETE_VOTE'), value: 'DELETE_VOTE'},
  {label: t('audit.actions.USER_LOGIN'), value: 'USER_LOGIN'},
  {label: t('audit.actions.USER_REGISTERED'), value: 'USER_REGISTERED'},
  {label: t('audit.actions.LOGIN_FAILED'), value: 'LOGIN_FAILED'},
  {label: t('audit.actions.REGISTER_FAILED'), value: 'REGISTER_FAILED'},
  {label: t('audit.actions.USER_PROFILE_UPDATED'), value: 'USER_PROFILE_UPDATED'},
]

const entityTypeOptions = [
  {label: t('audit.entityTypes.Survey'), value: 'Survey'},
  {label: t('audit.entityTypes.Vote'), value: 'Vote'},
  {label: t('audit.entityTypes.User'), value: 'User'},
]

const columns = [
  {
    title: t('audit.username'),
    key: 'username',
    width: 120,
  },
  {
    title: t('audit.action'),
    key: 'action',
    width: 150,
    render: (row: AuditLog) => {
      const colorMap: Record<string, "default" | "primary" | "info" | "success" | "warning" | "error"> = {
        CREATE_SURVEY: 'success',
        UPDATE_SURVEY: 'warning',
        DELETE_SURVEY: 'error',
        EXPORT_SURVEY: 'info',
        CREATE_VOTE: 'success',
        UPDATE_VOTE: 'warning',
        DELETE_VOTE: 'error',
        USER_LOGIN: 'success',
        USER_REGISTERED: 'info',
        LOGIN_FAILED: 'error',
        REGISTER_FAILED: 'error',
        USER_PROFILE_UPDATED: 'warning',
      }
      return h(NTag, {
        type: (colorMap[row.action] || 'default') as "default" | "primary" | "info" | "success" | "warning" | "error",
        size: 'small'
      }, {default: () => t(`audit.actions.${row.action}`)})
    },
  },
  {
    title: t('audit.entityType'),
    key: 'entityType',
    width: 100,
  },
  {
    title: t('audit.entityId'),
    key: 'entityId',
    width: 100,
  },
  {
    title: t('audit.details'),
    key: 'details',
    ellipsis: {
      tooltip: true,
    },
  },
  {
    title: t('audit.ipAddress'),
    key: 'ipAddress',
    width: 150,
  },
  {
    title: t('audit.createdAt'),
    key: 'createdAt',
    width: 180,
    render: (row: AuditLog) => new Date(row.createdAt).toLocaleString(),
  },
]

async function loadLogs() {
  loading.value = true
  try {
    const res = await auditApi.getLogs({
      action: filters.action || undefined,
      entityType: filters.entityType || undefined,
      username: filters.username || undefined,
      page: pagination.page - 1,
      size: pagination.pageSize,
    })
    logs.value = res.data.data.content
    pagination.itemCount = res.data.data.totalElements
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadLogs()
}

function handleReset() {
  filters.action = null
  filters.entityType = null
  filters.username = ''
  handleSearch()
}

function handlePageChange(page: number) {
  pagination.page = page
  loadLogs()
}

onMounted(loadLogs)
</script>
