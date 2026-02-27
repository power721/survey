# Survey API 系统集成测试

独立的 Maven 工程，使用 **JUnit 5 + REST Assured** 对 Survey 应用的 REST API 进行系统级集成测试。

## 技术栈

- Java 21
- JUnit 5
- REST Assured 5.5
- Jackson（JSON 序列化）

## 前置条件

- Survey 应用已启动并可访问（默认 `http://localhost:8080`）
- Redis 服务已启动（投票防重复需要）

## 运行测试

### 默认（localhost:8080）

```bash
mvn test
```

### 指定目标地址

```bash
mvn test -Dbase.url=http://192.168.1.100:8080
```

### 运行单个测试类

```bash
mvn test -Dtest=AuthApiTest
mvn test -Dtest=SurveyApiTest
mvn test -Dtest=VoteApiTest
mvn test -Dtest=FileApiTest
```

## 测试用例

| 测试类 | 用例数 | 覆盖功能 |
|--------|--------|----------|
| `AuthApiTest` | 12 | 注册（正常/重复/缺参数/短密码）、登录（正常/错误密码/不存在用户）、获取信息（正常/无Token）、更新信息（昵称/邮箱/改密码/错误旧密码） |
| `SurveyApiTest` | 20 | 创建（正常/无权限/缺标题）、获取（ID/ShareId/404/权限）、更新（正常/权限）、发布、提交（正常/空/关闭后）、回复列表（正常/权限）、统计、导出Excel、我的问卷（搜索）、公开问卷、模板、关闭、删除（权限/正常/删后404） |
| `VoteApiTest` | 27 | 创建（单选/多选/计分/无权限）、获取（ID/权限/ShareId草稿）、更新（正常/权限）、发布（3种/权限）、提交单选（正常/多选项/空）、提交多选（正常/超限）、提交计分（正常/超限）、列表（我的/公开）、关闭（正常/权限）、删除（权限/正常/删后404/清理） |
| `FileApiTest` | 6 | 上传（正常/无权限/空文件）、下载（正常/404/无需认证） |
| **合计** | **65** | — |

## 注意事项

- 每次运行会创建新的测试用户（UUID 随机用户名），不会互相干扰
- 测试完成后会清理创建的投票数据
- 测试按 `@Order` 顺序执行，保证数据依赖正确
