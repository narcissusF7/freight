/**
 * H5 冒烟与截图（联调自检，不进生产构建）。
 *
 * 用系统 Edge 以 iPhone 尺寸跑一遍真实流程：
 * 登录 → 工作台 → 货源大厅（接单）→ 运单详情（到达装货地）→ 我的运单 → 消息 → 我的。
 *
 * 说明：uni-app 的 input 是自定义组件，puppeteer 的裸键序不会同步到 v-model，
 * 因此用原生 value setter + input 事件注入（等价于用户粘贴），这是测试辅助手段，不改业务代码。
 */
import puppeteer from 'puppeteer-core'
import fs from 'node:fs'

const EDGE = 'C://Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const BASE = process.env.BASE || 'http://127.0.0.1:5174'
const OUT = process.env.OUT || 'shots'
const PHONE = process.env.PHONE || '13900000001'
// 注意：不要用 process.env.PWD——那是 shell 的当前目录，不是密码
const PASSWORD = process.env.DRIVER_PWD || '123456'
/** 装货地坐标（外高桥），用于模拟司机在场确认 */
const LAT = 31.3558
const LNG = 121.5817

fs.mkdirSync(OUT, { recursive: true })

const browser = await puppeteer.launch({
  executablePath: EDGE,
  headless: 'new',
  args: ['--no-sandbox', '--disable-dev-shm-usage', '--lang=zh-CN']
})

const page = await browser.newPage()
await page.setViewport({ width: 390, height: 844, deviceScaleFactor: 2, isMobile: true, hasTouch: true })
await page.emulateTimezone('Asia/Shanghai')

const ctx = browser.defaultBrowserContext()
await ctx.overridePermissions(BASE, ['geolocation'])
await page.setGeolocation({ latitude: LAT, longitude: LNG })

const errors = []
page.on('console', (m) => {
  if (m.type() === 'error' && !m.text().includes('404')) errors.push('console: ' + m.text())
})
page.on('pageerror', (e) => errors.push('pageerror: ' + e.message))

const wait = (ms) => new Promise((r) => setTimeout(r, ms))

async function shot(name) {
  await wait(800)
  await page.screenshot({ path: `${OUT}/${name}.png`, fullPage: false })
  const text = await page.evaluate(() => document.body.innerText.slice(0, 200).replace(/\n+/g, ' | '))
  console.log(`[${name}] ${text}`)
}

async function go(hash) {
  await page.evaluate((h) => { location.hash = h }, hash)
  await wait(2200)
}

async function fill() {
  await page.evaluate((phone, pwd) => {
    const set = (el, v) => {
      const s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set
      s.call(el, v)
      el.dispatchEvent(new Event('input', { bubbles: true }))
    }
    set(document.querySelector('input[type="number"]'), phone)
    set(document.querySelector('input[type="password"]'), pwd)
  }, PHONE, PASSWORD)
  await wait(300)
}

async function dispatchMessages() {
  const t = await fetch('http://127.0.0.1:8088/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  }).then((r) => r.json())
  await fetch('http://127.0.0.1:8088/api/messages/dispatch', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + t.data.token }
  })
}

async function main() {
  await page.goto(BASE + '/#/pages/login/login', { waitUntil: 'networkidle2', timeout: 60000 })
  await shot('01-login')

  await fill()
  await page.evaluate(() => document.querySelector('.btn-primary').click())
  await wait(2600)
  await shot('02-index')

  await go('#/pages/hall/hall')
  await shot('03-hall')

  // 接单：点第一张卡片的「立即接单」，确认弹窗
  // 优先接「冷轧钢卷」（装货地在外高桥，与模拟定位一致，便于演示到厂确认）
  const accepted = await page.evaluate(() => {
    const cards = [...document.querySelectorAll('.card.job')]
    const target = cards.find((c) => c.innerText.includes('冷轧钢卷')) || cards[0]
    if (!target) return false
    const btn = target.querySelector('.accept')
    if (!btn) return false
    btn.click()
    return true
  })
  if (accepted) {
    await wait(700)
    // uni.showModal 在 H5 是原生 confirm，puppeteer 需要处理 dialog
    await shot('04-accept-confirm')
    await page.evaluate(() => {
      const ok = [...document.querySelectorAll('uni-modal .uni-modal__btn_primary, .uni-modal__btn_primary')][0]
      if (ok) ok.click()
    })
    await wait(2600)
    await shot('05-detail')
  } else {
    console.log('!! 大厅没有可接的货源，跳过接单')
  }

  // 到达装货地（已授权定位，落在装货地附近）
  const confirmed = await page.evaluate(() => {
    const bar = document.querySelector('.action-main .btn-primary')
    if (!bar) return false
    bar.click()
    return true
  })
  if (confirmed) {
    await wait(3000)
    await shot('06-arrived')
  }

  await go('#/pages/waybill/list?scope=ALL')
  await shot('07-waybills')

  // 让接单回执真正投递（生产由调度器 30s 内自动完成，dev 环境默认关闭后台任务）
  await dispatchMessages()

  await go('#/pages/message/list')
  await shot('08-messages')

  await go('#/pages/mine/mine')
  await shot('09-mine')

  console.log('\n=== console errors ===')
  console.log(errors.length ? errors.join('\n') : '(none)')
}

try {
  await main()
} finally {
  await browser.close()
}
