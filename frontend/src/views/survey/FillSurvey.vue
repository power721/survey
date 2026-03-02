<template>
  <div class="card-container" ref="containerRef">
    <n-spin :show="loading">
      <template v-if="submitted">
        <n-result status="success" :title="t('survey.submitSuccess')" :description="t('survey.thankYou')">
          <template #footer>
            <n-button @click="router.push('/')">{{ t('common.home') }}</n-button>
          </template>
        </n-result>
      </template>

      <template v-else-if="myResponse && survey && !editing">
        <img v-if="survey.logoUrl" :src="survey.logoUrl" :alt="survey.title"
             style="width: 100%; display: block; border-radius: 8px 8px 0 0"/>
        <n-card :title="survey.title"
                :style="survey.logoUrl ? { borderTopLeftRadius: 0, borderTopRightRadius: 0 } : {}">
          <template #header-extra>
            <n-tag type="success" size="small">{{ t('survey.alreadySubmitted') }}</n-tag>
          </template>

          <n-space style="margin-bottom: 16px">
            <n-space v-if="survey.creator" align="center" :size="4" style="cursor: pointer"
                     @click="router.push(`/user/${survey.creator.username}`)">
              <n-text depth="3">{{ t('common.creator') }}:</n-text>
              <n-avatar :src="survey.creator.avatar || undefined" :size="20" round>
                <template v-if="!survey.creator.avatar" #default>
                  <span style="font-size: 11px">{{ (survey.creator.nickname || '?').charAt(0) }}</span>
                </template>
              </n-avatar>
              <n-text depth="3" style="text-decoration: underline">{{ survey.creator.nickname }}</n-text>
            </n-space>
            <n-text v-if="survey.startTime" depth="3">{{ t('common.startTime') }}:
              {{ new Date(survey.startTime).toLocaleString() }}
            </n-text>
            <n-text v-if="survey.endTime" depth="3">{{ t('survey.endTime') }}:
              {{ new Date(survey.endTime).toLocaleString() }}
            </n-text>
          </n-space>

          <div v-if="survey.description" class="description-html" style="margin-bottom: 24px; color: #666"
               v-html="survey.description"></div>

          <n-alert type="info" style="margin-bottom: 16px">{{ t('survey.viewMyResponse') }}</n-alert>

          <div v-for="(answer, ai) in myResponse.answers" :key="answer.id" style="margin-bottom: 20px">
            <n-space align="center" style="margin-bottom: 8px">
              <span style="font-weight: 600">{{ ai + 1 }}. {{ answer.questionTitle }}</span>
            </n-space>
            <div style="padding-left: 16px; color: #333">
              <template v-if="answer.selectedOptionContent">
                <n-tag type="info" size="small">{{ answer.selectedOptionContent }}</n-tag>
              </template>
              <template v-else-if="answer.selectedOptionContents && answer.selectedOptionContents.length > 0">
                <n-space>
                  <n-tag v-for="(opt, oi) in answer.selectedOptionContents" :key="oi" type="info" size="small">{{
                      opt
                    }}
                  </n-tag>
                </n-space>
              </template>
              <template v-else-if="answer.textValue">
                <n-text>{{ answer.textValue }}</n-text>
              </template>
              <template v-else>
                <n-text depth="3">-</n-text>
              </template>
            </div>
          </div>

          <n-space justify="center" style="margin-top: 24px">
            <n-button v-if="survey.allowUpdate" type="primary" @click="startEditing">{{
                t('survey.updateResponse')
              }}
            </n-button>
            <n-button @click="router.push('/')">{{ t('common.home') }}</n-button>
          </n-space>
        </n-card>
      </template>

      <template v-else-if="survey">
        <img v-if="survey.logoUrl" :src="survey.logoUrl" :alt="survey.title"
             style="width: 100%; display: block; border-radius: 8px 8px 0 0"/>
        <n-card :title="survey.title"
                :style="survey.logoUrl ? { borderTopLeftRadius: 0, borderTopRightRadius: 0 } : {}">
          <template #header-extra>
            <n-space>
              <n-tag v-if="!isNotStarted && survey.anonymous" type="info" size="small">{{
                  t('survey.anonymous')
                }}
              </n-tag>
            </n-space>
          </template>

          <n-space style="margin-bottom: 16px">
            <n-space v-if="survey.creator" align="center" :size="4" style="cursor: pointer"
                     @click="router.push(`/user/${survey.creator.username}`)">
              <n-text depth="3">{{ t('common.creator') }}:</n-text>
              <n-avatar :src="survey.creator.avatar || undefined" :size="20" round>
                <template v-if="!survey.creator.avatar" #default>
                  <span style="font-size: 11px">{{ (survey.creator.nickname || '?').charAt(0) }}</span>
                </template>
              </n-avatar>
              <n-text depth="3" style="text-decoration: underline">{{ survey.creator.nickname }}</n-text>
            </n-space>
            <n-text v-if="survey.startTime" depth="3">{{ t('common.startTime') }}:
              {{ new Date(survey.startTime).toLocaleString() }}
            </n-text>
            <n-text v-if="survey.endTime" depth="3">{{ t('survey.endTime') }}:
              {{ new Date(survey.endTime).toLocaleString() }}
            </n-text>
          </n-space>

          <n-alert v-if="isNotStarted" type="warning" style="margin-bottom: 16px">
            {{ t('survey.notStarted') }}
          </n-alert>

          <n-alert v-if="loginRequired" type="warning" style="margin-bottom: 16px">
            {{ t('survey.loginRequired') }}
            <n-button text type="primary" @click="router.push('/login')" style="margin-left: 8px">{{
                t('common.login')
              }}
            </n-button>
          </n-alert>

          <template v-if="!loginRequired && !isNotStarted">
            <n-steps v-if="sections.length > 1" :current="currentStep + 1" style="margin-bottom: 24px">
              <n-step v-for="(sec, si) in sections" :key="si" :title="sec.title || `${t('survey.section')} ${si + 1}`"/>
            </n-steps>

            <n-form label-placement="top">
              <div v-for="(question, qi) in currentSection.questions" :key="question.id" style="margin-bottom: 24px">
                <n-form-item>
                  <template #label>
                    <n-space align="center">
                      <span style="font-weight: 600">{{ qi + 1 }}. {{ question.title }}</span>
                      <n-tag v-if="question.required" type="error" size="small">{{ t('common.required') }}</n-tag>
                      <n-tag size="small">{{ t(`survey.types.${question.type}`) }}</n-tag>
                    </n-space>
                  </template>

                  <template v-if="question.type === 'SINGLE_CHOICE'">
                    <n-radio-group v-model:value="answers[question.id].selectedOptionId">
                      <n-space vertical>
                        <n-radio v-for="opt in question.options" :key="opt.id" :value="opt.id">{{
                            opt.content
                          }}
                        </n-radio>
                      </n-space>
                    </n-radio-group>
                  </template>

                  <template v-else-if="question.type === 'MULTIPLE_CHOICE'">
                    <n-checkbox-group v-model:value="answers[question.id].selectedOptionIds">
                      <n-space vertical>
                        <n-checkbox v-for="opt in question.options" :key="opt.id" :value="opt.id">{{
                            opt.content
                          }}
                        </n-checkbox>
                      </n-space>
                    </n-checkbox-group>
                  </template>

                  <template v-else-if="question.type === 'TEXTAREA'">
                    <n-input v-model:value="answers[question.id].textValue" type="textarea" :rows="4"/>
                  </template>

                  <template v-else-if="question.type === 'NUMBER'">
                    <n-input-number v-model:value="answers[question.id].numberValue" style="width: 100%"/>
                  </template>

                  <template v-else-if="question.type === 'RATING'">
                    <n-rate :value="answers[question.id].numberValue ?? undefined"
                            @update:value="(v: number) => answers[question.id].numberValue = v" :count="5"/>
                  </template>

                  <template v-else-if="question.type === 'DATE'">
                    <n-date-picker
                        :value="answers[question.id].textValue ? new Date(answers[question.id].textValue).getTime() : null"
                        @update:value="(v: number | null) => answers[question.id].textValue = v ? new Date(v).toISOString().slice(0, 10) : ''"
                        type="date" style="width: 100%"/>
                  </template>

                  <template v-else-if="question.type === 'FILE'">
                    <n-space vertical>
                      <n-space v-if="answers[question.id].textValue" align="center">
                        <a :href="answers[question.id].textValue"
                           target="_blank">{{ answers[question.id].textValue.split('/').pop() }}</a>
                        <n-button size="tiny" type="error" quaternary @click="handleFileRemove(question.id)">
                          {{ t('common.delete') }}
                        </n-button>
                      </n-space>
                      <n-upload
                          :show-file-list="false"
                          :custom-request="({ file, onFinish, onError }) => handleFileUpload(question.id, file, onFinish, onError)"
                      >
                        <n-button>{{ t('survey.uploadFile') }}</n-button>
                      </n-upload>
                    </n-space>
                  </template>

                  <template v-else>
                    <n-input v-model:value="answers[question.id].textValue"
                             :placeholder="getPlaceholder(question.type)"/>
                  </template>
                </n-form-item>
              </div>

              <n-space justify="center">
                <n-button v-if="currentStep > 0" size="large" @click="prevStep">{{ t('survey.prevSection') }}</n-button>
                <n-button v-if="currentStep < sections.length - 1" type="primary" size="large" @click="nextStep">
                  {{ t('survey.nextSection') }}
                </n-button>
                <n-button v-if="currentStep === sections.length - 1" type="primary" size="large" :loading="submitting"
                          @click="handleSubmit">{{ t('common.submit') }}
                </n-button>
                <n-button v-if="editing" size="large" @click="cancelEditing">{{ t('common.cancel') }}</n-button>
              </n-space>
            </n-form>
          </template>
        </n-card>
      </template>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, reactive, ref, watchEffect} from 'vue'
