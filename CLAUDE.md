# FlipScout

Android app (Kotlin, Jetpack Compose, Material 3) for tracking resale flips: items, sellers, and quick eBay / Facebook Marketplace searches.

## Product rule: always minimise user friction
Every feature and every change should take the fewest taps, the least typing, and the least screen space possible.
Before finishing any UI work, check it against these:

- **Fewest taps.** If an action can happen in one tap, it must. Never make the user paste into a box when the app can read the clipboard for them (one-tap "Import from clipboard").
- **Prefer undo over confirm.** Don't add "Are you sure?" dialogs. Do the action and show a snackbar with Undo.
- **Secondary features live behind a button, not on the main screen.** Don't spend permanent screen space on things used occasionally (use a bottom sheet or menu).
- **Long lists must be compact.** Use collapsible sections (collapsed by default), compact single-line rows, and counts, so the user scans instead of scrolls. Group related items.
- **Pre-fill everything we can.** Parse shared text for link, title and price; remember the last choice; default sensibly; keep optional fields optional.
- **Don't lose the user's work.** Persist immediately (no Save-to-keep), survive rotation and process death (`rememberSaveable`, `ViewModel`).
- **No dead ends.** Disabled buttons need an obvious reason (e.g. empty field); empty states say what to do next; failures say how to fix them.
- **Hand off to other apps in one step.** Copy what's needed to the clipboard, say so in a toast, and open the target app directly.
- **Look good.** Every screen should feel polished and consistent with the theme (animations, rounded cards, clear hierarchy).

## Architecture (package `com.rubixscript.flipscout`)
- `data/`: models, repository interfaces, JSON-file implementations (`JsonListStore`). No UI code.
- `domain/`: pure Kotlin logic (stats, parsers, prompt builders, URL builders). Must be unit-testable on the JVM: no `android.*` and no `org.json` here (android.jar's `org.json` is a stub in unit tests; use `JsonReader.kt`).
- `ui/<feature>/`: a screen plus its `ViewModel` and `UiState` (`home`, `sellers`, `search`, `editor`); `ui/components/` shared widgets; `ui/theme/` palette and theme.
- `util/`: small Android helpers (intents, clipboard, formatting).
- ViewModels expose `StateFlow<UiState>` and one-off `events` (snackbars); screens collect with `collectAsStateWithLifecycle`. Stateless composables take state and lambdas.
- Persist through a conflated save queue (see `HomeViewModel`) so writes are ordered and off the main thread.

## Conventions
- All user-facing strings go in `res/values/strings.xml` (escape apostrophes as `\'`, use `\n` for line breaks).
- Add unit tests for every new `domain/` function and keep them passing.
- Don't add Gradle dependencies casually: this machine's network can fail SSL when resolving new artifacts. Prefer the standard library / existing dependencies.
- Don't bypass login walls or bot checks to scrape Facebook/eBay. Only use what the share sheet or public deep links provide.

## Commands
- Unit tests: `./gradlew.bat :app:testDebugUnitTest`
- Install on device: `./gradlew.bat :app:installDebug`
