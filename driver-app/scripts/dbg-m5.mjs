import puppeteer from 'puppeteer-core'
const EDGE='C://Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const b=await puppeteer.launch({executablePath:EDGE,headless:'new',args:['--no-sandbox']})
const p=await b.newPage(); await p.setViewport({width:1440,height:900})
await p.goto('http://localhost:5173/login',{waitUntil:'networkidle2'})
const ins=await p.$$('input'); await ins[0].type('admin'); await ins[1].type('admin123')
await p.evaluate(()=>document.querySelector('.login-btn').click())
await new Promise(r=>setTimeout(r,2200))
await p.goto('http://localhost:5173/complaint',{waitUntil:'networkidle2'})
await new Promise(r=>setTimeout(r,1800))
const info = await p.evaluate(()=>{
  const rows=[...document.querySelectorAll('.el-table__body tbody tr')]
  const r = rows[3]
  const cell = r.querySelectorAll('td')[7]
  const cs = getComputedStyle(cell.querySelector('.cell') || cell)
  return {
    cellRect: cell.getBoundingClientRect().toJSON(),
    cellHtml: cell.innerHTML.slice(0, 300),
    cellStyle: {overflow: cs.overflow, whiteSpace: cs.whiteSpace, width: cs.width},
    btns: [...cell.querySelectorAll('button')].map(b=>({t:b.innerText, rect:b.getBoundingClientRect().toJSON()}))
  }
})
console.log(JSON.stringify(info,null,1))
await b.close()