import {onBeforeRouteLeave, useRoute, useRouter} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {useMessage} from 'naive-ui'
import {surveyApi} from '@/api/survey'
import {fileApi} from '@/api/file'
import {useAuthStore} from '@/stores/auth'
import type {QuestionDto, SurveyDto, SurveyResponseDto, SurveySubmitRequest} from '@/types'

const router = useRouter()
const route = useRoute()
const {t} = useI18n()
const message = useMessage()

const authStore = useAuthStore()
const containerRef = ref<HTMLElement | null>(null)
const loading = ref(true)
const submitting = ref(false)
const submitted = ref(false)
const editing = ref(false)
const survey = ref<SurveyDto | null>(null)
const myResponse = ref<SurveyResponseDto | null>(null)
const currentStep = ref(0)

const loginRequired = computed(() => {
  return survey.value && !survey.value.anonymous && !authStore.isLoggedIn
})

const isNotStarted = computed(() => {
  if (!survey.value?.startTime) return false
  return new Date(survey.value.startTime).getTime() > Date.now()
})

const sections = computed(() => {
  if (!survey.value) return []
  if (survey.value.sections && survey.value.sections.length > 0) {
    return survey.value.sections
  }
  return [{id: 0, title: '', sortOrder: 0, questions: survey.value.questions}]
})

