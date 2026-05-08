# Codebase Structure

**Analysis Date:** 2026-05-07

## Directory Layout

```
scrm/
├── db/                        # 数据库脚本
│   ├── scrm.sql               # SCRM 业务数据库 (MySQL)
│   └── xxl_job.sql            # XXL-Job 调度中心数据库
├── scrm-server/               # 后端 Java 服务
│   ├── pom.xml                # 父 POM (Maven 多模块)
│   ├── api/                   # API 模块 (DTO + Feign 接口 + 实体)
│   │   ├── pom.xml
│   │   └── src/main/java/com/scrm/api/wx/cp/
│   │       ├── client/        # Feign 客户端接口 + Fallback
│   │       ├── dto/           # 请求/响应 DTO
│   │       ├── entity/        # 数据实体类
│   │       ├── enums/         # 枚举
│   │       └── vo/            # 视图值对象
│   ├── scrm-common/           # 公共模块 (工具 + 配置 + 异常)
│   │   ├── pom.xml
│   │   └── src/main/java/com/scrm/common/
│   │       ├── annotation/    # 自定义注解 (@PassToken)
│   │       ├── config/        # Spring 配置类 (Swagger, Redis, MyBatisPlus, etc.)
│   │       ├── constant/      # 常量 (AppConstant, Constants, RabbitMqConstants)
│   │       ├── dto/           # 通用 DTO (BasePageDTO, BatchDTO)
│   │       ├── entity/        # 系统实体 (SysMenu, SysRole, SysOperLog)
│   │       ├── exception/     # 异常框架 (BaseException, GlobalExceptionHandler)
│   │       ├── filter/        # XSS 过滤器
│   │       ├── log/           # 操作日志注解 + 切面 + Feign client
│   │       ├── util/          # 工具类 (JwtUtil, EasyPoiUtils, BeanUtil, etc.)
│   │       └── vo/            # 通用 VO (FailResultVO, MenuTreeVO)
│   └── server/                # Spring Boot 主应用模块
│       ├── pom.xml
│       ├── lib/               # 本地依赖 jar
│       └── src/
│           ├── main/java/com/scrm/server/wx/cp/
│           │   ├── MaxScrmWxCpServerApplication.java   # 启动类
│           │   ├── builder/           # 企业微信消息构建器
│           │   ├── client/            # Feign 客户端实现 (如 WxStaffClient)
│           │   ├── config/            # 业务配置 (WxCp, RabbitMQ, RestTemplate, XxlJob)
│           │   ├── dto/              # 服务内部 DTO (Br*DTO)
│           │   ├── entity/           # 服务内部实体 (Br*Entity, Wx*Entity)
│           │   ├── feign/            # 第三方 Feign 客户端 (CpTpFeign)
│           │   ├── feign/config/     # Feign 配置
│           │   ├── feign/dto/        # Feign 响应 DTO
│           │   ├── feign/fallback/   # Feign 降级实现
│           │   ├── filter/           # 请求过滤器
│           │   ├── handler/          # 企业微信事件处理器
│           │   ├── handler/customer/ # 客户相关事件
│           │   ├── handler/mp/       # 公众号相关事件
│           │   ├── handler/tp/       # 第三方应用事件
│           │   ├── mapper/           # MyBatis-Plus Mapper
│           │   ├── mapper/xml/       # MyBatis XML 映射文件
│           │   ├── schedule/         # XXL-Job 定时任务
│           │   ├── service/          # Service 接口
│           │   ├── service/impl/     # Service 实现
│           │   ├── thread/           # 线程池/异步任务
│           │   ├── utils/            # 工具类
│           │   ├── vo/              # 服务内部 VO
│           │   └── web/             # Controller 层 (REST API)
│           └── main/resources/
│               ├── bootstrap.yml          # 实际运行配置 (git ignored)
│               └── bootstrap-template.yml # 配置模板 (含所有配置项注释)
├── scrm-web/                  # 前端应用
│   ├── package.json           # React 主应用 + Vue Admin 依赖
│   ├── src/                   # React SPA 源码
│   │   ├── App.js             # 应用入口
│   │   ├── App.less           # 全局样式
│   │   ├── assets/            # 静态资源 (图片/字体/图标)
│   │   ├── components/        # 通用业务组件 (约 30+ 组件)
│   │   ├── data/              # 静态数据 (menu.json)
│   │   ├── hooks/             # 自定义 Hook
│   │   ├── layout/            # 布局组件 (侧边栏/面包屑/页面容器)
│   │   ├── pages/             # 页面组件 (按功能模块组织)
│   │   ├── routes/            # 路由配置 + 懒加载
│   │   ├── services/          # API 接口层
│   │   │   ├── modules/       # 按业务模块拆分的 API 函数
│   │   │   ├── request.js     # Axios 封装
│   │   │   └── utils.js       # 请求工具函数
│   │   ├── store/             # MobX 状态管理
│   │   ├── style/             # 全局样式变量
│   │   └── utils/             # 工具函数
│   ├── public/                # HTML 模板
│   ├── config/                # Webpack 配置
│   ├── scripts/               # 构建脚本
│   ├── build/                 # 构建产物
│   └── mars-admin/            # Vue 3 管理后台
│       ├── mars-admin-ui/     # Vue 3 + TypeScript 管理界面
│       │   └── src/
│       │       ├── App.vue
│       │       ├── layouts/   # 布局组件
│       │       ├── views/     # 管理功能页面
│       │       │   ├── home/           # 首页
│       │       │   └── manage/         # 管理功能 (部门/菜单/字典/配置/缓存)
│       │       ├── components/         # 通用组件
│       │       ├── store/              # Pinia 状态管理
│       │       ├── hooks/              # 自定义 Hooks
│       │       ├── constants/          # 常量
│       │       ├── enum/               # 枚举
│       │       ├── locales/            # 国际化
│       │       └── styles/             # 样式
│       └── snail-job/        # SnailJob 分布式调度 (嵌入项目内)
│       └── mars-admin/       # 管理后台后端 (Java Maven)
├── .planning/                # 规划文档
├── .claude/                  # Claude 配置
├── .idea/                    # IntelliJ IDEA 配置
├── .gitignore
├── README.md
└── LICENSE
```

