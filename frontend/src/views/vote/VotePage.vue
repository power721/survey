<template>
  <div class="card-container">
    <n-spin :show="loading">
      <template v-if="poll">
        <n-card :title="poll.title">
          <template #header-extra>
            <n-space>
              <n-tag :type="voteTypeTagType" size="small">{{ voteTypeLabel }}</n-tag>
              <n-tag size="small" type="success">{{ t('vote.totalVotes') }}: {{ poll.totalVoteCount }}</n-tag>
            </n-space>
          </template>

          <p v-if="poll.description" style="margin-bottom: 16px; color: #666">{{ poll.description }}</p>

          <div v-if="poll.endTime" style="margin-bottom: 16px">
            <n-text depth="3">{{ t('vote.endTime') }}: {{ formatTime(poll.endTime) }}</n-text>
          </div>

          <!-- Voting form -->
          <template v-if="!hasVoted && !voted">
            <n-space vertical size="large" style="margin-bottom: 24px">
              <!-- SINGLE: radio buttons -->
              <template v-if="poll.voteType === 'SINGLE'">
                <n-radio-group v-model:value="selectedSingle">
                  <n-space vertical>
                    <n-radio v-for="opt in poll.options" :key="opt.id" :value="opt.id">
                      <n-space align="center">
                        <img v-if="opt.imageUrl" :src="opt.imageUrl" style="max-width: 100px; max-height: 60px; border-radius: 4px" />
                        <span>{{ opt.title }}</span>
                        <n-text v-if="opt.content" depth="3" style="font-size: 12px">{{ opt.content }}</n-text>
                      </n-space>
                    </n-radio>
                  </n-space>
                </n-radio-group>
              </template>

              <!-- MULTIPLE: checkboxes -->
              <template v-else-if="poll.voteType === 'MULTIPLE'">
                <n-checkbox-group v-model:value="selectedMultiple">
                  <n-space vertical>
                    <n-checkbox v-for="opt in poll.options" :key="opt.id" :value="opt.id">
                      <n-space align="center">
                        <img v-if="opt.imageUrl" :src="opt.imageUrl" style="max-width: 100px; max-height: 60px; border-radius: 4px" />
                        <span>{{ opt.title }}</span>
                        <n-text v-if="opt.content" depth="3" style="font-size: 12px">{{ opt.content }}</n-text>
                      </n-space>
                    </n-checkbox>
                  </n-space>
                </n-checkbox-group>
              </template>

              <!-- SCORED: input-number per option -->
              <template v-else-if="poll.voteType === 'SCORED'">
                <div v-for="opt in poll.options" :key="opt.id" style="margin-bottom: 12px">
                  <n-space align="center" justify="space-between">
                    <n-space vertical :size="0">
                      <n-space align="center">
                        <img v-if="opt.imageUrl" :src="opt.imageUrl" style="max-width: 100px; max-height: 60px; border-radius: 4px" />
                        <span>{{ opt.title }}</span>
                      </n-space>
                      <n-text v-if="opt.content" depth="3" style="font-size: 12px">{{ opt.content }}</n-text>
                    </n-space>
                    <n-input-number
                      :value="scoredVotes[opt.id] || 0"
                      :min="0"
                      :max="perOptionMax"
                      size="small"
                      style="width: 120px"
                      @update:value="(v: number | null) => setScoredVote(opt.id, v ?? 0)"
                    />
                  </n-space>
                </div>
              </template>
            </n-space>

            <n-space vertical align="center" style="margin-bottom: 12px">
              <n-text v-if="poll.voteType === 'MULTIPLE' && poll.maxOptions" depth="3">
                {{ t('vote.maxOptions') }}: {{ poll.maxOptions }}
              </n-text>
              <n-text v-if="poll.voteType === 'SCORED' && poll.maxTotalVotes" depth="3">
                {{ t('vote.remainingVotes') }}: {{ remainingVotes }}
              </n-text>
              <n-text v-if="poll.voteType === 'SCORED' && poll.maxVotesPerOption" depth="3">
                {{ t('vote.maxVotesPerOption') }}: {{ poll.maxVotesPerOption }}
              </n-text>
            </n-space>

            <n-button type="primary" block size="large" :loading="submitting" @click="handleVote">
              {{ t('vote.submitVote') }}
            </n-button>
          </template>

          <!-- Already voted notice -->
          <n-alert v-if="hasVoted && !voted" type="info" style="margin-bottom: 16px">
            {{ t('vote.hasVoted') }}
          </n-alert>

          <!-- Results -->
          <template v-if="hasVoted || voted">
            <n-divider>{{ t('vote.results') }}</n-divider>
            <div v-for="opt in poll.options" :key="opt.id" style="margin-bottom: 16px">
              <n-space justify="space-between" style="margin-bottom: 4px">
                <n-space align="center">
                  <img v-if="opt.imageUrl" :src="opt.imageUrl" style="max-width: 40px; max-height: 30px; border-radius: 2px" />
                  <span>{{ opt.title }}</span>
                </n-space>
                <span>{{ opt.voteCount }} {{ t('vote.votes') }} ({{ opt.percentage.toFixed(1) }}%)</span>
              </n-space>
              <n-progress type="line" :percentage="opt.percentage" :show-indicator="false"
                          :color="getBarColor(opt.percentage)" />
            </div>
          </template>
        </n-card>
      </template>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import { voteApi } from '@/api/vote'
