// JS port of the app's domain/ layer (Item.kt, PortfolioStats, Insights, ItemQuery,
// SearchLinks, ListingPrompt, ItemAge, KeywordPacks). Same field names as the app JSON.

export const STATUSES = ['WATCHING', 'BOUGHT', 'LISTED', 'SOLD'];
export const PLATFORMS = ['eBay', 'Facebook Marketplace', 'Etsy', 'Mercari', 'Poshmark', 'Depop'];
export const nextStatus = (s) => STATUSES[STATUSES.indexOf(s) + 1] || null;

// Same URLs the manifest lets the content script run on (background.js keeps its own copy).
export const isListingUrl = (u) =>
  /^https?:\/\/([a-z0-9-]+\.)*ebay\.com\/itm\//i.test(u) ||
  /^https?:\/\/([a-z0-9-]+\.)*facebook\.com\/marketplace\/item\//i.test(u);

export const profit = (i) => (i.sell || 0) - (i.buy || 0) - (i.fees || 0) - (i.ship || 0);
export const roi = (i) => ((i.buy || 0) + (i.ship || 0) > 0 ? (profit(i) / ((i.buy || 0) + (i.ship || 0))) * 100 : 0);
export const newItem = (over = {}) => ({
  id: crypto.randomUUID(), title: '', buy: 0, sell: 0, fees: 0, ship: 0, qty: 1, status: 'WATCHING',
  cat: '', plat: '', tags: [], seller: '', link: '', notes: '', at: Date.now(), soldAt: 0, photo: '', ...over,
});

export function portfolioStats(items) {
  const sold = items.filter((i) => i.status === 'SOLD');
  const rois = sold.filter((i) => i.buy > 0).map(roi);
  return {
    profit: sold.reduce((a, i) => a + profit(i), 0),
    inStock: items.filter((i) => i.status === 'BOUGHT' || i.status === 'LISTED').reduce((a, i) => a + i.buy + (i.ship || 0), 0),
    soldCount: sold.length,
    avgRoi: rois.length ? rois.reduce((a, b) => a + b, 0) / rois.length : 0,
  };
}

export function monthlyProfit(items, months = 6) {
  const sold = items.filter((i) => i.status === 'SOLD');
  const now = new Date();
  return Array.from({ length: months }, (_, k) => {
    const d = new Date(now.getFullYear(), now.getMonth() - (months - 1 - k), 1);
    const total = sold
      .filter((i) => {
        const t = new Date(i.soldAt > 0 ? i.soldAt : i.at);
        return t.getFullYear() === d.getFullYear() && t.getMonth() === d.getMonth();
      })
      .reduce((a, i) => a + profit(i), 0);
    return { label: d.toLocaleString(undefined, { month: 'short' }), profit: total };
  });
}

export function profitBy(items, keyFn, fallback) {
  const groups = {};
  items.filter((i) => i.status === 'SOLD').forEach((i) => {
    const k = keyFn(i) || fallback;
    (groups[k] = groups[k] || { label: k, profit: 0, count: 0 });
    groups[k].profit += profit(i);
    groups[k].count += 1;
  });
  return Object.values(groups).sort((a, b) => b.profit - a.profit || a.label.localeCompare(b.label));
}

export const ageDays = (i, now = Date.now()) => (i.at > 0 ? Math.max(0, Math.floor((now - i.at) / 86400000)) : 0);
export const isStale = (i, days = 14) => (i.status === 'BOUGHT' || i.status === 'LISTED') && ageDays(i) >= days;

export function queryItems(items, { text = '', status = null, cats = [], sort = 'NEWEST' }) {
  const n = text.trim().toLowerCase();
  const m = items.filter(
    (i) =>
      (!status || i.status === status) &&
      (!cats.length || cats.includes(i.cat)) &&
      (!n || [i.title, i.notes, i.cat].some((s) => (s || '').toLowerCase().includes(n)) ||
        (i.tags || []).some((t) => t.toLowerCase().includes(n)))
  );
  const by = {
    NEWEST: (a, b) => b.at - a.at, OLDEST: (a, b) => a.at - b.at,
    PROFIT: (a, b) => profit(b) - profit(a), ROI: (a, b) => roi(b) - roi(a), PRICE: (a, b) => b.sell - a.sell,
  }[sort];
  return [...m].sort(by);
}

