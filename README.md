# 问卷调查与投票系统 / Survey & Vote System

## 技术栈 / Tech Stack

| Layer    | Technology                                      |
|----------|------------------------------------------------|
| Frontend | Vue 3 + Vite + TypeScript + naive-ui           |
| Backend  | Spring Boot 3.5 + Spring Security + Spring Data JPA |
| Database | H2 (dev) / MySQL / PostgreSQL                  |
| Cache    | Redis                                          |
| Auth     | JWT                                            |
| Deploy   | Docker + Nginx                                 |

## 功能 / Features

### 问卷调查 / Survey
- 创建问卷，支持多种题型：单选/多选/填空/文本/数字/评分/日期/邮箱/网址/手机号码/身份证ID
- 发布/关闭问卷
- 填写问卷（支持公开链接）
- 统计分析（比例展示）
- 导出 Excel
- 问卷模板

### 投票系统 / Vote
- 单选/多选投票
- 一次性投票或每天可投票
- 每个选项最大投票数量
- 总共最大投票数量
- 实时 WebSocket 结果推送
- 防重复投票（Redis + IP + UA + 设备ID）
- 截止时间控制

### 安全 / Security
- Spring Security + JWT 认证
- Redis 防刷（Bucket4j 限流）
- IP / 用户 / 设备 限制
- 服务端校验

### 其他 / Other
- 公开/私密访问权限
- 随机生成分享ID
- 匿名支持
- I18N：中文 + 英文
- 深色模式
- 手机端适配

## 本地开发 / Local Development

### 前置条件 / Prerequisites
- Java 21+
- Node.js 18+
- Redis (可选，本地开发可暂时不启动)
- Maven 3.9+

### 后端启动 / Start Backend
```bash
# 使用H2数据库（默认），无需额外数据库
mvn spring-boot:run
```
后端运行在 http://localhost:8080

### 前端启动 / Start Frontend
```bash
cd frontend
npm install
npm run dev
```
前端运行在 http://localhost:5173 （API请求自动代理到8080）

### 注意 / Notes
- 本地开发默认使用 H2 数据库，无需安装 MySQL/PostgreSQL
- Redis 用于防刷和投票限制，如果未启动 Redis，相关功能可能报错
- 首次使用需注册账号

## Docker 部署 / Docker Deployment

```bash
# 一键启动（包含 PostgreSQL + Redis + Nginx）
docker-compose up -d
```

访问 http://localhost

## API 文档 / API Endpoints

### Auth
- `POST /api/auth/register` - 注册
- `POST /api/auth/login` - 登录
- `GET /api/auth/profile` - 获取用户信息

### Survey
- `POST /api/surveys` - 创建问卷
- `PUT /api/surveys/{id}` - 更新问卷
- `GET /api/surveys/{id}` - 获取问卷详情
- `GET /api/surveys/my` - 我的问卷列表
- `GET /api/surveys/public` - 公开问卷列表
- `GET /api/surveys/templates` - 问卷模板
- `POST /api/surveys/{id}/publish` - 发布问卷
- `POST /api/surveys/{id}/close` - 关闭问卷
- `DELETE /api/surveys/{id}` - 删除问卷
- `GET /api/surveys/s/{shareId}` - 通过分享ID获取问卷
- `POST /api/surveys/s/{shareId}/submit` - 提交问卷
- `GET /api/surveys/{id}/responses` - 获取回复列表
- `GET /api/surveys/{id}/stats` - 统计分析
- `GET /api/surveys/{id}/export` - 导出Excel

### Vote
- `POST /api/votes` - 创建投票
- `PUT /api/votes/{id}` - 更新投票
- `GET /api/votes/{id}` - 获取投票详情
- `GET /api/votes/my` - 我的投票列表
- `GET /api/votes/public` - 公开投票列表
- `POST /api/votes/{id}/publish` - 发布投票
- `POST /api/votes/{id}/close` - 关闭投票
- `DELETE /api/votes/{id}` - 删除投票
- `GET /api/votes/v/{shareId}` - 通过分享ID获取投票
- `POST /api/votes/v/{shareId}/submit` - 提交投票

### WebSocket
- `/ws` - WebSocket 端点
- `/topic/vote/{shareId}` - 投票实时结果推送
