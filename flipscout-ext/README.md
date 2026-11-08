# FlipScout Extension (`flipscout-ext`)

Browser companion for the FlipScout Android app: one-tap capture of eBay / Facebook
Marketplace listings while browsing, stored locally in the browser and exportable to the
phone via the app's own backup format.

House pattern (same as SeoScout / RedScout / SocialKatExtension): Manifest V3, React
popup, extension shell in `public/`, message-driven content script with no injected page
UI, `chrome.storage.local` as the single source of truth, no backend.

## Install (development)

```
npm install
npm run icons   # regenerates public/icons/*.png (only needed if the design changes)
npm run build
```

Then in Chrome: `chrome://extensions` → Developer mode → **Load unpacked** → select the
`build/` folder.

## Structure

| Path | Role |
|---|---|
| `public/manifest.json` | MV3 manifest — narrow hosts (eBay + Facebook Marketplace) |
| `public/content.js` | Passive extractor on listing pages (`ping`, `extractListing`) + accumulating feed scanner (`scanListings`) |
| `public/background.js` | Message router; saves items/sellers (single + bulk); context menus (page + link); keyboard command; backup export |
| `public/fullscreen.html` | Full-page dashboard (SocialKat pattern) — same bundle, opened from the popup's ⛶ button |
| `src/` | React popup UI (capture card, tracked list, export) |
| `scripts/post-build.js` | Copies shell into `build/`, fixes CRA asset paths, injects the bundle into `fullscreen.html` |
| `scripts/make-icons.js` | Zero-dependency PNG icon generator |

## Capturing a listing

Four ways in, all one action, all sharing one save path (dedupe by link, seller auto-created):

- **Popup button** — open the popup on an eBay / Marketplace listing → "Track this flip".
- **Right-click on the listing page** — "Save listing to FlipScout".
- **Right-click on a listing link anywhere** (search results, chats, forums) — "Save this listing to FlipScout". If a tab with that listing is already open it is reused; otherwise the link opens quietly in a background tab, the content script reads what the logged-in page shows, and the tab closes. A page that won't render (login wall, bot check) fails extraction — nothing is bypassed.
- **Keyboard** — `Alt+Shift+S` saves the open listing; the toolbar icon badges ✓ (green) or ! (red).

The popup also takes a **pasted link** (empty state): one-tap clipboard paste → Track, using the same background flow as the right-click-on-link menu.

## Scanning a whole feed

**Scan** (header, next to Import) captures a Facebook Marketplace feed in bulk. The content
script quietly accumulates every listing card as you browse (RedScout-style watcher — the
virtualized grid unmounts cards as you scroll, so a one-shot scan would miss them), and
Scan opens a review modal: photo, title and price per card, already-tracked ones tagged
and locked off, All / None shortcuts, and nothing written until you click **Add N**.
The saved batch gets one Undo in the toast. The same listings dedupe by link against every
other capture path. eBay search pages can be added by dropping a `scanEbaySearch` into the
`SCANNERS` registry in `public/content.js`.

## Data + sync

Items and sellers are stored byte-compatible with the Android app's JSON (`Item.kt` /
`Seller.kt`), plus one extension-only `photoUrl` field the app ignores. **Export backup**
downloads `{app:"FlipScout", version:1, exportedAt, items, sellers}` — exactly what the
app's Settings → import already accepts, so no Android changes are needed.

See `CHECKLIST.md` for the build plan and what's verified so far.

## Permissions rationale

| Permission | Why |
|---|---|
| `activeTab`, `tabs` | Read the active tab's URL / message its content script |
| `storage` | Items and sellers in `chrome.storage.local` |
| `scripting` | Reserved for the executeScript fallback on listing pages |
| `clipboardWrite` | Copy listing data / prompts to the clipboard |
| `clipboardRead` | One-tap paste of a listing link / backup JSON into the popup |
| `contextMenus` | "Save listing to FlipScout" on listing pages **and on listing links anywhere** |
| `downloads` | Save the backup JSON for the Android app |
| host: `https://*.ebay.com/*`, `https://www.facebook.com/*` | Run the extractor on eBay item pages and Facebook Marketplace (item pages + feed scan); only content the logged-in page already shows is read |
| command: `Alt+Shift+S` | Save the open listing without opening the popup |
