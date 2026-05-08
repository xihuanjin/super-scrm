# Code Conventions

**Analysis Date:** 2026/05/07

## Backend Conventions (Java / Spring Boot)

### Project Structure

```
scrm-server/
├── api/           # DTO + Feign Client 接口定义 (对外契约)
│   └── src/main/java/com/scrm/api/wx/cp/
│       ├── client/    # Feign 客户端接口 + Fallback
│       ├── dto/       # 数据传输对象 (~132 文件)
│       └── entity/    # 实体类
├── scrm-common/   # 公共工具、常量、异常、日志
│   └── src/main/java/com/scrm/common/
│       ├── constant/  # R(响应), AppConstant, Constants
│       ├── config/    # ScrmConfig 基类
│       └── log/       # 操作日志注解和客户端
└── server/        # 主应用 (Controller + Service + Mapper)
    └── src/main/java/com/scrm/server/wx/cp/
        ├── web/       # Controller 层 (~47 控制器)
        ├── service/   # Service 接口 + impl 实现
        ├── mapper/    # MyBatis Mapper + XML
        ├── config/    # 配置类 (WxCp, RabbitMQ, XXL-Job)
        ├── handler/   # 企微回调事件处理器
        ├── jobhandler/# XXL-Job 任务处理器
        └── feign/     # 外部 Feign 客户端 (MpAuth, CpTp)
```

### Naming Conventions

| 类型 | 模式 | 示例 |
|---|---|---|
| Controller | `Wx{Entity}Controller` | `WxStaffController`, `WxCustomerController` |
| Service 接口 | `I{Entity}Service` | `IStaffService`, `IWxCustomerService` |
| Service 实现 | `{Entity}ServiceImpl` | `StaffServiceImpl`, `WxCustomerServiceImpl` |
| DTO (查询) | `{Entity}PageDTO` | `StaffPageDTO`, `WxCustomerPageDTO` |
| DTO (新增) | `{Entity}SaveDTO` | `StaffSaveDTO`, `ContactWaySaveDTO` |
| DTO (更新) | `{Entity}UpdateDTO` | `StaffUpdateDTO`, `WxCustomerUpdateDTO` |
| DTO (导出) | `{Entity}ExportDTO` | `StaffExportDTO`, `WxCustomerExportDTO` |
| Feign Client | `I{Service}Client` + `{Service}ClientFallback` | `IWxStaffClient` + `IWxStaffClientFallback` |
| Mapper | `{Entity}Mapper` | (MyBatis-Plus BaseMapper) |
| Config | `{Feature}Configuration` 或 `{Feature}Config` | `WxCpConfiguration`, `XxlJobConfig` |

### Package Naming
- 根包: `com.scrm`
- API 模块: `com.scrm.api.wx.cp` (企业微信平台)
- 服务模块: `com.scrm.server.wx.cp`
- 公共模块: `com.scrm.common`
- 分组: `wx.cp` = WeChat Work (企业微信) platform

### DTO Patterns

每个业务实体通常有 5-7 个对应 DTO:
```
Entity:  WxCustomer
DTOs:    WxCustomerSaveDTO        (新增 — @Valid 校验注解)
         WxCustomerUpdateDTO      (更新)
         WxCustomerPageDTO        (分页查询 — 继承 BasePageDTO)
         WxCustomerExportDTO      (Excel 导出)
         WxCustomerQueryDTO       (条件查询)
         WxCustomerStatisticsDTO  (统计数据)
```

DTO 使用:
- `javax.validation.constraints` 进行参数校验
- 分页 DTO 继承基类 `BasePageDTO`
- 使用 Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)

### Controller Conventions

