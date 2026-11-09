// FlipScout background service worker — message router + context-menu / command save.
// Save logic lives here so the popup button, the right-click menus (page or link), the
// keyboard command and the popup's paste-a-link box all share one code path.
// Item/seller JSON is byte-compatible with the Android app (Item.kt / Seller.kt), so an
// exported backup imports straight into the phone app; the extra `photoUrl` key is
// ignored by the app and only used for popup thumbnails.

const PLATFORM_LABEL = { EBAY: 'eBay', MARKETPLACE: 'Facebook Marketplace' };
const LISTING_URL_PATTERNS = ['*://*.ebay.com/itm/*', 'https://www.facebook.com/marketplace/item/*'];

// Mirrors isListingUrl in src/utils/logic.js and the content-script matches — the shell
// files are not bundled with the popup, so each side keeps its own copy.
function isListingUrl(url) {
  return (
    /^https?:\/\/([a-z0-9-]+\.)*ebay\.com\/itm\//i.test(url) ||
    /^https?:\/\/([a-z0-9-]+\.)*facebook\.com\/marketplace\/item\//i.test(url)
  );
}

chrome.runtime.onInstalled.addListener(() => {
  // Menus persist across updates — clear first or re-creating the ids throws.
  chrome.contextMenus.removeAll(() => {
    chrome.contextMenus.create({
      id: 'flipscout-save',
      title: 'Save listing to FlipScout',
      contexts: ['page'],
      documentUrlPatterns: LISTING_URL_PATTERNS,
    });
    // SocialKat-style share-link capture: right-click a listing link anywhere (search
    // results, chats, forums) — no need to open the page first.
    chrome.contextMenus.create({
      id: 'flipscout-save-link',
      title: 'Save this listing to FlipScout',
      contexts: ['link'],
      targetUrlPatterns: LISTING_URL_PATTERNS,
    });
  });
});

