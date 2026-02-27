<template>
  <div class="card-container">
    <n-card :title="isEdit ? t('vote.editVote') : t('vote.createVote')">
      <n-form ref="formRef" :model="form" label-placement="top">
        <n-form-item :label="t('vote.voteTitle')" path="title" required>
          <n-input v-model:value="form.title" :placeholder="t('vote.voteTitle')" />
        </n-form-item>
        <n-form-item :label="t('survey.description')">
          <n-input v-model:value="form.description" type="textarea" :rows="3" />
        </n-form-item>

        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item :label="t('vote.voteType')">
              <n-select v-model:value="form.voteType" :options="voteTypeOptions" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('vote.frequency')">
              <n-select v-model:value="form.frequency" :options="frequencyOptions" />
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item :label="t('survey.accessLevel')">
              <n-select v-model:value="form.accessLevel" :options="accessOptions" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('survey.anonymous')">
              <n-switch v-model:value="form.anonymous" />
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item :label="t('vote.endTime')">
              <n-date-picker v-model:value="endTimeTs" type="datetime" clearable style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi v-if="form.voteType === 'MULTIPLE'">
            <n-form-item :label="t('vote.maxOptions')">
              <n-input-number v-model:value="form.maxOptions" :min="2" clearable style="width: 100%" />
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-grid v-if="form.voteType === 'SCORED'" :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item :label="t('vote.maxTotalVotes')">
              <n-input-number v-model:value="form.maxTotalVotes" :min="1" clearable style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('vote.maxVotesPerOption')">
              <n-input-number v-model:value="form.maxVotesPerOption" :min="1" clearable style="width: 100%" />
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-divider>{{ t('survey.options') }}</n-divider>

        <draggable v-model="form.options" item-key="_key" handle=".drag-handle" animation="200" style="width: 100%">
          <template #item="{ element: opt, index: oi }">
            <n-card size="small" closable style="margin-bottom: 8px" @close="removeOption(oi)">
              <template #header>
                <n-space align="center" size="small">
                  <span class="drag-handle" style="cursor: grab; font-size: 18px; color: #999; user-select: none">&#x2630;</span>
                  <span>{{ t('survey.options') }} {{ oi + 1 }}</span>
                </n-space>
              </template>
              <n-form-item :label="t('vote.optionTitle')" required>
                <n-input v-model:value="opt.title" :placeholder="t('vote.optionTitle')" />
              </n-form-item>
              <n-form-item :label="t('vote.optionContent')">
                <n-input v-model:value="opt.content" type="textarea" :rows="2" :placeholder="t('vote.optionContent')" />
              </n-form-item>
              <n-form-item label="Image URL">
                <n-input v-model:value="opt.imageUrl" placeholder="https://..." />
              </n-form-item>
            </n-card>
          </template>
        </draggable>
        <n-button dashed block @click="addOption" style="margin-top: 8px">+ {{ t('survey.addOption') }}</n-button>

        <n-space justify="end" style="margin-top: 24px">
          <n-button @click="router.back()">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="saving" @click="handleSave">{{ t('common.save') }}</n-button>
        </n-space>
      </n-form>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import draggable from 'vuedraggable'
import { voteApi } from '@/api/vote'
import type { VotePollCreateRequest } from '@/types'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const message = useMessage()

const isEdit = computed(() => !!route.params.id)
const saving = ref(false)
const endTimeTs = ref<number | null>(null)
let keySeq = 0
function nextKey() { return `opt_${++keySeq}` }

const form = ref<VotePollCreateRequest>({
  title: '',
  description: '',
  voteType: 'SINGLE',
  frequency: 'ONCE',
  accessLevel: 'PUBLIC',
  anonymous: true,
  maxTotalVotes: null,
  maxOptions: null,
  maxVotesPerOption: null,
  endTime: null,
  options: [
    { title: '', sortOrder: 0, _key: nextKey() },
    { title: '', sortOrder: 1, _key: nextKey() },
  ],
})

const voteTypeOptions = [
  { label: t('vote.single'), value: 'SINGLE' },
  { label: t('vote.multiple'), value: 'MULTIPLE' },
  { label: t('vote.scored'), value: 'SCORED' },
]

const frequencyOptions = [
  { label: t('vote.once'), value: 'ONCE' },
  { label: t('vote.daily'), value: 'DAILY' },
]

const accessOptions = [
  { label: t('common.public'), value: 'PUBLIC' },
  { label: t('common.private'), value: 'PRIVATE' },
]

function addOption() {
  form.value.options.push({ title: '', sortOrder: form.value.options.length, _key: nextKey() })
}

function removeOption(index: number) {
  if (form.value.options.length <= 2) {
    message.warning('At least 2 options required')
    return
  }
  form.value.options.splice(index, 1)
}

async function loadPoll() {
  if (!isEdit.value) return
  try {
    const res = await voteApi.getById(Number(route.params.id))
    const poll = res.data.data
    form.value = {
      title: poll.title,
      description: poll.description || '',
      voteType: poll.voteType,
      frequency: poll.frequency,
      accessLevel: poll.accessLevel,
      anonymous: poll.anonymous,
      maxTotalVotes: poll.maxTotalVotes,
      maxOptions: poll.maxOptions,
      maxVotesPerOption: poll.maxVotesPerOption,
      endTime: poll.endTime,
      options: poll.options.map((o) => ({
        id: o.id,
        title: o.title,
        content: o.content || '',
        imageUrl: o.imageUrl || '',
        sortOrder: o.sortOrder,
        _key: nextKey(),
      })),
    }
    if (poll.endTime) {
      const ts = new Date(poll.endTime).getTime()
      endTimeTs.value = Number.isNaN(ts) ? null : ts
    }
  } catch (e) {
    console.error(e)
  }
}

async function handleSave() {
  if (!form.value.title.trim()) {
    message.warning(t('vote.voteTitle'))
    return
  }
  if (form.value.options.some((o) => !o.title.trim())) {
    message.warning(t('vote.optionTitle'))
    return
  }

  saving.value = true
  try {
    form.value.endTime = endTimeTs.value ? new Date(endTimeTs.value).toISOString() : null
    form.value.options.forEach((o, i) => { o.sortOrder = i })
    if (isEdit.value) {
      await voteApi.update(Number(route.params.id), form.value)
    } else {
      await voteApi.create(form.value)
    }
    message.success(t('common.save'))
    router.push('/votes')
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error')
  } finally {
    saving.value = false
  }
}

onMounted(loadPoll)
</script>