import type { VotePollDto } from '@/types'
import { Client } from '@stomp/stompjs'

const route = useRoute()
const { t } = useI18n()
const message = useMessage()

const loading = ref(true)
const submitting = ref(false)
const poll = ref<VotePollDto | null>(null)
const voted = ref(false)
const selectedSingle = ref<number | null>(null)
const selectedMultiple = ref<number[]>([])
const scoredVotes = reactive<Record<number, number>>({})
let stompClient: Client | null = null

const hasVoted = computed(() => poll.value?.hasVoted ?? false)

const voteTypeLabel = computed(() => {
  if (!poll.value) return ''
  if (poll.value.voteType === 'SINGLE') return t('vote.single')
  if (poll.value.voteType === 'MULTIPLE') return t('vote.multiple')
  return t('vote.scored')
})

const voteTypeTagType = computed(() => {
  if (!poll.value) return 'default'
  if (poll.value.voteType === 'SINGLE') return 'info'
  if (poll.value.voteType === 'MULTIPLE') return 'warning'
  return 'success'
})

const totalScoredVotes = computed(() => {
  return Object.values(scoredVotes).reduce((sum, v) => sum + (v || 0), 0)
})

const remainingVotes = computed(() => {
  if (!poll.value?.maxTotalVotes) return Infinity
  return poll.value.maxTotalVotes - totalScoredVotes.value
})

const perOptionMax = computed(() => {
  return poll.value?.maxVotesPerOption ?? poll.value?.maxTotalVotes ?? 999
})

function setScoredVote(optionId: number, value: number) {
  if (!poll.value) return
  const maxPerOption = poll.value.maxVotesPerOption ?? poll.value.maxTotalVotes ?? 999
  const currentOther = totalScoredVotes.value - (scoredVotes[optionId] || 0)
  const maxAllowed = poll.value.maxTotalVotes ? Math.min(maxPerOption, poll.value.maxTotalVotes - currentOther) : maxPerOption
  scoredVotes[optionId] = Math.max(0, Math.min(value, maxAllowed))
}

const colors = ['#18a058', '#2080f0', '#f0a020', '#d03050', '#8a2be2', '#ff6347']

function getBarColor(percentage: number): string {
  return colors[Math.floor(percentage / 20) % colors.length]
}

function formatTime(time: string) {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

function getDeviceId(): string {
  let deviceId = localStorage.getItem('deviceId')
  if (!deviceId) {
    deviceId = 'dev_' + Math.random().toString(36).substring(2) + Date.now().toString(36)
    localStorage.setItem('deviceId', deviceId)
  }
  return deviceId
}

async function loadPoll() {
  try {
    const res = await voteApi.getByShareId(route.params.shareId as string)
    poll.value = res.data.data
    connectWebSocket()
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error loading vote')
  } finally {
    loading.value = false
  }
}

function connectWebSocket() {
  if (!poll.value) return
  try {
    const wsUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/websocket`
    stompClient = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      onConnect: () => {
        stompClient?.subscribe(`/topic/vote/${poll.value?.shareId}`, (msg) => {
          const updated = JSON.parse(msg.body) as VotePollDto
          if (poll.value) {
            poll.value.options = updated.options
            poll.value.totalVoteCount = updated.totalVoteCount
          }
        })
      },
    })
    stompClient.activate()
  } catch (e) {
    console.warn('WebSocket connection failed:', e)
  }
}

async function handleVote() {
  if (!poll.value) return

  if (poll.value.voteType === 'SCORED') {
    // Scored mode: send votes map
    const votes: Record<number, number> = {}
    for (const [k, v] of Object.entries(scoredVotes)) {
      if (v > 0) votes[Number(k)] = v
    }
    if (Object.keys(votes).length === 0) {
      message.warning(t('survey.options'))
      return
    }

    submitting.value = true
    try {
      const res = await voteApi.submit(route.params.shareId as string, {
        votes,
        deviceId: getDeviceId(),
      })
      poll.value = res.data.data
      voted.value = true
      message.success(t('vote.voteSuccess'))
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Error')
    } finally {
      submitting.value = false
    }
  } else {
    // SINGLE or MULTIPLE mode
    const optionIds: number[] = poll.value.voteType === 'SINGLE'
      ? (selectedSingle.value ? [selectedSingle.value] : [])
      : selectedMultiple.value

    if (optionIds.length === 0) {
      message.warning(t('survey.options'))
      return
    }

    if (poll.value.voteType === 'MULTIPLE' && poll.value.maxOptions && optionIds.length > poll.value.maxOptions) {
      message.warning(`${t('vote.maxOptions')}: ${poll.value.maxOptions}`)
      return
    }

    submitting.value = true
    try {
      const res = await voteApi.submit(route.params.shareId as string, {
        optionIds,
        deviceId: getDeviceId(),
      })
      poll.value = res.data.data
      voted.value = true
      message.success(t('vote.voteSuccess'))
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Error')
    } finally {
      submitting.value = false
    }
  }
}

onMounted(loadPoll)

onUnmounted(() => {
  if (stompClient) {
    stompClient.deactivate()
  }
})
</script>
