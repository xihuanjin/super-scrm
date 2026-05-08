# 蓬勃来客 SCRM 系统设计文档

**版本:** 1.0-SNAPSHOT
**日期:** 2026/05/07
**项目代号:** scrm-wx-cp (企业微信SCRM)

---

## 目录

1. [项目概述](#1-项目概述)
2. [系统架构设计](#2-系统架构设计)
3. [业务领域设计](#3-业务领域设计)
4. [API接口设计](#4-api接口设计)
5. [数据模型设计](#5-数据模型设计)
6. [认证与安全](#6-认证与安全)
7. [集成服务设计](#7-集成服务设计)
8. [前端架构设计](#8-前端架构设计)
9. [部署架构](#9-部署架构)
10. [开发指南](#10-开发指南)

---

## 1. 项目概述

### 1.1 项目定位

蓬勃来客（Bluelris SCRM）是面向企业微信生态的**社交客户关系管理系统**。系统帮助企业通过企业微信管理客户资源、实现精准营销、跟踪销售全流程、沉淀客户数据。

### 1.2 核心能力

- **客户管理**: 企业微信客户同步、客户画像、客户标签、流失预警
- **客户群管理**: 群聊管理、群标签、群发消息、群统计分析
- **销售管理**: 商机管理、订单管理、产品管理、销售目标
- **营销获客**: 渠道活码、客户群发、欢迎语、客户旅程
- **内容中心**: 素材管理、话术库、轨迹素材（感知素材）
- **企业管理**: 员工管理、角色权限、管理员配置

### 1.3 技术栈概览

| 层级 | 技术 | 版本 |
|---|---|---|
| 后端框架 | Spring Boot + Spring Cloud | 2.3.2 / Hoxton.SR9 |
| ORM | MyBatis-Plus | 3.4.3.4 |
| 数据库 | MySQL 8.0 + Druid连接池 | 1.2.5 |
| 缓存 | Redis + Redisson | 3.11.1 |
| 消息队列 | RabbitMQ | Spring AMQP |
| 任务调度 | XXL-Job | 2.3.0 |
| 对象存储 | 腾讯云COS | 5.6.54 |
| 企微SDK | weixin-java-cp (binarywang) | 4.4.0 |
| 前端框架 | React 17 + Ant Design 4 | 17.0.2 / 4.19.0 |
| 状态管理 | MobX 4 | 4.15.7 |
| 构建工具 | Webpack 4 (CRA) | 4.44.2 |
| 新管理后台 | Vue 3 + Vite + Ant Design Vue | 3.5.13 / 6.1.0 |

---

## 2. 系统架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      前端层 (Frontend)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ React SPA    │  │ Vue3 Admin   │  │ UniApp 移动端 │      │
│  │ (主应用)      │  │ (管理后台)    │  │ (H5页面)      │      │
│  │ Webpack 4    │  │ Vite 6       │  │              │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                 │                  │               │
│         └────────────┬────┘──────────────────┘               │
│                      │ HTTP/REST + token header              │
└──────────────────────┼──────────────────────────────────────┘
                       │
┌──────────────────────┼──────────────────────────────────────┐
│                   API网关层 (Nginx/统一入口)                   │
│              context-path: /api (port 9900)                   │
└──────────────────────┼──────────────────────────────────────┘
                       │
┌──────────────────────┼──────────────────────────────────────┐
│                 Spring Boot 应用层 (scrm-server)              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Interceptor (认证) → Controller → Service → Mapper  │   │
│  │       │                    │                         │   │
│  │  JWT验证              @Log AOP                      │   │
│  │  PassToken跳过        操作日志记录                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │ WxCp     │ │ Feign    │ │ RabbitMQ │ │ XXL-Job  │      │
│  │ 企微配置  │ │ 远程调用  │ │ 事件队列  │ │ 定时任务  │      │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘      │
└───────┼────────────┼────────────┼────────────┼─────────────┘
        │            │            │            │
┌───────┼────────────┼────────────┼────────────┼─────────────┐
│    基础设施层                                                  │
│  ┌───────┐  ┌───────┐  ┌───────┐  ┌───────┐  ┌───────┐   │
│  │ MySQL │  │ Redis │  │  COS  │  │ 企业微信│  │RabbitMQ│   │
│  │ 8.0   │  │       │  │ 云存储 │  │ API    │  │       │   │
│  └───────┘  └───────┘  └───────┘  └───────┘  └───────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 分层架构

```
scrm-server/
├── api/                    # API契约层 (被server和外部引用)
│   └── com.scrm.api.wx.cp
│       ├── client/        # FeignClient接口定义 (服务间调用契约)
│       ├── dto/           # 数据传输对象 (前端↔后端)
│       ├── entity/        # 共享实体类
│       └── vo/            # 视图对象 (展示用)
│
├── scrm-common/            # 公共基础设施层
│   └── com.scrm.common
│       ├── config/        # 通用配置 (ScrmConfig, MybatisPlus, Redis, Swagger)
│       ├── constant/      # 常量 (R响应体, ResultCode, Constants)
│       ├── exception/     # 异常体系 (BaseException, GlobalExceptionHandler)
│       ├── util/          # 工具类 (JWT, AES, BCrypt, 文件, 树形)
│       ├── log/           # 操作日志 (@Log注解+AOP)
│       ├── filter/        # XSS过滤器
│       └── dto/           # 通用DTO (BasePageDTO分页基类)
│
└── server/                 # 业务应用层
    └── com.scrm.server.wx.cp
        ├── web/           # Controller层 (50+控制器)
        ├── service/       # Service接口 + impl实现
        ├── mapper/        # MyBatis Mapper接口 + XML映射
        ├── config/        # 服务配置 (WxCp, RabbitMQ队列, XXL-Job, Feign)
        ├── handler/       # 企微回调事件处理器
        ├── jobhandler/    # XXL-Job定时任务处理器
        ├── feign/         # 外部Feign客户端 (MpAuth, CpTp)
        ├── filter/        # 认证拦截器
        └── dto/           # 服务内部DTO
```

### 2.3 请求处理流程

```
HTTP Request
    │
    ▼
┌──────────────────┐
│ AuthenticationInterceptor │  ← 从Header取token, JWT验证
│ (排除: /api-docs,       │     @PassToken注解可跳过
│  /swagger-ui/, /druid/) │     失败返回401
└────────┬─────────┘
         │ 通过
         ▼
┌──────────────────┐
│ XssFilter        │  ← 请求参数XSS过滤
│ (可配置路径排除)   │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Controller       │  ← @Valid参数校验, 调用Service
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Service          │  ← 业务逻辑, @Transactional事务
│      │           │     @Log AOP记录操作日志
│      ├── Mapper  │  ← MyBatis-Plus CRUD
│      ├── Redis   │  ← 分布式锁/缓存
│      ├── Feign   │  ← 远程服务调用
│      └── RabbitMQ│  ← 异步事件发布
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ R<T> 统一响应     │  ← { code, success, data, msg, traceId }
└──────────────────┘
```

### 2.4 模块依赖关系

```
server ──────────────► api
  │                     │
  ├──► scrm-common ◄────┘
  │
  ├──► weixin-java-cp (企业微信SDK)
  ├──► mybatis-plus (ORM)
  ├──► spring-cloud-openfeign (远程调用)
  ├──► spring-boot-starter-amqp (RabbitMQ)
  ├──► redisson (分布式锁)
  └──► xxl-job (定时任务)
```

依赖方向: `server → api → scrm-common`, `server → scrm-common`。无循环依赖。

---

## 3. 业务领域设计

### 3.1 领域全景图

系统包含 **6大业务域**、**25+功能模块**:

```
蓬勃来客 SCRM
├── 1. 客户管理域
│   ├── 客户管理      (客户列表、客户详情、客户标签、客户画像)
│   ├── 客户群管理    (群列表、群详情、群成员、群标签)
│   ├── 客户群标签    (标签分组管理)
│   ├── 客户旅程      (客户生命周期阶段管理)
│   ├── 在职转接      (员工在职期间的客户交接)
│   ├── 离职继承      (员工离职后的客户/群分配)
│   └── 流失记录      (流失客户/删员工历史)
│
├── 2. 销售管理域
│   ├── 商机管理      (商机创建、阶段推进、协作人)
│   ├── 商机配置      (商机分组、阶段定义)
│   ├── 订单管理      (订单录入、审批、产品关联)
│   ├── 产品管理      (产品CRUD、产品分类)
│   ├── 产品分类      (分类树形管理)
│   └── 销售目标      (目标设定、达成统计)
│
├── 3. 营销获客域
│   ├── 渠道活码      (员工活码、群活码、数据统计)
│   ├── 客户群发      (批量消息、定时发送、发送统计)
│   ├── 客户群群发    (群发到群、发送统计)
│   ├── 欢迎语设置    (好友欢迎语、入群欢迎语)
│   └── 企微应用宝    (裂变活动、H5活动页面)
│
├── 4. 内容中心域
│   ├── 普通素材      (图片/视频/文件素材)
│   ├── 轨迹素材      (H5文章、追踪客户浏览行为)
│   ├── 话术库        (销售话术分类管理)
│   └── 素材标签      (素材标签分组)
│
├── 5. 数据总览域
│   ├── 首页V2        (关键指标卡片、图表)
│   └── 报表统计      (客户统计、群统计、素材统计、拉新统计)
│
└── 6. 企业管理域
    ├── 员工管理      (员工同步、角色分配)
    ├── 管理员列表    (超级管理员配置)
    ├── 角色管理      (角色CRUD、权限配置)
    └── 系统配置      (开关配置、功能开关)
```

### 3.2 核心业务流程

#### 3.2.1 企业微信数据同步流程

```
┌───────────┐     ┌───────────────┐     ┌───────────────┐
│ XXL-Job   │────►│ SyncService   │────►│ 企业微信API    │
│ 定时触发   │     │ (定时/手动)    │     │ weixin-java-cp │
└───────────┘     └───────┬───────┘     └───────┬───────┘
                          │                      │
                          │ 获取增量数据           │ 返回客户/员工/群
                          ▼                      ▼
                   ┌───────────────┐     ┌───────────────┐
                   │ Redisson分布式锁│     │ 数据写入MySQL  │
                   │ (防并发冲突)    │     │ + Redis缓存    │
                   └───────────────┘     └───────────────┘
```

**关键点:**
- 同步使用Redisson分布式锁 (`customerSyncLock:{extCorpId}:{shard}`) 防止并发
- 分片锁设计: 客户按ID哈希分多片，支持并行同步
- 同步失败通过 `BrAsyncErrorInfo` 记录异步错误信息

#### 3.2.2 企业微信回调事件处理流程

```
企业微信回调
    │
    ▼
┌──────────────────────┐
│ WxPortalController   │  ← GET验证URL, POST接收事件
│ /api/portal/{corpId} │
└──────┬───────────────┘
       │ XML解密(AES)
       ▼
┌──────────────────────┐
│ WxCpMessageRouter    │  ← 消息路由器
│ (根据事件类型分发)     │
└──────┬───────────────┘
       │
       ├── change_external_contact ──► AbstractHandler (客户变更)
       ├── change_external_chat    ──► AbstractHandler (群聊变更)
       ├── change_external_tag     ──► AbstractHandler (标签变更)
       └── change_contact          ──► AbstractHandler (通讯录变更)
              │
              ▼
       ┌──────────────┐
       │ RabbitMQ队列  │  ← 异步处理，22个持久化队列
       │ (解耦+削峰)   │
       └──────┬───────┘
              │
              ▼
       ┌──────────────┐
       │ 业务处理       │  ← 更新数据库、触发通知
       └──────────────┘
```

**22个RabbitMQ队列对应的事件:**

| 域 | 事件 | 队列 |
|---|---|---|
| 通讯录 | 新增/修改/删除员工 | `create_user`, `update_user`, `delete_user` |
| 通讯录 | 新增/修改/删除部门 | `create_party`, `update_party`, `delete_party` |
| 客户 | 添加/修改/删除客户 | `add_external_contact`, `edit_external_contact`, `del_external_contact` |
| 客户 | 接替失败 | `transfer_fail` |
| 客户 | 免验证添加 | `add_half_external_contact` |
| 群聊 | 新增/解散/修改群聊 | `create`, `dismiss`, `change_detail` |
| 群聊 | 成员入群/退群 | `add_member`, `del_member` |
| 标签 | 新增/修改/删除/重排 | `change_external_tag_{create,update,delete,shuffle}` |
| 授权 | 修改授权 | `change_auth` |

#### 3.2.3 登录认证流程

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│ 前端页面  │────►│ LoginController│────►│ 企业微信OAuth │
│          │     │ /staff-login  │     │ 扫码授权      │
└──────────┘     └──────┬───────┘     └──────┬───────┘
                        │                      │
                        │ 1. POST /login       │ 返回构造的OAuth URL
                        │    (获取登录链接)      │ (含redirect_uri)
                        ▼                      │
                 ┌──────────────┐              │
                 │ 用户扫码授权   │◄─────────────┘
                 └──────┬───────┘
                        │
                        │ 2. GET /login-callback?code=&state=
                        │    (企业微信回调)
                        ▼
                 ┌──────────────┐
                 │ 换取用户信息   │  ← getStaffByCode(code)
                 │ 生成JWT Token │  ← JwtUtil.createToken(staffId)
                 └──────┬───────┘
                        │
                        │ 3. Redirect到前端, 携带token
                        ▼
                 ┌──────────────┐
                 │ 前端存储Token  │  ← localStorage.setItem(TOKEN_KEY, token)
                 │ 跳转首页       │
                 └──────────────┘
```

**Token存储:** `localStorage` → Header: `token` → `AuthenticationInterceptor` 验证

#### 3.2.4 客户群发流程

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│ 创建群发  │────►│ 选择目标客户   │────►│ 编辑消息内容   │
│          │     │ (标签筛选/人)  │     │ (文本+附件)    │
└──────────┘     └──────────────┘     └──────┬───────┘
                                             │
              ┌──────────────────────────────┘
              ▼
       ┌──────────────┐
       │ XXL-Job调度   │  ← 创建定时任务: sendMsgHandler
       │ 到时间触发     │
       └──────┬───────┘
              │
              ▼
       ┌──────────────┐
       │ 遍历目标员工   │
       │ 调用企业微信API │  ← weixin-java-cp 发送消息
       │ 记录发送结果    │
       └──────┬───────┘
              │
              ▼
       ┌──────────────┐
       │ 统计展示       │  ← 发送成功/失败数
       └──────────────┘
```

### 3.3 关键业务实体关系

```
BrCorpAccredit (企业授权)
    │ 1:N
    ├──► Staff (员工)
    │       │ 1:N
    │       ├──► WxCustomer (客户 - 通过员工关联)
    │       │       │ N:N
    │       │       └──► WxTag (标签)
    │       │       │ 1:N
    │       │       └──► WxCustomerInfo (客户扩展信息)
    │       │
    │       └──► WxGroupChat (客户群 - 群主)
    │               │ 1:N
    │               ├──► WxGroupChatMember (群成员)
    │               └──► WxGroupChatTag (群标签)
    │
    ├──► BrOpportunity (商机)
    │       │ 1:N
    │       ├──► BrOpportunityCooperator (协作者)
    │       └──► BrCustomerFollow (跟进记录)
    │               │ 1:N
    │               └──► BrCustomerFollowReply (跟进回复)
    │
    ├──► BrOrder (订单)
    │       │ 1:N
    │       ├──► BrOrderProduct (订单产品)
    │       └──► BrOrderAttachment (订单附件)
    │
    ├──► BrProductInfo (产品)
    │       │ 1:1
    │       └──► BrProductType (产品分类)
    │
    ├──► ContactWay (渠道活码)
    │       │ 1:N
    │       ├──► ContactWayStaff (活码关联员工)
    │       └──► ContactWayGroup (活码分组)
    │
    ├──► MediaInfo (素材)
    │       │ N:N
    │       └──► MediaTag (素材标签)
    │
    └──► WxMsgTemplate (群发模板)
            │ 1:N
            └──► WxMsgTemplateDetail (发送明细)
```

---

## 4. API接口设计

### 4.1 接口规范

**基础URL:** `http://{host}:9900/api`

**统一响应格式:**
```json
{
  "code": 200,           // 状态码, 200=成功, 其他=失败
  "success": true,       // 是否成功
  "data": {},            // 响应数据 (泛型T)
  "msg": "操作成功",      // 提示消息
  "traceId": "abc123"    // 链路追踪ID (Spring Cloud Sleuth)
}
```

**状态码定义 (`ResultCode`):**
| 状态码 | 含义 |
|---|---|
| 200 | 操作成功 |
| 500 | 操作失败 |
| 111401 | 未安装应用 |
| 111405 | 不在可见范围/无席位 |
| 401 | 认证失败 (HTTP Status) |

**请求规范:**
- Content-Type: `application/json; charset=UTF-8`
- 认证: Header `token: {jwt_token}`
- 分页参数: 统一使用 POST body 传参
- 校验: 使用 `javax.validation` 注解 + `@Valid`

### 4.2 接口总览

系统共有 **50+ Controller**，按业务域分类如下：

#### 4.2.1 认证登录接口 (`/api/staff-login`)

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| POST | `/staff-login/login` | 获取企业微信扫码登录链接 | @PassToken |
| GET | `/staff-login/login-callback` | 扫码登录回调(企业微信→系统) | @PassToken |
| GET | `/staff-login/getStaffByState` | 根据state获取登录员工信息 | @PassToken |
| GET | `/staff-login/getStaffByCode` | 根据code获取员工信息 | @PassToken |
| GET | `/staff-login/getCurrentStaff` | 获取当前登录员工信息 | 需要token |
| GET | `/staff-login/loginByAuthCode` | 根据授权码登录 | @PassToken |
| GET | `/staff-login/getLoginUrl` | 获取登录URL | @PassToken |
| GET | `/staff-login/v2/getStaffByCode` | V2版根据code获取员工(支持web登录) | @PassToken |

#### 4.2.2 客户管理接口 (`/api/wxCustomer`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/wxCustomer/sync` | 同步企业微信客户数据(分布式锁) |
| POST | `/wxCustomer/pageList` | 分页查询客户列表 |
| POST | `/wxCustomer/pageAssistList` | 分页查询跟进客户列表 |
| POST | `/wxCustomer` | 新增客户 |
| PUT | `/wxCustomer` | 修改客户 |
| GET | `/wxCustomer/{id}` | 获取客户详情 |
| DELETE | `/wxCustomer/{id}` | 删除客户 |
| POST | `/wxCustomer/batchMarking` | 批量打标签 |
| POST | `/wxCustomer/statistics` | 客户统计数据 |
| POST | `/wxCustomer/todayStatistics` | 今日客户统计 |
| POST | `/wxCustomer/pullNewStatistics` | 拉新统计 |
| POST | `/wxCustomer/export` | 导出客户列表 |
| POST | `/wxCustomer/loss/pageList` | 分页查询流失客户 |

#### 4.2.3 客户群管理接口 (`/api/wxGroupChat`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/wxGroupChat/sync` | 同步企业微信群聊数据 |
| POST | `/wxGroupChat/pageList` | 分页查询群聊列表 |
| POST | `/wxGroupChat` | 新增群聊 |
| PUT | `/wxGroupChat` | 修改群聊 |
| GET | `/wxGroupChat/{id}` | 获取群聊详情 |
| POST | `/wxGroupChat/statistics` | 群聊统计数据 |
| POST | `/wxGroupChat/member/pageList` | 分页查询群成员 |
| POST | `/wxGroupChat/tag/batchMarking` | 批量群打标签 |

#### 4.2.4 员工管理接口 (`/api/staff`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/staff/sync` | 同步企业微信员工 |
| POST | `/staff/pageList` | 分页查询员工 |
| GET | `/staff/{id}` | 获取员工详情 |
| POST | `/staff` | 新增员工 |
| PUT | `/staff` | 修改员工 |

#### 4.2.5 渠道活码接口 (`/api/contactWay`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/contactWay/pageList` | 分页查询活码 |
| POST | `/contactWay` | 创建活码 |
| PUT | `/contactWay` | 修改活码 |
| GET | `/contactWay/{id}` | 获取活码详情 |
| DELETE | `/contactWay/{id}` | 删除活码 |
| GET | `/contactWay/count/date/export` | 导出按日统计 |
| GET | `/contactWay/count/staff/export` | 导出按员工统计 |

#### 4.2.6 素材管理接口 (`/api/mediaInfo`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/mediaInfo/pageList` | 分页查询素材 |
| POST | `/mediaInfo` | 新增素材 |
| PUT | `/mediaInfo` | 修改素材 |
| GET | `/mediaInfo/{id}` | 获取素材详情 |
| DELETE | `/mediaInfo/{id}` | 删除素材 |
| GET | `/mediaInfo/h5/{id}` | H5页面获取素材详情 |

#### 4.2.7 素材标签接口 (`/api/mediaTag` 和 `/api/mediaTagGroup`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/mediaTag/pageList` | 分页查询素材标签 |
| POST | `/mediaTag` | 新增标签 |
| PUT | `/mediaTag` | 修改标签 |
| DELETE | `/mediaTag/{id}` | 删除标签 |
| POST | `/mediaTagGroup/pageList` | 分页查询标签分组 |
| POST | `/mediaTagGroup` | 新增分组 |

#### 4.2.8 商机管理接口 (`/api/opportunity`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/opportunity/pageList` | 分页查询商机 |
| POST | `/opportunity` | 创建商机 |
| PUT | `/opportunity` | 修改商机 |
| GET | `/opportunity/{id}` | 获取商机详情 |
| DELETE | `/opportunity/{id}` | 删除商机 |
| POST | `/opportunity/cooperator/pageList` | 分页查询协作者 |
| POST | `/opportunity/group/pageList` | 分页查询商机分组 |

#### 4.2.9 订单管理接口 (`/api/order`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/order/pageList` | 分页查询订单 |
| POST | `/order` | 创建订单 |
| PUT | `/order` | 修改订单 |
| GET | `/order/{id}` | 获取订单详情 |
| GET | `/order/product/pageList` | 订单产品列表 |

#### 4.2.10 客户群发接口 (`/api/msgTemplate`)

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/msgTemplate/pageList` | 分页查询群发 |
| POST | `/msgTemplate` | 创建群发任务 |
| PUT | `/msgTemplate` | 修改群发任务 |
| GET | `/msgTemplate/{id}` | 获取群发详情 |
| POST | `/msgTemplate/customer/export` | 导出群发明细(按客户) |
| POST | `/msgTemplate/staff/export` | 导出群发明细(按员工) |

#### 4.2.11 其他主要接口

| 路径前缀 | 说明 |
|---|---|
| `/api/wxTag`, `/api/wxTagGroup` | 客户标签管理 |
| `/api/wxGroupChatTag`, `/api/wxGroupChatTagGroup` | 群标签管理 |
| `/api/wxDepartment` | 部门管理 |
| `/api/dynamicMedia` | 动态素材(轨迹素材) |
| `/api/friendWelcome` | 好友欢迎语 |
| `/api/groupChatWelcome` | 入群欢迎语 |
| `/api/journey`, `/api/journeyStage`, `/api/journeyStageCustomer` | 客户旅程 |
| `/api/todo` | 待办事项 |
| `/api/report`, `/api/reportEveryday` | 报表统计 |
| `/api/productInfo`, `/api/productType` | 产品管理 |
| `/api/saleTarget` | 销售目标 |
| `/api/followTask` | 跟进任务 |
| `/api/commonConf` | 通用配置 |
| `/api/sysSwitch` | 系统开关 |
| `/api/sysRole`, `/api/sysRoleStaff` | 角色权限管理 |
| `/api/mpAuth` | 微信公众号授权 |
| `/api/sysOperLog` | 操作日志查询 |
| `/api/resigned/transfer/customer` | 离职客户继承 |
| `/api/resigned/transfer/groupChat` | 离群客户群继承 |
| `/api/staffOnJobTransfer` | 在职转接 |
| `/api/xxlJob` | XXL-Job任务管理 |

### 4.3 分页请求/响应格式

**请求:**
```json
{
  "pageNum": 1,          // 页码 (从1开始)
  "pageSize": 20,        // 每页条数
  "extCorpId": "ww...",  // 企业ID (多企业必传)
  // ... 其他查询条件
}
```

**响应:**
```json
{
  "code": 200,
  "data": {
    "records": [ ... ],  // 数据列表
    "total": 150,        // 总记录数
    "size": 20,          // 每页条数
    "current": 1,        // 当前页码
    "pages": 8           // 总页数
  }
}
```

### 4.4 DTO命名约定

| 后缀 | 用途 | 示例 |
|---|---|---|
| `SaveDTO` | 新增请求体 | `WxCustomerSaveDTO` |
| `UpdateDTO` | 更新请求体 | `WxCustomerUpdateDTO` |
| `PageDTO` | 分页查询(继承BasePageDTO) | `WxCustomerPageDTO` |
| `QueryDTO` | 条件查询(不分页) | `WxCustomerQueryDTO` |
| `ExportDTO` | 导出参数 | `WxCustomerExportDTO` |
| `VO` | 视图对象(返回前端) | `WxCustomerVO` |

---

## 5. 数据模型设计

### 5.1 数据库概览

- **数据库名:** `scrm` (实际使用 `xxx-test`)
- **字符集:** UTF-8 (`utf8mb4`)
- **时区:** GMT+8
- **ORM:** MyBatis-Plus 3.4.3.4
- **主键策略:** 自定义ID生成 (String类型UUID)
- **软删除:** `deleted_at` 字段 (datetime, NULL=未删除)

### 5.2 核心表结构

#### 5.2.1 客户相关表

```
wx_customer (企业微信客户)
├── id (PK)
├── ext_corp_id       -- 企业ID (多企业隔离)
├── external_userid    -- 企业微信外部联系人ID
├── name               -- 客户名称
├── avatar             -- 头像URL
├── type               -- 客户类型 (1=微信用户, 2=企业微信用户)
├── gender             -- 性别
├── unionid            -- 微信unionId
├── created_at / updated_at
├── creator / editor    -- 创建人/编辑人
└── deleted_at          -- 软删除标记

wx_customer_info (客户扩展信息)
├── id (PK)
├── customer_id (FK → wx_customer)
├── staff_id (FK → staff)  -- 跟进员工
├── ext_corp_id
├── remark             -- 员工对客户的备注
├── description        -- 描述
├── add_way            -- 添加来源
├── status             -- 跟进状态
└── ...

wx_customer_staff (客户-员工关系)
├── customer_id (FK)
├── staff_id (FK)
├── ext_corp_id
├── remark             -- 备注
├── add_time           -- 添加时间
├── tags               -- 标签(JSON或关联表)
└── ...

wx_customer_staff_tag (客户标签关联)
├── customer_id
├── staff_id
├── tag_id
├── ext_corp_id
└── ...

wx_tag (客户标签)
├── id
├── group_id (FK → wx_tag_group)
├── name               -- 标签名
├── order              -- 排序
└── ...

wx_tag_group (标签分组)
├── id
├── name               -- 分组名
├── order              -- 排序
└── ...

wx_customer_loss_info (流失客户信息)
├── id
├── customer_id
├── staff_id
├── ext_corp_id
├── loss_type          -- 流失类型 (员工删客户/客户删员工)
├── loss_time          -- 流失时间
└── ...
```

#### 5.2.2 客户群相关表

```
wx_group_chat (企业微信客户群)
├── id
├── ext_corp_id
├── chat_id            -- 企业微信群ID
├── name               -- 群名
├── owner_id (FK → staff) -- 群主
├── notice             -- 群公告
├── status             -- 群状态
├── member_num         -- 成员数
├── created_at / updated_at
└── ...

wx_group_chat_member (群成员)
├── id
├── group_chat_id (FK)
├── user_id            -- 成员userId
├── type               -- 成员类型 (1=员工, 2=客户)
├── join_time          -- 入群时间
├── invitor_id         -- 邀请人
└── ...

wx_group_chat_tag / wx_group_chat_tag_group (群标签体系)
```

#### 5.2.3 销售相关表

```
br_opportunity (商机)
├── id
├── group_id (FK → br_opportunity_group)
├── name               -- 商机名称
├── desp               -- 描述
├── priority           -- 优先级
├── ext_corp_id
├── created_at / updated_at
├── creator / editor
└── deleted_at

br_opportunity_cooperator (商机协作者)
├── id
├── opportunity_id (FK)
├── staff_id (FK)
└── ...

br_opportunity_group (商机分组)
├── id
├── name
├── stages (JSON)      -- 阶段定义(JSON)
└── ...

br_customer_follow (客户跟进)
├── id
├── opportunity_id (FK)
├── customer_id (FK)
├── staff_id (FK)
├── content            -- 跟进内容
├── follow_type        -- 跟进方式
├── next_follow_time   -- 下次跟进时间
├── created_at
└── ...

br_customer_follow_reply (跟进回复)
├── id
├── follow_id (FK)
├── replier_id         -- 回复人
├── content            -- 回复内容
└── ...

br_order (订单)
├── id
├── customer_id (FK)
├── staff_id (FK)
├── ext_corp_id
├── order_no           -- 订单号
├── total_amount       -- 订单金额
├── status             -- 状态 (待审核/已通过/已驳回)
├── created_at
└── ...

br_order_product (订单产品关联)
├── order_id (FK)
├── product_id (FK)
├── quantity           -- 数量
├── price              -- 单价
└── ...

br_product_info (产品)
├── id
├── type_id (FK → br_product_type)
├── name               -- 产品名
├── price              -- 价格
├── description        -- 描述
└── ...

br_product_type (产品分类, 树形结构)
├── id
├── parent_id          -- 父分类ID (树形)
├── name
└── ...

br_sale_target (销售目标)
├── id
├── staff_id / department_id  -- 目标对象
├── target_amount      -- 目标金额
├── period_type        -- 周期 (月/季/年)
├── period_value       -- 周期值 (2022-01)
└── ...
```

#### 5.2.4 素材相关表

```
media_info (素材)
├── id
├── ext_corp_id
├── type               -- 素材类型 (图片/视频/文件/文章)
├── name               -- 素材名称
├── url                -- 素材URL (COS地址)
├── size               -- 文件大小
├── cover_url          -- 封面图
├── tags               -- 关联标签
├── status             -- 状态
├── created_at / updated_at
└── ...

media_tag / media_tag_group (素材标签体系)

wx_dynamic_media (轨迹素材/感知素材)
├── id
├── ext_corp_id
├── media_info_id (FK)
├── title              -- 文章标题
├── content            -- 文章内容(HTML)
├── cover_url          -- 封面
├── view_count         -- 浏览数
├── share_count        -- 分享数
└── ...

br_customer_dynamic (客户动态)
├── id
├── customer_id (FK)
├── staff_id (FK)
├── media_id (FK)      -- 关联素材
├── type               -- 动态类型 (查看素材/参与活动/完成阶段...)
├── created_at
└── ...
```

#### 5.2.5 系统管理相关表

```
sys_role (角色)
├── id
├── name               -- 角色名
├── role_key           -- 角色标识 (enterpriseAdmin)
├── permissions (JSON) -- 权限集合
└── ...

sys_role_staff (角色-员工关联)
├── role_id (FK)
├── staff_id (FK)
└── ...

sys_menu (菜单)
├── id
├── parent_id          -- 父菜单ID (树形)
├── name               -- 菜单名
├── url                -- 前端路由
├── type               -- 类型 (1=菜单, 2=按钮)
├── sort               -- 排序
└── ...

sys_oper_log (操作日志)
├── id
├── staff_id           -- 操作员工
├── model_name         -- 模块名 (@Log注解)
├── operator_type      -- 操作类型
├── request_method     -- 请求方法
├── request_url        -- 请求URL
├── request_param      -- 请求参数
├── cost_time          -- 耗时(ms)
├── created_at
└── ...

sys_switch (系统开关)
├── id
├── switch_key         -- 开关标识
├── switch_value       -- 开关值
└── ...

br_async_error_info (异步错误记录)
├── id
├── business_type      -- 业务类型
├── error_msg          -- 错误信息
├── params             -- 参数
├── created_at
└── ...

br_corp_accredit (企业授权信息)
├── id
├── ext_corp_id (unique)
├── corp_name          -- 企业名称
├── permanent_code     -- 永久授权码
├── auth_info (JSON)   -- 授权信息
├── status             -- 状态
└── ...
```

### 5.3 关键设计决策

1. **多企业隔离:** 所有业务表包含 `ext_corp_id` 字段，实现多企业数据隔离
2. **主键策略:** 使用 String UUID 而非自增ID，避免分布式ID冲突
3. **软删除:** 使用 MyBatis-Plus `@TableLogic` + `deleted_at` 字段
4. **审计字段:** 统一使用 `created_at`, `updated_at`, `creator`, `editor`
5. **JSON字段:** 部分配置数据使用 JSON 类型存储 (如 `auth_info`, `permissions`)

---

## 6. 认证与安全

### 6.1 认证机制

```
┌──────────────────────────────────────────────┐
│                 认证架构                       │
│                                               │
│  登录: 企业微信OAuth2.0扫码 → JWT Token        │
│  传输: HTTP Header "token: {jwt}"              │
│  验证: AuthenticationInterceptor 拦截器         │
│  存储: localStorage (前端)                      │
│  跳过: @PassToken 注解                         │
│  白名单: /api-docs, /swagger-ui/, /druid/     │
└──────────────────────────────────────────────┘
```

**JWT验证流程 (`JwtUtil.verify`):**
1. 从Header提取 `token`
2. 验证签名和过期时间
3. 检查 `@PassToken` 注解 (登录接口使用)
4. 验证失败返回 HTTP 401

### 6.2 安全措施

| 措施 | 实现 |
|---|---|
| XSS过滤 | `XssFilter` + `XssHttpServletRequestWrapper`，可配置路径排除 |
| SQL注入防护 | Druid Wall Filter (`multi-statement-allow: true`) |
| 密码加密 | BCrypt (`BCryptUtil.java`) |
| 敏感数据加密 | AES (`AESUtil.java`) |
| 参数校验 | Hibernate Validator (`@Valid` + `Assert`) |
| 分布式锁 | Redisson (防止并发数据冲突) |
| Druid监控 | `/druid/*` 控制台 (需配置用户名密码) |

### 6.3 已知安全缺陷

- **无Spring Security**: 缺少 `@PreAuthorize` 细粒度权限控制
- **无CSRF保护**: 缺少跨站请求伪造防护
- **Token无刷新机制**: Token过期后无自动续期
- **密钥硬编码**: `bootstrap.yml` 中含生产密钥（**需立即外迁**）
- **CORS未限制**: 缺少 `@CrossOrigin` 或 Nginx CORS配置

---

## 7. 集成服务设计

### 7.1 企业微信集成

**SDK:** `weixin-java-cp` 4.4.0 (binarywang)

**初始化流程:**
1. `ScrmConfig` 加载企业微信配置 (corpId, secrets)
2. `WxCpConfiguration` 创建 `WxCpService` 实例 (每个企业一个)
3. 配置 `WxCpMessageRouter` 消息路由规则
4. 注册各类事件处理器 (`AbstractHandler` 子类)

**API调用范围:**
- 通讯录管理: 部门/员工 CRUD
- 客户联系: 外部联系人管理、标签、在职/离职继承
- 客户群管理: 群聊CRUD、群发消息
- 会话存档: 消息拉取、解密
- 第三方应用: 授权流程、代开发应用

### 7.2 腾讯云COS

**用途:** 文件存储 (素材图片、视频、文件、文章封面)
**架构:** 前端直传模式
```
前端 → GET /api/common/getCosTempSecret → 服务端生成STS临时密钥
前端 → PUT (直传COS with STS Token)
前端 → POST /api/mediaInfo → 保存素材记录(含COS URL)
```

### 7.3 RabbitMQ事件队列

用于企业微信回调事件的异步处理，22个持久化队列。详见 3.2.2 节。

### 7.4 XXL-Job任务调度

**调度的任务类型:**
| Handler名称 | 用途 |
|---|---|
| `sendMsgHandler` | 执行客户群发/客户群群发 |
| `createJobHandler` | 创建定时群发任务 |
| `stopJobHandler` | 停止定时任务 |
| `followRemindHandler` | 客户跟进提醒 |
| `clueRemindHandler` | 线索提醒 |
| `fissionTaskExpireJobHandler` | 裂变活动过期处理 |
| `wxMsgTaskHandler` | 企微消息任务处理 |

---

## 8. 前端架构设计

### 8.1 React主应用 (scrm-web)

**路由设计 (`routes/config.js`):**

```
/                           → Layout (主布局)
├── /login                  → Login (扫码登录)
├── /login-middle           → LoginCallbackPage (登录回调中间页)
├── /home                   → HomeV2 (数据总览首页)
├── /customerManage         → CustomerManage (客户管理列表+标签)
├── /groupList              → CustomerGroup (客户群管理)
│   └── /detail/:id         → GroupDetail (群详情)
├── /groupTagManage         → GroupTagManage (群标签)
├── /customerMass           → CustomerMass (客户群发列表)
│   ├── /add                → AddOrEditPage (新建群发)
│   ├── /edit/:id           → AddOrEditPage (编辑群发)
│   └── /detail/:id         → DetailPage (群发详情)
├── /customerGroupMass      → GroupMass (客户群群发)
├── /lossingHistory         → LossingCustomer (流失记录)
├── /channelQrCode          → ChannelCode (渠道活码)
├── /customerJourney        → CustomerJourney (客户旅程)
├── /incumbencyTransfer     → IncumbencyTransfer (在职转接)
├── /dimissionInherit       → DimissionInherit (离职继承)
├── /welcomeSetting         → WelcomeSetting (欢迎语设置)
├── /talkScript             → TalkScript (话术库)
├── /userManage             → UserManage (员工管理)
├── /adminList              → AdminList (管理员列表)
├── /commercialOpportunity  → CommercialOpportunity (商机管理)
├── /commercialOppConfiguration → 商机配置
├── /saleTarget             → SaleTarget (销售目标)
├── /productCategory        → ProductCategory (产品分类)
├── /productList            → ProductList (产品管理)
├── /orderList              → OrderList (订单管理)
├── /saleOperation/
│   ├── /trackMaterial      → TrackMaterial (轨迹素材)
│   ├── /ordinaryMaterial   → OrdinaryMaterial (普通素材)
│   └── /materialTags       → MaterialTags (素材标签)
├── /no-menu                → NoMenu (无菜单提示)
├── /system-error           → SysError
├── /no-auth                → NotAuth
└── *                       → NotFound
```

**组件树结构:**
```
App
├── Layout (主布局)
│   ├── SideMenu (左侧菜单 - 从menu.json渲染)
│   ├── Header (顶部栏 - 用户头像/企业切换)
│   └── Content (内容区)
│       └── {PageComponent} (懒加载: @loadable/component)
│           ├── SearchForm (搜索表单)
│           ├── TableContent (表格列表)
│           │   └── Table (Ant Table封装)
│           ├── CommonDrawer (详情/编辑抽屉)
│           ├── CommonModal (通用弹窗)
│           ├── CommonUpload (文件上传)
│           └── Pagination
└── MobX Store (全局状态)
    ├── UserStore (用户信息、权限、企业信息)
    └── ...
```

**状态管理 (MobX):**
- `UserStore`: 用户信息 (`userData`)、extId、token
- 组件级状态: React Hooks (`useState`, `useEffect` + 自定义hooks)
- 自定义Hooks: `useTable`, `useModalHook`, `useUploadHook`, `useDebounce`

**API调用模式:**
```
页面组件 → services/modules/{module}.js → request.js (Axios) → /api/*
                                              │
                                              ├── 请求拦截: 注入token header
                                              └── 响应拦截: 统一错误处理
```

### 8.2 Vue3管理后台 (mars-admin-ui)

新的管理系统界面，使用更现代的技术栈:
- **Vue 3** Composition API + TypeScript
- **Pinia** 状态管理 (替代Vuex)
- **Vite 6** 构建 (替代Webpack)
- **UnoCSS** + Sass 样式方案
- **布局模式**: 垂直菜单、水平菜单、混合菜单等多种模式

---

## 9. 部署架构

### 9.1 部署拓扑

```
                     ┌──────────┐
                     │  Nginx   │  (反向代理 + SSL)
                     └────┬─────┘
                          │
            ┌─────────────┼─────────────┐
            │             │             │
      ┌─────▼─────┐ ┌────▼──────┐ ┌───▼──────┐
      │ Scrm服务   │ │ 静态资源   │ │ H5页面   │
      │ (port 9900)│ │ (React)   │ │ (UniApp) │
      └─────┬─────┘ └───────────┘ └──────────┘
            │
    ┌───────┼───────────┬──────────────┐
    │       │           │              │
┌───▼──┐ ┌──▼──┐  ┌────▼────┐  ┌─────▼─────┐
│MySQL │ │Redis│  │RabbitMQ │  │ XXL-Job   │
│8.0   │ │     │  │         │  │ Admin     │
└──────┘ └─────┘  └─────────┘  └───────────┘
```

### 9.2 环境配置

| 环境 | Profile | 数据库 | 说明 |
|---|---|---|---|
| 本地开发 | `local` (默认) | xxx-test | 开发调试 |
| 生产环境 | `pd` | scrm | 生产运行 |

### 9.3 Docker支持

项目包含 `Dockerfile` 和 `docker-compose.yml`:
- scrm-server: 基于Java 8镜像运行fat JAR
- snail-job: 使用Docker部署的异步任务重试组件

### 9.4 服务端口

| 服务 | 端口 |
|---|---|
| SCRM API服务 | 9900 |
| XXL-Job Executor | 9999 |
| XXL-Job Admin | 9100 |
| MySQL | 3306 |
| Redis | 32263 |
| RabbitMQ | 10808 (开发) / 5672 (生产) |

---

## 10. 开发指南

### 10.1 环境要求

| 工具 | 版本要求 |
|---|---|
| JDK | 1.8+ |
| Maven | 3.x |
| Node.js | 14+ (React) / 18.12+ (Vue Admin) |
| MySQL | 8.0 |
| Redis | 任意版本 |
| RabbitMQ | 3.8+ |
| IDE | IntelliJ IDEA (推荐) |

### 10.2 本地启动

**后端:**
```bash
# 1. 确保 MySQL, Redis, RabbitMQ 已启动
# 2. 修改 bootstrap.yml 中的数据库/Redis/RabbitMQ连接信息
# 3. 启动应用
cd scrm-server
mvn clean install -DskipTests
cd server
mvn spring-boot:run
# 服务启动在 http://localhost:9900/api
# Swagger文档: http://localhost:9900/api/swagger-ui/
# Druid监控: http://localhost:9900/api/druid/
```

**前端 (React):**
```bash
cd scrm-web
yarn install
yarn start
# 开发服务器启动在 http://localhost:3000
```

**前端 (Vue3 Admin):**
```bash
cd scrm-web/mars-admin/mars-admin-ui
pnpm install
pnpm dev
```

### 10.3 代码生成

项目使用 MyBatis-Plus Generator 生成代码:
```java
// CodeGeneratorUtil.java — 运行可生成 Entity, Mapper, Service, Controller
```

### 10.4 添加新业务模块步骤

1. **定义Entity** → `server/.../entity/XxxEntity.java` (对应数据库表)
2. **定义DTO** → `api/.../dto/` (XxxSaveDTO, XxxUpdateDTO, XxxPageDTO, XxxVO)
3. **定义Mapper** → `server/.../mapper/XxxMapper.java` + XML
4. **定义Service接口** → `server/.../service/IXxxService.java`
5. **实现Service** → `server/.../service/impl/XxxServiceImpl.java`
6. **实现Controller** → `server/.../web/XxxController.java`
7. **前端API服务** → `scrm-web/src/services/modules/xxx.js`
8. **前端页面** → `scrm-web/src/pages/Xxx/`
9. **添加路由** → `scrm-web/src/routes/config.js`
10. **添加菜单** → 数据库中 `sys_menu` 表或 `menu.json`

### 10.5 注意事项

- **多企业数据隔离:** 所有查询必须带 `extCorpId` 参数
- **分布式锁:** 数据同步操作必须使用 Redisson 锁 (`customerSyncLock:{corpId}:{shard}`)
- **企微API限流:** 调用企业微信API需注意频率限制，避免触发限流
- **操作日志:** 使用 `@Log(modelName, operatorType)` 注解自动记录
- **文件上传:** 小于10MB的文件使用 `CommonUpload` 组件直传COS
- **异步处理:** 耗时操作发布到 RabbitMQ 队列异步处理

### 10.6 Git分支策略

```
main (当前分支)
├── feature/*  (功能开发分支)
├── bugfix/*   (缺陷修复分支)
└── hotfix/*   (紧急修复分支)
```

当前 `main` 分支最近的提交主要是功能移除 (remove sidebar, doc, file-h5, product-h5)，属于功能精简阶段。

---

## 附录

### A. 项目文件统计

| 模块 | Java文件 | JS/TS文件 | Vue文件 | 配置文件 |
|---|---|---|---|---|
| scrm-server/server | ~400 | - | - | 2 (bootstrap.yml×2) |
| scrm-server/api | ~150 | - | - | 1 (pom.xml) |
| scrm-server/scrm-common | ~70 | - | - | 1 (pom.xml) |
| scrm-web (React) | - | ~300 | - | 1 (package.json) |
| mars-admin-ui (Vue) | - | ~60 | ~80 | 5 (vite/ts/uno) |
| **合计** | **~620** | **~360** | **~80** | **10+** |

### B. 关键配置文件清单

| 文件 | 用途 |
|---|---|
| `scrm-server/pom.xml` | Maven依赖管理 (Spring Boot, MyBatis-Plus, Feign, 企微SDK...) |
| `scrm-server/server/.../bootstrap.yml` | 运行时配置 (数据库, Redis, RabbitMQ, 企微密钥) |
| `scrm-server/server/.../bootstrap-template.yml` | 配置模板 (供参考) |
| `scrm-web/package.json` | 前端依赖+构建配置 (React, AntD, MobX, Jest) |
| `scrm-web/src/config.js` | 前端全局配置 (CORP_ID) |
| `scrm-web/src/routes/config.js` | 前端路由表 |
| `scrm-web/src/data/menu.json` | 菜单结构 |
| `scrm-web/mars-admin/mars-admin-ui/vite.config.ts` | Vue Admin构建配置 |

### C. 重要源码路径速查

| 类别 | 路径 |
|---|---|
| 企微配置 | `server/.../config/WxCpConfiguration.java` |
| 企微回调 | `server/.../web/WxPortalController.java` |
| 事件处理器 | `server/.../handler/` |
| RabbitMQ队列定义 | `server/.../config/RabbitMqQueueConfig.java` |
| 认证拦截器 | `server/.../filter/AuthenticationInterceptor.java` |
| 全局异常处理 | `scrm-common/.../exception/GlobalExceptionHandler.java` |
| 统一响应体 | `scrm-common/.../constant/R.java` |
| 系统配置 | `scrm-common/.../config/ScrmConfig.java` |
| Axios封装 | `scrm-web/src/services/request.js` |
| 用户Store | `scrm-web/src/store/User.js` |
| 文件上传组件 | `scrm-web/src/components/CommonUpload/` |
| 企业管理后台 | `scrm-web/mars-admin/mars-admin-ui/` |

---

*文档版本: 1.0 | 生成时间: 2026/05/07 | 基于代码库静态分析自动生成*
