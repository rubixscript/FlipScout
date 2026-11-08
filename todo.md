# FlipScout TODO

> Design principle: **every screen should look super aesthetic.** Polished, modern, delightful. Treat UI quality as a requirement on every task below, not a later phase.

Sorted by priority (P1 = do next, P2 = soon, P3 = later). Area tags: 🎨 UI · 🏪 Sellers · 🔍 Search · 📦 Data · 🔎 Browse · 📊 Insights · ✨ Listing · ⚙️ Settings · 🧱 Code

- [ ] **P1** 🎨 Item photos with rounded thumbnails (replace the letter avatar), shimmer placeholders

- [ ] **P2** 🎨 Polished dark + light themes (true dark/AMOLED option), Material You dynamic color toggle
- [ ] **P2** 🎨 Bottom navigation (Items / Insights / Settings) with animated indicator
- [ ] **P2** 🎨 Consistent iconography (Material Symbols), no emoji-as-icons
- [ ] **P2** 🎨 Custom illustrated empty states and onboarding screens
- [ ] **P2** 🎨 Profit/loss color language and subtle micro-interactions (confetti on a profitable sale)
- [ ] **P2** 🏪 Replace emoji tab labels with proper icons (needs `material-icons-extended`)
- [ ] **P2** 🏪 Search/filter sellers; open seller profile from an item
- [ ] **P2** 🔍 "Start search from item title" shortcut in the item editor
- [ ] **P2** 🔍 Offer to import automatically when the clipboard holds an AI keyword reply on return to the app
- [ ] **P2** 🔍 Search filters (max price, location/radius, condition)
- [ ] **P2** 📦 Automatic backup/restore (review `allowBackup` behavior)
- [ ] **P2** 📦 Migrate JSON file to Room database
- [ ] **P2** 🔎 Aging-stock reminders and notifications (item unsold for N days)
- [ ] **P2** 📊 Break-even calculator and price/fee calculators per platform (eBay, Marketplace, etc.)
- [ ] **P2** 📊 Best/worst flips, average days-to-sell, sell-through rate
- [ ] **P2** ✨ Persist platform choice; add more platforms (Etsy, Mercari, Poshmark, Depop)
- [ ] **P2** ✨ Sold-comps / price research helper
- [ ] **P2** ⚙️ Settings screen: currency, default fees per platform, theme
- [ ] **P2** ⚙️ Multi-currency (stop hardcoding `$`)
- [ ] **P2** 🧱 ViewModel tests (add `kotlinx-coroutines-test` + fake repository)
- [ ] **P2** 🧱 Release setup: signing, ProGuard rules, Play Store listing assets

- [ ] **P3** 🎨 Splash screen and polished adaptive launcher icon
- [ ] **P3** 🎨 Edge-to-edge and large-screen/tablet layouts
- [ ] **P3** 🏪 Seller price rating and "last bought from" date
- [ ] **P3** 🔍 Rename/merge/delete whole categories; reorder
- [ ] **P3** 🔍 Export keyword library as JSON (share between devices)
- [ ] **P3** 🔍 Price alerts for saved searches
- [ ] **P3** 📦 Optional cloud sync
- [ ] **P3** 🔎 Duplicate item, bulk select/actions
- [ ] **P3** 📊 Goals (monthly profit target) with progress ring
- [ ] **P3** 📊 Tax-friendly summary/report export
- [ ] **P3** ✨ Attach/share item photos straight into the AI app
- [ ] **P3** ✨ Prompt templates per category
- [ ] **P3** ✨ Optional direct AI API integration (no copy/paste)
- [ ] **P3** ⚙️ Localization (i18n)
- [ ] **P3** 🧱 Compose UI tests and screenshot tests
- [ ] **P3** 🧱 Crash reporting and basic analytics
- [ ] **P3** 🧱 Hilt/Koin DI once there is more than one repository
- [ ] **P3** 🧱 Split into Gradle modules (`:core:data`, `:feature:home`) if the app grows
