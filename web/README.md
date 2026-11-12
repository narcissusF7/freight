# 网络货运平台 · 管理后台前端

Vue 3 + Vite + Element Plus 实现的后台管理系统，对接 Spring Boot 2.7 模块化单体后端 `freight-platform/freight-app`（端口 8080）。

## 技术栈

- Vue 3（`<script setup>` 组合式 API）
- Vite 5（开发服务器 + 构建）
- Vue Router 4（路由 + 登录守卫）
- Pinia（用户态：token / 用户信息）
- Element Plus 2.8 + 图标
- Axios（HTTP，统一拦截器）

## 开发运行

```bash
npm install
npm run dev      # 启动开发服务器，默认 http://localhost:5173
```

> 开发态通过 Vite 代理把 `/api` 转发到 `http://localhost:8080`，因此需先启动后端 `freight-app`（见后端工程 `freight-platform` 的 `mvn` 启动说明）。
> 默认演示账号：`admin / admin123`（由后端 `DataInitializer` 在 dev 环境自动初始化，生产务必改密或移除）。

## 生产构建

```bash
npm run build    # 产物输出到 dist/
npm run preview  # 本地预览构建产物（端口 4173）
```

生产部署建议由 Nginx 统一反代前端静态资源与 `/api` 后端接口。

## 与后端的对接契约（来自 freight-app）

- 所有响应统一结构：`{ code, message, data, traceId, timestamp }`，成功约定 `code === '00000'`。
- 鉴权：登录后令牌以 `Bearer <token>` 放入 `Authorization` 请求头；无令牌访问受保护端点返回 **HTTP 401**。
- 统一前缀：`/api`。主要端点：
  - `POST /api/auth/login` 登录；`GET /api/auth/me` 当前用户；`POST /api/auth/logout` 登出
  - `GET /api/orgs/tree`、`POST/PUT/DELETE /api/orgs[/id]` 机构
  - `GET /api/roles`、`POST /api/roles/{id}/menus` 角色与授权
  - `GET /api/menus/tree` 菜单树
  - `GET /api/employees`、`POST /api/employees/{id}/roles` 员工与授权
  - `POST /api/files/upload`（multipart：`orgId`/`bizType`/`file`）

## 目录结构

```
src/
├── api/          # 各业务模块的 HTTP 封装（auth/org/role/menu/employee/file）
├── layout/       # 全局布局：侧边栏 + 顶栏
├── router/       # 路由表与登录守卫（含内置导航配置 navMenus）
├── store/        # Pinia 用户态
├── styles/       # 全局样式
├── utils/        # request.js：axios 实例 + 拦截器（注入 token / 401 跳转 / 解包 data）
└── views/        # 页面：login / dashboard / org / role / menu / employee / file
```

> 侧边栏导航当前使用 `router/index.js` 中的 `navMenus` 内置配置；后续接入后端动态菜单时，可改为调用 `GET /api/menus/tree` 生成路由与菜单。
