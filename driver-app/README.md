# freight-driver-app · 司机端（M3）

网络货运平台的司机端，uni-app + Vue 3 一套代码编译 **Android / iOS / H5**。
覆盖「找货 → 接单 → 到达装货 → 装货完成 → 发车 → 到达卸货 → 卸货完成 → 签收 → 回单」全链路，
以及接单开关、消息中心、在途轨迹自动采集。

配套后端：`../freight-platform`（Spring Boot 2.7，`/api/driver/**`）。

---

## 1. 快速开始（H5 联调）

```bash
# 1) 先起后端（8088）
cd ../freight-platform
/d/Java/jdk1.8.0_281/bin/java -jar freight-app/target/freight-app-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=dev --server.port=8088

# 2) 起司机端 H5（5174，/api 已代理到 8088）
cd ../freight-driver-app
npm install
npm run dev:h5
# 浏览器打开 http://127.0.0.1:5174 ，手机模式下体验
```

演示账号（dev 环境启动时会给「已审核通过且未设密码」的司机写入默认密码）：

| 手机号 | 密码 | 说明 |
| --- | --- | --- |
| `13900000001` | `123456` | 已审核 + 名下 40 吨车，可直接接单 |
| 其他已审核司机 | `123456` | 见启动日志 `开发环境已为 N 位审核通过的司机初始化司机端密码` |

> 密码初始化只对 `@Profile("dev")` 生效。生产环境司机必须用「手机号 + 短信验证码」自助开通，
> 系统批量下发初始密码是典型的弱口令来源。

## 2. 打包成 APP

```bash
npm run build:app      # 产出 dist/build/app，交给 HBuilderX 云打包 / 本地离线打包
npm run build:h5       # 产出 dist/build/h5，可直接丢到 Nginx
```

打包前需要在 `src/manifest.json` 填三项（**都不要提交仓库**）：

1. `appid`：DCloud 应用标识；
2. `app-plus.distribute.sdkConfigs.maps.tencent.key`：腾讯位置服务 key（地图与逆地址）；
3. `app-plus.distribute.sdkConfigs.push.unipush`：uni-push 2.0 证书（Android 厂商通道 / iOS APNs）。

APP 端的产品形态还没最终定（原生插件保活 vs 小程序 vs 轻应用，见大纲 14 节「司机端形态」），
当前实现刻意把「平台能力」收在三个文件里，换形态时只改这三处：
`utils/location.js`（定位）、`utils/tracker.js`（采集节流与批量上报）、`pages/waybill/detail.vue` 里的 `#ifdef APP-PLUS` 地图块。

## 3. 目录与页面

```
src/
├─ api/            request.js（统一鉴权/错误码/401 跳登录）、driver.js（16 个司机端接口）
├─ components/     TabBar.vue（自定义底部导航）、StatusTag.vue
├─ pages/
│  ├─ login/       登录 + 首次开通（手机号/验证码/新密码）
│  ├─ index/       工作台：在线开关、待办四项、进行中运单
│  ├─ hall/        货源大厅：四维打分、距离、脱敏联系人、接单
│  ├─ waybill/     list.vue（四分组）、detail.vue（路线/货物/时间轴/节点确认/回单/异常）
│  ├─ message/     消息中心（未读高亮、全部已读）
│  └─ mine/        我的：准入状态、车辆、证照提醒、改密、推送登记
├─ store/auth.js   会话（token + 司机档案），刻意不引 Pinia
└─ utils/          format.js、location.js（含 WGS84→GCJ02）、tracker.js（轨迹采集）
```

## 4. 几个关键设计（改代码前请先读）

**① 节点动作是封闭枚举，前端不提交目标状态。**
`ARRIVE_LOAD / LOADED / DEPART / ARRIVE_UNLOAD / UNLOADED / SIGNED` 六个动作由服务端映射到运单状态，
`nextAction` 与 `availableActions` 也是服务端算好下发的——状态机规则只有一份，不会在 APP 里漂移。

**② 「到达」不是状态。**
到达装货地/卸货地只写节点打点（带经纬度、逆地址、距参考点米数、照片、IP），运单状态不变。
装卸等待时长、到厂准时率都从打点算，不需要往状态机里塞新状态。

**③ 到厂确认必须在 5 公里内。**
后端 `freight.driver.geo-check.radius-meters=5000`；超出返回 `A0001` 并提示实际距离。
定位失败时按钮会明确提示「未获取到定位」，不会静默失败让司机以为提交了。

**④ 坐标统一 GCJ-02。**
H5 端 `uni.getLocation({type:'gcj02'})` 需要地图服务商 key，因此取原生 WGS-84 后本地转 GCJ-02；
APP 端原生支持 gcj02 直接取用。**任何新接入的位置来源都必须先转成 GCJ-02 再入库。**

**⑤ 轨迹采集 30 秒 / 1 点，静止不重复上报。**
精度 > 100 米的点丢弃，位移 < 100 米且无速度的点跳过，一批 5 点或 60 秒兜底上报，
失败回填缓存最多 200 点。只在 APP 前台运行——后台保活需要原生插件，属于上线前待办。

**⑥ 司机令牌只能调司机端接口。**
后端 `DriverScopeFilter` 限制 `ROLE_DRIVER` 只能访问 `/api/driver/**` 与文件下载，
防止拿司机手机上的令牌去调后台运单接口。

## 5. 自检脚本（可选）

```bash
npm run dev:h5                       # 另开一个终端
node scripts/snap.mjs                # 用系统 Edge 以 iPhone 尺寸跑一遍并截图到 shots/
```

会依次走「登录 → 工作台 → 货源大厅 → 接单 → 运单详情 → 到达装货地 → 我的运单 → 消息 → 我的」，
并在结尾打印所有 console 错误（正常应为 none）。脚本依赖 `puppeteer-core`，仅本地自检用。

## 6. 已知未做（与大纲「待补」一致）

- **推送**：`manifest.json` 已留 uni-push 配置位、后端已有 `POST /api/driver/push/register` 落 `push_client_id`，
  但证书与真机通道未接；当前消息中心靠进入页面拉取（生产建议 30s 轮询 + 推送双通道）。
- **后台轨迹**：APP 退到后台后采集会停，需要原生保活插件或厂商白名单，须按目标机型实测。
- **地图**：H5 用路线示意图（无需 key）；APP 端用原生 `<map>`，需在 manifest 填腾讯 key。
- **短信验证码**：`/password/init` 在 mock 模式下接受任意 6 位，接真实短信网关前不可上生产。
