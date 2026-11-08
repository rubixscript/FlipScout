# FlipScout Chrome Extension — Remaining Work

Browser companion for the FlipScout Android app: one-tap capture of eBay / Facebook Marketplace
listings while browsing, tracked in the browser, syncing to the phone via the app's own backup format.

**House pattern** (matches SeoScout / RedScout / SocialKatExtension — verified 2026-11-06):
Manifest V3, React popup in a CRA-style repo, extension shell in `public/`, message-driven content
scripts with **no injected page UI**, `chrome.storage.local` as the single source of truth, no backend, no auth.
Completed groundwork (scaffold, manifest, extractor, popup capture, storage model, export, context
menus + link/command capture) is documented in `README.md`; this file tracks what's left.

---

## Popup features

- [ ] Quick actions per item: eBay search, eBay **sold** search, Marketplace search — same URLs as `SearchLinks.kt` (`searchUrl` is already ported in `src/utils/logic.js`)
- [ ] Copy listing prompt to clipboard (port `ListingPrompt.kt` — `listingPrompt` already in `src/utils/logic.js`) + "Open ChatGPT/Gemini" one-step hand-off, like SocialKat's prompt URLs
- [ ] Settings behind a menu, not on the main screen: default buy/sell behavior, export, import
- [ ] Status pipeline: advance one stage per tap from the popup (`WATCHING | BOUGHT | LISTED | SOLD`, `nextStatus` already in logic.js)

## Sync with the Android app

- [ ] **Import**: file picker reads an app-exported `flipscout-backup.json`, merges by `id` (read-merge-write; never blind overwrite)
- [ ] Clipboard path as a fallback: copy the backup JSON to clipboard for paste-import into the app
- [ ] Later (optional): QR code in the popup / scan on Android, or a deep link `flipscout://import?...`

## Live tests (need a real browser session — headless Chrome gets bot-error pages on eBay)

- [ ] eBay item page extract against the live DOM (selectors fixture-verified only — og:title strip, JSON-LD offers, `.x-price-primary`, `/usr/` seller link)
- [ ] Right-click a listing link on a non-listing page (eBay search results) → saved with full title/price/seller; background tab opens and closes itself
- [ ] Right-click a link with the listing already open in another tab → reuses the tab, no second tab opened
- [ ] `Alt+Shift+S` on a listing page → badge ✓ → item saved as WATCHING; on a non-listing page → badge !
- [ ] Popup paste box: clipboard paste → Track → saved card with photo; bad link → inline hint, nothing saved
- [ ] Feed scan: browse a Marketplace feed, scroll a screen or two → popup **Scan** → modal lists every card seen (with scontent photos + prices), already-tracked rows tagged and locked → Add N → items appear as WATCHING, toast Undo removes the batch
- [ ] Feed scan edge cases: scan with no Marketplace tab open (button disabled + hint); empty viewport-cached feed; rescanning after adding (previously added rows show as Tracked); thousands-separator price (`US $1,234.56`) not truncated
- [ ] FB Marketplace item saved via link-capture **has a photo** — logged-in pages often lack `og:image`, so the scontent-CDN fallback in `content.js` (`marketplacePhoto`) must fire
- [ ] FB Marketplace link while logged out of Facebook → no save, badge ! (login wall not bypassed)
- [ ] Fullscreen: ⛶ in the popup header opens `fullscreen.html` as a full-width dashboard; items/sellers stay in live sync with the popup (storage `onChanged`)
- [ ] Export → Android app import shows the items (round trip both directions) — needs a manual phone test
- [ ] App export → extension import merges without losing extension-only items — blocked on import above

## Later / optional

- [ ] MCP dev bridge on **ws://localhost:18770** (port registry: SignalScout 18766, SocialKat 18767, SeoScout 18768, RedScout 18769) — only if `vedai-agent-platform` tooling should drive it
- [ ] Chrome Web Store publish via the vedai CWS service (`.claude/tools/chrome-store`), after the permissions table is clean
- [ ] Price-history snapshots per saved listing (sold-price comps for the same search)
- [ ] International eBay domains (ebay.de etc.) — add per-domain host permissions when requested

---

### Sibling reference map

| Extension | Path | Pattern worth copying |
|---|---|---|
| RedScout | `Documents\ReactWS\RedScout` | post-build script, message-driven scraper w/ `chrome.scripting` fallback |
| SeoScout | `Documents\ReactWS\SeoScout` | no persistent content script — one-shot `executeScript` + `fetchUrl` router, `useStorage` hook |
| SocialKat | `Documents\ReactWS\SocialKatApps\SocialKatExtension` | context-menu save, clipboard + AI-prompt hand-off, permissions docs |
| vedai platform | `Documents\PythonWS\vedai-agent-platform` | control plane only: skills, MCP bridge docs, CWS publish service (no extension code) |
