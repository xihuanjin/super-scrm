# Technology Stack

**Analysis Date:** 2026/05/07

## Languages

**Primary (Backend):**
- Java 1.8 (Java 8) - SCRM server (`scrm-server/`)
- Java 17 - Mars Admin backend (`scrm-web/mars-admin/mars-admin/`)
- Compiler target: 1.8 (SCRM) / 17 (Mars Admin)

**Primary (Frontend):**
- TypeScript 4.x (SCRM Web SPA) - React app
- TypeScript 5.7.3 (Mars Admin UI) - Vue 3 admin panel
- JavaScript (mars-admin-uniapp) - Mobile app

## Runtime

**Environment:**
- Node.js >= 18.12.0 required for mars-admin-ui (Vue Vite project)
- JVM (Spring Boot fat JAR deployment)
- uni-app runtime for mobile (mars-admin-uniapp)

**Package Managers:**
- Maven 3.x - Java backend (`scrm-server/pom.xml`)
- Yarn 1.x - SCRM Web frontend (`scrm-web/yarn.lock`)
- pnpm >= 8.7.0 - Mars Admin UI frontend (`scrm-web/mars-admin/mars-admin-ui/`)

**Lockfiles:**
- `scrm-web/yarn.lock` - React app dependencies locked
- `scrm-web/mars-admin/mars-admin-ui/pnpm-lock.yaml` - Vue admin locked
- `scrm-web/mars-admin/mars-admin-ui/pnpm-workspace.yaml` - pnpm workspace

## Frameworks

**Core Backend (SCRM):**
- Spring Boot 2.3.2.RELEASE - Main application framework
- Spring Cloud Hoxton.SR9 - Microservice ecosystem
- Spring Cloud Alibaba 2.2.6.RELEASE - Alibaba cloud components
- Spring Cloud OpenFeign 2.2.2.RELEASE - Declarative HTTP clients
- Spring Cloud Sleuth 2.2.6.RELEASE - Distributed tracing
- MyBatis-Plus 3.4.3.4 - ORM (with code generator 3.4.1)
- Druid 1.2.5 - Database connection pool
- Swagger 3.0.0 / Knife4j 2.0.8 - API documentation
- Hibernate Validator 6.0.13.Final - Input validation

**Core Backend (Mars Admin):**
- Spring Boot 3.0.5 - Main application framework
- MyBatis-Flex 1.10.9 - ORM (lightweight MyBatis enhancement)
- Druid 1.2.23 - Database connection pool
- Knife4j 4.4.0 (OpenAPI 3) - API documentation
- Sa-Token 1.38.0 - Authentication and authorization
- Hutool 5.8.20 - Java utility library

**Frontend (SCRM Web - React SPA):**
- React 17.0.2 - UI framework
- Ant Design 4.19.0 - Component library
- MobX 4.15.7 / mobx-react 6.0.0 - State management
- React Router DOM 6.9.0 - Routing
- Axios 0.24.0 - HTTP client
- ahooks 3.0.1 - React Hooks library

**Frontend (Mars Admin UI - Vue 3 SPA):**
- Vue 3.5.13 - UI framework
- Ant Design Vue 4.2.6 - Component library
- Pinia 3.0.0 - State management
- Vue Router 4.5.0 - Routing
- Vue I18n 11.1.1 - Internationalization
- @vueuse/core 12.5.0 - Composition API utilities

**Frontend (Mars Admin UniApp - Mobile):**
- uni-app (Vue-based cross-platform framework)
- uView UI 2.0.38 - Mobile component library

**Testing:**
- Jest 26.6.0 + @testing-library/react 11.1.0 - SCRM Web unit tests
- Spring Boot Starter Test - Backend unit tests
- eslint-plugin-testing-library - Test lint rules

**Build/Dev:**
- Webpack 4.44.2 (custom CRA-based scripts) - SCRM Web bundler
- Vite 6.1.0 (with @vitejs/plugin-vue 5.2.1) - Mars Admin UI bundler
- Babel 7.12.3 - JavaScript transpilation (React app)
- Less 3.12.2 + less-loader 7.0.2 - CSS preprocessing (React app)
- Sass 1.84.0 with UnoCSS 65.4.3 - CSS preprocessing (Vue app)
- Vue TSC 2.2.0 - Type checking for Vue
- ESLint 9.20.0 / ESLint 7.11.0 - Linting
- PostCSS with preset-env - CSS post-processing

## Key Dependencies