// Pulls the Marketplace location out of any Marketplace link ("/marketplace/109493702409366/search/?query=x"
// or "/marketplace/toronto/") or accepts a bare id/slug. Returns '' when there is none.
export function parseMarketplaceLocation(text) {
  const s = String(text || '').trim();
  const m = s.match(/marketplace\/([^/?#]+)/i);
  const slug = m ? m[1] : /^[\w-]+$/.test(s) ? s : '';
  return ['search', 'item', 'category', 'you', 'inbox', 'notifications'].includes(slug.toLowerCase()) ? '' : slug;
}

// loc is optional: without it Facebook falls back to the location saved on the account.
export function searchUrl(target, query, { loc = '' } = {}) {
  const q = encodeURIComponent(query.trim()).replace(/%20/g, '+');
  if (target === 'EBAY') return `https://www.ebay.com/sch/i.php?_nkw=${q}`;
  if (target === 'EBAY_SOLD') return `https://www.ebay.com/sch/i.php?_nkw=${q}&LH_Sold=1&LH_Complete=1`;
  const base = loc ? `https://www.facebook.com/marketplace/${encodeURIComponent(loc)}/search/` : 'https://www.facebook.com/marketplace/search/';
  return `${base}?query=${q}&sortBy=creation_time_descend`; // newest first: catch deals before others do
}

export function listingPrompt(item, platform) {
  const lines = [
    `You are an expert reseller. Write a high-converting ${platform} listing for the item in the attached photos.`,
    '',
    `Item: ${item.title || '(identify from photos)'}`,
  ];
  if (item.sell > 0) lines.push(`Target price: $${item.sell}`);
  if (item.notes) lines.push(`Condition/details: ${item.notes}`);
  lines.push('', 'Return:', '1. Title (max 80 chars, keyword-rich)',
    '2. Description (condition, specs, what is included, honest flaws)',
    '3. Suggested category and 10 search keywords', '4. Price recommendation (quick-sale / fair / ambitious)');
  return lines.join('\n');
}

export function keywordsPrompt(topic) {
  return `You are an expert reseller. Give me 40 eBay / Facebook Marketplace search keywords for flipping ${topic || 'resale items'} that sell for a profit. Reply with ONLY JSON like {"Category":["keyword","keyword"]}.`;
}

// Words that say nothing about WHAT an item is, so they never count as a keyword match.
const KW_STOP = new Set(['new', 'sealed', 'unopened', 'kit', 'set', 'lot', 'used', 'the', 'and', 'for', 'with', 'bundle', 'box', 'nib', 'nwt', 'brand', 'vintage', 'retro', 'rare', 'mint', 'original']);
const kwWords = (s) => (String(s || '').toLowerCase().match(/[a-z0-9]+/g) || []).filter((w) => w.length >= 3 && !KW_STOP.has(w));

// Category for a listing title, from the user's saved keywords (each filed under a category).
// A keyword matches when 2 of its words (or its only word) appear in the title; the best match wins.
// Returns '' when nothing matches. background.js keeps its own copy of this function.
export function guessCategory(title, searches) {
  const have = new Set(kwWords(title));
  let best = '';
  let bestHits = 0;
  for (const s of searches || []) {
    const words = kwWords(s.q);
    if (!s.cat || !words.length) continue;
    const hits = words.filter((w) => have.has(w)).length;
    if (hits >= Math.min(2, words.length) && hits > bestHits) { best = s.cat; bestHits = hits; }
  }
  return best;
}

// Tolerant parse of an AI reply: {"Cat":[...]}, [{category,keywords}], or a plain array.
export function parseKeywords(raw) {
  // Copying from a chat window leaves "&#x20;" entities and a "\" at the end of each line; drop both.
  const text = String(raw || '').replace(/&#x20;|&nbsp;/g, ' ').replace(/\\[ \t]*$/gm, '');
  const m = text.match(/[[{][\s\S]*[\]}]/);
  if (!m) return [];
  let data;
  try { data = JSON.parse(m[0]); } catch (e) { return []; }
  const out = [];
  const add = (q, cat) => { if (typeof q === 'string' && q.trim()) out.push({ q: q.trim(), cat: cat || '' }); };
  const walk = (v, cat) => {
    if (typeof v === 'string') add(v, cat);
    else if (Array.isArray(v)) v.forEach((x) => walk(x, cat));
    else if (v && typeof v === 'object') {
      const c = v.category || v.name || v.cat;
      const k = v.keywords || v.items;
      if (k) walk(k, typeof c === 'string' ? c : cat);
      else Object.entries(v).forEach(([key, val]) => walk(val, key));
    }
  };
  walk(data, '');
  return out.slice(0, 200);
}

export const KEYWORD_PACKS = [
  ['Electronics', ['sony walkman', 'bose quietcomfort', 'apple watch lot', 'ipad mini', 'jbl speaker', 'garmin gps watch', 'kindle paperwhite', 'airpods pro']],
  ['Video Games', ['nintendo switch lot', 'gameboy advance', 'ps2 console bundle', 'sega genesis games', 'n64 controller', 'xbox 360 lot', 'retro game lot', 'pokemon games lot']],
  ['Cameras', ['canon ae-1', 'nikon fm2', 'polaroid sx-70', 'film camera lot', 'gopro hero', 'fujifilm instax', 'olympus om-1', 'camera lens vintage']],
  ['Clothing', ['vintage levis 501', 'patagonia fleece', 'carhartt jacket', 'north face puffer', 'vintage band tee', 'pendleton wool', 'ralph lauren polo lot', 'lululemon lot']],
  ['Shoes', ['nike dunk low', 'air jordan 1', 'new balance 990', 'adidas samba', 'vintage nike', 'red wing boots', 'dr martens boots', 'birkenstock']],
  ['Jewelry & Watches', ['sterling silver lot', 'vintage seiko', 'citizen eco-drive', 'turquoise ring', 'gold filled chain', 'pandora charm lot', 'costume jewelry lot', 'vintage watch parts']],
  ['Toys', ['lego star wars sealed', 'lego retired set', 'vintage star wars figures', 'hot wheels lot', 'barbie vintage', 'funko pop lot', 'transformers g1', 'he-man figures']],
  ['Collectibles', ['pokemon card lot', 'baseball card lot', 'magic the gathering lot', 'vintage comic lot', 'coin lot silver', 'vinyl record lot', 'stamp collection', 'sports memorabilia']],
  ['Home', ['kitchenaid stand mixer', 'cast iron skillet', 'vintage pyrex', 'le creuset', 'dyson vacuum', 'vitamix blender', 'corelle lot', 'yeti cooler']],
  ['Furniture', ['mid century modern', 'herman miller chair', 'ikea lack lot', 'vintage dresser', 'danish teak', 'rattan chair', 'solid wood desk', 'floor lamp vintage']],
  ['Tools', ['dewalt drill bare tool', 'milwaukee m18', 'makita cordless', 'snap-on wrench', 'craftsman tool set', 'stanley hand plane', 'festool', 'dremel kit']],
].map(([name, keywords]) => ({ name, keywords }));
