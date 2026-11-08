// Generates public/icons/icon{16,32,48,128}.png with zero dependencies:
// hand-built PNG encoding over Node's built-in zlib.
// Design: graphite rounded square (the app's GraphiteBrush stops) with an emerald "F" (Palette.Emerald).
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

// --- minimal PNG writer -------------------------------------------------------
const CRC_TABLE = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c;
  }
  return t;
})();

function crc32(buf) {
  let c = -1;
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ -1) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body));
  return Buffer.concat([len, body, crc]);
}

function png(size, rgba) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(size, 0);
  ihdr.writeUInt32BE(size, 4);
  ihdr[8] = 8; // bit depth
  ihdr[9] = 6; // color type RGBA
  const stride = size * 4;
  const raw = Buffer.alloc(size * (stride + 1));
  for (let y = 0; y < size; y++) {
    raw[y * (stride + 1)] = 0; // filter: none
    rgba.copy(raw, y * (stride + 1) + 1, y * stride, (y + 1) * stride);
  }
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}

// --- icon renderer ------------------------------------------------------------
const MASK = ['XXXXX', 'X....', 'XXXX.', 'X....', 'X....', 'X....', 'X....'].map((r) =>
  [...r].map((c) => c === 'X')
);
const GRAD = [
  [0x5a, 0x5b, 0x63],
  [0x2b, 0x2b, 0x30],
  [0x15, 0x15, 0x17],
]; // GraphiteBrush stops
const EMERALD = [0x34, 0xb2, 0x7a]; // Palette.Emerald

const lerp = (a, b, f) => a + (b - a) * f;

function render(size) {
  const px = Buffer.alloc(size * size * 4);
  const S = 4; // 4x4 supersampling
  const radius = size * 0.225;
  const gs = (size * 0.6) / 7; // "F" cell size (7 rows tall)
  const x0 = (size - 5 * gs) / 2;
  const y0 = (size - 7 * gs) / 2;
  const inRounded = (u, v) => {
    const half = size / 2;
    const cx = Math.max(Math.abs(u - half) - (half - radius), 0);
    const cy = Math.max(Math.abs(v - half) - (half - radius), 0);
    return cx * cx + cy * cy <= radius * radius;
  };
  const inGlyph = (u, v) => {
    const c = Math.floor((u - x0) / gs);
    const r = Math.floor((v - y0) / gs);
    return c >= 0 && c < 5 && r >= 0 && r < 7 && MASK[r][c];
  };
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      let bgCount = 0, glyphCount = 0, g0 = 0, g1 = 0, g2 = 0;
      for (let sy = 0; sy < S; sy++) {
        for (let sx = 0; sx < S; sx++) {
          const u = x + (sx + 0.5) / S;
          const v = y + (sy + 0.5) / S;
          if (!inRounded(u, v)) continue;
          bgCount++;
          const f = v / size;
          const i = f < 0.5 ? 0 : 1;
          const ff = f < 0.5 ? f * 2 : (f - 0.5) * 2;
          g0 += lerp(GRAD[i][0], GRAD[i + 1][0], ff);
          g1 += lerp(GRAD[i][1], GRAD[i + 1][1], ff);
          g2 += lerp(GRAD[i][2], GRAD[i + 1][2], ff);
          if (inGlyph(u, v)) glyphCount++;
        }
      }
      const o = (y * size + x) * 4;
      const total = S * S;
      px[o + 3] = Math.round((255 * bgCount) / total);
      const bgShare = bgCount ? (bgCount - glyphCount) / bgCount : 0;
      const glShare = bgCount ? glyphCount / bgCount : 0;
      const gb = bgCount ? [g0 / bgCount, g1 / bgCount, g2 / bgCount] : [0, 0, 0];
      px[o] = Math.round(gb[0] * bgShare + EMERALD[0] * glShare);
      px[o + 1] = Math.round(gb[1] * bgShare + EMERALD[1] * glShare);
      px[o + 2] = Math.round(gb[2] * bgShare + EMERALD[2] * glShare);
    }
  }
  return px;
}

function downsample(src, srcSize, dstSize) {
  const k = srcSize / dstSize;
  const out = Buffer.alloc(dstSize * dstSize * 4);
  for (let y = 0; y < dstSize; y++) {
    for (let x = 0; x < dstSize; x++) {
      let r = 0, g = 0, b = 0, a = 0;
      for (let j = 0; j < k; j++) {
        for (let i = 0; i < k; i++) {
          const o = ((y * k + j) * srcSize + (x * k + i)) * 4;
          r += src[o]; g += src[o + 1]; b += src[o + 2]; a += src[o + 3];
        }
      }
      const n = k * k;
      const o = (y * dstSize + x) * 4;
      out[o] = r / n; out[o + 1] = g / n; out[o + 2] = b / n; out[o + 3] = a / n;
    }
  }
  return out;
}

const MASTER = 512;
const rgba = render(MASTER);
const outDir = path.join(__dirname, '..', 'public', 'icons');
fs.mkdirSync(outDir, { recursive: true });
for (const s of [128, 48, 32, 16]) {
  fs.writeFileSync(path.join(outDir, `icon${s}.png`), png(s, downsample(rgba, MASTER, s)));
}
console.log('✓ Wrote public/icons/icon{16,32,48,128}.png');
