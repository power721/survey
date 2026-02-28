<template>
  <div class="page-container">
    <h2 style="margin-bottom: 16px">{{ t('vote.publicVotes') }}</h2>

    <n-spin :show="loading">
      <n-grid :cols="1" :x-gap="16" :y-gap="16" responsive="screen" :item-responsive="true">
        <n-gi v-for="poll in polls" :key="poll.id" span="1">
          <n-card hoverable style="cursor: pointer" @click="router.push(`/v/${poll.shareId}`)">
            <template #header>{{ poll.title }}</template>
            <template #header-extra>
              <n-space>
                <n-tag :type="poll.voteType === 'SINGLE' ? 'info' : 'warning'" size="small">
                  {{ poll.voteType === 'SINGLE' ? t('vote.single') : t('vote.multiple') }}
                </n-tag>
                <n-tag size="small" type="success">{{ poll.totalVoteCount }} {{ t('vote.votes') }}</n-tag>
                <n-text v-if="poll.endTime" depth="3">{{ t('vote.endTime') }}: {{ new Date(poll.endTime).toLocaleString() }}</n-text>
                <n-text depth="3">{{ poll.creatorName }}</n-text>
              </n-space>
            </template>
            <p v-if="poll.description">{{ poll.description }}</p>
          </n-card>
        </n-gi>
      </n-grid>

      <n-empty v-if="!loading && polls.length === 0" :description="t('common.noData')" style="margin-top: 40px" />

      <n-pagination
        v-if="totalPages > 1"
        v-model:page="page"
        :page-count="totalPages"
        @update:page="loadPolls"
        style="margin-top: 16px; justify-content: center"
      />
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { voteApi } from '@/api/vote'
import type { VotePollDto } from '@/types'

const router = useRouter()
const { t } = useI18n()

const polls = ref<VotePollDto[]>([])
const loading = ref(true)
const page = ref(1)
const totalPages = ref(0)

async function loadPolls() {
  loading.value = true
  try {
    const res = await voteApi.getPublic({ page: page.value - 1, size: 12 })
    polls.value = res.data.data.content
    totalPages.value = res.data.data.totalPages
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(loadPolls)
</script>
