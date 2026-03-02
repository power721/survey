<template>
  <div class="page-container">
    <n-space justify="space-between" align="center" style="margin-bottom: 16px">
      <h2>{{ t('vote.myVotes') }}</h2>
      <n-button type="primary" @click="router.push('/votes/create')">{{ t('vote.createVote') }}</n-button>
    </n-space>

    <n-spin :show="loading">
      <n-space vertical size="large">
        <n-card v-for="poll in polls" :key="poll.id" hoverable>
          <template #header>
            <n-space align="center">
              <span>{{ poll.title }}</span>
              <n-tag :type="statusType(poll.status)" size="small">{{ statusLabel(poll.status) }}</n-tag>
              <n-tag size="small">{{ poll.voteType === 'SINGLE' ? t('vote.single') : t('vote.multiple') }}</n-tag>
              <n-tag v-if="poll.anonymous" size="small" type="info">{{ t('survey.anonymous') }}</n-tag>
            </n-space>
          </template>
          <template #header-extra>
            <n-space>
              <n-button size="small" v-if="poll.status === 'DRAFT'" @click="publishPoll(poll.id)">{{ t('vote.publish') }}</n-button>
              <n-button size="small" v-if="poll.status === 'PUBLISHED'" type="warning" @click="closePoll(poll.id)">{{ t('vote.close') }}</n-button>
              <n-button size="small" @click="router.push(`/votes/${poll.id}/edit`)">{{ t('common.edit') }}</n-button>
              <n-button size="small" type="error" @click="deletePoll(poll.id)">{{ t('common.delete') }}</n-button>
            </n-space>
          </template>
          <n-space vertical>
            <p v-if="poll.description">{{ poll.description }}</p>
            <n-space>
              <n-text depth="3">{{ t('vote.totalVotes') }}: {{ poll.totalVoteCount }}</n-text>
              <n-text depth="3">{{ t('vote.frequency') }}: {{ poll.frequency === 'ONCE' ? t('vote.once') : t('vote.daily') }}</n-text>
              <n-text v-if="poll.startTime" depth="3">{{ t('common.startTime') }}: {{ new Date(poll.startTime).toLocaleString() }}</n-text>
              <n-text v-if="poll.endTime" depth="3">{{ t('vote.endTime') }}: {{ new Date(poll.endTime).toLocaleString() }}</n-text>
              <n-text depth="3" v-if="poll.status === 'PUBLISHED'">
                {{ t('vote.submitVote') }}: <a :href="`/v/${poll.shareId}`" target="_blank">{{ baseUrl }}/v/{{ poll.shareId }}</a>
              </n-text>
            </n-space>
            <n-button text type="primary" size="small" @click="router.push(`/votes/${poll.id}/records`)">
              {{ t('vote.records') }} ({{ poll.totalVoteCount }}) →
            </n-button>
          </n-space>
        </n-card>

        <n-empty v-if="!loading && polls.length === 0" :description="t('common.noData')" />

        <n-pagination
          v-if="totalPages > 1"
          v-model:page="page"
          :page-count="totalPages"
          @update:page="loadPolls"
        />
      </n-space>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage, useDialog } from 'naive-ui'
import { voteApi } from '@/api/vote'
import type { VotePollDto } from '@/types'

const router = useRouter()
const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const polls = ref<VotePollDto[]>([])
const loading = ref(false)
const page = ref(1)
const totalPages = ref(0)
const baseUrl = window.location.origin

function statusType(status: string) {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'CLOSED') return 'error'
  return 'default'
}

function statusLabel(status: string) {
  if (status === 'PUBLISHED') return t('common.published')
  if (status === 'CLOSED') return t('common.closed')
  return t('common.draft')
}

async function loadPolls() {
  loading.value = true
  try {
    const res = await voteApi.getMy({ page: page.value - 1, size: 10 })
    polls.value = res.data.data.content
    totalPages.value = res.data.data.totalPages
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function publishPoll(id: number) {
  try {
    await voteApi.publish(id)
    message.success(t('vote.publish'))
    loadPolls()
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error')
  }
}

async function closePoll(id: number) {
  try {
    await voteApi.close(id)
    message.success(t('vote.close'))
    loadPolls()
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error')
  }
}

function deletePoll(id: number) {
  dialog.warning({
    title: t('common.confirm'),
    content: t('common.confirm') + '?',
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      try {
        await voteApi.delete(id)
        message.success(t('common.delete'))
        loadPolls()
      } catch (e: any) {
        message.error(e?.response?.data?.message || 'Error')
      }
    },
  })
}

onMounted(loadPolls)
</script>
