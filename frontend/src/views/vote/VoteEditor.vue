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
            <n-form-item :label="t('vote.maxTotalVotes')">
              <n-input-number v-model:value="form.maxTotalVotes" :min="0" clearable style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('vote.endTime')">
              <n-date-picker v-model:value="endTimeTs" type="datetime" clearable style="width: 100%" />
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-divider>{{ t('survey.options') }}</n-divider>

        <n-space vertical style="width: 100%">
          <n-card v-for="(opt, oi) in form.options" :key="oi" size="small" closable @close="removeOption(oi)">
            <n-grid :cols="2" :x-gap="12">
              <n-gi span="2">
                <n-form-item :label="`${t('survey.options')} ${oi + 1}`">
                  <n-input v-model:value="opt.content" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item :label="t('vote.maxOptionVotes')">
                  <n-input-number v-model:value="opt.maxVotes" :min="0" clearable style="width: 100%" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="Image URL">
                  <n-input v-model:value="opt.imageUrl" placeholder="https://..." />
                </n-form-item>
              </n-gi>
            </n-grid>
          </n-card>
          <n-button dashed block @click="addOption">+ {{ t('survey.addOption') }}</n-button>
        </n-space>

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
import { voteApi } from '@/api/vote'
import type { VotePollCreateRequest } from '@/types'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const message = useMessage()

const isEdit = computed(() => !!route.params.id)
const saving = ref(false)
const endTimeTs = ref<number | null>(null)

const form = ref<VotePollCreateRequest>({
  title: '',
  description: '',
  voteType: 'SINGLE',
  frequency: 'ONCE',
  accessLevel: 'PUBLIC',
  anonymous: true,
  maxTotalVotes: null,
  endTime: null,
  options: [
    { content: '', sortOrder: 0 },
    { content: '', sortOrder: 1 },
  ],
})

const voteTypeOptions = [
  { label: t('vote.single'), value: 'SINGLE' },
  { label: t('vote.multiple'), value: 'MULTIPLE' },
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
  form.value.options.push({ content: '', sortOrder: form.value.options.length })
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
      endTime: poll.endTime,
      options: poll.options.map((o) => ({
        id: o.id,
        content: o.content,
        imageUrl: o.imageUrl || '',
        maxVotes: o.maxVotes,
        sortOrder: o.sortOrder,
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
  if (form.value.options.some((o) => !o.content.trim())) {
    message.warning(t('survey.options'))
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