## Directory Purposes

### scrm-server/ 后端

**api 模块:**
- Purpose: 定义跨模块契约，DTO/Entity/VO 和 Feign 接口
- Contains: DTO、Entity、枚举、VO、Feign客户端接口
- Key files: `scrm-server/api/src/main/java/com/scrm/api/wx/cp/`
- 注意: Feign 接口的 `@FeignClient` 注解当前被注释未启用

**scrm-common 模块:**
- Purpose: 全项目共享的工具库、配置、异常处理框架
- Contains: 配置类 (Swagger/Redis/MyBatisPlus/Druid)、工具类 (JWT/XSS/POI/加密)、异常框架、日志切面、系统实体
- Key files: `scrm-server/scrm-common/src/main/java/com/scrm/common/`
- 依赖: 包含所有主要的第三方依赖 (weixin-java-cp, redisson, xxl-job, cos_api 等)

**server 模块:**
- Purpose: Spring Boot 主应用，包含全部业务逻辑
- Contains: Controller (web/)、Service (service/)、Mapper (mapper/)、事件处理 (handler/)、定时任务 (schedule/)、实体 (entity/)
- Key files: `scrm-server/server/`
- 配置文件: `bootstrap.yml` (实际使用) / `bootstrap-template.yml` (模板)

### scrm-web/ 前端

