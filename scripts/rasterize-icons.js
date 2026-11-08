// Rasterizes marketing/icon-source.webp into every icon the project needs
// (Android mipmaps + Chrome extension PNGs) using headless Chrome's canvas.
// Run: node scripts/rasterize-icons.js   (from the repo root)
/* eslint-disable no-undef */
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SOURCE = path.join(ROOT, 'marketing', 'icon-source.webp');
const CHROME = 'C:\\Users\\reddi\\.cache\\puppeteer\\chrome\\win64-131.0.6778.204\\chrome-win64\\chrome.exe';
const PORT = 9333;

const LEGACY = { 48: 'mdpi', 72: 'hdpi', 96: 'xhdpi', 144: 'xxhdpi', 192: 'xxxhdpi' };
const FG = { 108: 'mdpi', 162: 'hdpi', 216: 'xhdpi', 324: 'xxhdpi', 432: 'xxxhdpi' };

// Runs inside the page: builds every size from the loaded image and
// returns [{ name, dataUrl }].
const PAGE_SCRIPT = `
(() => {
  const img = document.images[0];
  const W = img.naturalWidth;
  const scaleTo = (size, cover) => {
    const o = document.createElement('canvas'); o.width = o.height = size;
    const g = o.getContext('2d'); g.imageSmoothingEnabled = true; g.imageSmoothingQuality = 'high';
    if (cover) { const s = size / (0.8 * W); g.drawImage(img, (size - s * W) / 2, (size - s * W) / 2, s * W, s * W); }
    else g.drawImage(img, 0, 0, size, size);
    return o;
  };
  const roundCrop = (size) => {
    const c = scaleTo(size, true);
    const o = document.createElement('canvas'); o.width = o.height = size;
    const g = o.getContext('2d');
    g.beginPath(); g.arc(size / 2, size / 2, size / 2, 0, Math.PI * 2); g.closePath(); g.clip();
    g.imageSmoothingEnabled = true; g.imageSmoothingQuality = 'high';
    g.drawImage(c, 0, 0);
    return o;
  };
  const out = [];
  const add = (canvas, name, type, q) => out.push({ name, dataUrl: canvas.toDataURL(type, q) });
  const legacy = ${JSON.stringify(LEGACY)};
  for (const [size, dir] of Object.entries(legacy)) {
    add(scaleTo(+size), 'ic_launcher_' + dir + '.webp', 'image/webp', 0.96);
    add(roundCrop(+size), 'ic_launcher_round_' + dir + '.webp', 'image/webp', 0.96);
  }
  const fg = ${JSON.stringify(FG)};
  for (const [size, dir] of Object.entries(fg)) add(scaleTo(+size), 'ic_launcher_fg_' + dir + '.webp', 'image/webp', 0.96);
  for (const s of [16, 32, 48, 128]) add(scaleTo(s), 'icon' + s + '.png', 'image/png');
  add(scaleTo(512), 'icon-512.png', 'image/png');
  return JSON.stringify(out);
})()`;

function cdp(wsUrl) {
  const ws = new WebSocket(wsUrl);
  let nextId = 1;
  const pending = new Map();
  const listeners = [];
  ws.onmessage = (ev) => {
    const msg = JSON.parse(ev.data);
    if (msg.id && pending.has(msg.id)) {
      pending.get(msg.id)(msg);
      pending.delete(msg.id);
    } else if (msg.method) {
      listeners.forEach((l) => l(msg));
    }
  };
  const ready = new Promise((res, rej) => {
    ws.onopen = () => res();
    ws.onerror = (e) => rej(new Error('ws error: ' + (e.message || e.type)));
  });
  return {
    ready,
    send(method, params = {}) {
      const id = nextId++;
      ws.send(JSON.stringify({ id, method, params }));
      return new Promise((res, rej) =>
        pending.set(id, (msg) => (msg.error ? rej(new Error(method + ': ' + msg.error.message)) : res(msg.result)))
      );
    },
    on(fn) { listeners.push(fn); },
    close() { ws.close(); },
  };
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

(async () => {
  if (!fs.existsSync(SOURCE)) throw new Error('missing source: ' + SOURCE);
  const userDataDir = path.join(ROOT, 'build', '.icon-chrome-profile');
  fs.rmSync(userDataDir, { recursive: true, force: true });

  const chrome = spawn(CHROME, [
    '--headless=new',
    '--remote-debugging-port=' + PORT,
    '--user-data-dir=' + userDataDir,
    '--no-first-run',
    '--allow-file-access-from-files',
    'about:blank',
  ], { stdio: 'ignore' });

  try {
    // Wait for the DevTools endpoint
    let targets;
    for (let i = 0; i < 50; i++) {
      try {
        targets = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json();
        if (targets.length) break;
      } catch (e) { /* not up yet */ }
      await sleep(200);
    }
    const page = targets.find((t) => t.type === 'page');
    if (!page) throw new Error('no page target');

    const conn = cdp(page.webSocketDebuggerUrl);
    await conn.ready;
    await conn.send('Page.enable');
    await conn.send('Page.navigate', { url: 'file:///' + SOURCE.replace(/\\/g, '/') });

    // Wait for the image to decode
    for (let i = 0; i < 50; i++) {
      const r = await conn.send('Runtime.evaluate', {
        expression: '!!document.images[0] && document.images[0].naturalWidth > 0',
        returnByValue: true,
      });
      if (r.result.value) break;
      await sleep(200);
    }

    const r = await conn.send('Runtime.evaluate', {
      expression: PAGE_SCRIPT,
      returnByValue: true,
      awaitPromise: false,
    });
    const files = JSON.parse(r.result.value);
    if (!files.length) throw new Error('page produced no files');

    const res = path.join(ROOT, 'app', 'src', 'main', 'res');
    const extIcons = path.join(ROOT, 'flipscout-ext', 'public', 'icons');
    const written = [];
    for (const f of files) {
      const buf = Buffer.from(f.dataUrl.split(',')[1], 'base64');
      let dest;
      const leg = f.name.match(/^ic_launcher_(\w+)\.webp$/);
      const fgm = f.name.match(/^ic_launcher_fg_(\w+)\.webp$/);
      if (f.name.startsWith('ic_launcher_round_')) {
        dest = path.join(res, 'mipmap-' + f.name.replace('ic_launcher_round_', '').replace('.webp', ''), 'ic_launcher_round.webp');
      } else if (fgm) {
        dest = path.join(res, 'mipmap-' + fgm[1], 'ic_launcher_fg.webp');
      } else if (leg) {
        dest = path.join(res, 'mipmap-' + leg[1], 'ic_launcher.webp');
      } else if (/^icon\d+\.png$/.test(f.name)) {
        dest = path.join(extIcons, f.name);
      } else if (f.name === 'icon-512.png') {
        dest = path.join(ROOT, 'marketing', f.name);
      }
      fs.mkdirSync(path.dirname(dest), { recursive: true });
      fs.writeFileSync(dest, buf);
      written.push(path.relative(ROOT, dest) + ' (' + buf.length + ' B)');
    }
    console.log(written.join('\n'));
    conn.close();
  } finally {
    chrome.kill();
    fs.rmSync(userDataDir, { recursive: true, force: true });
  }
})().catch((e) => { console.error('FAILED:', e.message); process.exit(1); });
