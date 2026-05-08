# External Integrations

**Analysis Date:** 2026/05/07

## Integration Overview

The SCRM system integrates with multiple external services. The core integration is with WeChat Enterprise (企业微信), supplemented by cloud storage, message queue, job scheduling, and Redis.

---

## 1. WeChat Enterprise API (企业微信) — 核心集成

**SDK:** `weixin-java-cp` v4.4.0-RELEASE (binarywang/Weixin-Java-Tools)

**Configuration (`bootstrap.yml` / `ScrmConfig.java`):**
| Config Key | Purpose |
|---|---|
| `scrm.extCorpID` | 企业ID |
| `scrm.contactSecret` | 通讯录API Secret |
| `scrm.customerSecret` | 客户联系API Secret |
| `scrm.mainAgentID` | 自建主应用ID |
| `scrm.mainAgentSecret` | 自建主应用Secret |
| `scrm.msgAuditSecret` | 会话存档Secret |
| `scrm.callbackToken` / `scrm.callbackAesKey` | 回调加解密 |
| `scrm.priKeyPath` | 会话存档RSA私钥路径 |
| `scrm.domainName` | 业务域名 |

**集成模块:**
- **通讯录同步:** 部门/员工 CRUD，回调同步 (`WxCpConsts.ContactChangeType`)
- **客户联系:** 外部联系人管理、客户标签、在职/离职继承
- **客户群:** 群聊管理、群标签、群发消息
- **会话存档:** 企业微信会话内容存档 (`msgAuditSecret` + RSA 私钥)
- **企微应用宝:** 裂变活动、参与活动
- **回调事件处理:** 通过 `WxCpPortalController` 接收企业微信回调，事件分发给对应的 `AbstractHandler`

**服务端点:**
- `WxCpConfiguration.java` — 初始化 `WxCpService` 实例和消息路由器
- `WxCpTpConfiguration.java` — 第三方应用配置
- 回调路由: `/api/wxPortal` (推测自 `WxPortalController`)
- 事件线程池: `async.executor.thread.*` 配置

---

## 2. Tencent Cloud COS (对象存储)

**SDK:** `cos_api` v5.6.54 + `cos-sts_api` v3.1.0

**配置 (`tq.cos.*`):**
- Bucket, SecretId, SecretKey, Region (ap-guangzhou)
- QueueId: 用于 COS 事件队列

**用途:**
- 文件/图片上传存储 (`COSUtils.java`)
- 临时密钥生成 (`cos-sts_api` 用于前端直传)
- 前端 SDK: `cos-js-sdk-v5` v1.4.17 用于浏览器端直传

**集成位置:**
- `COSUtils.java` (497 lines) — 服务端 COS 操作封装
- `CommonUpload` 组件 — 前端上传组件
- 图片处理: `ImgInfoDTO`, `ImgRbgDTO` (图片信息/去底)

---

## 3. RabbitMQ — 消息队列

**SDK:** `spring-boot-starter-amqp` (Spring AMQP)

**配置:**
- Host: 独立部署 (bootstrap.yml 中配置)
- 重试策略: 最多5次，指数退避 (multiplier=2, initial=2s, max=10s)
- 队列定义: `RabbitMqQueueConfig.java` — 22 个持久化队列

**队列列表及用途:**

| 队列常量 | 用途 |
|---|---|
| `WX_CHANGE_TYPE_CREATE` | 新增群聊事件 |
| `DISMISS` | 解散群聊事件 |
| `WX_UPDATE_DETAIL` | 修改群信息事件 |
| `CHANGE_AUTH` | 修改授权事件 |
| `WX_UPDATE_DETAIL_ADD_MEMBER` | 成员入群事件 |
| `WX_UPDATE_DETAIL_DEL_MEMBER` | 成员退群事件 |
| `CREATE_USER` | 新增员工事件 |
| `UPDATE_USER` | 修改员工事件 |
| `DELETE_USER` | 删除员工事件 |
| `CREATE_PARTY` | 创建部门事件 |
| `UPDATE_PARTY` | 修改部门事件 |
| `DELETE_PARTY` | 删除部门事件 |
| `ADD_EXTERNAL_CONTACT` | 添加客户事件 |
| `ADD_HALF_EXTERNAL_CONTACT` | 免验证添加客户事件 |
| `DEL_EXTERNAL_CONTACT` | 员工删除客户事件 |
| `DEL_FOLLOW_USER` | 客户删除员工事件 |
| `WX_UPDATE_EDIT_EXTERNAL_CONTACT` | 修改客户事件 |
| `WX_STAFF_CUSTOMER_TRANSFER_FAIL` | 客户接替失败事件 |
| `WX_TAG_CREATE` | 标签新增事件 |
| `WX_TAG_UPDATE` | 标签修改事件 |
| `WX_TAG_DELETE` | 标签删除事件 |
| `WX_TAG_SHUFFLE` | 标签重排事件 |

