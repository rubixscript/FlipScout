// FlipScout content script — passive and message-driven (house pattern: RedScout / SocialKat).
// The popup or background asks; this answers. It never renders UI into the page and only
// reads what the logged-in page already shows the user (no login-wall or bot-check bypass).

// Same price shape the Android app parses (SharedListingParser.kt): "US $1,234.56" -> 1234.56.
const PRICE_RE = /(?:US\s?|USD\s?)?\$\s?(\d{1,3}(?:,\d{3})+|\d+)(\.\d{1,2})?/;
function parsePrice(value) {
  if (typeof value === 'number') return value;
  const m = PRICE_RE.exec(String(value || ''));
  return m ? parseFloat(m[1].replace(/,/g, '') + (m[2] || '')) : 0;
}

function metaContent(name) {
  const el = document.querySelector(`meta[property="${name}"], meta[name="${name}"]`);
  return (el && el.content ? el.content : '').trim();
}

function text(sel) {
  const el = document.querySelector(sel);
  return el ? el.textContent.trim() : '';
}

// Finds a schema.org Product in any ld+json block (handles @graph and arrays).
function jsonLdProduct() {
  for (const script of document.querySelectorAll('script[type="application/ld+json"]')) {
    try {
      const stack = [JSON.parse(script.textContent)];
      while (stack.length) {
        const node = stack.pop();
        if (!node || typeof node !== 'object') continue;
        const types = [].concat(node['@type'] || []);
        if (types.includes('Product')) return node;
        if (node['@graph']) stack.push(...[].concat(node['@graph']));
        if (Array.isArray(node)) stack.push(...node);
      }
    } catch (e) {
      // malformed ld+json — fall through to DOM selectors
    }
  }
  return null;
}

function firstOffer(product) {
  const offers = product && product.offers;
  return Array.isArray(offers) ? offers[0] : offers;
}

// JSON-LD image can be a string, an array, or an ImageObject.
function jsonLdImage(product) {
  const image = product && product.image;
  const first = Array.isArray(image) ? image[0] : image;
  if (!first) return '';
  return typeof first === 'string' ? first : first.url || '';
}

// eBay photo: og:image is server-rendered, JSON-LD and the gallery img as fallbacks.
function ebayPhoto(product) {
  return (
    metaContent('og:image') ||
    jsonLdImage(product) ||
    (document.querySelector('img[src*="i.ebayimg.com"]') || {}).src ||
    ''
  );
}