**src/components/:**
- Purpose: 通用业务组件，可在不同页面复用
- Contains: 约 30+ 组件目录，每个组件独立文件夹
- 关键组件:
  - `CommonTransfer/` - 穿梭框 (支持无限滚动/分页/远程搜索)
  - `CommonUpload/` - 文件/图片上传 (封装 COS 上传)
  - `MyChart/` - ECharts 图表封装 (柱状图/折线图)
  - `WeChatMsgEditor/` - 企业微信消息编辑器
  - `TagSelect/TagGroupSelect/TagGroupCard` - 标签选择与展示
  - `CustomerSelect/GroupSelect/GroupOwnerSelect` - 客户/群选择器
  - `MySelect/` - 多功能选择器 (员工/客户/群/管理员)

**src/pages/:**
- Purpose: 页面级组件，对应路由
- Contains: 按功能模块命名，每页一个文件夹
- 功能模块:
  - `Home/HomeV2` - 数据总览/仪表盘
  - `CustomerManage/` - 客户管理
  - `CustomerGroup/` - 客户群管理
  - `ChannelCode/` - 渠道活码
  - `CustomerMass/` - 客户群发
  - `GroupMass/` - 客户群群发
  - `CommercialOpportunity/` - 商机管理
  - `CommercialOpportunityConfiguration/` - 商机配置
  - `SaleOperations/` - 内容中心 (素材/话术)
  - `WelcomeSetting/` - 欢迎语设置
  - `UserManage/` - 员工管理
  - `AdminList/` - 管理员列表
  - `DimissionInherit/` - 离职继承
  - `IncumbencyTransfer/` - 在职转接
  - `Login/` - 登录
  - `OrderList/` - 订单管理
  - `ProductList/ProductCategory/` - 产品管理
  - `RoleManage/` - 角色管理
  - `SaleTarget/` - 销售目标
  - `TalkScript/` - 话术库
  - `GroupTagManage/` - 客户群标签
  - `LossingCustomer/` - 流失记录
  - `CustomerJourney/` - 客户旅程
  - `ExceptionPage/` - 错误页面

**src/store/:**
- Purpose: MobX 状态管理
- Contains: `index.js` (RootStore), `User.js`, `Menu.js`, `Wxwork.js`
- Pattern: 单例 RootStore 包含三个子 Store

**src/services/:**
- Purpose: API 请求封装
- Contains: `modules/*.js` (按业务模块拆分), `request.js` (Axios 实例), `utils.js` (请求工具)
- Pattern: 每个 API 函数返回 Promise，统一路径前缀 `/api/`

## Key File Locations

**Entry Points:**
- `scrm-server/server/.../MaxScrmWxCpServerApplication.java`: Spring Boot 启动类
- `scrm-web/src/App.js`: React 应用入口
- `scrm-web/mars-admin/mars-admin-ui/src/App.vue`: Vue 3 管理后台入口

**Configuration:**
- `scrm-server/pom.xml`: Maven 父 POM，版本管理
- `scrm-server/server/src/main/resources/bootstrap-template.yml`: 全部后端配置项模板
- `scrm-server/server/src/main/resources/bootstrap.yml`: 实际运行配置
- `scrm-web/package.json`: 前端依赖 + 构建脚本
- `scrm-web/config/`: Webpack 构建配置
- `scrm-web/mars-admin/mars-admin-ui/build/config/`: Vite 构建配置

**Core Logic (Backend):**
- `scrm-server/server/.../web/`: 全部 Controller (REST 端点)
- `scrm-server/server/.../service/`: Service 接口
- `scrm-server/server/.../service/impl/`: Service 实现
- `scrm-server/server/.../mapper/`: MyBatis-Plus Mapper
- `scrm-server/server/.../handler/`: 企业微信事件处理
- `scrm-server/server/.../schedule/`: 定时任务 (XXL-Job)

**Core Logic (Frontend):**
- `scrm-web/src/routes/config.js`: 路由配置
- `scrm-web/src/services/modules/`: 按业务模块的 API 函数
- `scrm-web/src/store/`: MobX 状态管理
- `scrm-web/src/pages/`: 页面组件

