/**
 * M6 监管上报页面冒烟截图（本地自检）。
 * 登录 → 监管上报（看板统计 + 待上报概览 + 上报记录列表）。
 */
import puppeteer from 'puppeteer-core'
import fs from 'node:fs'

const EDGE = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const BASE = process.env.WEB_BASE || 'http://localhost:5173'
const OUT = process.env.OUT || 'shots-m6'
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
  const text = await page.evaluate(() => document.body.innerText.slice(0, 220).replace(/\n+/g, ' | '))
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

  await page.goto(BASE + '/report', { waitUntil: 'networkidle2' })
  await wait(2200)
  await shot('m01-report')
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