所有队列均为持久化队列 (`durable=true`)，确保消息不丢失。这些队列用于解耦企业微信回调事件的异步处理。

---

## 4. Redis / Redisson

**SDK:** `redisson` v3.11.1 + `jedis` v2.9.0

**配置:**
- 连接: host, port, password
- 用途:
  - 分布式锁: `shedlock-provider-redis-spring` v4.20.0 (定时任务防重)
  - 缓存: 企业微信 access_token、ticket 存储 (`WxCpMemoryConfigStorage`)
  - 会话管理: 用户登录 token
  - 分布式协调: Redisson 提供分布式对象

---

## 5. XXL-Job — 分布式任务调度

**SDK:** `xxl-job-core` v2.3.0

**配置 (`xxl.job.*`):**
- Admin 地址: `http://127.0.0.1:9100/xxl-job-admin`
- 执行器: `scrm-job-executor`, 端口 9999
- 日志路径: `/usr/local/scrm/xxl-job/jobhandler`
- 日志保留: 30 天

**用途:**
- 定时同步通讯录
- 定时同步客户数据
- 数据统计报表生成
- 过期数据清理
- 控制器: `XxlJobController.java`

**分布式锁保障:** `shedlock` + Redis 确保多节点部署时任务不重复执行。

---

## 6. WeChat Open Platform (微信开放平台)

**Feign Client:** `MpAuthFeign.java`

**配置:**
- `scrm.mpAppId` — 微信开放平台 AppId
- `scrm.mpAppSecret` — 微信开放平台 AppSecret
- 授权流程: 预授权码 → 授权 → 获取 authorizer 信息
- DTO 定义: `feign/dto/` 包下包含完整授权流程的数据模型
  - `GetAccessTokenRes`, `PreAuthCodeRes`, `QueryAuthRes`
  - `GetAuthorizerInfoRes`, `ListActivedAccountRes`

**集成模块:**
- `MpAuthController.java` — 处理微信公众号授权回调
- `MpAuthServiceImpl.java` — 授权逻辑实现

---

## 7. 企业微信第三方应用 (服务商模式)

**Feign Client:** `CpTpFeign.java`

**DTO:**
- `ComponentTokenParams/Res` — 第三方平台 component_access_token
- `TpIdTranslateParams/Res` — ID 转换 (openId ↔ unionId)
- `GetAuthInfoParams/Res` — 授权信息查询
- `TpUploadFileRes` — 文件上传

---

## 8. Frontend Integrations

### COS Browser Direct Upload
- SDK: `cos-js-sdk-v5` v1.4.17
- 通过 `CosFileTempSecretDTO` 获取临时凭证后直传
- 实现位置: `CommonUpload` 组件系列

### WeChat JS-SDK
- H5 页面调用微信 JS-SDK (通过 `JsSignatureDTO` 获取签名)
- 实现: `JsSignatureServiceImpl.java`

### WeChat Login
- 企业微信 OAuth 登录流程
- `LoginController.java` — 登录回调处理
- 前端: `loginRedirectUrl` → `/scrm-wx-cp/api/staff-login/login-callback`

---

## 9. Build & Deployment Integrations

**Docker:**
- `docker-compose` 支持 (包含 snail-job)
- `Dockerfile` 存在于子模块中
- 未使用 Kubernetes

**DB Migration:**
- 手动 SQL 文件 (`scrm-server/sql/`)
- 无 Flyway/Liquibase

---

## 10. Mars Admin 额外集成

Mars Admin (Vue 3 后台) 有自己独立的集成层:

- **MinIO:** `minio` v8.5.2 — S3 兼容对象存储
- **Aliyun OSS:** `aliyun-sdk-oss` v3.17.4 — 阿里云对象存储
- **Sa-Token:** v1.38.0 — RBAC 认证框架，支持 Redis 会话
- **JustAuth:** v1.16.6 — 第三方登录集成

---

*Integration analysis: 2026/05/07*
