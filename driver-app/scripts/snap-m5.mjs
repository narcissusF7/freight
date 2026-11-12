/**
 * M5 客服与统计页面冒烟截图（本地自检，不进生产构建）。
 * 登录 → 投诉管理（含详情抽屉）→ 星级评价 → 信用管理（含流水抽屉）→ 统计中心（含穿透抽屉）。
 */
import puppeteer from 'puppeteer-core'
import fs from 'node:fs'

const EDGE = 'C://Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const BASE = process.env.WEB_BASE || 'http://localhost:5173'
const OUT = process.env.OUT || 'shots-m5'
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
page.on('console', (m) => { if (m.type() === 'error' && !m.text().includes('404')) errors.push('console: ' + m.text()) })
page.on('pageerror', (e) => errors.push('pageerror: ' + e.message))

const wait = (ms) => new Promise((r) => setTimeout(r, ms))

async function shot(name) {
  await wait(900)
  await page.screenshot({ path: `${OUT}/${name}.png` })
  const text = await page.evaluate(() => document.body.innerText.slice(0, 180).replace(/\n+/g, ' | '))
  console.log(`[${name}] ${text}`)
}

async function main() {
  await page.goto(BASE + '/login', { waitUntil: 'networkidle2', timeout: 60000 })
  await wait(700)
  const inputs = await page.$$('input')
  await inputs[0].type('admin')
  await inputs[1].type('admin123')
  await page.evaluate(() => document.querySelector('.login-btn').click())
  await wait(2600)

  // 投诉管理
  await page.goto(BASE + '/complaint', { waitUntil: 'networkidle2' })
  await wait(1800)
  await shot('m01-complaint')

  await page.evaluate(() => {
    const rows = document.querySelectorAll('.el-table__body tbody tr')
    if (rows.length) rows[1].querySelector('td').click()
  })
  await wait(1600)
  await shot('m02-complaint-detail')

  // 评价
  await page.goto(BASE + '/rating', { waitUntil: 'networkidle2' })
  await wait(1800)
  await shot('m03-rating')

  // 信用
  await page.goto(BASE + '/credit', { waitUntil: 'networkidle2' })
  await wait(1800)
  await shot('m04-credit')

  await page.evaluate(() => {
    const btn = [...document.querySelectorAll('button')].find((b) => b.innerText.includes('变动明细'))
    if (btn) btn.click()
  })
  await wait(1500)
  await shot('m05-credit-flows')

  // 统计中心
  await page.goto(BASE + '/stat', { waitUntil: 'networkidle2' })
  await wait(3200)
  await shot('m06-stat')

  // 点指标卡穿透
  await page.evaluate(() => {
    const card = document.querySelector('.stat-card.clickable')
    if (card) card.click()
  })
  await wait(1600)
  await shot('m07-stat-drill')
}

try {
  await main()
} catch (e) {
  console.log('ERROR:', e.message)
} finally {
  await browser.close()
  console.log('\n=== console errors ===')
  console.log(errors.length ? errors.join('\n') : '(none)')
}
