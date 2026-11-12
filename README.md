# Freight 物流中台（Monorepo）

本仓库采用 **monorepo** 结构，集中管理物流业务平台的三个工程：后端微服务中台、管理后台前端、司机端前端。

| 目录 | 工程 | 技术栈 | 说明 |
|------|------|--------|------|
| `backend/` | freight-platform | Java 8 · Spring Boot · Spring Cloud Alibaba(Nacos) · MyBatis · Maven 多模块 | 后端微服务中台（运单 / 客户 / 承运 / 结算 / 支付 / 轨迹 / 消息 / 报表 / 合同 / 鉴权 / 基础数据 等） |
| `web/` | freight-web | Vue 3 · Vite · Element Plus | 管理后台前端 |
| `driver-app/` | freight-driver-app | Vue 3 · Vite | 司机端 H5 / 移动端 |

## 目录结构

```
freight/
├── backend/        # 后端多模块微服务工程（Maven）
│   ├── pom.xml     # 父工程，聚合各 freight-* 子模块
│   ├── freight-api / freight-common / freight-infra   # 契约 / 公共 / 基础设施
│   ├── freight-auth / freight-waybill / freight-customer / freight-carrier
│   ├── freight-contract / freight-payment / freight-settlement
│   ├── freight-track / freight-message / freight-report / freight-stat
│   ├── freight-basedata / freight-app
│   └── scripts/    # 脚本
├── web/            # 管理后台前端（Vue3 + Vite）
├── driver-app/     # 司机端前端（Vue3 + Vite）
└── README.md
```

## 快速开始

### 后端（backend/）
```bash
cd backend
mvn clean package -DskipTests
# 各子模块为独立 Spring Boot 服务，按需启动；需准备 MySQL 与 Nacos
```

### 管理后台（web/）
```bash
cd web
npm install
npm run dev      # 默认 http://localhost:5173
```

### 司机端（driver-app/）
```bash
cd driver-app
npm install
npm run dev
```

## 仓库约定

- 各子目录保留独立 `.gitignore`；构建产物（`target/` `node_modules/` `dist/` `unpackage/`）均已忽略。
- 仓库为**私有**，请勿提交真实密钥 / 内部配置。本地配置请基于各目录的 `.env.example` 创建 `.env`（已被忽略）。
- 提交前请确认没有把凭据、内部地址等敏感信息带入版本库。
