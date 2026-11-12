/**
 * 后台财务页冒烟与截图（本地自检用）。
 * 登录 → 结算管理 → 支付管理 → 钱包与提现 → 发票管理 → 对账中心。
 */
import puppeteer from 'puppeteer-core'
import fs from 'node:fs'

const EDGE = 'C://Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const BASE = process.env.WEB_BASE || 'http://localhost:5173'
const OUT = process.env.OUT || 'shots-finance'

fs.mkdirSync(OUT, { recursive: true })

const browser = await puppeteer.launch({
  executablePath: EDGE,
  headless: 'new',
  args: ['--no-sandbox', '--disable-dev-shm-usage', '--lang=zh-CN']
})
const page = await browser.newPage()
await page.setViewport({ width: 1440, height: 900, deviceScaleFactor: 1.5 })
await page.emulateTimezone('Asia/Shanghai')

const errors = []
page.on('console', (m) => { if (m.type() === 'error') errors.push('console: ' + m.text()) })
page.on('pageerror', (e) => errors.push('pageerror: ' + e.message))

const wait = (ms) => new Promise((r) => setTimeout(r, ms))

async function shot(name) {
  await wait(900)
  await page.screenshot({ path: `${OUT}/${name}.png`, fullPage: false })
  const text = await page.evaluate(() => document.body.innerText.slice(0, 220).replace(/\n+/g, ' | '))
  console.log(`[${name}] ${text}`)
}

async function main() {
  await page.goto(BASE + '/login', { waitUntil: 'networkidle2', timeout: 60000 })
  await wait(800)
  const inputs = await page.$$('input')
  if (inputs.length < 2) throw new Error('登录表单未渲染')
  await inputs[0].type('admin')
  await inputs[1].type('admin123')
  await page.evaluate(() => document.querySelector('.login-btn').click())
  await wait(2800)
  await shot('w01-dashboard')

  const pages = [
    ['w02-settlement', '/settlement'],
    ['w03-payment', '/payment'],
    ['w04-wallet', '/wallet'],
    ['w05-invoice', '/invoice'],
    ['w06-reconciliation', '/reconciliation']
  ]
  for (const [name, path] of pages) {
    await page.goto(BASE + path, { waitUntil: 'networkidle2' })
    await wait(1600)
    if (name === 'w06-reconciliation') {
      // 跑一次对账，让页面有结果
      await page.evaluate(() => {
        const btn = [...document.querySelectorAll('button')].find((b) => b.innerText.includes('运行四方对账'))
        if (btn) btn.click()
      })
      await wait(2200)
    }
    await shot(name)
  }

  console.log('\n=== console errors ===')
  console.log(errors.length ? errors.join('\n') : '(none)')
}

try {
  await main()
} finally {
  await browser.close()
}
