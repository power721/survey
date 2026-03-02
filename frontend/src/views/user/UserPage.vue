<template>
  <div class="page-container">
    <n-space align="center" style="margin-bottom: 24px">
      <n-avatar :src="creatorAvatar || undefined" :size="48" round>
        <template v-if="!creatorAvatar" #default>
          <span style="font-size: 20px">{{ (creatorName || username || '?').charAt(0) }}</span>
        </template>
      </n-avatar>
      <div>
        <div style="font-size: 18px; font-weight: 600">{{ creatorName || username }}</div>
        <div style="font-size: 13px; color: #999">@{{ username }}</div>
      </div>
    </n-space>

    <n-tabs v-model:value="activeTab" type="line" animated>
      <n-tab-pane name="surveys" :tab="t('survey.title')">
        <n-spin :show="loadingSurveys">
          <n-grid :cols="1" :x-gap="12" :y-gap="12" style="margin-top: 12px">
            <n-gi v-for="survey in surveys" :key="survey.id">
              <n-card hoverable style="cursor: pointer" @click="router.push(`/s/${survey.shareId}`)">
                <template #header>{{ survey.title }}</template>
                <template #header-extra>
                  <n-space>
                    <n-tag v-if="survey.anonymous" size="small" type="info">{{ t('survey.anonymous') }}</n-tag>
                    <n-tag size="small" type="info">{{ survey.responseCount }} {{ t('survey.responses') }}</n-tag>
                  </n-space>
                </template>
              </n-card>
            </n-gi>
          </n-grid>
          <n-space justify="center" style="margin-top: 16px">
            <n-pagination
                v-if="surveyTotalPages > 1"
                v-model:page="surveyPage"
                :page-count="surveyTotalPages"
                @update:page="loadSurveys"
            />
          </n-space>
          <n-empty v-if="!loadingSurveys && surveys.length === 0" :description="t('common.noData')"
                   style="margin-top: 32px"/>
        </n-spin>
      </n-tab-pane>

      <n-tab-pane name="votes" :tab="t('vote.title')">
        <n-spin :show="loadingVotes">
          <n-grid :cols="1" :x-gap="12" :y-gap="12" style="margin-top: 12px">
            <n-gi v-for="poll in votes" :key="poll.id">
              <n-card hoverable style="cursor: pointer" @click="router.push(`/v/${poll.shareId}`)">
                <template #header>{{ poll.title }}</template>
                <template #header-extra>
                  <n-space>
                    <n-tag :type="voteTypeTag(poll.voteType)" size="small">{{ voteTypeLabel(poll.voteType) }}</n-tag>
                    <n-tag v-if="poll.anonymous" size="small" type="info">{{ t('survey.anonymous') }}</n-tag>
                    <n-tag size="small" type="success">{{ poll.totalVoteCount }} {{ t('vote.votes') }}</n-tag>
                  </n-space>
                </template>
              </n-card>
            </n-gi>
          </n-grid>
          <n-space justify="center" style="margin-top: 16px">
            <n-pagination
                v-if="voteTotalPages > 1"
                v-model:page="votePage"
                :page-count="voteTotalPages"
                @update:page="loadVotes"
            />
          </n-space>
          <n-empty v-if="!loadingVotes && votes.length === 0" :description="t('common.noData')"
                   style="margin-top: 32px"/>
        </n-spin>
      </n-tab-pane>
    </n-tabs>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {surveyApi} from '@/api/survey'
import {voteApi} from '@/api/vote'
import type {SurveyListDto, VotePollListDto} from '@/types'

const route = useRoute()
const router = useRouter()
const {t} = useI18n()

const username = route.params.username as string
const activeTab = ref((route.query.tab as string) || 'surveys')

const creatorName = ref('')
const creatorAvatar = ref<string | null>(null)

const surveys = ref<SurveyListDto[]>([])
const loadingSurveys = ref(false)
const surveyPage = ref(1)
const surveyTotalPages = ref(0)

const votes = ref<VotePollListDto[]>([])
const loadingVotes = ref(false)
const votePage = ref(1)
const voteTotalPages = ref(0)

function voteTypeLabel(type: string) {
  if (type === 'SINGLE') return t('vote.single')
  if (type === 'MULTIPLE') return t('vote.multiple')
  return t('vote.scored')
}

function voteTypeTag(type: string) {
  if (type === 'SINGLE') return 'info'
  if (type === 'MULTIPLE') return 'warning'
  return 'success'
}

async function loadSurveys() {
  loadingSurveys.value = true
  try {
    const res = await surveyApi.getPublic({username, page: surveyPage.value - 1, size: 10})
    const data = res.data.data
    surveys.value = data.content
    surveyTotalPages.value = data.totalPages
    if (data.content.length > 0 && !creatorName.value) {
      creatorName.value = data.content[0].creator?.nickname
      creatorAvatar.value = data.content[0].creator?.avatar ?? null
    }
  } catch (e) {
    console.error(e)
  } finally {
    loadingSurveys.value = false
  }
}

async function loadVotes() {
  loadingVotes.value = true
  try {
    const res = await voteApi.getPublic({username, page: votePage.value - 1, size: 10})
    const data = res.data.data
    votes.value = data.content
    voteTotalPages.value = data.totalPages
    if (data.content.length > 0 && !creatorName.value) {
      creatorName.value = data.content[0].creator?.nickname
      creatorAvatar.value = data.content[0].creator?.avatar ?? null
    }
  } catch (e) {
    console.error(e)
  } finally {
    loadingVotes.value = false
  }
}

onMounted(() => {
  loadSurveys()
  loadVotes()
})
</script>