const currentSection = computed(() => sections.value[currentStep.value] ?? {questions: []})

const allQuestions = computed((): QuestionDto[] => {
  if (!survey.value) return []
  if (survey.value.sections && survey.value.sections.length > 0) {
    return survey.value.sections.flatMap(s => s.questions)
  }
  return survey.value.questions
})

const answers = reactive<Record<number, {
  textValue: string;
  selectedOptionId: number | null;
  selectedOptionIds: number[];
  numberValue: number | null
}>>({})
const pendingFileDeletes = ref<string[]>([])
const newUploadedFiles = ref<string[]>([])

function validateSection(sectionIndex: number): boolean {
  const sec = sections.value[sectionIndex]
  if (!sec) return true
  for (const q of sec.questions) {
    if (!q.required) continue
    const a = answers[q.id]
    if (!a) continue
    if (q.type === 'SINGLE_CHOICE' && !a.selectedOptionId) {
      message.warning(`${q.title} ${t('common.required')}`)
      return false
    }
    if (q.type === 'MULTIPLE_CHOICE' && a.selectedOptionIds.length === 0) {
      message.warning(`${q.title} ${t('common.required')}`)
      return false
    }
    if (['NUMBER', 'RATING'].includes(q.type) && a.numberValue === null) {
      message.warning(`${q.title} ${t('common.required')}`)
      return false
    }
    if (!['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'NUMBER', 'RATING'].includes(q.type) && !a.textValue) {
      message.warning(`${q.title} ${t('common.required')}`)
      return false
    }
  }
  return true
}

function nextStep() {
  if (!validateSection(currentStep.value)) return
  if (currentStep.value < sections.value.length - 1) {
    currentStep.value++
  }
}

function prevStep() {
  if (currentStep.value > 0) currentStep.value--
}

async function handleFileUpload(questionId: number, file: any, onFinish: () => void, onError: () => void) {
  try {
    const oldUrl = answers[questionId].textValue
    if (oldUrl) {
      const oldFileName = oldUrl.split('/').pop()
      if (oldFileName) {
        const idx = newUploadedFiles.value.indexOf(oldFileName)
        if (idx !== -1) {
          newUploadedFiles.value.splice(idx, 1)
          fileApi.delete(oldFileName).catch(() => {
          })
        } else {
          pendingFileDeletes.value.push(oldFileName)
        }
      }
    }
    const res = await fileApi.upload(file.file)
    answers[questionId].textValue = res.data.data.url
    const newFileName = res.data.data.url.split('/').pop()
    if (newFileName) {
      newUploadedFiles.value.push(newFileName)
    }
    onFinish()
  } catch (e) {
    message.error('Upload failed')
    onError()
  }
}

function handleFileRemove(questionId: number) {
  const url = answers[questionId].textValue
  if (url) {
    const fileName = url.split('/').pop()
    if (fileName) {
      pendingFileDeletes.value.push(fileName)
    }
  }
  answers[questionId].textValue = ''
}

function getPlaceholder(type: string): string {
  const map: Record<string, string> = {
    TEXT: t('survey.types.TEXT'),
    EMAIL: 'name@example.com',
    URL: 'https://',
    PHONE: '13800138000',
    ID_CARD: t('survey.types.ID_CARD'),
  }
  return map[type] || ''
}

function initAnswers() {
  for (const q of allQuestions.value) {
    answers[q.id] = {textValue: '', selectedOptionId: null, selectedOptionIds: [], numberValue: null}
  }
}

function cancelEditing() {
  for (const fileName of newUploadedFiles.value) {
    fileApi.delete(fileName).catch(() => {
    })
  }
  pendingFileDeletes.value = []
  newUploadedFiles.value = []
  editing.value = false
  currentStep.value = 0
  initAnswers()
}

function startEditing() {
  if (!survey.value || !myResponse.value) return
  for (const answer of myResponse.value.answers) {
    const a = answers[answer.questionId]
    if (!a) continue
    if (answer.selectedOptionId) {
      a.selectedOptionId = answer.selectedOptionId
    }
    if (answer.selectedOptionIds && answer.selectedOptionIds.length > 0) {
      a.selectedOptionIds = [...answer.selectedOptionIds]
    }
    if (answer.textValue) {
      const question = allQuestions.value.find(q => q.id === answer.questionId)
      if (question && ['NUMBER', 'RATING'].includes(question.type)) {
        a.numberValue = Number(answer.textValue)
      } else {
        a.textValue = answer.textValue
      }
    }
  }
  currentStep.value = 0
  editing.value = true
}

async function loadSurvey() {
  try {
    const res = await surveyApi.getByShareId(route.params.shareId as string)
    survey.value = res.data.data
    initAnswers()

    if (authStore.isLoggedIn) {
      try {
        const respRes = await surveyApi.getMyResponse(route.params.shareId as string)
        if (respRes.data.data) {
          myResponse.value = respRes.data.data
        }
      } catch (_) {
        // ignore - user may not have responded yet
      }
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error loading survey')
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!survey.value) return
  if (!validateSection(currentStep.value)) return

  submitting.value = true
  try {
    const request: SurveySubmitRequest = {
      answers: allQuestions.value.map((q) => {
        const a = answers[q.id]
        return {
          questionId: q.id,
          textValue: ['NUMBER', 'RATING'].includes(q.type) ? String(a.numberValue ?? '') : a.textValue || undefined,
          selectedOptionId: a.selectedOptionId || undefined,
          selectedOptionIds: a.selectedOptionIds.length > 0 ? a.selectedOptionIds : undefined,
        }
      }),
    }
    await surveyApi.submit(route.params.shareId as string, request)
    for (const fileName of pendingFileDeletes.value) {
      await fileApi.delete(fileName).catch(() => {
      })
    }
    pendingFileDeletes.value = []
    newUploadedFiles.value = []
    submitted.value = true
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'Error')
  } finally {
    submitting.value = false
  }
}

function cleanupUnsubmittedFiles() {
  if (submitted.value) return
  for (const fileName of newUploadedFiles.value) {
    fileApi.delete(fileName).catch(() => {
    })
  }
  newUploadedFiles.value = []
  pendingFileDeletes.value = []
}

function onBeforeUnload() {
  if (submitted.value || newUploadedFiles.value.length === 0) return
  for (const fileName of newUploadedFiles.value) {
    navigator.sendBeacon(`/api/files/${fileName}/delete`)
  }
}

window.addEventListener('beforeunload', onBeforeUnload)

onBeforeRouteLeave(() => {
  cleanupUnsubmittedFiles()
})

onBeforeUnmount(() => {
  const parent = containerRef.value?.parentElement
  if (parent) {
    parent.style.backgroundImage = ''
    parent.style.backgroundSize = ''
    parent.style.backgroundPosition = ''
    parent.style.backgroundRepeat = ''
  }
  window.removeEventListener('beforeunload', onBeforeUnload)
  cleanupUnsubmittedFiles()
})

watchEffect(() => {
  const parent = containerRef.value?.parentElement
  if (parent && survey.value?.backgroundUrl) {
    parent.style.backgroundImage = `url(${survey.value.backgroundUrl})`
    parent.style.backgroundSize = 'cover'
    parent.style.backgroundPosition = 'center'
    parent.style.backgroundRepeat = 'no-repeat'
  }
})

onMounted(loadSurvey)
</script>

<style scoped>
.description-html :deep(img) {
  max-width: 100%;
  border-radius: 4px;
}

.description-html :deep(a) {
  color: #2080f0;
  text-decoration: underline;
}

.description-html :deep(p) {
  margin: 0 0 8px;
}

.description-html :deep(ul),
.description-html :deep(ol) {
  padding-left: 20px;
  margin: 0 0 8px;
}
</style>