```java
@RestController
@RequestMapping("/{resource}")
@Api(tags = {"中文描述"})
public class WxXxxController {

    @Autowired
    private IXxxService xxxService;

    @GetMapping("/sync")
    @ApiOperation(value = "同步企业微信数据")
    @Log(modelName = "模块名", operatorType = "操作类型")
    public R<Boolean> sync(String extCorpId) { ... }

    @PostMapping("/pageList")
    @ApiOperation(value = "分页查询")
    public R<IPage<XxxVO>> pageList(@RequestBody @Valid XxxPageDTO dto) { ... }

    @GetMapping("/{id}")
    @ApiOperation(value = "根据主键查询")
    public R<Xxx> findById(@PathVariable String id) { ... }

    @PostMapping
    @ApiOperation(value = "新增")
    public R<Xxx> save(@RequestBody @Valid XxxSaveDTO dto) { ... }

    @PutMapping
    @ApiOperation(value = "修改")
    public R<Xxx> update(@RequestBody @Valid XxxUpdateDTO dto) { ... }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除")
    public R<Void> delete(@PathVariable String id) { ... }
}
```

**统一模式:**
- REST URL: `/api/{resource}` (context-path: `/api`)
- 统一响应: `R<T>` 包装 (code, msg, data)
- Swagger 文档: `@Api` + `@ApiOperation`
- 操作日志: `@Log` 自定义注解
- 参数校验: `@Valid` + `Assert.isTrue()`
- 无 `@PreAuthorize` 或 Spring Security 注解

### Service Layer Conventions

- 接口定义 (`I{Name}Service`) 在 `service/` 包
- 实现类 (`{Name}ServiceImpl`) 在 `service/impl/` 包
- 使用 `@Service` 注解
- 依赖注入: `@Autowired` 字段注入 (非构造器注入)
- MyBatis-Plus `BaseMapper` 进行 CRUD
- `IPage<T>` 分页查询
- 自定义 SQL 在 `mapper/xml/*.xml`

### Response Format

```java
// R.java — 统一响应体
R.data(data)           // 成功响应
R.fail(code, msg)      // 失败响应
R.ok()                 // 操作成功
```

### Error Handling

- `ResultCode.java` — 业务错误码枚举
- `ErrorMsgEnum.java` — 700+ 行企微错误码枚举
- `BaseException.java` — 基础异常类
- `GlobalExceptionHandler.java` — 全局异常处理 (@RestControllerAdvice)
- XSS 过滤器: 可配置的路径过滤

### Annotations Pattern

| 注解 | 用途 |
|---|---|
| `@RestController` | Controller 层 |
| `@Service` | Service 层 |
| `@Configuration` / `@Component` | 配置类 |
| `@FeignClient` | 远程调用客户端 |
| `@Log` | 自定义操作日志记录 |
| `@Data` + `@Builder` | Lombok 代码生成 |
| `@Api` / `@ApiOperation` | Swagger 文档 |

---

## Frontend Conventions (React SPA)

### Project Structure

```
scrm-web/src/
├── App.js              # 根组件
├── components/         # 通用组件 (~80 组件目录)
│   ├── CommonUpload/   # 上传组件 (含 hook, utils, 验证)
│   ├── CommonTransfer/ # 穿梭框组件 (含分页/远程加载)
│   ├── MySelect/       # 复杂选择器 (含 Modal 子组件)
│   ├── TagSelect/      # 标签选择器
│   ├── TableContent/   # 表格封装
│   ├── WeChatMsgEditor/# 微信消息编辑器
│   └── ...
├── services/           # API 服务层
│   ├── request.js      # Axios 实例 + 拦截器
│   ├── utils.js        # 请求工具函数
│   └── modules/        # 按业务模块分文件 (~25 模块)
├── store/              # MobX 状态管理
│   ├── index.js
│   └── User.js         # 用户状态 Store
├── hooks/              # 自定义 Hooks
│   ├── useTable.js
│   └── useModalHook.js
├── routes/             # 路由配置
│   ├── index.js
│   └── config.js
├── pages/              # 页面组件
├── layout/             # 布局组件
├── utils/              # 工具函数
├── data/               # 静态数据 (menu.json)
└── config.js           # 应用配置
```

### Component Pattern

