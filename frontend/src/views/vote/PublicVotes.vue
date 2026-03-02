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
                <n-tag v-if="poll.anonymous" size="small" type="info">{{ t('survey.anonymous') }}</n-tag>
                <n-tag size="small" type="success">{{ poll.totalVoteCount }} {{ t('vote.votes') }}</n-tag>
                <n-text v-if="poll.startTime && new Date(poll.startTime).getTime() > Date.now()" depth="3">
                  {{ t('common.startTime') }}: {{ new Date(poll.startTime).toLocaleString() }}
                </n-text>
                <n-text v-if="poll.endTime" depth="3">{{ t('vote.endTime') }}:
                  {{ new Date(poll.endTime).toLocaleString() }}
                </n-text>
                <n-space align="center" :size="4" style="cursor: pointer"
                         @click.stop="router.push(`/user/${poll.creator?.username}?tab=votes`)">
                  <n-avatar :src="poll.creator?.avatar || undefined" :size="20" round style="vertical-align: middle">
                    <template v-if="!poll.creator?.avatar" #default>
                      <span style="font-size: 11px">{{ (poll.creator?.nickname || '?').charAt(0) }}</span>
                    </template>
                  </n-avatar>
                  <n-text depth="3" style="text-decoration: underline">{{ poll.creator?.nickname }}</n-text>
                </n-space>
              </n-space>
            </template>
          </n-card>
        </n-gi>
      </n-grid>

      <n-empty v-if="!loading && polls.length === 0" :description="t('common.noData')" style="margin-top: 40px"/>

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
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {voteApi} from '@/api/vote'
import type {VotePollListDto} from '@/types'

const router = useRouter()
const {t} = useI18n()

const polls = ref<VotePollListDto[]>([])
const loading = ref(true)
const page = ref(1)
const totalPages = ref(0)

async function loadPolls() {
  loading.value = true
  try {
    const res = await voteApi.getPublic({page: page.value - 1, size: 12})
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
