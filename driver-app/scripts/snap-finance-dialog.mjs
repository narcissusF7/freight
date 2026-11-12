import puppeteer from 'puppeteer-core'
const EDGE = 'C://Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const BASE = 'http://localhost:5173'
const b = await puppeteer.launch({ executablePath: EDGE, headless: 'new', args: ['--no-sandbox'] })
const p = await b.newPage()
await p.setViewport({ width: 1440, height: 900, deviceScaleFactor: 1.5 })
const errs = []
p.on('pageerror', (e) => errs.push('pageerror: ' + e.message))
p.on('console', (m) => { if (m.type() === 'error' && !m.text().includes('404')) errs.push(m.text()) })
const wait = (ms) => new Promise((r) => setTimeout(r, ms))

await p.goto(BASE + '/login', { waitUntil: 'networkidle2' })
await wait(700)
const inputs = await p.$$('input')
await inputs[0].type('admin'); await inputs[1].type('admin123')
await p.evaluate(() => document.querySelector('.login-btn').click())
await wait(2500)

// 生成结算单弹窗
await p.goto(BASE + '/settlement', { waitUntil: 'networkidle2' })
await wait(1500)
await p.evaluate(() => [...document.querySelectorAll('button')].find(b => b.innerText.includes('按运单生成结算单')).click())
await wait(1200)
await p.screenshot({ path: 'shots-finance/w07-generate-dialog.png' })

// 明细弹窗
await p.evaluate(() => [...document.querySelectorAll('button')].find(b => b.innerText.trim() === '明细').click())
await wait(1200)
await p.screenshot({ path: 'shots-finance/w08-detail-dialog.png' })

// 钱包流水抽屉
await p.goto(BASE + '/wallet', { waitUntil: 'networkidle2' })
await wait(1500)
await p.evaluate(() => [...document.querySelectorAll('button')].find(b => b.innerText.includes('资金流水')).click())
await wait(1500)
await p.screenshot({ path: 'shots-finance/w09-wallet-flows.png' })

console.log('errors:', errs.length ? errs.join(' | ') : '(none)')
await b.close()
