# 问卷调查与投票系统 — 需求规格说明书

**版本**：1.0  
**日期**：2026-02-27  

---

## 目录

- [1. 引言](#1-引言)
  - [1.1 项目概述](#11-项目概述)
  - [1.2 目标用户](#12-目标用户)
  - [1.3 术语定义](#13-术语定义)
- [2. 系统架构](#2-系统架构)
  - [2.1 技术选型](#21-技术选型)
  - [2.2 系统组成](#22-系统组成)
  - [2.3 数据库支持](#23-数据库支持)
  - [2.4 部署架构](#24-部署架构)
- [3. 功能需求](#3-功能需求)
  - [3.1 用户管理模块](#31-用户管理模块)
  - [3.2 问卷调查模块](#32-问卷调查模块)
  - [3.3 投票系统模块](#33-投票系统模块)
  - [3.4 文件管理模块](#34-文件管理模块)
- [4. 非功能需求](#4-非功能需求)
  - [4.1 安全需求](#41-安全需求)
  - [4.2 性能需求](#42-性能需求)
  - [4.3 可用性需求](#43-可用性需求)
  - [4.4 国际化需求](#44-国际化需求)
- [5. 数据模型](#5-数据模型)
  - [5.1 实体关系总览](#51-实体关系总览)
  - [5.2 实体详细定义](#52-实体详细定义)
- [6. 接口规格](#6-接口规格)
  - [6.1 统一响应格式](#61-统一响应格式)
  - [6.2 用户认证接口](#62-用户认证接口)
  - [6.3 问卷调查接口](#63-问卷调查接口)
  - [6.4 投票系统接口](#64-投票系统接口)
  - [6.5 文件管理接口](#65-文件管理接口)
  - [6.6 WebSocket 接口](#66-websocket-接口)
- [7. 前端页面需求](#7-前端页面需求)
- [8. 测试需求](#8-测试需求)

---

## 1. 引言

### 1.1 项目概述

本系统是一个功能完善的**在线问卷调查与投票平台**，为用户提供问卷创建、发布、填写、统计分析、Excel 导出以及多种投票模式、实时结果推送等功能。系统采用前后端分离架构，支持多数据库、Docker 一键部署。

### 1.2 目标用户

| 角色 | 说明 |
|------|------|
| **问卷创建者** | 注册用户，可创建、管理问卷和投票，查看统计数据 |
| **问卷填写者** | 任意用户（无需注册），通过分享链接填写问卷 |
| **投票参与者** | 任意用户（无需注册），通过分享链接参与投票 |
| **系统管理员** | 拥有 ADMIN 角色，可管理系统（预留） |

### 1.3 术语定义

| 术语 | 说明 |
|------|------|
| **问卷（Survey）** | 包含多个题目的表单，用户填写后提交 |
| **题目（Question）** | 问卷中的一个问题项，包含题型、标题、选项等 |
| **投票（VotePoll）** | 包含多个选项的投票，用户可选择或打分 |
| **分享ID（ShareId）** | 系统自动生成的 12 位随机字符串，用于公开链接 |
| **设备ID（DeviceId）** | 浏览器端生成并存储在 localStorage 的唯一标识 |

---

## 2. 系统架构

### 2.1 技术选型

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 + TypeScript | 3.5 |
| 构建工具 | Vite | 6.0 |
| UI 组件库 | Naive UI | 2.40 |
| 状态管理 | Pinia | 2.3 |
| 前端路由 | Vue Router | 4.5 |
| 国际化 | Vue I18n | 11.2 |
| HTTP 客户端 | Axios | 1.7 |
| 拖拽排序 | vuedraggable | 4.1 |
| WebSocket 客户端 | @stomp/stompjs + sockjs-client | 7.0 / 1.6 |
| 后端框架 | Spring Boot | 3.5.10 |
| 安全框架 | Spring Security | 6.x |
| 数据层 | Spring Data JPA + Hibernate | 6.x |
| JWT 库 | JJWT | 0.12.6 |
| 缓存 | Spring Data Redis | — |
| 限流 | Bucket4j | 8.10.1 |
| WebSocket 服务端 | Spring WebSocket（STOMP） | — |
| Excel 导出 | Apache POI | 5.3.0 |
| 代码简化 | Lombok | — |
| 运行时 | Java | 21 |
| 数据库 | H2 / MySQL / PostgreSQL | — |
| 缓存服务 | Redis | 7 |
| 反向代理 | Nginx | Alpine |

### 2.2 系统组成

```
┌─────────────┐     HTTP/WS      ┌──────────────┐
│   Browser    │ ◄──────────────► │    Nginx     │
│  (Vue SPA)   │                  │  (端口 80)    │
└─────────────┘                  └──────┬───────┘
                                        │ proxy_pass
                                 ┌──────▼───────┐
                                 │  Spring Boot  │
                                 │  (端口 8080)   │
                                 └──┬────────┬───┘
                                    │        │
                              ┌─────▼──┐  ┌──▼────┐
                              │Database│  │ Redis  │
                              │PG/MySQL│  │(6379)  │
                              └────────┘  └───────┘
```

### 2.3 数据库支持

| Profile | 数据库 | 典型场景 | 配置文件 |
|---------|--------|----------|----------|
| `h2`（默认） | H2 文件数据库 | 本地开发 | `application-h2.yml` |
| `mysql` | MySQL | 自建部署 | `application-mysql.yml` |
| `postgresql` | PostgreSQL 16 | Docker 部署 | `application-postgresql.yml` |

### 2.4 部署架构

采用 Docker Compose 编排，包含 4 个服务：

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| `nginx` | nginx:alpine | 80 | 反向代理，支持 WebSocket 升级 |
| `app` | 自构建（多阶段） | 8080 | Spring Boot 应用 |
| `db` | postgres:16-alpine | 5432 | PostgreSQL 数据库，数据持久化到 volume |
| `redis` | redis:7-alpine | 6379 | 缓存服务，数据持久化到 volume |

Dockerfile 多阶段构建：
1. **Stage 1**：Node.js 20 编译前端
2. **Stage 2**：Maven 编译后端 + 打包前端静态资源
3. **Stage 3**：eclipse-temurin:21-jre-alpine 运行时

---

## 3. 功能需求

### 3.1 用户管理模块

#### 3.1.1 用户注册

| 属性 | 规则 |
|------|------|
| **用户名** | 必填，3-50 字符，全局唯一 |
| **密码** | 必填，6-100 字符，BCrypt 加密存储 |
| **邮箱** | 选填，Email 格式校验，全局唯一 |
| **昵称** | 选填，最长 50 字符；未填写时自动生成格式为 `用户` + 8 位随机字符 |

**业务规则**：
- 用户名已存在时返回 HTTP 409 Conflict
- 邮箱已存在时返回 HTTP 409 Conflict
- 注册成功后自动生成 JWT Token 并返回
- 默认角色为 USER，账号默认启用

#### 3.1.2 用户登录

| 属性 | 规则 |
|------|------|
| **用户名** | 必填 |
| **密码** | 必填 |

**业务规则**：
- 通过 Spring Security `AuthenticationManager` 认证
- 用户名或密码错误返回 HTTP 401，消息为 "Invalid username or password"
- 登录成功后生成 JWT Token（有效期 7 天 = 604800000 ms）
- Token 包含 `subject`（用户名）和 `role` 声明
- Token 有效期默认 7 天

#### 3.1.3 获取个人信息

**前置条件**：用户已登录（携带有效 JWT）

**返回字段**：
- `id`：用户 ID
- `username`：用户名
- `nickname`：昵称
- `email`：邮箱
- `avatar`：头像 URL
- `role`：角色（USER / ADMIN）
- `createdAt`：注册时间

**业务规则**：
- 未认证时返回 HTTP 401

#### 3.1.4 更新个人信息

**前置条件**：用户已登录

**可更新字段**：

| 字段 | 规则 |
|------|------|
| `nickname` | 空白时重新自动生成 |
| `email` | 全局唯一校验，空白时清除 |
| `avatar` | URL 字符串，空白时清除 |
| `oldPassword` + `newPassword` | 修改密码时必须提供旧密码且验证通过 |

**业务规则**：
- 邮箱被占用时返回 HTTP 409 Conflict
- 旧密码不正确时返回 HTTP 400

---

### 3.2 问卷调查模块

#### 3.2.1 创建问卷

**前置条件**：用户已登录

**请求数据**：

| 字段              | 类型      | 必填 | 约束                          | 说明           |
|-----------------|---------|----|-----------------------------|--------------|
| `title`         | String  | 是  | 最长 200 字符                   | 问卷标题         |
| `description`   | String  | 否  | 最长 2000 字符，支持简单 HTML（服务端净化） | 问卷描述         |
| `accessLevel`   | Enum    | 否  | PUBLIC / PRIVATE，默认 PUBLIC  | 访问权限         |
| `anonymous`     | Boolean | 否  | 默认 true                     | 是否匿名         |
| `template`      | Boolean | 否  | 默认 false                    | 是否为模板        |
| `startTime`     | Instant | 否  | —                           | 开始时间，未到时隐藏内容 |
| `endTime`       | Instant | 否  | —                           | 截止时间         |
| `logoUrl`       | String  | 否  | 最长 500 字符                   | 问卷 Logo URL  |
| `backgroundUrl` | String  | 否  | 最长 500 字符                   | 问卷背景图片 URL   |
| `questions`     | List    | 否  | 层平题目列表（无分组时使用）              | 题目列表         |
| `sections`      | List    | 否  | 分组列表（与 questions 互斥）        | 题目分组         |

**题目数据（QuestionRequest）**：

| 字段                    | 类型      | 必填 | 约束                          | 说明            |
|-----------------------|---------|----|-----------------------------|---------------|
| `type`                | Enum    | 是  | 见题型列表                       | 题目类型          |
| `title`               | String  | 是  | 最长 500 字符                   | 题目标题          |
| `description`         | String  | 否  | 最长 1000 字符                  | 题目描述          |
| `required`            | Boolean | 否  | 默认 false                    | 是否必填          |
| `sortOrder`           | int     | 否  | 默认按数组索引                     | 排序顺序          |
| `conditionQuestionId` | Long    | 否  | 需为同问卷中前置单选/多选题 ID           | 条件题目 ID（跳题逻辑） |
| `conditionOptionId`   | Long    | 否  | 需为 conditionQuestion 的选项 ID | 触发显示的选项 ID    |
| `options`             | List    | 否  | 单选/多选题必填                    | 选项列表          |
| `minOptions`          | Integer | 否  | 仅多选题有效，最小值 1               | 最少选项数         |
| `maxOptions`          | Integer | 否  | 仅多选题有效，最小值 1               | 最多选项数         |
| `minValue`           | Double  | 否  | 仅数字题有效                      | 最小值           |
| `maxValue`           | Double  | 否  | 仅数字题有效                      | 最大值           |

**选项数据（OptionRequest）**：

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| `content` | String | 是 | 最长 500 字符 |
| `sortOrder` | int | 否 | 默认按数组索引 |

**支持的题型（12 种）**：

| 枚举值 | 题型 | 需要选项 | 答案存储方式 |
|--------|------|----------|-------------|
| `SINGLE_CHOICE` | 单选题 | 是 | `selectedOption`（外键关联） |
| `MULTIPLE_CHOICE` | 多选题 | 是 | `selectedOptionIds`（逗号分隔 ID 字符串） |
| `TEXT` | 填空题 | 否 | `textValue` |
| `TEXTAREA` | 文本题 | 否 | `textValue` |
| `NUMBER` | 数字题 | 否 | `textValue`（数字转字符串） |
| `RATING` | 评分题 | 否 | `textValue`（1-5 数字转字符串） |
| `DATE` | 日期题 | 否 | `textValue`（ISO 日期字符串） |
| `EMAIL` | 邮箱 | 否 | `textValue` |
| `URL` | 网址 | 否 | `textValue` |
| `PHONE` | 手机号码 | 否 | `textValue` |
| `ID_CARD` | 身份证号 | 否 | `textValue` |
| `FILE` | 文件上传 | 否 | `textValue`（文件 URL），需登录后上传 |

**业务规则**：
- 自动生成 12 位随机 `shareId`（UUID 去横线截取前 12 位）
- 初始状态为 DRAFT
- 返回完整问卷 DTO，包含所有题目和选项
- `description` 经 `HtmlSanitizer` 净化后存储
- 有 `startTime` 且未到达时：返回问卷基本信息不返回题目内容，前端显示开始倒计时

**题目约束验证**（提交问卷时）：
- **多选题**：
  - 若设置了 `minOptions`，选择的选项数必须 ≥ 最小值
  - 若设置了 `maxOptions`，选择的选项数必须 ≤ 最大值
  - 不满足时返回 HTTP 400，提示具体错误信息
- **数字题**：
  - 若设置了 `minValue`，输入的数字必须 ≥ 最小值
  - 若设置了 `maxValue`，输入的数字必须 ≤ 最大值
  - 不满足时返回 HTTP 400，提示具体错误信息

#### 3.2.2 编辑问卷

**前置条件**：用户已登录，且为问卷创建者

**业务规则**：
- 非创建者操作返回 HTTP 403 Forbidden
- 支持增量更新：
  - 请求中包含 `id` 的题目/选项视为更新
  - 请求中不包含 `id` 的题目/选项视为新增
  - 现有但请求中未包含的题目/选项视为删除
- 删除题目时同时删除关联的答案记录（`answerRepository.deleteByQuestionId`）
- 删除选项时通过 JPA orphanRemoval 自动清理

#### 3.2.3 问卷状态管理

| 操作 | 前置状态 | 目标状态 | 权限 |
|------|----------|----------|------|
| **发布** | DRAFT | PUBLISHED | 创建者 |
| **关闭** | PUBLISHED | CLOSED | 创建者 |
| **删除** | 任意 | — | 创建者 |

状态枚举：`DRAFT` → `PUBLISHED` → `CLOSED`

#### 3.2.4 获取问卷

| 场景 | 接口 | 权限 | 说明 |
|------|------|------|------|
| 获取详情（by ID） | `GET /api/surveys/{id}` | 创建者 | 非创建者返回 403 |
| 获取详情（by ShareId） | `GET /api/surveys/s/{shareId}` | 公开 | 问卷必须为 PUBLISHED 状态且未过期 |
| 我的问卷列表 | `GET /api/surveys/my` | 已登录 | 支持 `keyword` 关键字搜索（标题模糊匹配），分页 |
| 公开问卷列表 | `GET /api/surveys/public` | 公开 | 仅 PUBLISHED + PUBLIC 的问卷，按创建时间倒序分页 |
| 问卷模板 | `GET /api/surveys/templates` | 已登录 | 仅 `template=true` 的问卷，分页 |

#### 3.2.5 题目分组（SurveySection）

题目可按分组组织，`SurveySectionRequest` 字段：

| 字段          | 类型     | 说明          |
|-------------|--------|-------------|
| `id`        | Long   | 更新时提供，新增时为空 |
| `title`     | String | 分组标题（可空）    |
| `sortOrder` | int    | 排序顺序        |
| `questions` | List   | 分组内题目列表     |

**业务规则**：

- 问卷必须至少有一个题目（层平或分组内均可）
- `sections` 与 `questions` 互斥，不可同时传入
- 提交回复时，`questionRepository.findBySurveyId` 统一查询所有题目（含分组题目）
- 前端编辑器支持拖拽排序分组和分组内题目，默认创建一个分组
- 填写时按分组分步展示（`n-steps`），每步独立校验后才能进入下一步

#### 3.2.6 条件跳题逻辑 ✅

每道题目可设置一个显示条件，由两个字段控制：

| 字段                    | 说明                    |
|-----------------------|-----------------------|
| `conditionQuestionId` | 前置条件题目的 ID（仅支持单选/多选题） |
| `conditionOptionId`   | 前置题选择某选项时才显示本题        |

**业务规则**：

- `conditionQuestionId` 为 null 时，题目始终显示
- `conditionQuestionId` 不为 null 但 `conditionOptionId` 为 null 时，前置题有任意选择即显示
- 只有条件满足时该题目才对填写者可见
- 隐藏的题目（条件不满足）不纳入必填校验，也不包含在提交的答案中
- 编辑器中只列出当前题目**前面**的单选/多选题作为条件来源
- 切换条件题目时自动清空已选条件选项

#### 3.2.7 填写问卷

**前置条件**：问卷状态为 PUBLISHED 且未过截止时间

**请求数据（SurveySubmitRequest）**：

```json
{
  "answers": [
    {
      "questionId": 1,
      "textValue": "文本答案",
      "selectedOptionId": 5,
      "selectedOptionIds": [5, 6, 7]
    }
  ]
}
```

**业务规则**：
- 答案列表不能为空（`@NotEmpty`）
- 每个答案的 `questionId` 不能为空（`@NotNull`）
- 根据题目类型选择不同的答案字段存储
- 记录提交者的 IP 地址和 User-Agent
- IP 获取优先级：`X-Forwarded-For` → `X-Real-IP` → `remoteAddr`
- 非匿名问卷记录当前用户信息（如已登录）
- 提交后问卷 `responseCount` 自增
- **防重复提交**：
    - 已登录用户（`allowUpdate=false`）：检查 `existsBySurveyIdAndUserId`，重复则返回 HTTP 400
    - 匿名用户：检查 `existsBySurveyIdAndIp`，前端同时写入 `localStorage` 标记
    - 重复提交返回错误码 `survey.already.submitted`
- **问卷配额** ✅：
    - 若 `maxResponses` 不为 null，提交前检查 `responseCount >= maxResponses`，超出返回 `survey.quota.reached`
    - 每次新提交成功后，若 `responseCount` 达到 `maxResponses`，自动将问卷状态改为 `CLOSED`

#### 3.2.7 查看自己的回复

**接口**：`GET /api/surveys/s/{shareId}/my-response`

**前置条件**：用户已登录

**返回**：该用户最近一次提交的回复，包含所有答案详情；未提交则返回 `null`。

**业务规则**：用于 `allowUpdate=true` 时前端加载历史答案回填表单。

#### 3.2.8 回复管理

**前置条件**：用户已登录，且为问卷创建者

**回复详情包含**：
- 回复 ID、提交时间、IP 地址
- 用户信息（非匿名问卷时展示用户名和昵称）
- 答案列表：每个答案包含题目标题、文本值、选中选项内容

**分页**：默认按创建时间倒序，每页 10 条

#### 3.2.9 统计分析

**前置条件**：用户已登录，且为问卷创建者

**统计规则**：

| 题型 | 统计方式 |
|------|----------|
| **单选题** | 按 `selectedOption` 外键分组计数，计算百分比 |
| **多选题** | 解析所有 `selectedOptionIds` 字符串，逐个 ID 计数，计算百分比 |
| **其他题型** | 汇总所有 `textValue` 为文本答案列表 |

**返回数据（SurveyStatsDto）**：
- 问卷 ID、标题、总回复数
- 每个题目的统计：题目 ID、标题、类型
  - 选择题：每个选项的选择次数和百分比
  - 非选择题：所有文本答案列表

#### 3.2.10 导出 Excel

**前置条件**：用户已登录，且为问卷创建者

**Excel 格式**：
- 文件名：`survey_{id}_responses.xlsx`
- Sheet 名：Responses
- 表头样式：蓝色背景 + 粗体字
- 列：`#` | `Submit Time` | `IP` | `User`（非匿名时） | 各题目标题
- 数据行：
  - 序号（1-based）
  - 提交时间（`yyyy-MM-dd HH:mm:ss` 格式）
  - IP 地址
  - 用户昵称或用户名（非匿名时）
  - 单选题：选项内容
  - 多选题：选中选项内容，逗号分隔
  - 其他题型：文本值
- 自动调整列宽

---

### 3.3 投票系统模块

#### 3.3.1 创建投票

**前置条件**：用户已登录

**请求数据（VotePollCreateRequest）**：

| 字段                  | 类型      | 必填 | 约束                                   | 说明                 |
|---------------------|---------|----|--------------------------------------|--------------------|
| `title`             | String  | 是  | 最长 200 字符                            | 投票标题               |
| `description`       | String  | 否  | 最长 2000 字符，支持简单 HTML（服务端净化）          | 投票描述               |
| `voteType`          | Enum    | 否  | SINGLE / MULTIPLE / SCORED，默认 SINGLE | 投票类型               |
| `frequency`         | Enum    | 否  | ONCE / DAILY，默认 ONCE                 | 投票频率               |
| `accessLevel`       | Enum    | 否  | PUBLIC / PRIVATE，默认 PUBLIC           | 访问权限               |
| `anonymous`         | Boolean | 否  | 默认 true                              | 是否匿名               |
| `maxTotalVotes`     | Integer | 否  | —                                    | 每人最多总票数（仅 SCORED）  |
| `maxOptions`        | Integer | 否  | —                                    | 最多可选项数（仅 MULTIPLE） |
| `maxVotesPerOption` | Integer | 否  | —                                    | 每项最多投票数（仅 SCORED）  |
| `startTime`         | Instant | 否  | —                                    | 开始时间，未到时隐藏内容       |
| `endTime`           | Instant | 否  | —                                    | 截止时间               |
| `logoUrl`           | String  | 否  | 最长 500 字符                            | 投票 Logo URL        |
| `backgroundUrl`     | String  | 否  | 最长 500 字符                            | 投票背景图片 URL         |
| `options`           | List    | 是  | 不能为空                                 | 投票选项列表             |

**选项数据（VoteOptionRequest）**：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| `title` | String | 是 | 最长 200 字符 | 选项标题 |
| `content` | String | 否 | 最长 500 字符 | 选项描述 |
| `imageUrl` | String | 否 | 最长 1000 字符 | 选项图片 URL |
| `sortOrder` | int | 否 | 默认按数组索引 | 排序顺序 |

#### 3.3.2 批量添加选项

编辑器提供批量输入模式：在文本框中每行输入一个选项，格式为：

```
选项标题
选项标题 https://example.com/image.jpg
```

**业务规则**：

- 每行以空格分割：第一部分为标题，第二部分（可选）为图片 URL
- 空行和空白行忽略
- 一次性创建多个选项，添加到现有选项列表末尾

#### 3.3.3 投票类型详细说明

##### SINGLE（单选投票）

- 用户选择**恰好 1 个**选项
- 提交时验证：`optionIds` 列表长度必须为 1
- 违反规则返回 "Single choice vote allows only one option"

##### MULTIPLE（多选投票）

- 用户可选择**多个**选项
- 可选约束 `maxOptions`：最多可选项数
- 提交时验证：选中数量不超过 `maxOptions`
- 违反规则返回 "You can select at most {N} options"

##### SCORED（计分投票）

- 用户可为**每个选项分配票数**
- 可选约束：
  - `maxVotesPerOption`：单个选项最多分配票数
  - `maxTotalVotes`：每人最多总票数（所有选项票数之和）
- 提交时使用 `votes` 字段（`Map<Long, Integer>`）
- 验证规则：
  - 每项票数 ≥ 0
  - 每项票数 ≤ `maxVotesPerOption`（若设置）
  - 已用总票数 + 本次总票数 ≤ `maxTotalVotes`（若设置）
  - 票数为 0 的选项不记录
- 违反规则返回相应错误消息

#### 3.3.4 投票频率控制

| 频率 | Redis 键格式 | TTL | 说明 |
|------|-------------|-----|------|
| **ONCE** | `vote:{pollId}:{identifier}` | 永久 | 一次性投票，永不过期 |
| **DAILY** | `vote:daily:{pollId}:{identifier}:{date}` | 1 天 | 每日投票，次日自动过期 |

`identifier` 格式：
- 已登录用户：`user:{userId}`
- 未登录用户：`ip:{ipAddress}`

#### 3.3.5 防重复投票机制

采用**多层次**检测策略，按优先级执行：

```
1. Redis 快速检测（ONCE / DAILY 模式）
   ├── 命中 → 返回"已投票"
   └── 未命中 → 继续
2. 数据库回退检测（仅 ONCE 模式）
   ├── 已登录 → 按 userId + pollId 查询 VoteRecord
   ├── 有 deviceId → 按 deviceId + pollId 查询
   └── 无 deviceId → 按 ip + pollId 查询
```

#### 3.3.6 API 限流

- 使用 **Bucket4j 令牌桶算法**
- 限流粒度：`vote:{ip}` 键
- 限流规则：**每分钟 10 次请求**
- 超限返回 HTTP 429 Too Many Requests

#### 3.3.7 提交投票

**请求数据（VoteSubmitRequest）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `optionIds` | List\<Long\> | 选中选项 ID 列表（SINGLE / MULTIPLE 类型使用） |
| `votes` | Map\<Long, Integer\> | 选项 ID → 票数映射（SCORED 类型使用） |
| `deviceId` | String | 浏览器设备 ID |

**业务流程**：

```
1. 验证投票状态（PUBLISHED 且未过期）
2. API 限流检测
3. 防重复投票检测
4. 构建投票数据 + 约束验证
5. 更新选项票数 + 创建投票记录
6. 更新投票总票数
7. Redis 标记已投票
8. WebSocket 广播实时结果
9. 返回最新投票数据
```

#### 3.3.8 倒计时显示

- 投票页面实时计算当前时间状态：

| 状态               | 限制   | 前端展示         |
|------------------|------|--------------|
| `startTime` 未到   | 不能投票 | 隐藏选项，展示开始倒计时 |
| 进行中（有 `endTime`） | 可投票  | 展示结束倒计时      |
| 进行中（无 `endTime`） | 可投票  | 不显示倒计时       |
| `endTime` 已过     | 不能投票 | 显示已结束提示      |

- 问卷页面同理：`startTime` 未到时隐藏题目内容，显示开始倒计时

#### 3.3.9 投票结果排名

- 投票选项按得票数降序排列
- 并列选项显示相同排名（如两个选项并列第 1，下一项为第 3）
- 实时推送的 `VotePollDto` 中包含每个选项的 `rank` 字段

#### 3.3.10 实时结果推送

- 协议：**STOMP over WebSocket**
- 服务端端点：`/ws`（支持 SockJS 回退）
- 订阅频道：`/topic/vote/{shareId}`
- 触发时机：每次投票提交成功后
- 推送内容：完整的 `VotePollDto`，包含所有选项的最新票数和百分比
- 前端自动更新投票计数和百分比进度条

#### 3.3.11 编辑投票

**前置条件**：用户已登录，且为投票创建者

**业务规则**：
- 非创建者操作返回 HTTP 403 Forbidden
- 增量更新逻辑与问卷编辑类似
- 删除选项前先删除关联的投票记录（`recordRepository.deleteByOptionId`）
- 更新现有选项时**保留** `voteCount`

#### 3.3.12 投票状态管理

| 操作 | 权限 | 说明 |
|------|------|------|
| **发布** | 创建者 | DRAFT → PUBLISHED |
| **关闭** | 创建者 | PUBLISHED → CLOSED |
| **删除** | 创建者 | 先删除所有投票记录再删除投票 |

#### 3.3.13 获取投票

| 场景 | 接口 | 权限 | 特殊处理 |
|------|------|------|----------|
| 获取详情（by ID） | `GET /api/votes/{id}` | 创建者 | — |
| 获取详情（by ShareId） | `GET /api/votes/v/{shareId}` | 公开 | 检测 hasVoted 状态，必须 PUBLISHED |
| 我的投票列表 | `GET /api/votes/my` | 已登录 | 按创建时间倒序分页 |
| 公开投票列表 | `GET /api/votes/public` | 公开 | 仅 PUBLISHED + PUBLIC |

#### 3.3.14 投票记录管理 ✅

**前置条件**：用户已登录，且为投票创建者或系统管理员

**功能描述**：查看投票的所有投票记录详情。

**接口**：`GET /api/votes/{id}/records`

**权限验证**：

- 仅投票创建者和 ADMIN 角色可访问
- 非创建者且非管理员返回 HTTP 403 Forbidden

**返回数据**：

- 投票记录列表（分页）
- 每条记录包含：投票人信息、选择的选项、投票时间、IP 地址

**前端页面**：

- 路由：`/votes/{id}/records`
- 在"我的投票"列表中，每个投票有"查看记录"按钮

#### 3.3.15 用户投票历史 ✅

**前置条件**：用户已登录

**功能描述**：用户查看自己在所有投票中的投票历史记录。

**接口**：`GET /api/votes/history`

**查询参数**：

- 支持分页（`page`, `size`）
- 默认按投票时间倒序排列

**返回数据（VoteRecordDto）**：

| 字段            | 说明           |
|---------------|--------------|
| `id`          | 记录 ID        |
| `pollId`      | 投票 ID        |
| `pollShareId` | 投票分享 ID      |
| `pollTitle`   | 投票标题         |
| `optionTitle` | 选择的选项标题      |
| `voteCount`   | 投票数（计分投票时显示） |
| `username`    | 投票用户名        |
| `nickname`    | 投票用户昵称       |
| `ip`          | 投票 IP        |
| `createdAt`   | 投票时间         |

**业务规则**：

- 使用 `@Transactional(readOnly = true)` 避免懒加载异常
- 通过 `pollShareId` 直接跳转到投票详情页（`/v/{shareId}`）
- 仅返回当前登录用户的投票记录

**前端页面**：

- 路由：`/votes/history`（需要登录）
- 侧边栏菜单："投票历史"
- 显示投票标题、选择的选项、投票时间
- 点击可跳转到投票详情页查看结果

#### 3.3.16 投票选项图片

- 每个选项可配置 `imageUrl`
- 前端投票页面以大图方式展示（最大宽度 100%，最大高度 300px）
- 点击图片打开**全屏预览浮层**：
  - 深色半透明遮罩（rgba(0,0,0,0.85)）
  - 图片居中展示（最大 90vw × 90vh）
  - 点击遮罩或右上角 × 按钮关闭
  - 点击图片本身不关闭
- 编辑器中图片 URL 输入框下方实时预览（最大高度 200px）

---

### 3.4 文件管理模块

#### 3.4.1 文件上传

| 属性 | 规则 |
|------|------|
| **最大文件大小** | 100MB（`app.upload.max-size`，默认 104857600 bytes） |
| **存储目录** | `app.upload.dir`，默认 `uploads/` |
| **文件命名** | UUID + 原始扩展名 |
| **空文件** | 拒绝上传 |
| **需要认证** | 必须携带有效 JWT Token |

**返回数据**：
- `url`：文件访问 URL（`/api/files/{uuid.ext}`）
- `name`：原始文件名

#### 3.4.2 文件下载

- 路径：`GET /api/files/{fileName}`
- Content-Type：`application/octet-stream`
- Content-Disposition：`attachment; filename="{fileName}"`
- 文件不存在返回 HTTP 404

---

### 3.5 管理员模块

#### 3.5.1 数据仪表盘 ✅

**前置条件**：用户角色为 ADMIN

**功能描述**：管理员可查看全站数据统计和趋势分析。

**统计指标**：

- 总用户数：系统注册用户总数
- 总问卷数：创建的问卷总数
- 总回复数：问卷回复总数
- 总投票数：创建的投票总数
- 总投票记录数：投票记录总数

**趋势数据**：

- 用户增长趋势：最近 30 天每日新增用户数
- 问卷创建趋势：最近 30 天每日新增问卷数
- 问卷回复趋势：最近 30 天每日新增回复数

**接口**：`GET /api/admin/dashboard/stats`

**返回数据**：

```json
{
  "totalUsers": 100,
  "totalSurveys": 50,
  "totalResponses": 500,
  "totalVotes": 30,
  "totalVoteRecords": 300,
  "userTrend": [
    {
      "date": "2025-01-01",
      "count": 5
    },
    ...
  ],
  "surveyTrend": [
    {
      "date": "2025-01-01",
      "count": 2
    },
    ...
  ],
  "responseTrend": [
    {
      "date": "2025-01-01",
      "count": 10
    },
    ...
  ]
}
```

**实现细节**：

- 使用 JPA 的 `DATE_FORMAT` 函数按日期分组统计
- 趋势数据填充缺失日期（count 为 0）以保证连续性
- 前端路由：`/admin/dashboard`（需要 ADMIN 角色）
- 使用 Naive UI 的 Statistic 和 Card 组件展示数据

#### 3.5.2 系统配置模块

**前置条件**：用户角色为 ADMIN

系统配置存储在数据库 `system_config` 表（键值对），启动时加载到内存缓存（`ConcurrentHashMap`），修改后立即生效。

**配置项说明**：

| 分类         | 配置键                           | 说明                                |
|------------|-------------------------------|-----------------------------------|
| **站点信息**   | `site.title`                  | 站点标题                              |
|            | `site.description`            | 站点描述                              |
|            | `site.logo`                   | Logo URL                          |
|            | `site.footer`                 | 页脚文字                              |
|            | `timezone`                    | 服务器时区                             |
| **注册控制**   | `register.enabled`            | 是否允许用户注册（`true`/`false`）          |
| **OAuth2** | `oauth2.enabled`              | 是否启用第三方登录                         |
|            | `oauth2.redirect-uri`         | 前端回调基础 URL                        |
|            | `oauth2.github.client-id`     | GitHub OAuth2 Client ID           |
|            | `oauth2.github.client-secret` | GitHub OAuth2 Client Secret       |
|            | `oauth2.google.client-id`     | Google OAuth2 Client ID           |
|            | `oauth2.google.client-secret` | Google OAuth2 Client Secret       |
| **文件上传**   | `upload.max-size`             | 最大文件大小（字节）                        |
|            | `upload.allowed-extensions`   | 允许的扩展名（逗号分隔）                      |
| **安全**     | `login.max-attempts`          | 登录最大失败次数                          |
|            | `jwt.expiration-ms`           | JWT 有效期（毫秒）                       |
|            | `jwt.secret`                  | JWT 签名密钥（首次启动自动随机生成 32 字节 Base64） |

#### 3.5.2 接口

| 方法  | 路径                   | 说明                              | 认证    |
|-----|----------------------|---------------------------------|-------|
| GET | `/api/admin/config`  | 获取所有配置                          | ADMIN |
| PUT | `/api/admin/config`  | 批量更新配置                          | ADMIN |
| GET | `/api/config/public` | 获取公开配置（站点信息、注册开关、oauth2Enabled） | 否     |

#### 3.5.3 集成点

- **AuthService**：注册时检查 `register.enabled`，关闭时返回 HTTP 403
- **OAuth2Service**：读取 DB 中的 client-id/secret 和 `oauth2.redirect-uri`，回退到 `application.yml`
- **FileService**：读取 `upload.max-size` 和 `upload.allowed-extensions`，回退到默认值
- **AuthController**：读取 `login.max-attempts` 用于登录失败限流
- **JwtTokenProvider**：读取 `jwt.expiration-ms` 和 `jwt.secret`；首次启动时自动生成随机密钥并持久化

#### 3.5.4 前端页面

- 路由：`/admin/config`（需要 ADMIN 角色）
- 5 个分组：基本信息、用户注册、OAuth2、文件上传、安全配置
- 应用启动时从 `/api/config/public` 获取公开配置，用于显示站点 Logo、页脚、注册入口、社交登录按钮

#### 3.5.5 操作审计日志 ✅

**前置条件**：用户角色为 ADMIN

**功能描述**：记录系统关键操作，供管理员审计和追溯。

**记录的操作类型**：

| 操作类型                | 说明        |
|---------------------|-----------|
| `CREATE_SURVEY`     | 创建问卷      |
| `UPDATE_SURVEY`     | 更新问卷      |
| `DELETE_SURVEY`     | 删除问卷      |
| `EXPORT_SURVEY`     | 导出问卷数据    |
| `CREATE_VOTE`       | 创建投票      |
| `UPDATE_VOTE`       | 更新投票      |
| `DELETE_VOTE`       | 删除投票      |
| `USER_LOGIN`        | 用户登录成功    |
| `USER_REGISTERED`   | 用户注册成功    |
| `LOGIN_FAILED`      | 登录失败（含限流） |
| `REGISTER_FAILED`   | 注册失败（含限流） |
| `USER_PROFILE_UPDATED` | 用户更新个人信息  |

**审计日志字段**：

| 字段           | 说明                         |
|--------------|----------------------------|
| `id`         | 日志 ID                      |
| `action`     | 操作类型（枚举）                   |
| `entityType` | 实体类型（Survey / Vote / User） |
| `entityId`   | 实体 ID                      |
| `userId`     | 操作用户 ID                    |
| `username`   | 操作用户名                      |
| `ip`         | 操作 IP 地址                   |
| `createdAt`  | 操作时间                       |

**接口**：`GET /api/admin/audit-logs`

**查询参数**：

- 支持分页（`page`, `size`）
- 默认按创建时间倒序排列

**前端页面**：

- 路由：`/admin/audit-logs`（需要 ADMIN 角色）
- 使用 Naive UI 的 DataTable 组件展示日志列表
- 支持分页浏览

---

## 4. 非功能需求

### 4.1 安全需求

#### 4.1.1 认证机制

- **JWT Token** 方式，无状态会话（`SessionCreationPolicy.STATELESS`）
- Token 在 HTTP Header 中传递：`Authorization: Bearer {token}`
- Token 有效期：7 天（604800000 ms）
- Token 内容：`subject`（用户名）+ `role`（角色）+ `iat`（签发时间）+ `exp`（过期时间）
- 签名算法：HMAC-SHA，密钥为 Base64 编码字符串

#### 4.1.2 授权规则

| 路径模式 | 权限 |
|----------|------|
| `/api/auth/**` | permitAll |
| `/h2-console/**` | permitAll |
| `/ws/**` | permitAll |
| `GET /api/surveys/public/**` | permitAll |
| `GET /api/surveys/s/**` | permitAll |
| `POST /api/surveys/s/*/submit` | permitAll |
| `GET /api/votes/public/**` | permitAll |
| `GET /api/votes/v/**` | permitAll |
| `POST /api/votes/v/*/submit` | permitAll |
| `GET /api/files/**` | permitAll |
| `/api/admin/**` | ROLE_ADMIN |
| 其他所有请求 | authenticated |

#### 4.1.3 密码安全

- 使用 **BCrypt** 哈希算法加密存储
- 修改密码时必须验证旧密码

#### 4.1.4 CORS 跨域

- 允许来源：可配置（`app.cors.allowed-origins`，默认 `http://localhost:5173`）
- 允许方法：GET, POST, PUT, DELETE, OPTIONS
- 允许头：`*`
- 允许凭证：true
- 预检缓存：3600 秒

#### 4.1.5 CSRF 防护

- 已禁用（无状态 JWT 场景不需要 CSRF）

### 4.2 性能需求

| 指标 | 要求 |
|------|------|
| **API 限流** | 每 IP 每分钟 10 次投票请求 |
| **投票判重** | Redis 快速查询 + 数据库回退 |
| **WebSocket** | 单次投票触发实时广播 |
| **文件上传** | 最大 100MB，需登录 |
| **数据库** | JPA 延迟加载（`FetchType.LAZY`），只读事务 |

### 4.3 可用性需求

| 特性 | 说明 |
|------|------|
| **响应式布局** | 可折叠侧边栏（宽度 220px / 折叠 64px），适配桌面和移动设备 |
| **深色模式** | Naive UI darkTheme 主题切换，存储在本地 |
| **路由守卫** | 未登录访问受保护页面自动跳转到登录页，登录后重定向回原页面 |
| **表单验证** | 前端实时校验 + 后端 Bean Validation 双重验证 |
| **拖拽排序** | vuedraggable 支持题目和选项拖拽排序 |
| **全局异常处理** | 统一错误响应格式，前端显示友好错误消息 |

### 4.4 国际化需求

#### 前端国际化

- 框架：Vue I18n
- 支持语言：**zh-CN**（简体）/ **zh-TW**（繁体）/ **en** / **ja** / **ko**
- 切换方式：顶部导航栏下拉选择，存储在 `localStorage`
- Naive UI 组件库跟随语言切换

#### 后端国际化

- 框架：Spring MessageSource
- 消息文件：`messages.properties`（英文）、`messages_zh_CN`（中文简体）、`messages_zh_TW`（繁体）、`messages_ja`（日语）、
  `messages_ko`（韩语）
- 编码：UTF-8

---

## 5. 数据模型

### 5.1 实体关系总览

```
User (1) ──────────── (N) Survey
User (1) ──────────── (N) VotePoll
User (1) ──────────── (N) SurveyResponse
User (1) ──────────── (N) VoteRecord

Survey (1) ────────── (N) SurveySection
Survey (1) ────────── (N) Question  (层平题目，居中兼容)
SurveySection (1) ─── (N) Question  (分组内题目)
Question (1) ──────── (N) QuestionOption
Survey (1) ────────── (N) SurveyResponse
SurveyResponse (1) ── (N) Answer
Answer (N) ────────── (1) Question
Answer (N) ────────── (0..1) QuestionOption

VotePoll (1) ──────── (N) VoteOption
VotePoll (1) ──────── (N) VoteRecord
VoteOption (1) ─────── (N) VoteRecord
```

### 5.2 实体详细定义

#### users 表

| 字段           | 类型           | 约束                 | 说明                          |
|--------------|--------------|--------------------|-----------------------------|
| `id`         | BIGINT       | PK, AUTO_INCREMENT | 用户 ID                       |
| `username`   | VARCHAR(50)  | UNIQUE, NOT NULL   | 用户名                         |
| `password`   | VARCHAR(255) | NOT NULL           | BCrypt 密码哈希                 |
| `email`      | VARCHAR(100) | UNIQUE, NULLABLE   | 邮箱                          |
| `nickname`   | VARCHAR(50)  | NULLABLE           | 昵称                          |
| `avatar`     | VARCHAR(500) | NULLABLE           | 头像 URL（有邮箱时前端自动使用 Gravatar） |
| `role`       | VARCHAR(20)  | NOT NULL, 默认 USER  | 角色枚举                        |
| `enabled`    | BOOLEAN      | 默认 true            | 是否启用                        |
| `created_at` | TIMESTAMP    | 自动生成               | 创建时间                        |
| `updated_at` | TIMESTAMP    | 自动更新               | 更新时间                        |

#### surveys 表

| 字段               | 类型            | 约束                   | 说明          |
|------------------|---------------|----------------------|-------------|
| `id`             | BIGINT        | PK, AUTO_INCREMENT   | 问卷 ID       |
| `share_id`       | VARCHAR(32)   | UNIQUE, NOT NULL     | 分享 ID       |
| `title`          | VARCHAR(200)  | NOT NULL             | 标题          |
| `description`    | VARCHAR(2000) | NULLABLE             | 描述          |
| `user_id`        | BIGINT        | FK → users, NOT NULL | 创建者         |
| `status`         | VARCHAR(20)   | NOT NULL, 默认 DRAFT   | 状态枚举        |
| `access_level`   | VARCHAR(20)   | NOT NULL, 默认 PUBLIC  | 访问权限        |
| `anonymous`      | BOOLEAN       | 默认 true              | 是否匿名        |
| `template`       | BOOLEAN       | 默认 false             | 是否为模板       |
| `start_time`     | TIMESTAMP     | NULLABLE             | 开始时间        |
| `end_time`       | TIMESTAMP     | NULLABLE             | 截止时间        |
| `logo_url`       | VARCHAR(500)  | NULLABLE             | 问卷 Logo URL |
| `background_url` | VARCHAR(500)  | NULLABLE             | 问卷背景图片 URL  |
| `response_count` | INT           | 默认 0                 | 回复数量        |
| `max_responses`  | INT           | NULLABLE             | 最大回复数量（配额）  |
| `created_at`     | TIMESTAMP     | 自动生成                 | 创建时间        |
| `updated_at`     | TIMESTAMP     | 自动更新                 | 更新时间        |

#### survey_sections 表

| 字段           | 类型           | 约束                     | 说明    |
|--------------|--------------|------------------------|-------|
| `id`         | BIGINT       | PK, AUTO_INCREMENT     | 分组 ID |
| `survey_id`  | BIGINT       | FK → surveys, NOT NULL | 所属问卷  |
| `title`      | VARCHAR(200) | NULLABLE               | 分组标题  |
| `sort_order` | INT          | 默认 0                   | 排序顺序  |

#### questions 表

| 字段            | 类型            | 约束                             | 说明            |
|---------------|---------------|--------------------------------|---------------|
| `id`          | BIGINT        | PK, AUTO_INCREMENT             | 题目 ID         |
| `survey_id`   | BIGINT        | FK → surveys, NOT NULL         | 所属问卷          |
| `section_id`  | BIGINT        | FK → survey_sections, NULLABLE | 所属分组（分组模式时非空） |
| `type`        | VARCHAR(30)   | NOT NULL                       | 题型枚举          |
| `title`       | VARCHAR(500)  | NOT NULL                       | 题目标题          |
| `description` | VARCHAR(1000) | NULLABLE                       | 题目描述          |
| `required`    | BOOLEAN       | 默认 false                       | 是否必填          |
| `sort_order`  | INT           | 默认 0                           | 排序顺序          |

#### question_options 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 选项 ID |
| `question_id` | BIGINT | FK → questions, NOT NULL | 所属题目 |
| `content` | VARCHAR(500) | NOT NULL | 选项内容 |
| `sort_order` | INT | 默认 0 | 排序顺序 |

#### survey_responses 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 回复 ID |
| `survey_id` | BIGINT | FK → surveys, NOT NULL | 所属问卷 |
| `user_id` | BIGINT | FK → users, NULLABLE | 提交用户 |
| `ip` | VARCHAR(45) | NULLABLE | 提交者 IP |
| `user_agent` | VARCHAR(500) | NULLABLE | 浏览器 UA |
| `created_at` | TIMESTAMP | 自动生成 | 提交时间 |

#### answers 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 答案 ID |
| `response_id` | BIGINT | FK → survey_responses, NOT NULL | 所属回复 |
| `question_id` | BIGINT | FK → questions, NOT NULL | 所属题目 |
| `text_value` | VARCHAR(5000) | NULLABLE | 文本答案 |
| `option_id` | BIGINT | FK → question_options, NULLABLE | 单选-选中选项 |
| `selected_option_ids` | VARCHAR(2000) | NULLABLE | 多选-选项 ID 列表（逗号分隔） |

#### vote_polls 表

| 字段                     | 类型            | 约束                   | 说明          |
|------------------------|---------------|----------------------|-------------|
| `id`                   | BIGINT        | PK, AUTO_INCREMENT   | 投票 ID       |
| `share_id`             | VARCHAR(32)   | UNIQUE, NOT NULL     | 分享 ID       |
| `title`                | VARCHAR(200)  | NOT NULL             | 标题          |
| `description`          | VARCHAR(2000) | NULLABLE             | 描述          |
| `user_id`              | BIGINT        | FK → users, NOT NULL | 创建者         |
| `vote_type`            | VARCHAR(20)   | NOT NULL, 默认 SINGLE  | 投票类型        |
| `frequency`            | VARCHAR(20)   | NOT NULL, 默认 ONCE    | 投票频率        |
| `status`               | VARCHAR(20)   | NOT NULL, 默认 DRAFT   | 状态          |
| `access_level`         | VARCHAR(20)   | NOT NULL, 默认 PUBLIC  | 访问权限        |
| `anonymous`            | BOOLEAN       | 默认 true              | 是否匿名        |
| `max_total_votes`      | INT           | NULLABLE             | 每人最多总票数     |
| `max_options`          | INT           | NULLABLE             | 最多可选项数      |
| `max_votes_per_option` | INT           | NULLABLE             | 每项最多投票数     |
| `start_time`           | TIMESTAMP     | NULLABLE             | 开始时间        |
| `end_time`             | TIMESTAMP     | NULLABLE             | 截止时间        |
| `logo_url`             | VARCHAR(500)  | NULLABLE             | 投票 Logo URL |
| `background_url`       | VARCHAR(500)  | NULLABLE             | 投票背景图片 URL  |
| `total_vote_count`     | INT           | 默认 0                 | 总票数         |
| `created_at`           | TIMESTAMP     | 自动生成                 | 创建时间        |
| `updated_at`           | TIMESTAMP     | 自动更新                 | 更新时间        |

#### vote_options 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 选项 ID |
| `poll_id` | BIGINT | FK → vote_polls, NOT NULL | 所属投票 |
| `title` | VARCHAR(200) | NOT NULL | 选项标题 |
| `content` | VARCHAR(500) | NULLABLE | 选项描述 |
| `image_url` | VARCHAR(1000) | NULLABLE | 图片 URL |
| `vote_count` | INT | 默认 0 | 得票数 |
| `sort_order` | INT | 默认 0 | 排序顺序 |

#### vote_records 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 记录 ID |
| `poll_id` | BIGINT | FK → vote_polls, NOT NULL | 所属投票 |
| `option_id` | BIGINT | FK → vote_options, NOT NULL | 选中选项 |
| `user_id` | BIGINT | FK → users, NULLABLE | 投票用户 |
| `ip` | VARCHAR(45) | NULLABLE | 投票者 IP |
| `user_agent` | VARCHAR(500) | NULLABLE | 浏览器 UA |
| `device_id` | VARCHAR(200) | NULLABLE | 设备 ID |
| `created_at` | TIMESTAMP | 自动生成 | 投票时间 |

---

## 6. 接口规格

### 6.1 统一响应格式

所有 API 返回统一的 JSON 格式：

```json
{
  "success": true,
  "message": "success",
  "data": { ... }
}
```

错误响应：

```json
{
  "success": false,
  "message": "错误消息",
  "data": null
}
```

校验失败响应：

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "fieldName": "校验错误描述"
  }
}
```

**全局异常处理**：

| 异常类型 | HTTP 状态码 | 说明 |
|----------|-------------|------|
| `BusinessException` | 自定义（默认 400） | 业务异常 |
| `ResourceNotFoundException` | 404 | 资源不存在 |
| `BadCredentialsException` | 401 | 用户名或密码错误 |
| `MethodArgumentNotValidException` | 400 | 参数校验失败 |
| `Exception`（兜底） | 500 | 服务器内部错误 |

### 6.2 用户认证接口

#### POST /api/auth/register

注册新用户。

**Request Body**：
```json
{
  "username": "testuser",
  "password": "123456",
  "email": "test@example.com",
  "nickname": "测试用户"
}
```

**Response**：
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJI...",
    "username": "testuser",
    "nickname": "测试用户",
    "role": "USER"
  }
}
```

#### POST /api/auth/login

用户登录。

**Request Body**：
```json
{
  "username": "testuser",
  "password": "123456"
}
```

**Response**：同注册响应格式。

#### GET /api/auth/profile

获取当前用户信息。需要 Authorization 头。

**Response**：
```json
{
  "success": true,
  "message": "success",
  "data": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "email": "test@example.com",
    "avatar": null,
    "role": "USER",
    "createdAt": "2026-02-27T07:00:00Z"
  }
}
```

#### PUT /api/auth/profile

更新个人信息。需要 Authorization 头。

**Request Body**（所有字段可选）：
```json
{
  "nickname": "新昵称",
  "email": "new@example.com",
  "avatar": "https://example.com/avatar.png",
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

### 6.3 问卷调查接口

#### POST /api/surveys

创建问卷。需要 Authorization 头。

**Request Body**：
```json
{
  "title": "用户满意度调查",
  "description": "请花几分钟填写此问卷",
  "accessLevel": "PUBLIC",
  "anonymous": true,
  "questions": [
    {
      "type": "SINGLE_CHOICE",
      "title": "您的性别",
      "required": true,
      "options": [
        { "content": "男" },
        { "content": "女" }
      ]
    },
    {
      "type": "RATING",
      "title": "请为我们的服务评分",
      "required": true
    }
  ]
}
```

#### PUT /api/surveys/{id}

更新问卷。需要 Authorization 头。Request Body 同创建。

#### GET /api/surveys/{id}

获取问卷详情（仅创建者）。

#### GET /api/surveys/s/{shareId}

通过分享 ID 获取问卷（公开，必须 PUBLISHED 且未过期）。返回数据包含 `sections`（分组列表，每组含 `questions`）和 `questions`
（层平题目，兼容旧格式）。

#### GET /api/surveys/s/{shareId}/my-response

获取当前登录用户对该问卷的最近一次回复记录（含答案详情）。未提交则返回 `null`。需要 Authorization 头。

#### GET /api/surveys/my?keyword=xxx&page=0&size=10&sort=createdAt,desc

获取我的问卷列表。支持关键字搜索和分页。

#### GET /api/surveys/public?page=0&size=10

获取公开问卷列表。

#### GET /api/surveys/templates?page=0&size=10

获取问卷模板列表。

#### POST /api/surveys/{id}/publish

发布问卷。

#### POST /api/surveys/{id}/close

关闭问卷。

#### DELETE /api/surveys/{id}

删除问卷。

#### POST /api/surveys/s/{shareId}/submit

提交问卷回复。

**Request Body**：
```json
{
  "answers": [
    { "questionId": 1, "selectedOptionId": 3 },
    { "questionId": 2, "textValue": "5" },
    { "questionId": 3, "selectedOptionIds": [5, 6] },
    { "questionId": 4, "textValue": "非常满意" }
  ]
}
```

#### GET /api/surveys/{id}/responses?page=0&size=10

获取问卷回复列表（仅创建者）。

#### GET /api/surveys/{id}/stats

获取问卷统计分析（仅创建者）。

**Response**：
```json
{
  "success": true,
  "data": {
    "surveyId": 1,
    "title": "用户满意度调查",
    "totalResponses": 100,
    "questionStats": [
      {
        "questionId": 1,
        "questionTitle": "您的性别",
        "questionType": "SINGLE_CHOICE",
        "optionStats": [
          { "optionId": 1, "content": "男", "count": 55, "percentage": 55.0 },
          { "optionId": 2, "content": "女", "count": 45, "percentage": 45.0 }
        ],
        "textAnswers": null
      },
      {
        "questionId": 2,
        "questionTitle": "其他建议",
        "questionType": "TEXTAREA",
        "optionStats": null,
        "textAnswers": ["很好", "希望增加更多功能", "..."]
      }
    ]
  }
}
```

#### GET /api/surveys/{id}/export

导出问卷回复为 Excel 文件（仅创建者）。返回 `.xlsx` 二进制流。

### 6.4 投票系统接口

#### POST /api/votes

创建投票。需要 Authorization 头。

**Request Body**：
```json
{
  "title": "最喜欢的编程语言",
  "description": "请选择你最喜欢的编程语言",
  "voteType": "MULTIPLE",
  "frequency": "ONCE",
  "accessLevel": "PUBLIC",
  "anonymous": true,
  "maxOptions": 3,
  "options": [
    { "title": "Java", "content": "面向对象语言", "imageUrl": "https://example.com/java.png" },
    { "title": "Python" },
    { "title": "JavaScript" },
    { "title": "Go" }
  ]
}
```

#### PUT /api/votes/{id}

更新投票。需要 Authorization 头。Request Body 同创建。

#### GET /api/votes/{id}

获取投票详情（仅创建者）。

#### GET /api/votes/v/{shareId}

通过分享 ID 获取投票（公开，必须 PUBLISHED）。返回 `hasVoted` 标识。

#### GET /api/votes/my?page=0&size=10

获取我的投票列表。

#### GET /api/votes/public?page=0&size=10

获取公开投票列表。

#### POST /api/votes/{id}/publish

发布投票。

#### POST /api/votes/{id}/close

关闭投票。

#### DELETE /api/votes/{id}

删除投票。

#### POST /api/votes/v/{shareId}/submit

提交投票。

**单选/多选投票 Request Body**：
```json
{
  "optionIds": [1, 3],
  "deviceId": "browser-uuid-xxxxx"
}
```

**计分投票 Request Body**：
```json
{
  "votes": {
    "1": 3,
    "2": 5,
    "3": 2
  },
  "deviceId": "browser-uuid-xxxxx"
}
```

**投票响应（VotePollDto）**：
```json
{
  "success": true,
  "data": {
    "id": 1,
    "shareId": "abc123def456",
    "title": "最喜欢的编程语言",
    "voteType": "MULTIPLE",
    "frequency": "ONCE",
    "status": "PUBLISHED",
    "totalVoteCount": 150,
    "hasVoted": true,
    "options": [
      { "id": 1, "title": "Java", "imageUrl": "...", "voteCount": 45, "percentage": 30.0 },
      { "id": 2, "title": "Python", "voteCount": 60, "percentage": 40.0 },
      { "id": 3, "title": "JavaScript", "voteCount": 30, "percentage": 20.0 },
      { "id": 4, "title": "Go", "voteCount": 15, "percentage": 10.0 }
    ]
  }
}
```

### 6.5 文件管理接口

#### POST /api/files/upload

上传文件。需要 Authorization 头。`multipart/form-data`，字段名为 `file`。

**Response**：
```json
{
  "success": true,
  "message": "File uploaded",
  "data": {
    "url": "/api/files/a1b2c3d4-e5f6-7890-abcd-ef1234567890.png",
    "name": "original-filename.png"
  }
}
```

#### GET /api/files/{fileName}

下载文件。返回二进制流，Content-Type 为 `application/octet-stream`。

### 6.6 WebSocket 接口

| 配置 | 值 |
|------|-----|
| **端点** | `/ws` |
| **协议** | STOMP |
| **回退** | SockJS |
| **订阅频道** | `/topic/vote/{shareId}` |
| **消息格式** | JSON（VotePollDto） |

前端连接示例：
```typescript
const client = new Client({
  brokerURL: `ws://${location.host}/ws`,
  reconnectDelay: 5000,
  onConnect: () => {
    client.subscribe(`/topic/vote/${shareId}`, (msg) => {
      const pollData = JSON.parse(msg.body)
      // 更新投票结果
    })
  }
})
client.activate()
```

---

## 6.7 构建脚本

项目提供一键构建脚本，将前端产物打包并复制到后端 `src/main/resources/static/` 目录，通过 Spring Boot 直接响应静态文件，无需单独部署
Nginx：

| 脚本          | 系统            | 说明                  |
|-------------|---------------|---------------------|
| `build.bat` | Windows       | 构建前端 + 复制到后端 static |
| `build.sh`  | Linux / macOS | 同上                  |

**构建步骤**：

1. 在 `frontend/` 目录执行 `npm run build`
2. 清空后端 `src/main/resources/static/` 目录
3. 将 `frontend/dist/` 内容复制到上述目录
4. 后端直接通过 Spring Boot 静态资源服务响应前端页面

---

## 7. 前端页面需求

### 7.1 页面清单

| #  | 路由                       | 页面   | 认证 | 说明                                 |
|----|--------------------------|------|----|------------------------------------|
| 1  | `/`                      | 首页   | 否  | 欢迎页，展示创建问卷/投票入口，支持中文/英文多语言切换       |
| 2  | `/login`                 | 登录   | 游客 | 用户名 + 密码表单                         |
| 3  | `/register`              | 注册   | 游客 | 用户名 + 密码 + 邮箱 + 昵称表单               |
| 4  | `/profile`               | 个人信息 | 是  | 编辑昵称、邮箱、头像、密码                      |
| 5  | `/surveys`               | 我的问卷 | 是  | 问卷列表，支持搜索、分页、操作（编辑/发布/关闭/删除/统计/回复） |
| 6  | `/surveys/create`        | 创建问卷 | 是  | 问卷编辑器，拖拽排序题目，12 种题型                |
| 7  | `/surveys/:id/edit`      | 编辑问卷 | 是  | 同创建，加载已有数据                         |
| 8  | `/surveys/:id/stats`     | 统计分析 | 是  | 各题目选择比例进度条 + 文本答案列表 + 导出 Excel     |
| 9  | `/surveys/:id/responses` | 回复列表 | 是  | 分页展示回复详情                           |
| 10 | `/surveys/public`        | 公开问卷 | 否  | 公开问卷列表                             |
| 11 | `/s/:shareId`            | 填写问卷 | 否  | 问卷填写表单 + 提交成功页                     |
| 12 | `/votes`                 | 我的投票 | 是  | 投票列表                               |
| 13 | `/votes/create`          | 创建投票 | 是  | 投票编辑器，拖拽排序选项                       |
| 14 | `/votes/:id/edit`        | 编辑投票 | 是  | 同创建，加载已有数据                         |
| 15 | `/votes/public`          | 公开投票 | 否  | 公开投票列表                             |
| 16 | `/v/:shareId`            | 投票页面 | 否  | 投票表单 + 实时结果（WebSocket）+ 图片全屏预览     |

### 7.2 布局

- **侧边栏**：可折叠（220px / 64px），包含导航菜单
  - 公共菜单：首页、公开问卷、公开投票
  - 登录后额外菜单：我的问卷、创建问卷、我的投票、创建投票
- **顶部导航栏**：面包屑 + 深色模式切换 + 语言切换 + 用户菜单（登录/注册 或 个人信息/退出）
- **内容区域**：`router-view` 渲染

### 7.3 前端交互细节

#### 问卷编辑器

- **分组管理**：支持添加/删除/拖拽排序分组，每个分组可设置标题
- 拖拽手柄图标（☰）排序分组和分组内题目
- 每个题目卡片：类型选择、标题输入、必填开关
- 单选/多选题额外显示选项列表（可增删）
- **高级设置**（默认折叠，点击展开）：
  - 多选题：最少/最多选项数设置（同一行显示）
  - 数字题：最小/最大值设置（同一行显示）
  - 所有题型：条件逻辑（跳题规则）设置
- 复制题目功能
- 保存时自动更新分组和题目的 `sortOrder`

#### 问卷填写

- **分步展示**：有分组时使用 `n-steps` 按分组分步展示，每步独立校验后才能前进；无分组时一页展示所有题目
- 根据题型渲染不同输入组件：
  - `SINGLE_CHOICE` → Radio Group
  - `MULTIPLE_CHOICE` → Checkbox Group
  - `TEXT` → Input
  - `TEXTAREA` → Textarea
  - `NUMBER` → InputNumber
  - `RATING` → Rate（5 星）
  - `DATE` → DatePicker
  - `EMAIL` / `URL` / `PHONE` / `ID_CARD` → Input（带 placeholder 提示）
  - `FILE` → Upload 组件 + 文件名展示
- 必填题显示红色 "必填" 标签
- 提交前前端校验必填项
- 已登录用户加载历史回复（`my-response`）自动回填答案，`allowUpdate=true` 时展示更新按钮
- 匿名问卷提交成功后写入 `localStorage` 标记防止重复提交
- 提交成功显示感谢页面

#### 投票页面

- **倒计时**：
    - 未到 `startTime`：隐藏选项内容，显示开始倒计时
    - 进行中（有 `endTime`）：显示结束倒计时
    - 已过 `endTime`：显示已结束提示，禁止投票
- **Logo/背景图**：若投票设置了 `logoUrl` 或 `backgroundUrl`，在页面顶部展示
- 根据投票类型渲染：
  - `SINGLE` → Radio Group
  - `MULTIPLE` → Checkbox Group（受 `maxOptions` 限制）
  - `SCORED` → InputNumber per option（受 `maxVotesPerOption` 和 `maxTotalVotes` 限制，显示剩余票数）
- 选项图片以大图方式显示（最大 100%×300px），鼠标悬停降低透明度
- 点击图片全屏预览（深色遮罩 + 居中展示 + 关闭按钮）
- 已投票状态显示信息提示
- 投票结果显示：每个选项的票数、百分比、彩色进度条、**排名**（第 N 名）
- WebSocket 连接：自动重连（5 秒），实时更新结果和排名

#### 投票编辑器
- 拖拽排序选项
- 每个选项：标题、描述、图片 URL + 图片预览（最大 200px）
- 点击预览图片可全屏查看
- **批量添加**：切换到批量模式，文本框每行输入一个选项（可含图片 URL），一键生成

#### 问卷填写（补充）

- **未到 `startTime`**：隐藏题目，显示开始倒计时
- **Logo/背景图**：若问卷设置了 `logoUrl` 或 `backgroundUrl`，在页面顶部展示
- **描述富文本**：问卷/投票描述使用 `v-html` 渲染，支持简单 HTML

#### 用户头像

- 个人信息页和导航栏展示用户头像
- 有邮箱时自动使用 Gravatar（`https://www.gravatar.com/avatar/{md5(email)}`）
- 无邮箱或头像时显示默认头像

---

## 8. 测试需求

### 8.1 单元测试

| 测试类 | 测试目标 |
|--------|----------|
| `AuthServiceTest` | 用户注册、登录、查询、更新逻辑 |
| `SurveyServiceTest` | 问卷 CRUD、提交、统计逻辑 |
| `VoteServiceTest` | 投票 CRUD、提交、限流逻辑 |
| `FileServiceTest` | 文件上传、路径生成逻辑 |
| `RateLimitServiceTest` | 限流和投票标记逻辑 |
| `ExcelExportServiceTest` | Excel 导出逻辑 |

### 8.2 集成测试

使用 H2 内存数据库 + Mock Redis（`TestRedisConfig`） + Mock WebSocket（`@MockBean SimpMessagingTemplate`），测试 Profile 为 `test`。

| 测试类 | 用例数 | 覆盖接口 |
|--------|--------|----------|
| `AuthControllerTest` | 11 | 注册（正常/用户名冲突/参数缺失）、登录（正常/密码错误/用户不存在）、获取信息、更新信息（昵称/邮箱/密码） |
| `VoteControllerTest` | 24 | 创建/更新/获取/发布/关闭/删除投票、提交投票（单选/多选/计分）、分享链接获取、公开列表、权限验证 |
| `SurveyControllerTest` | 26 | 创建/更新/获取/发布/关闭/删除问卷、提交回复、获取回复列表、统计分析、导出 Excel、模板列表、分享链接获取、权限验证 |
| `FileControllerTest` | 4 | 文件上传、上传鉴权（403）、文件下载、文件不存在 |
| **合计** | **65** | — |

**测试执行**：
```bash
# 运行所有测试
mvn test

# 运行集成测试
mvn test -Dtest="cn.har01d.survey.tests.AuthControllerTest,cn.har01d.survey.tests.VoteControllerTest,cn.har01d.survey.tests.SurveyControllerTest,cn.har01d.survey.tests.FileControllerTest"
```

**测试配置特殊说明**：
- `spring.main.allow-bean-definition-overriding: true`（允许 TestRedisConfig 覆盖默认 Redis Bean）
- 排除 `RedisAutoConfiguration` 和 `RedisRepositoriesAutoConfiguration`（避免连接真实 Redis）
- WebSocket 配置类添加 `@Profile("!test")` 注解（测试时不加载）
- 测试按 `@Order` 顺序执行，确保数据依赖正确
