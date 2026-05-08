<!-- refreshed: 2026-05-07 -->
# Architecture

**Analysis Date:** 2026-05-07

## System Overview

```text
┌───────────────────────────────────────────────────────────────────────────┐
│                          前端展示层 (scrm-web)                              │
├──────────────────────────────┬────────────────────────────────────────────┤
│    React SPA (Ant Design)    │    Vue 3 Admin (mars-admin/mars-admin-ui)   │
│    `scrm-web/src/`           │    `scrm-web/mars-admin/mars-admin-ui/`     │
├──────────────┬───────────────┴────────────────┬───────────────────────────┤
│  MobX Store  │  Axios HTTP Client              │  COS JS SDK (文件上传)    │
│  (4 stores)  │  `services/modules/*.js`        │                          │
└──────┬───────┴──────────────┬──────────────────┴──────────┬────────────────┘
       │                      │                             │
       │     HTTP REST API    │                             │
       ▼                      ▼                             ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                        后端 API 层 (scrm-server)                           │
├───────────────────────────────────────────────────────────────────────────┤
│  Spring Boot 2.3.2 + Spring Cloud Hoxton.SR9 + Spring Cloud Alibaba 2.2.6 │
│  `scrm-server/server/src/main/java/com/scrm/server/wx/cp/web/`           │
│  Controller 层 (REST API 端点)                                            │
├───────────────────────────────────────────────────────────────────────────┤
│  Service 层                                                               │
│  `scrm-server/server/src/main/java/com/scrm/server/wx/cp/service/`       │
│  I{Name}Service 接口 + {Name}ServiceImpl 实现                             │
├───────────────────────────────────────────────────────────────────────────┤
│  Mapper 层 (MyBatis-Plus)                                                 │
│  `scrm-server/server/src/main/java/com/scrm/server/wx/cp/mapper/`        │
│  XML: `mapper/xml/`                                                       │
├───────────────────────────────────────────────────────────────────────────┤
│                        数据层                                              │
├────────────────────┬──────────────────────┬───────────────────────────────┤
│  MySQL             │  Redis               │  腾讯云 COS (文件存储)          │
│  `db/scrm.sql`     │  缓存/分布式锁        │                              │
│  Druid 连接池       │  Redisson + Jedis    │                              │
└────────────────────┴──────────────────────┴───────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│                      异步消息 & 外部集成                                    │
├──────────────────────┬──────────────────────┬────────────────────────────┤
│  RabbitMQ             │  XXL-Job             │  企业微信 (WeChat Work)    │
│  企微事件异步处理       │  分布式定时任务        │  weixin-java-cp SDK       │
│  `config/RabbitMq*`   │  调度中心+执行器       │  WxCpConfiguration        │
└──────────────────────┴──────────────────────┴────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| React SPA | 主应用界面，SCRM业务操作 | `scrm-web/src/App.js` |
| Vue 3 Admin | 后台管理系统 (部门/菜单/字典/配置管理) | `scrm-web/mars-admin/mars-admin-ui/` |
| `api` 模块 | DTO定义、Feign客户端接口、实体类 | `scrm-server/api/` |
| `scrm-common` 模块 | 共享工具、配置、异常处理、JWT、OSS | `scrm-server/scrm-common/` |
| `server` 模块 | Spring Boot主应用，Controller+Service+Mapper | `scrm-server/server/` |
| RabbitMQ | 企业微信回调事件异步处理 | `scrm-server/server/.../config/RabbitMqQueueConfig.java` |
| XXL-Job | 分布式定时任务 (数据同步、报表) | `scrm-server/server/.../config/XxlJobConfig.java` |

## Pattern Overview

**Overall:** 分层单体架构 (Layered Monolith) — 兼容未来微服务拆分

**Key Characteristics:**
- Maven多模块项目，按职责分模块（api / common / server）
- Controller-Service-Mapper 三层架构，使用 MyBatis-Plus 作为 ORM
- Feign 接口定义在 `api` 模块，目前 `@FeignClient` 注解被注释，实际走本地方法调用
- 统一响应体 `R<T>` 封装所有 API 返回
- Swagger/Knife4j 自动生成 API 文档
- 跨域已全局配置 (WebAppConfig 中 CorsFilter)
- 异步事件通过 RabbitMQ 解耦处理
- SaaS多租户: `ext_corp_id` 字段区分不同企业

## Layers

**Controller 层 (web):**
- Purpose: REST API 端点，接收请求，调用 Service，返回统一响应
- Location: `scrm-server/server/src/main/java/com/scrm/server/wx/cp/web/`
- Contains: 约 30+ Controller 类，按业务模块命名 (ContactWayController, BrOrderController 等)
- Depends on: Service 层接口
- Consumed by: React前端、Vue后台、企微侧边栏

**Service 层:**
- Purpose: 业务逻辑编排 + 事务管理
- Location: `scrm-server/server/src/main/java/com/scrm/server/wx/cp/service/` (接口) + `impl/` (实现)
- Contains: `I{Name}Service` 接口 + `{Name}ServiceImpl` 实现，继承 MyBatis-Plus `IService/ServiceImpl`
- Depends on: 其他 Service、Mapper、外部SDK (weixin-java-cp)
- Used by: Controller层、Feign Client实现

**Mapper 层:**
- Purpose: 数据库访问
- Location: `scrm-server/server/src/main/java/com/scrm/server/wx/cp/mapper/`
- Contains: MyBatis-Plus Mapper 接口 + XML 映射文件 (`mapper/xml/`)
- Pattern: 继承 `BaseMapper<T>`，无特殊需求无需手写SQL

**API 模块 (暴露给外部/Feign):**
- Location: `scrm-server/api/src/main/java/com/scrm/api/wx/cp/`
- Contains: DTO (请求/响应), Entity (实体类), Client (Feign接口), Enums, VO
- 依赖关系: scrm-common -> api 无依赖; server -> api -> scrm-common

## Data Flow

### Primary Request Path (前端请求)

1. React 组件触发 (如页面加载、按钮点击) (`scrm-web/src/pages/CustomerManage/*.js`)
2. 调用 services/modules 中的 API 函数 (`scrm-web/src/services/modules/customerManage.js`)
3. Axios 发送 HTTP POST/GET 到 `/api/{controller}/{action}` (`scrm-web/src/services/request.js`)
4. Nginx/网关 转发到 Spring Boot backend (context-path: `/api`)
5. Controller 接收请求，自动校验参数 (`@Valid`) (`scrm-server/server/.../web/ContactWayController.java`)
6. Service 执行业务逻辑，调用 Mapper 操作数据库 (`scrm-server/server/.../service/impl/StaffServiceImpl.java`)
7. 返回统一响应 `R<T>` 到前端

### 企业微信回调/事件处理流程

1. 企业微信推送事件 (客户添加、群聊变更、标签变更等) 到回调 URL
2. WxCpConfiguration 中的 WxCpMessageRouter 路由事件到对应 Handler
3. Handler 将事件消息发送到 RabbitMQ 队列 (参见 `RabbitMqQueueConfig.java`)
4. RabbitMQ 消费者异步处理事件 (更新数据库、同步数据等)
5. 异步执行结果记录到 `br_async_error_info` 表

### Feign 跨模块调用 (预留)

1. `api` 模块定义 Feign 接口 `IWxStaffClient` (`scrm-server/api/.../client/IWxStaffClient.java`)
2. `server` 模块实现接口 `WxStaffClient` 提供 REST 端点 (`scrm-server/server/.../client/WxStaffClient.java`)
3. Fallback 实现 `IWxStaffClientFallback` 提供降级逻辑
4. 当前 `@FeignClient` 注解被注释，跨模块调用走本地方法调用

**State Management:**
- 前端: MobX 全局 store (UserStore, MenuStore, WxWorkStore)，localStorage 持久化 token
- 后端: 无状态服务，JWT token 携带用户身份，Redis 缓存会话/配置

## Key Abstractions

**统一响应 R<T>:**
- Purpose: 所有 API 返回的标准包装
- Location: `scrm-server/scrm-common/src/main/java/com/scrm/common/constant/R.java`
- Pattern: 静态工厂方法 `R.data()`, `R.success()`, `R.fail()`

**DTO 命名规范:**
- 新增: `{Entity}SaveDTO`
- 修改: `{Entity}UpdateDTO`
- 分页查询: `{Entity}PageDTO`
- 无分页查询: `{Entity}QueryDTO`
- 导出: `{Entity}ExportDTO`
- VO: `{Entity}VO`
- 批量操作: `BatchDTO<T>`

**统一异常处理:**
- Location: `scrm-server/scrm-common/src/main/java/com/scrm/common/exception/`
- `GlobalExceptionHandler` 使用 `@ControllerAdvice` 拦截异常
- 自定义异常 `BaseException`
- 错误码枚举 `ErrorMsgEnum`, `CommonExceptionCode`, `WxErrorEnum`

## Entry Points

**Spring Boot 应用:**
- Location: `scrm-server/server/src/main/java/com/scrm/server/wx/cp/MaxScrmWxCpServerApplication.java`
- 配置: `@SpringBootApplication(scanBasePackages = "com.scrm.*")`
- 组件: `@EnableFeignClients`, `@EnableDiscoveryClient(autoRegister = false)`, `@EnableAsync`
- Mapper扫描: `@MapperScan("com.scrm.server.wx.cp.mapper")`

**React 应用:**
- Location: `scrm-web/src/App.js`
- 路由: React Router v6 `useRoutes()` 在 `routes/index.js` 中定义
- 入口路径: `/login`, `/login-middle`, `/` 下的业务路由

**Vue 3 管理后台:**
- Location: `scrm-web/mars-admin/mars-admin-ui/src/App.vue`
- 框架: Vue 3 + TypeScript + Vite + Ant Design Vue
- 页面: 部门管理、菜单管理、字典管理、配置管理、缓存管理等

## Architectural Constraints

- **Threading:** 单线程事件循环 (Netty) + 异步线程池 (async.executor.thread 配置，默认 core=10, max=100)
- **Global state:** `WxCpConfiguration` 中有 `cpServiceMap` 和 `routers` 静态Map存储 WeChat SDK 实例
- **SaaS多租户:** 所有业务表都有 `ext_corp_id` 字段区分企业
- **Java版本:** Java 8，限制了部分语言特性使用
- **事务管理:** Service 层使用 `@Transactional(rollbackFor = Exception.class)`
- **XSS防御:** `XssFilter` + `XssHttpServletRequestWrapper` 过滤请求参数

## Anti-Patterns

### 注释掉的 Feign 客户端

**What happens:** `IWxStaffClient` 接口上的 `@FeignClient` 注解被注释，预留的微服务间RPC能力未启用。server模块直接注入被`@RestController`标注的 `WxStaffClient` 实现类。
**Why it's wrong:** 使得`api`模块中的Feign接口定义失去意义，与实际调用方式不匹配。未来如果需要拆分为独立微服务，需要大量改造。
**Do this instead:** 统一使用 `api` 模块中的 Feign 接口访问，或移除 Feign 接口改用直接 Service 依赖。

### 静态方法持有配置

**What happens:** `ScrmConfig` 中大量配置项使用 `private static` + public static getter + 非static setter 的模式。
**Why it's wrong:** 这种模式容易在单元测试或并发场景下产生问题，不符合Spring Boot配置注入的最佳实践。
**Do this instead:** 使用 `@ConfigurationProperties` + 非static字段，注入时通过Spring管理，仅在需要时做静态适配。

### ScrmConfig 继承混乱

**What happens:** `WxCpConfiguration extends ScrmConfig`，文档注释说明 "继承ScrmConfig是为了让ScrmConfig先初始化"。
**Why it's wrong:** 通过继承来控制初始化顺序是设计上的代码异味(hack)，应使用 `@DependsOn` 或 `@PostConstruct` + `@AutoConfigureOrder`。
**Do this instead:** 使用 `@DependsOn("scrmConfig")` 或 `@AutoConfigureOrder` 注解控制初始化顺序。

## Error Handling

**Strategy:** 全局统一异常处理 + 业务异常 + 错误码枚举

**Patterns:**
- Controller 层方法统一包装在 `try` 中，抛出的异常由 `GlobalExceptionHandler` 处理
- Service 层通过 `Assert.isTrue()`/`Assert.notNull()` 校验参数
- 自定义异常 `BaseException` 携带错误码和消息
- 使用 `ErrorMsgEnum` 枚举定义错误信息
- 校验失败返回 `R.fail()` 格式

## Cross-Cutting Concerns

**Logging:** `@Log` 注解 + `LogAspect` 切面实现操作日志记录 (`scrm-common/log/`)，同时记录到 `SysOperLog` 表
**Validation:** JSR-303 Bean Validation (`@Valid` + `@NotNull/@Size` 等) 在 DTO 上声明，Controller 自动校验
**Authentication:** JWT token 验证，`@PassToken` 注解标记无需登录的接口，token 存储在 localStorage
**Authorization:** 角色管理 (`SysRole` + `SysRoleStaff`)，`StaffServiceImpl.isAdmin()` 判断管理员权限

---

*Architecture analysis: 2026-05-07*
