<div align="center">

# 📦 FlipScout

### Buy low. Sell high. Keep score.

The offline-first flip tracker for Android. Track items, sellers and profits —
then jump straight into eBay & Facebook Marketplace research in one tap.

**No account. No permissions. No subscriptions. Your data never leaves your phone.**

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)
![Design](https://img.shields.io/badge/Design-Material%203-10B981)
![Min SDK](https://img.shields.io/badge/minSDK-26%20(Android%208.0)-111827)
![Offline](https://img.shields.io/badge/permissions-0-success)
![Tests](https://img.shields.io/badge/domain%20logic-100%25%20unit%20tested-8B5CF6)

</div>

---

## ✨ Why FlipScout?

Most reseller tools want an account, a subscription, and your data in their cloud.
FlipScout is a pocket ledger with a pipeline: it lives entirely on your device,
costs nothing, and gets out of your way. Every screen follows one rule —
**minimum friction**:

- One tap to save an item — **share any eBay / Marketplace listing straight from the app you found it in**, and the title, link and price are parsed for you
- One swipe to move an item through the pipeline — Watching → Bought → Listed → Sold
- One tap to fire a search at eBay, **eBay sold listings** (what things *actually* sell for), or Facebook Marketplace
- One tap to hand off listing copy to ChatGPT or Gemini with a ready-made expert prompt

## 📱 Features

### 📦 Items — your whole flip pipeline

| | |
|---|---|
| 📊 **Hero dashboard** | Total profit (with count-up animation), capital tied up in stock, items sold, average ROI |
| 👆 **Swipe actions** | Swipe right to advance status (with haptic tick), swipe left to delete — with Undo, never a confirm dialog |
| 💰 **Live profit math** | `profit = sell − buy − fees − shipping`, ROI computed live as you type; projected vs. actual depending on status |
| 🔎 **Search, filter, sort** | Full-text across title/notes/tags, status pills with live counts, category & platform filters, 5 sort modes |
| 🖼 **Item details** | Quantity, category (with suggestions from your own history), platform (eBay, FB Marketplace, Etsy, Mercari, Poshmark, Depop), tags, sold-date picker, seller link |
| ↗ **Quick-open** | Jump to any item's listing URL in one tap, straight from the list row |

### 📊 Insights — keep score

- **Monthly profit chart** — 6 months of sold profit as an animated, hand-drawn Canvas bar chart (negative months dip below the line)
- **Profit by category** and **profit by platform** — ranked breakdowns with sold counts, so you learn what actually pays

### 🔍 Search — research in one tap

- **Three one-tap targets**: eBay, **eBay sold** (completed listings only), Facebook Marketplace — the native app opens if installed
- **Keyword library** — pin your best searches into categorised, collapsible groups; every row has instant per-engine chips
- **✨ AI keyword mining** — copy an expert-prompt to ChatGPT/Gemini, and when you come back, **Import from clipboard** parses the AI's JSON reply (tolerant of code fences, prose, any shape) and pins up to 200 keywords into categories, deduped
- **Recent searches** — last 30, deduped, most recent first

### ⭐ Sellers — remember the good sources

- Every seller card shows live stats computed from your items: *N bought · M sold · total profit*
- Favourite the reliable ones; favourites float to the top
- Profile link, notes ("what they sell, prices, how to reach them")

### ✨ Listing assistant

Pick eBay or Facebook Marketplace, tap **ChatGPT** or **Gemini** — FlipScout copies a tuned reseller prompt (title, target price, condition → asks for an 80-char keyword-rich title, description, category, 10 keywords and quick-sale / fair / ambitious pricing) and opens the AI in one step.

### 🔐 Share-sheet intake & data ownership

- **Share text from anywhere** → FlipScout asks "What did you share?" → pre-fills a new item *or* seller (URL, price and title parsed)
- **CSV export/import** (RFC 4180-safe, merges back by id — no duplicates)
- **Full JSON backup** → share sheet → Drive, mail, anywhere
- Everything persists immediately, survives rotation and process death

## 🛡 The FlipScout way

- **Offline-first** — no `INTERNET` permission, zero permissions at all. Searches and AI handoffs are plain links opened by *other* apps.
- **No scraping** — only what the share sheet and public deep links provide. No login walls, no bot-check dodging.
- **Undo, don't confirm** — destructive actions happen instantly with a snackbar undo.
- **Your data** — plain JSON files on your device (`items.json`, `sellers.json`, `searches.json`), atomic writes, ordered off the main thread.

## 🏗 Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.2.0 (JVM target 17) |
| UI | Jetpack Compose (BOM 2025.09), Material 3, edge-to-edge, dark + light "Midnight & Emerald" theme |
| Architecture | MVVM — `StateFlow<UiState>` + one-off event flows, stateless composables |
| Storage | JSON files with atomic write-then-rename; repository interfaces ready for a Room swap |
| Domain | Pure Kotlin, zero Android dependencies — fully JVM unit-testable |
| Extras | **No** extra dependencies: no Room, no navigation lib, no DI framework. Just Compose + lifecycle + JUnit. |

```
app/src/main/java/com/rubixscript/flipscout/
├── data/        Models + JSON repositories (no UI code)
├── domain/      Pure Kotlin: stats, parsers, prompt & URL builders, CSV, JSON reader
├── ui/
│   ├── home/      Items list + hero dashboard
│   ├── insights/  Charts & breakdowns
│   ├── search/    Quick searches + keyword library
│   ├── sellers/   Source tracking
│   ├── editor/    Item editor + listing assistant
│   ├── components/ Shared widgets
│   └── theme/     Palette & shapes
└── util/        Intents, clipboard, formatting
```

All domain logic is covered by 13 JUnit test suites (stats, parsers, CSV, prompts, URL builders, filters).

## 🥊 The competition

FlipScout is deliberately *not* trying to be these apps — but it's honest to know them:

| App | Focus | What they're great at | What they cost |
|---|---|---|---|
| **[FlipScout](#)** | Tracking + research | 100% offline, zero permissions, one-tap AI handoffs, friction-first UX | **Free, forever** |
| [Flippd](https://getflippd.com) | All-in-one reselling | Inventory, sales, fees, expenses & mileage tracking, AI coach | Freemium |
| [FlipAI](https://flipai.app) | AI sourcing | Point camera at an item → instant ID, sold comps, max buy price | Freemium |
| [Flippr](https://play.google.com/store/apps/details?id=com.sawyerhalverson.flipprMobileApp) | AI sourcing | Photo of a whole shelf → identifies *multiple* items at once, eBay comps, barcode scan | Freemium |
| [Vendoo](https://www.vendoo.co) | Crosslisting | Post to 10+ marketplaces from one inventory | ~$20–40/mo |
| [Flipwise](https://selltheflip.com/compare/flipwise) | eBay bookkeeping | Auto-relist, auto-delist, discounting, tax reports | ~$10–20/mo |
| [SellerAmp SAS](https://selleramp.com) | Amazon RA scouting | Barcode scan → fees, profit, restrictions, sales history | Subscription |
| [ScoutIQ](https://www.scoutiq.co) | Book scouting | **Offline** sold-price database — scan books with no signal | Subscription |

The pattern: competitors put their best features behind subscriptions and cloud accounts, and focus on *sourcing* or *crosslisting*. Almost none of them nail the simple, private, instant "keep score" loop — that's FlipScout's lane.

## 💡 The best features FlipScout doesn't have (yet)

Ranked by how much they'd level up the app — a research-backed wishlist:

### 1. 📸 AI photo valuation — *"point, shoot, know"* ⭐ the one to steal
**Seen in:** [FlipAI](https://flipai.app), [Flippr](https://play.google.com/store/apps/details?id=com.sawyerhalverson.flipprMobileApp), FlipIQ, Fliptip
Snap a photo at the thrift store → AI identifies the item → real **sold comps** → estimated profit and a **maximum buy price** ("buy or skip"). This is the killer feature of the whole 2026 reseller wave, and FlipScout already owns the perfect hand-off pattern for it: the listing assistant proves one-tap ChatGPT/Gemini round-trips work. *Planned in the roadmap (P2: sold-comps helper).*

### 2. 🔖 Barcode scanning
**Seen in:** SellerAmp, ScoutIQ, Flippr
Books, games and media all carry barcodes. Scan → instant sold-price lookup. No typing, no photo, no AI tokens. Cheap to build, huge at flea markets and library sales.

### 3. 🔁 Crosslisting
**Seen in:** [Vendoo](https://www.vendoo.co), Nifty, List Perfectly
One inventory, posted to eBay + Mercari + Poshmark + Depop simultaneously, delisted everywhere when it sells. This is a *big* build (and a ToS minefield) — the honest answer is staying a tracker and linking out, but it's the #1 reason people pay $20+/mo today.

### 4. 🧾 Expenses & mileage
**Seen in:** [Flippd](https://getflippd.com), FlipperHelper
Fuel, parking, shipping supplies, thrift-store runs — real flippers have real costs that eat the "profit". A simple expense log + mileage tracker would make the Insights tab tell the *true* story.

### 5. 🤖 Automation — auto-relist, price-drop, aging alerts
**Seen in:** Flipwise
Flipwise auto-relists stale eBay items and applies scheduled discounts. FlipScout's offline angle can't reach eBay's API — but **local aging-stock notifications** ("unsold for 30 days — cut the price?") need no API at all. *Planned (P2).*

### 6. 🧮 Fee & break-even calculator
**Seen in:** SellerAmp, most paid tools
Per-platform fee math (eBay ~13%, Marketplace local = 0%) before you commit. Pure domain logic — a perfect fit for FlipScout's testable-`domain/` style. *Planned (P2).*

### 7. 📈 Price alerts on saved searches
**Seen in:** various paid tools
FlipScout already has saved searches — waking you up when a "psp console" appears under $60 is the natural next step. *Planned (P3).*

### 8. 🧾 Tax-ready export
**Seen in:** Flipwise, Flippd
A yearly summary PDF/CSV with cost basis, proceeds, fees and mileage — the unglamorous feature every full-timer eventually needs. *Planned (P3).*

> **Not on the list on purpose:** cloud sync and accounts. They'd break the thing that makes FlipScout special.

## 🚀 Build & run

```bash
# Run the unit tests
./gradlew.bat :app:testDebugUnitTest

# Install on a connected device
./gradlew.bat :app:installDebug
```

Requires Android Studio, minSdk 26 (Android 8.0)+ device or emulator.

## 🗺 Roadmap

See [`todo.md`](todo.md) for the prioritised backlog — design polish, Room migration,
more platforms, calculators, notifications, and the sold-comps research helper.

---

<div align="center">

**📦 FlipScout** — Buy low. Sell high. Keep score.

Made with Kotlin, Compose, and a dislike of subscription fees.

</div>