**Testing:**
- `scrm-web/package.json` 包含 Jest 配置 (roots: `<rootDir>/src`, testMatch: `src/**/*.{spec,test}.{js,jsx,ts,tsx}`)
- 后端测试: `scrm-server/server/` 依赖 `spring-boot-starter-test`

## Naming Conventions

**Files:**
- Java 类: PascalCase (如 `ContactWayController.java`, `StaffServiceImpl.java`)
- Java 接口: `I{Name}` 前缀 (如 `IStaffService`, `IWxStaffClient`)
- DTO 后缀: `{Entity}SaveDTO`, `{Entity}PageDTO`, `{Entity}UpdateDTO`, `{Entity}QueryDTO`
- Controller 命名: `{Entity}Controller.java`
- MyBatis XML: 与 Mapper 接口同名，放在 `mapper/xml/` 目录
- React 组件: PascalCase 文件夹 + `index.js` (如 `CustomerSelect/index.js`)
- Vue 组件: kebab-case 文件名 (如 `dict-data-operate-drawer.vue`)
- API 模块文件: camelCase (如 `customerManage.js`)

**Directories:**
- Java 包: `com.scrm.{module}.{layer}.{business}`
- React 目录: camelCase (如 `customerManage/`, `channelCode/`)
- Vue 目录: kebab-case (如 `cache-list/`, `file-manage/`)

## Where to Add New Code

**New Feature (后端):**
- Entity: `scrm-server/api/src/main/java/com/scrm/api/wx/cp/entity/` (共享实体) 或 `scrm-server/server/.../entity/` (服务内部)
- DTO: `scrm-server/api/src/main/java/com/scrm/api/wx/cp/dto/`
- Controller: `scrm-server/server/.../web/{Entity}Controller.java`
- Service 接口: `scrm-server/server/.../service/I{Entity}Service.java`
- Service 实现: `scrm-server/server/.../service/impl/{Entity}ServiceImpl.java`
- Mapper: `scrm-server/server/.../mapper/{Entity}Mapper.java`
- Mapper XML: `scrm-server/server/.../mapper/xml/{Entity}Mapper.xml`

**New Feature (前端 React):**
- 页面组件: `scrm-web/src/pages/{FeatureName}/index.js`
- API 函数: `scrm-web/src/services/modules/{featureName}.js`
- 路由注册: `scrm-web/src/routes/config.js` 添加 lazy-loaded 页面
- 通用组件: `scrm-web/src/components/{ComponentName}/index.js`

**New Feature (Vue 管理后台):**
- 页面: `scrm-web/mars-admin/mars-admin-ui/src/views/manage/{feature-name}/index.vue`
- 状态: `scrm-web/mars-admin/mars-admin-ui/src/store/modules/`

**Utilities:**
- 后端工具: `scrm-server/scrm-common/src/main/java/com/scrm/common/util/`
- 前端工具: `scrm-web/src/utils/`

## Special Directories

**scrm-web/mars-admin/snail-job/:**
- Purpose: 内嵌的 SnailJob 分布式调度框架源代码 (完整 Maven 项目)
- Generated: No
- Committed: Yes
- 注意: 包含 snail-job-server/snail-job-client/snail-job-common 等子模块，与 XXL-Job 并存

**scrm-web/mars-admin/mars-admin/:**
- Purpose: 管理后台的 Java Maven 后端项目
- Generated: No
- Committed: Yes

**scrm-web/build/:**
- Purpose: React 应用构建产物
- Generated: Yes (build script)
- Committed: Yes

**scrm-web/node_modules/:**
- Purpose: NPM 依赖
- Generated: Yes (yarn install)
- Committed: No (.gitignore)

**scrm-server/server/lib/:**
- Purpose: 本地依赖 jar 包 (非 Maven Central)
- Generated: No
- Committed: Yes

---

*Structure analysis: 2026-05-07*