**Critical Backend:**
- `weixin-java-cp` 4.4.0 - WeChat Work SDK (binarywang): Core integration for enterprise WeChat APIs (`scrm-server/scrm-common/pom.xml`)
- `redisson` 3.11.1 + `jedis` 2.9.0 - Redis clients for caching and distributed state
- `spring-boot-starter-amqp` - RabbitMQ for async event processing
- `xxl-job-core` 2.3.0 - Distributed task scheduling
- `shedlock-spring` 4.20.0 + `shedlock-provider-redis-spring` 4.20.0 - Distributed lock for scheduled tasks
- `cos_api` 5.6.54 + `cos-sts_api` 3.1.0 - Tencent Cloud Object Storage SDK
- `java-jwt` 3.3.0 - JWT token authentication
- `easypoi` 4.3.0 - Excel import/export (`scrm-server/scrm-common/pom.xml`)

**Critical Backend (Mars Admin):**
- `sa-token-spring-boot3-starter` 1.38.0 - RBAC authentication
- `sa-token-redis-jackson` - Redis-backed session management
- `minio` 8.5.2 - S3-compatible object storage client
- `aliyun-sdk-oss` 3.17.4 - Alibaba Cloud OSS client
- `mybatis-flex-spring-boot-starter` 1.10.9 - ORM mapping

**Critical Frontend:**
- `cos-js-sdk-v5` 1.4.17 - Tencent Cloud COS browser SDK (`scrm-web/package.json`)
- `braft-editor` 2.3.9 + `braft-extensions` 0.1.1 - Rich text editor (React)
- `wangeditor` 4.7.12 - Alternative rich text editor
- `echarts` 5.2.2 (React) / `echarts` 5.6.0 (Vue) - Charts and visualization
- `@dnd-kit/core` 5.0.1 + `@dnd-kit/sortable` 6.0.0 - Drag-and-drop (React)
- `@loadable/component` 5.15.3 - Code splitting (React)
- `@iconify/vue` 4.3.0 - Icon library (Vue)

**Infrastructure:**
- `mysql-connector-java` / `mysql-connector-j` - MySQL JDBC driver
- `druid-spring-boot-starter` - Database connection pooling with monitoring
- `snakeyaml` - YAML configuration parsing

## Configuration

**Environment:**
- `scrm-server/server/src/main/resources/bootstrap.yml` - Main configuration (active profile: local by default)
- `scrm-server/server/src/main/resources/bootstrap-template.yml` - Template (all commented out) for reference
- Profiles: `dev` (default), `pd` (production) via Maven profiles
- All external service credentials configured in `bootstrap.yml`:
  - Redis connection (host, port, password)
  - RabbitMQ connection (host, port, credentials)
  - MySQL connection (Druid datasource URL, username, password)
  - Tencent Cloud COS (bucket, secretId, secretKey, region)
  - WeChat Work credentials (corpID, secrets, tokens, AES keys, agent IDs)
  - XXL-Job admin addresses

**Key config namespaces:**
- `scrm.*` - SCRM business config via `ScrmConfig.java` (`@ConfigurationProperties(prefix = "scrm")`)
- `redis.*` - Redis connection via `RedisConfig.java`
- `tq.cos.*` - Tencent Cloud COS config
- `xxl.job.*` - XXL-Job scheduler config

**Build:**
- `scrm-server/pom.xml` - Root Maven POM with dependency management
- `scrm-web/package.json` - React app config with embedded ESLint/Babel/Jest config
- `scrm-web/mars-admin/mars-admin-ui/vite.config.ts` - Vite bundler configuration
- `scrm-web/mars-admin/mars-admin-ui/tsconfig.json` - TypeScript config
- `scrm-web/mars-admin/mars-admin-ui/uno.config.ts` - UnoCSS configuration

## Platform Requirements

**Development:**
- JDK 1.8+ (for SCRM server), JDK 17+ (for Mars Admin)
- Node.js >= 18.12.0 (for mars-admin-ui)
- MySQL 8.0
- Redis
- RabbitMQ
- Maven 3.x
- pnpm >= 8.7.0 (for mars-admin-ui)
- Yarn (for React app)
- IDE: IntelliJ IDEA recommended (Java), VSCode/WebStorm (frontend)

**Production:**
- Linux server (CentOS/Ubuntu)
- JRE 1.8+ / JRE 17+
- MySQL 8.0
- Redis
- RabbitMQ
- XXL-Job admin console (separate deployment)
- Nginx (for reverse proxy, implied by `nginx.conf` in mars-admin-web)
- Docker (for snail-job deployment, optional for main app)

---

*Stack analysis: 2026/05/07*