// Same matching as guessCategory in src/utils/logic.js (the shell files are not bundled with the popup).
const KW_STOP = new Set(['new', 'sealed', 'unopened', 'kit', 'set', 'lot', 'used', 'the', 'and', 'for', 'with', 'bundle', 'box', 'nib', 'nwt', 'brand', 'vintage', 'retro', 'rare', 'mint', 'original']);
const kwWords = (s) => (String(s || '').toLowerCase().match(/[a-z0-9]+/g) || []).filter((w) => w.length >= 3 && !KW_STOP.has(w));
function guessCategory(title, searches) {
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

async function saveListing(listing) {
  if (!listing || !listing.url) return { ok: false, error: 'nothing to save' };
  const { items = [], sellers = [], searches = [] } = await chrome.storage.local.get(['items', 'sellers', 'searches']);

  const dup = items.find((i) => i.link === listing.url);
  if (dup) return { ok: true, duplicate: true, item: dup };

  // Find or create the seller the listing came from.
  let sellerId = '';
  if (listing.sellerUrl) {
    let seller = sellers.find((s) => s.url === listing.sellerUrl);
    if (!seller) {
      seller = {
        id: crypto.randomUUID(),
        name: listing.sellerName || 'Seller',
        url: listing.sellerUrl,
        notes: '',
        fav: false,
        at: Date.now(),
      };
      sellers.push(seller);
      await chrome.storage.local.set({ sellers });
    }
    sellerId = seller.id;
  }

  const item = {
    id: crypto.randomUUID(),
    title: listing.title || '',
    buy: listing.price || 0,
    sell: 0,
    fees: 0,
    ship: 0,
    qty: 1,
    status: 'WATCHING',
    cat: guessCategory(listing.title, searches),
    plat: PLATFORM_LABEL[listing.platform] || '',
    tags: [],
    seller: sellerId,
    link: listing.url || '',
    notes: '',
    at: Date.now(),
    soldAt: 0,
    photo: '', // app-side: local photo filename; the URL goes in photoUrl below
    photoUrl: listing.photoUrl || '',
  };
  items.unshift(item);
  await chrome.storage.local.set({ items });
  return { ok: true, duplicate: false, item };
}

async function undoSave(id) {
  const { items = [] } = await chrome.storage.local.get('items');
  await chrome.storage.local.set({ items: items.filter((i) => i.id !== id) });
  return { ok: true };
}

// Bulk save for the feed scanner. Every row goes through the same saveListing path, so
// dedupe-by-link applies per row and a re-scan can never double-add anything the user
// already tracked. Returns the new ids so the popup can offer one Undo for the whole batch.
async function saveListings(listings) {
  const ids = [];
  let duplicates = 0;
  for (const listing of Array.isArray(listings) ? listings : []) {
    const res = await saveListing(listing);
    if (res && res.ok) {
      if (res.duplicate) duplicates += 1;
      else ids.push(res.item.id);
    }
  }
  return { ok: true, added: ids.length, duplicates, ids };
}

// Exports the app's own backup format (Backup.kt): {app, version, exportedAt, items, sellers}.
async function exportBackup() {
  const { items = [], sellers = [] } = await chrome.storage.local.get(['items', 'sellers']);
  const backup = { app: 'FlipScout', version: 1, exportedAt: Date.now(), items, sellers };
  const url = 'data:application/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(backup, null, 2));
  await chrome.downloads.download({ url, filename: 'flipscout-backup.json', saveAs: false });
  return { ok: true };
}

// --- capture a listing from its URL alone (right-clicked link or pasted into the popup) ---

// Badges the toolbar icon instead of firing notifications (no extra permission); clears itself.
async function badge(ok) {
  await chrome.action.setBadgeBackgroundColor({ color: ok ? '#34B27A' : '#E5484D' });
  await chrome.action.setBadgeText({ text: ok ? '✓' : '!' });
  setTimeout(() => chrome.action.setBadgeText({ text: '' }), 3500);
}

function extractFromTab(tabId) {
  return chrome.tabs.sendMessage(tabId, { action: 'extractListing' });
}

// The content script registers at document_idle, which can land after tab 'complete';
// ping until it answers (bounded) so a fresh tab is never read too early.
async function waitContentScript(tabId, tries = 15) {
  for (let i = 0; i < tries; i++) {
    const pong = await chrome.tabs.sendMessage(tabId, { action: 'ping' }).catch(() => null);
    if (pong && pong.ok) return true;
    await new Promise((r) => setTimeout(r, 300));
  }
  return false;
}

// Opens the listing quietly in a background tab and lets the content script read what
// the logged-in page already shows the user, then closes the tab. A page that won't
// render (login wall, bot check) simply fails extraction — nothing is bypassed.
async function extractFromFreshTab(url) {
  const tab = await chrome.tabs.create({ url, active: false });
  try {
    await new Promise((resolve) => {
      const timer = setTimeout(finish, 15000);
      function finish() {
        clearTimeout(timer);
        chrome.tabs.onUpdated.removeListener(wait);
        resolve();
      }
      function wait(tabId, info) {
        if (tabId === tab.id && info.status === 'complete') finish();
      }
      chrome.tabs.onUpdated.addListener(wait);
    });
    if (!(await waitContentScript(tab.id))) return null;
    // Marketplace is an SPA — the PDP and its photo can render seconds after load, so
    // retry until the listing is complete instead of taking the first answer.
    let res = null;
    for (let i = 0; i < 6; i++) {
      res = await extractFromTab(tab.id).catch(() => null);
      if (res && res.ok && res.listing && res.listing.photoUrl) return res;
      await new Promise((r) => setTimeout(r, 1000));
    }
    return res && res.ok ? res : null;
  } finally {
    chrome.tabs.remove(tab.id).catch(() => {});
  }
}

// Reuse a tab that already shows this listing (SocialKat's find-an-open-tab pattern),
// else open one quietly. The extracted URL is verified against the requested one so a
// pattern prefix-match (e.g. item 123 vs 123456) can never save the wrong listing.
async function saveFromUrl(rawUrl) {
  const url = String(rawUrl || '').split(/[?#]/)[0].replace(/\/+$/, '');
  if (!isListingUrl(url)) return { ok: false, error: 'not a listing link' };

  const { items = [] } = await chrome.storage.local.get('items');
  const dup = items.find((i) => i.link === url);
  if (dup) return { ok: true, duplicate: true, item: dup };

  try {
    const tabs = await chrome.tabs.query({ url: url + '*' });
    for (const tab of tabs) {
      const res = await extractFromTab(tab.id).catch(() => null);
      if (res && res.ok && res.listing && res.listing.url === url) return await saveListing(res.listing);
    }
    const res = await extractFromFreshTab(url);
    if (res && res.ok) return await saveListing(res.listing);
    return { ok: false, error: 'could not read the listing (page needs a login or was too slow)' };
  } catch (e) {
    return { ok: false, error: String(e) };
  }
}

async function saveActiveTab(tabId) {
  const res = await extractFromTab(tabId).catch(() => null);
  if (!res || !res.ok) return { ok: false, error: 'not a listing page' };
  return saveListing(res.listing);
}

chrome.contextMenus.onClicked.addListener(async (info, tab) => {
  if (info.menuItemId === 'flipscout-save' && tab && tab.id) {
    await badge((await saveActiveTab(tab.id)).ok);
  } else if (info.menuItemId === 'flipscout-save-link' && info.linkUrl) {
    await badge((await saveFromUrl(info.linkUrl)).ok);
  }
});

chrome.commands.onCommand.addListener(async (command) => {
  if (command !== 'save-current-listing') return;
  const [tab] = await chrome.tabs.query({ active: true, lastFocusedWindow: true });
  if (!tab || !tab.id) return;
  await badge((await saveActiveTab(tab.id)).ok);
});

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  (async () => {
    try {
      if (msg.action === 'saveListing') sendResponse(await saveListing(msg.listing));
      else if (msg.action === 'saveListings') sendResponse(await saveListings(msg.listings));
      else if (msg.action === 'saveFromUrl') sendResponse(await saveFromUrl(msg.url));
      else if (msg.action === 'undoSave') sendResponse(await undoSave(msg.id));
      else if (msg.action === 'exportBackup') sendResponse(await exportBackup());
      else if (msg.action === 'openUrl') {
        await chrome.tabs.create({ url: msg.url });
        sendResponse({ ok: true });
      } else sendResponse({ ok: false, error: 'unknown action: ' + msg.action });
    } catch (e) {
      sendResponse({ ok: false, error: String(e) });
    }
  })();
  return true; // keep the channel open for the async sendResponse
});

// --- dev bridge: lets the flipscout-inject / flipscout-rate Claude skills read and write
// chrome.storage.local. The dev machine hosts a WS server on 18771; this worker connects out
// to it (the service worker idles after ~30s, so an alarm reconnects it). Local only.
const BRIDGE_URL = 'ws://localhost:18771';
const BRIDGE_KEYS = ['items', 'sellers', 'searches', 'settings'];
let bridge = null;

async function bridgeAction(action, params) {
  if (action === 'ping') return { ok: true };
  if (action === 'get_storage') return chrome.storage.local.get(params.keys || BRIDGE_KEYS);
  if (action === 'inject_storage') {
    const data = {};
    for (const k of Object.keys(params.data || {})) if (BRIDGE_KEYS.includes(k)) data[k] = params.data[k];
    await chrome.storage.local.set(data);
    return { written: Object.keys(data) };
  }
  throw new Error('unknown bridge action: ' + action);
}

function connectBridge() {
  if (bridge && bridge.readyState <= 1) return;
  try {
    bridge = new WebSocket(BRIDGE_URL);
  } catch (e) {
    bridge = null;
    return;
  }
  const ws = bridge;
  ws.onmessage = async (ev) => {
    let msg;
    try { msg = JSON.parse(ev.data); } catch (e) { return; }
    try {
      const data = await bridgeAction(msg.action, msg.params || {});
      ws.send(JSON.stringify({ id: msg.id, success: true, data }));
    } catch (e) {
      ws.send(JSON.stringify({ id: msg.id, success: false, error: String(e) }));
    }
  };
  ws.onclose = () => { if (bridge === ws) bridge = null; };
  ws.onerror = () => {};
}

chrome.alarms.create('flipscout-bridge', { periodInMinutes: 0.5 });
chrome.alarms.onAlarm.addListener((a) => { if (a.name === 'flipscout-bridge') connectBridge(); });
chrome.runtime.onStartup.addListener(connectBridge);
connectBridge();