每个组件目录使用 `index.js` 作为入口:
```
MySelect/
├── index.js            # 主组件导出
├── constants.js        # 常量定义
├── utils.js            # 工具函数
├── requestUrls.js      # API 路径配置
├── ModalContext.js     # Context 定义
└── components/         # 子组件
    ├── MySelectModal/
    ├── TableSide/
    └── ...
```

### API Calling Pattern

**`services/request.js`** — Axios 封装:
- 请求拦截器: 自动附加 `token` (从 `localStorage` 读取)
- 响应拦截器:
  - 200: 返回 `response.data`
  - 401: 登出
  - 500: 根据 code 处理 (未安装/不在可见范围/token 过期)
- 导出 `post(url, params, options)` 和 `get(url, params, options)`
- 支持 `needJson` (JSON body) 和 `needForm` (FormData) 模式

**`services/modules/{moduleName}.js`** — 按业务拆分 API:
```javascript
// customerManage.js
import { post, get } from '../request'
export const getCustomerList = (params) => post('/api/customer/pageList', params)
export const getCustomerDetail = (id) => get(`/api/customer/${id}`)
```

### State Management (MobX)

```javascript
// store/User.js
class UserStore {
    @observable userData = {}
    @action setUserData(data) { ... }
    logout() { ... }
}
```

使用 MobX 4.x 装饰器语法 (`@observable`, `@action`)。

### Routing Pattern

- React Router DOM v6.9.0
- 路由配置在 `routes/config.js`
- 布局包裹在 `layout/` 中
- 菜单数据在 `src/data/menu.json`

### Styling

- Less 预处理 (`less-loader` v7.0.2)
- Ant Design 主题变量覆盖
- 无 CSS Modules 统一使用 (部分可能使用)

### Custom Hooks

- `useTable.js` — 表格分页/查询状态管理
- `useModalHook.js` — 弹窗状态管理
- `useUploadHook.js` — 文件上传状态
- `useDebounce.js` — 防抖
- `usePagedHook.js` — 分页加载

---

## Vue Admin Conventions (Mars Admin UI)

### Project Structure
```
mars-admin-ui/src/
├── App.vue             # 根组件
├── main.ts             # 入口文件
├── components/         # 通用组件
│   ├── common/         # (app-provider, dark-mode, menu-toggler, etc.)
│   ├── custom/         # (svg-icon, count-to, wave-bg, etc.)
│   └── advanced/       # (table-column-setting, etc.)
├── layouts/            # 布局组件
│   ├── base-layout/
│   ├── blank-layout/
│   └── modules/        # (global-header, global-menu, theme-drawer, etc.)
├── views/              # 页面视图
│   ├── _builtin/       # (login, 403, 404, 500)
│   ├── home/
│   └── manage/         # (dept, menu, dict, config, file-manage, etc.)
```

### Tech Stack
- Vue 3 (Composition API) + TypeScript
- Pinia 状态管理 (替代 Vuex)
- UnoCSS + Sass 样式方案
- Vite 构建
- Ant Design Vue 组件库

---

## Cross-Cutting Conventions

### API Naming
- 后端: RESTful — `/api/{resource}`, GET/POST/PUT/DELETE
- 分页: `/api/{resource}/pageList` (POST)
- 导出: `/api/{resource}/export` (GET/POST)
- 同步: `/api/{resource}/sync` (GET)
- 前端: `/api/` 前缀 + 资源名 + 操作名

### Authentication
- Token 存储: `localStorage` (key: `TOKEN_KEY`)
- 每次请求通过 Axios 拦截器自动附加 Header: `token`
- 无 OAuth2, 无 JWT refresh token 机制

### Date Handling
- 数据库: MySQL datetime
- 时区: `serverTimezone=GMT%2B8`
- Java: `java.util.Date` / `LocalDateTime`
- 零值日期处理: `zeroDateTimeBehavior=convertToNull`

### Logging
- 后端: `@Log` 注解 + `ISysOperLogClient` Feign 调用记录操作日志
- 日志级别: `com.scrm: debug`, Spring: `debug`
- 异步错误记录: `BrAsyncErrorInfoServiceImpl`

---

*Conventions analysis: 2026/05/07*