// The logged-in Marketplace SPA often has no og:image (Facebook only emits full og tags
// to logged-out crawlers) — fall back to the first large photo from Facebook's CDN,
// skipping avatars (/sNNNxNNN/) and UI sprites (rsrc.php).
function marketplacePhoto() {
  for (const img of document.querySelectorAll('img')) {
    const src = img.currentSrc || img.src || '';
    if (!/scontent[\w.-]*\.fbcdn\.net/.test(src)) continue;
    if (/\/s\d+x\d+\//.test(src) || src.includes('/rsrc.php')) continue;
    const w = img.naturalWidth || img.width;
    if (w && w < 200) continue;
    return src;
  }
  return '';
}

function canonicalUrl() {
  // Query/hash stripped and no trailing slash, so the same listing always dedupes to one
  // link whether captured from its page, a shared link or a feed scan.
  return location.href.split(/[?#]/)[0].replace(/\/+$/, '');
}

function extractEbay() {
  const product = jsonLdProduct();
  const offer = firstOffer(product) || {};
  const title = (
    metaContent('og:title') ||
    (product && product.name) ||
    text('h1.x-item-title-label')
  ).replace(/\s*\|\s*eBay\s*$/i, '');
  const price = parsePrice(
    offer.price !== undefined ? offer.price : text('.x-price-primary .ux-textspans') || text('[itemprop="price"]')
  );
  const sellerLink = document.querySelector('a[href*="/usr/"]');
  return {
    platform: 'EBAY',
    url: canonicalUrl(),
    title,
    price,
    currency: offer.priceCurrency || 'USD',
    sellerName: sellerLink ? sellerLink.textContent.trim() : '',
    sellerUrl: sellerLink ? new URL(sellerLink.href, location.href).href : '',
    photoUrl: ebayPhoto(product),
  };
}

function extractMarketplace() {
  const ogTitle = metaContent('og:title').replace(/\s*\|\s*(Facebook|Marketplace)\s*$/i, '');
  const title = text('h1 span') || ogTitle;
  const price = parsePrice(text('[data-testid="marketplace_pdu_price"]')) ||
    parsePrice(metaContent('og:description'));
  const sellerLink = document.querySelector('a[href*="/marketplace/profile/"]');
  const photo = metaContent('og:image');
  // The profile anchor usually carries the name in aria-label, an img alt, or its text.
  const sellerName = sellerLink
    ? (sellerLink.getAttribute('aria-label') ||
       (sellerLink.querySelector('img') && sellerLink.querySelector('img').alt) ||
       sellerLink.textContent || '').trim()
    : '';
  return {
    platform: 'MARKETPLACE',
    url: canonicalUrl(),
    title,
    price,
    currency: 'USD',
    sellerName,
    sellerUrl: sellerLink ? new URL(sellerLink.href, location.href).href : '',
    photoUrl: (photo && photo.startsWith('http') ? photo : '') || marketplacePhoto(),
  };
}

// ==========================================
// Bulk feed scanning (Marketplace grid today; eBay search later — see SCANNERS)
// ==========================================

// A whole-card price segment from a card's aria-label / spans: "CA$450", "US $1,234.56", "£30".
const CARD_PRICE_RE = /^(?:C\$|CA\$|A\$|NZ\$|US\s?\$|USD\s?\$|\$|£|€)\s?[\d,]+(\.\d{1,2})?$/;

function cardCurrency(raw) {
  if (/^CA?\$/.test(raw)) return 'CAD';
  if (/^A\$$/.test(raw)) return 'AUD';
  if (/^NZ\$/.test(raw)) return 'NZD';
  if (raw.includes('£')) return 'GBP';
  if (raw.includes('€')) return 'EUR';
  return 'USD';
}

// One Marketplace grid card: a plain anchor whose aria-label carries everything —
// "Title, CA$450, City, ON, listing 123456". Titles can contain commas and even $ amounts,
// so the split point is the first segment that is *purely* a price.
function marketplaceCard(a) {
  const label = a.getAttribute('aria-label') || '';
  const idMatch = label.match(/listing (\d+)\s*$/) || (a.getAttribute('href') || '').match(/\/marketplace\/item\/(\d+)/);
  if (!idMatch) return null;
  const url = 'https://www.facebook.com/marketplace/item/' + idMatch[1];

  let title = '';
  let priceText = '';
  // Split on commas that don't sit inside a number, so "US $1,234.56" survives intact.
  const segs = label.split(/,(?!\d)/).map((s) => s.trim());
  const priceIdx = segs.findIndex((s) => CARD_PRICE_RE.test(s));
  if (priceIdx > 0) {
    title = segs.slice(0, priceIdx).join(', ');
    priceText = segs[priceIdx];
  } else {
    // aria-label missing or unparsed — fall back to the card's text spans.
    const spans = [...a.querySelectorAll('span[dir="auto"]')].map((el) => el.textContent.trim());
    priceText = spans.find((s) => CARD_PRICE_RE.test(s)) || '';
    title = spans.find((s) => s && !CARD_PRICE_RE.test(s) && s !== 'Just listed') || '';
  }
  if (!title && !priceText) return null;

  const img = a.querySelector('img');
  return {
    platform: 'MARKETPLACE',
    url,
    title,
    price: parsePrice(priceText),
    priceText,
    currency: priceText ? cardCurrency(priceText) : 'USD',
    sellerName: '',
    sellerUrl: '',
    photoUrl: img && img.src && img.src.startsWith('https') ? img.src : '',
  };
}

// Scans every listing card currently in the DOM.
function scanMarketplaceFeed() {
  const seen = new Map();
  for (const a of document.querySelectorAll('a[href*="/marketplace/item/"]')) {
    const card = marketplaceCard(a);
    if (card) seen.set(card.url, card);
  }
  return [...seen.values()];
}

// Accumulating feed cache (RedScout pattern). Facebook's grid is virtualized — cards the
// user scrolls past are unmounted long before they click Scan — so a quiet MutationObserver
// keeps every card as it appears and collect() returns everything seen this session.
function makeWatcher(scan) {
  const cache = new Map();
  let observer = null;
  let timer = null;
  const record = () => {
    timer = null;
    for (const card of scan()) cache.set(card.url, card);
  };
  return {
    ensure() {
      if (observer || !document.body) return;
      observer = new MutationObserver(() => {
        if (!timer) timer = setTimeout(record, 400); // one pass per DOM burst
      });
      observer.observe(document.body, { childList: true, subtree: true });
      record();
    },
    collect() {
      this.ensure();
      record(); // synchronous pass so a first-ever Scan still sees the viewport
      return [...cache.values()];
    },
  };
}

// ==========================================
// Per-platform registry — one entry per site; popup and background stay site-agnostic.
// Adding eBay bulk capture later = write scanEbaySearch (search-grid cards), set it here
// with isList for /sch/ + makeWatcher(scanEbaySearch), and widen the eBay match in manifest.json.
// ==========================================
const SCANNERS = [
  {
    key: 'EBAY',
    host: /(^|\.)ebay\.com$/,
    isItem: (path) => /\/itm\//.test(path),
    isList: () => false,
    extract: extractEbay,
    watcher: null,
  },
  {
    key: 'MARKETPLACE',
    host: /(^|\.)facebook\.com$/,
    isItem: (path) => /\/marketplace\/item\//.test(path),
    isList: (path) => /^\/marketplace(?!\/item\/)/.test(path),
    extract: extractMarketplace,
    watcher: makeWatcher(scanMarketplaceFeed),
  },
];

const ACTIVE = SCANNERS.find((s) => s.host.test(location.hostname)) || null;

// Start accumulating as soon as the script loads on a host that supports scanning, so the
// cache is warm even after an SPA route change from an item page into the feed.
if (ACTIVE && ACTIVE.watcher) ACTIVE.watcher.ensure();

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  if (msg.action === 'ping') {
    sendResponse({
      ok: true,
      platform: ACTIVE && ACTIVE.key,
      item: !!(ACTIVE && ACTIVE.isItem(location.pathname)),
      list: !!(ACTIVE && ACTIVE.watcher && ACTIVE.isList(location.pathname)),
    });
    return;
  }
  if (msg.action === 'extractListing') {
    if (!ACTIVE || !ACTIVE.isItem(location.pathname)) {
      sendResponse({ ok: false, reason: 'not a listing page' });
      return;
    }
    const listing = ACTIVE.extract();
    sendResponse({ ok: !!listing.url && !!listing.title, listing });
    return;
  }
  if (msg.action === 'scanListings') {
    if (!ACTIVE || !ACTIVE.watcher || !ACTIVE.isList(location.pathname)) {
      sendResponse({ ok: false, reason: 'not a scannable page' });
      return;
    }
    sendResponse({ ok: true, listings: ACTIVE.watcher.collect() });
    return;
  }
});
