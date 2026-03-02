export interface CreatorDto {
    username: string
    nickname: string
    avatar: string | null
}

export interface ApiResponse<T> {
    success: boolean
    message: string
    data: T
}

export interface AuthResponse {
    token: string
    username: string
    nickname: string
    avatar: string | null
    role: string
}

export interface UserProfile {
    id: number
    username: string
    nickname: string
    email: string
    avatar: string
    role: string
    createdAt: string
}

export interface SurveyListDto {
    id: number
    shareId: string
    title: string
    status: string
    accessLevel: string
    anonymous: boolean
    template: boolean
    allowUpdate: boolean
    logoUrl: string | null
    startTime: string | null
    endTime: string | null
    responseCount: number
    creator: CreatorDto
    createdAt: string
    updatedAt: string
}

export interface SurveyDto {
    id: number
    shareId: string
    title: string
    description: string
    status: string
    accessLevel: string
    anonymous: boolean
    template: boolean
    allowUpdate: boolean
    logoUrl: string | null
    backgroundUrl: string | null
    startTime: string | null
    endTime: string | null
    responseCount: number
    questions: QuestionDto[]
    sections: SurveySectionDto[]
    creator: CreatorDto
    createdAt: string
    updatedAt: string
}

export interface SurveySectionDto {
    id: number
    title: string | null
    sortOrder: number
    questions: QuestionDto[]
}

export interface QuestionDto {
    id: number
    type: string
    title: string
    description: string
    required: boolean
    sortOrder: number
    options: OptionDto[]
}

export interface OptionDto {
    id: number
    content: string
    sortOrder: number
}

export interface SurveyCreateRequest {
    title: string
    description: string
    accessLevel: string
    anonymous: boolean
    template: boolean
    allowUpdate: boolean
    logoUrl: string | null
    backgroundUrl: string | null
    startTime: string | null
    endTime: string | null
    questions: QuestionRequest[]
    sections: SurveySectionRequest[]
}

export interface SurveySectionRequest {
    id?: number
    title: string
    sortOrder: number
    questions: QuestionRequest[]
    _key?: string
}

export interface QuestionRequest {
    id?: number
    type: string
    title: string
    description: string
    required: boolean
    sortOrder: number
    options: OptionRequest[]
    _key?: string
}

export interface OptionRequest {
    id?: number
    content: string
    sortOrder: number
}

export interface SurveySubmitRequest {
    answers: AnswerRequest[]
}

export interface AnswerRequest {
    questionId: number
    textValue?: string
    selectedOptionId?: number
    selectedOptionIds?: number[]
}

export interface SurveyResponseDto {
    id: number
    ip: string
    username: string | null
    nickname: string | null
    answers: AnswerDto[]
    createdAt: string
}

export interface AnswerDto {
    id: number
    questionId: number
    questionTitle: string
    textValue: string
    selectedOptionId: number
    selectedOptionContent: string
    selectedOptionIds: number[]
    selectedOptionContents: string[]
}

export interface SurveyStatsDto {
    surveyId: number
    title: string
    totalResponses: number
    questionStats: QuestionStatsDto[]
}

export interface QuestionStatsDto {
    questionId: number
    questionTitle: string
    questionType: string
    optionStats: OptionStatsDto[]
    textAnswers: string[]
}

export interface OptionStatsDto {
    optionId: number
    content: string
    count: number
    percentage: number
}

export interface VotePollListDto {
    id: number
    shareId: string
    title: string
    voteType: string
    status: string
    accessLevel: string
    anonymous: boolean
    logoUrl: string | null
    startTime: string
    endTime: string
    totalVoteCount: number
    creator: CreatorDto
    createdAt: string
    updatedAt: string
}

export interface VotePollDto {
    id: number
    shareId: string
    title: string
    description: string
    voteType: string
    frequency: string
    status: string
    accessLevel: string
    anonymous: boolean
    showVoters: boolean
    maxTotalVotes: number | null
    maxOptions: number | null
    maxVotesPerOption: number | null
    logoUrl: string | null
    backgroundUrl: string | null
    startTime: string
    endTime: string
    totalVoteCount: number
    options: VoteOptionDto[]
    creator: CreatorDto
    hasVoted: boolean
    createdAt: string
    updatedAt: string
}

export interface VoteOptionDto {
    id: number
    title: string
    content: string
    imageUrl: string
    voteCount: number
    percentage: number
    sortOrder: number
    voters?: VoterDto[]
}

export interface VoterDto {
    name: string
    avatar: string | null
}

export interface VotePollCreateRequest {
    title: string
    description: string
    voteType: string
    frequency: string
    accessLevel: string
    anonymous: boolean
    showVoters: boolean
    maxTotalVotes: number | null
    maxOptions: number | null
    maxVotesPerOption: number | null
    logoUrl: string | null
    backgroundUrl: string | null
    startTime: string | null
    endTime: string | null
    options: VoteOptionRequest[]
}

export interface VoteOptionRequest {
    id?: number
    title: string
    content?: string
    imageUrl?: string
    sortOrder: number
    _key?: string
}

export interface VoteSubmitRequest {
    optionIds?: number[]
    votes?: Record<number, number>
    deviceId?: string
}

export interface VoteRecordDto {
    id: number
    optionTitle: string
    voteCount: number
    username: string | null
    nickname: string | null
    ip: string
    createdAt: string
}

export interface Page<T> {
    content: T[]
    totalElements: number
    totalPages: number
    number: number
    size: number
    first: boolean
    last: boolean
}
