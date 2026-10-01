# Redesign Migration Strategy — Sovereign Ledger UI (`ui-design.pen`)

> Status: **Approved strategy** (2026-09-25). This document is the executable detail of
> **Phase 7 — Redesign** from [`plans/kmp-ui-unification-plan.md`](kmp-ui-unification-plan.md).
>
> Decisions locked with the owner:
> 1. **Navigation**: `androidx.navigation.compose` multiplatform (NavHost in `shared/commonMain`).
> 2. **Data gaps**: build for real — SQLDelight `transactions` table + `/historical` integration.
> 3. **Branding/copy**: adopt the design's "Sovereign Ledger" copy as-is.
> 4. **Tabs**: 3 — **Home · History · Account** (matches design bottom nav).
>
> Prime directive: **business logic is frozen during visual phases.** UI work must never
> modify `IRepository`/`SharedViewModel` behavior; new data features arrive only in their
> own data phase, as *additions* (new methods/tables), never as edits to existing ones.

---

## 1. Ground-truth audit

### 1.1 Design source of truth — `ui-design.pen` (project root)

Single source of truth for all visual work. 8 mobile screens (390pt, dark theme):

| Frame ID | Screen | Generation | Fate in migration |
|---|---|---|---|
| `RbQhR` | Sovereign Market Dashboard | New | **Build** → `HomeScreen` |
| `72Xe2` | Asset Performance & Records | New | **Build** → `HistoryScreen` |
| `qDCZE` | Profile & Settings | New | **Build** → `AccountScreen` |
| `MegbU` | Frequently Asked Questions | New | **Build** → `FaqScreen` (static) |
| `9vaMD` | Support & Information | New | **Build** → `SupportScreen` (static) |
| `0ZYbh` | Contact Support | New | **Build** → `ContactSupportScreen` (static + stub submit) |
| `gbbrU` | User Feedback | New | **Build** → `FeedbackScreen` (static + stub submit) |
| `2XSA3` | Home Page – Rates Display | **Old** (mirrors current app UI: USD base selector, rate cards, graph placeholder) | **Retire** — keep only as business-mapping reference |

Note: the file lives at project **root**, not `design/` (older plan references to
`design/ui-design.pen` / `.fig` are stale — those files no longer exist).

### 1.2 Measured defects in the design file (fix before porting — the pen file is the spec)

| ID | Node | Defect | Fix |
|---|---|---|---|
| D1 | `eXoFx` (Dashboard) | Bottom nav at y=2325 in a 2325px frame → **fully invisible** | Set y = frameH − navH |
| D2 | `gzyss` (Asset Perf) | Nav clipped 14px | Same rule |
| D3 | `6arpw` (Profile) | Nav clipped 4px; avatar edit button `f05sM` clipped 8px | Same rule + widen container |
| D4 | `VQa4S` (Support) | Nav clipped 2px | Same rule |
| D5 | all screens | Nav heights inconsistent: 89 / 101 / 103px | Standardize: **101px** |
| D6 | `jhX42` (Asset Perf) | "USD/BTC" text overflows container ~12px | Widen label / shorten copy |
| D7 | `3P9Fd` (Home Rates, old gen) | Subheading overflows 5px | Widen container |
| D8 | several | "partially clipped" text ≤1px (font rounding) | Cosmetic; ignore unless visible |

### 1.3 Extracted design tokens (from the .pen, measured 2026-09-25)

Colors (hex → role):

| Value | Role |
|---|---|
| `#0e0e0e` `#131313` `#1c1b1b` `#201f1f` | App/canvas backgrounds (layered) |
| `#2a2a2a` `#353534` `#393939` | Card/surface fills (+ alpha variants `80/40/33/1a/0d` for glass layers) |
| `#e5e2e1` `#ffffff` | Text primary (warm white) |
| `#c5c6cd` `#8f9097` | Text secondary / tertiary |
| `#64748b` `#74829d` | Muted labels / dividers-adjacent text |
| `#0d1c32` `#0a192f` `#44474d` | Deep navy panels / info surfaces |
| `#4edea3` | **Green accent** — positive change, live indicators (alpha variants for glows) |
| `#e9c349` | **Gold accent** — sovereign/premium highlights, selected stars |
| `#b9c7e4` `#d6e3ff` | Blue info accents (balance, finance icons) |
| `#ffb4ab` | Error text |
| `#020617` | Overlay/scrim |

Typography (Google fonts available in Compose via embedding or `FontFamily`):

| Font | Weights | Sizes used | Role |
|---|---|---|---|
| **Manrope** | 600/700/800 | 14–48 (dominant: 700/24, 700/20, 800/36) | Headings, section titles |
| **Inter** | 500/600 | 10–20 (dominant: 600/12, 500/11, 600/14) | Body, labels, chips |
| **Liberation Mono** | normal/700 | 10–14 | Ledger numbers, rates, codes |

Spacing rhythm: 4-based scale; cards use 24/32 inner padding, sections 24 side margins,
16–24 gaps. Corner radii: large bento cards ~24, chips/buttons ~12, inputs ~16.

### 1.4 Current code inventory — what must keep working

**Business logic (FROZEN — preserve as-is):**

| Asset | Location | Notes |
|---|---|---|
| `SharedViewModel` | `shared/.../ui/viewmodel/SharedViewModel.kt` | Rates + conversion + swap + base-currency session. Public API is the stability contract. |
| `IRepository` / `CurrencyRepositoryImpl` | `shared/.../domain|data` | Live rates, SQLDelight cache, rate-limited refresh, session currency |
| `CurrencyApi` (Ktor) | `shared/.../data/network` | `/live`, `/convert`, `/historical` + access key |
| `CurrenciesCatalog` | `shared/.../data` | Static currency list |
| `SharedKoin.kt` / `initSharedKoin(serverURL, accessKey)` | `shared/.../di` | DI entry — both platforms |
| `ISessionStorage` (+ platform actuals) | `shared/.../data/storage` | iOS NSUserDefaults / Android SharedPreferences |
| SQLDelight `currencies.sq` | `shared/.../database` | Do not touch existing table |

**UI (replace incrementally):** `ExchangeCurrencyApp` (2-tab state nav), `ConverterCurrencyScreen`,
`RatesScreen`, `SharedBottomNavigation`, `CurrencyPickerSheet`, `theme/*`.

**Legacy Android (`app` module):** `MainActivity` + app-local Compose pages/components/theme.
Android is **not yet wired** to the shared UI. Legacy screens are deleted only in Phase 6
(never before feature parity), per [`agents.md`](../agents.md) §9.2 rule 3.

---

## 2. Target information architecture

```
Bottom nav (3 tabs)
├── HOME  → HomeScreen        (design: RbQhR Sovereign Market Dashboard)
│            ├── Market overview: top pairs + mini charts     [rates + /historical]
│            ├── Quick Exchange card  → converter (existing SharedViewModel logic)
│            ├── Recent history                               [transactions table]
│            └── sub-route: ConverterScreen (full converter, kept as route)
├── HISTORY → HistoryScreen   (design: 72Xe2 Asset Performance & Records)
│            ├── Balance cards (per asset)                    [computed from session+rates]
│            ├── Historical graph with range chips (1W/1M/…)  [/historical]
│            ├── All-time best rate + comparison              [computed + stored]
│            └── Transactions list (History for this pair)    [transactions table]
└── ACCOUNT → AccountScreen   (design: qDCZE Profile & Settings)
             ├── Profile hero (avatar, tier badges)
             ├── Personal info, security stats, preferences, vault actions
             ├── → FaqScreen           (MegbU, static accordion)
             ├── → SupportScreen       (9vaMD, static hub)
             │     ├── → ContactSupportScreen (0ZYbh, form → stub confirmation)
             │     └── → FeedbackScreen       (gbbrU, form → stub confirmation)
             └── → legal list rows (Terms/Privacy/About/Rate/Share — no-ops)
```

### 2.1 Screen ↔ logic mapping (the "keep the logic" contract)

| Design frame | Target file (shared/commonMain) | State source | Data dependency |
|---|---|---|---|
| `RbQhR` | `screens/HomeScreen.kt` + `viewmodel/HomeViewModel.kt` | `HomeViewModel` | existing rates via repository; transactions (Phase 4) |
| Quick-exchange card | `components/QuickExchangeCard.kt` | **`SharedViewModel` (unchanged)** | none new |
| `72Xe2` | `screens/HistoryScreen.kt` + `viewmodel/HistoryViewModel.kt` | `HistoryViewModel` | `/historical` (new repo method, Phase 4); transactions |
| `qDCZE` | `screens/AccountScreen.kt` + `viewmodel/AccountViewModel.kt` | `AccountViewModel` | `ISessionStorage` extensions only |
| `MegbU` | `screens/FaqScreen.kt` | none (static) | none |
| `9vaMD` | `screens/SupportScreen.kt` | none (static) | none |
| `0ZYbh` | `screens/ContactSupportScreen.kt` | local UI state only | none (stub submit) |
| `gbbrU` | `screens/FeedbackScreen.kt` | local UI state only | none (stub submit) |
| `2XSA3` | — | — | retired; its mapping (USD base selector = session currency, rate cards = `getListRatesCurrencies`) is preserved inside Home |

---

## 3. Architecture decisions

### 3.1 Navigation (Navigation-Compose MP)
- `androidx.navigation:navigation-compose` in `shared/commonMain` (KMP-stable in recent versions).
- `NavHost` lives in `shared/ui/ExchangeCurrencyApp.kt`. Routes:
  `home` · `history` · `account` (top-level, bottom bar visible)
  `faq` · `support` · `contact` · `feedback` · `converter` (pushed, bottom bar hidden).
- `SharedDestination` enum + `when(destination)` is **deleted** in Phase 2 once parity is verified.

### 3.2 ViewModels
- Keep `SharedViewModel` exactly as-is (converter/quick-exchange; also the base-currency session writer).
- Add `HomeViewModel`, `HistoryViewModel`, `AccountViewModel` — constructor-injected via Koin,
  created per destination with a `koinViewModel()`-style factory; same KMP-safe
  `StateFlow` pattern as `SharedViewModel` (no platform base class).
- Static screens get **no ViewModel** — pure `@Composable` with local `remember` state.

### 3.3 Data additions (Phase 4, additive only)
- **Models**: `Transaction(id, base, quote, amountBase, amountQuote, rate, timestamp, direction)`,
  `HistoricalRate(date, rate)`.
- **`IRepository` extensions** (new methods on the interface + impl):
  - `getHistoricalRates(base, symbol, from, to): MyResponse<List<HistoricalRate>>` → `CurrencyApi` `/historical`
  - `getTransactionHistory(): List<Transaction>` · `recordTransaction(tx)`
- **SQLDelight**: new `transactions.sq` table + queries; schema version bump;
  destructive migration acceptable at this dev stage; `currencies.sq` untouched.
- **Recording trigger**: on every successful conversion — hook the *success path* of
  `SharedViewModel.convert()` after `loadRates()` succeeds (single-line call into
  `recordTransaction`; the only permitted touch to `SharedViewModel` in the whole migration).
- **All-time best rate**: computed client-side from stored conversion history (per pair);
  needs no new API.

### 3.4 Theming & branding
- Adopt "Sovereign Ledger" copy/wording from the design (§1.1 decision #3).
- Rewrite `shared/ui/theme/Color.kt|Type.kt|Shape.kt` from §1.3 tokens (keep the
  `ExchangeCurrencyAppTheme` entry point so call-sites don't change).
- Fonts: bundle Manrope + Inter (variable) as shared resources; Liberation Mono can be
  replaced by `FontFamily.Monospace` if bundling is a problem — visually equivalent for ledger digits.
- App *display name* (launcher label) is a separate decision — currently
  `Exchange Convert App` in Info.plist / strings.xml. **Open item §6.1.**

### 3.5 Component library (Phase 1 deliverables)
`shared/ui/components/`:

| Component | Notes |
|---|---|
| `BentoCard` | Rounded-24 surface; fill variants (elevated `#2a2a2a`, navy `#0d1c32`) |
| `GlassCard` | Gradient + blur layers (design's glassmorphism); scrim `#020617` |
| `SectionHeader` | Icon tile (40/48) + Manrope title + optional trailing action |
| `LedgerButton` | Primary (green), premium (gold), tonal, ghost variants; 56px height |
| `LedgerInput` | 16-radius field, label + placeholder, optional trailing selector |
| `TopAppBar` | Back arrow + centered title + avatar action (three design variants unified) |
| `LedgerBottomNav` | 3 tabs, active pill highlight, 101px height, flush bottom |
| `Sparkline` | Compose Canvas path, green/red gradient fill (dashboard + history reuse) |
| `AccordionRow` | FAQ expand/collapse item |
| `EmptyState` | Icon + message + optional CTA |

---

## 4. Phased execution plan

Each phase ends with: `:shared:build` + `:shared:iosSimulatorArm64Test` green +
manual smoke on both platforms. Commit per phase; UI-only and data phases never mix.

### Phase 0 — Hygiene & quick wins (no app UI)
1. **iOS logo fix (root cause found)**: `Assets.xcassets` is **not registered in
   `iosApp/ExchangeConvertApp.xcodeproj/project.pbxproj`** — not in the group, and the
   Resources build phase has `files = ()`. The catalog never compiles → blank icon
   (also missing `AccentColor`). Add PBXFileReference + PBXBuildFile for the catalog
   into the Resources phase (or convert the folder to a
   `PBXFileSystemSynchronizedRootGroup`), verify icons land in the bundle.
   > ✅ **Done 2026-09-25** — added `A1000013`/`A2000013` (build file + file reference),
   > wired into the group and Resources phase; flattened `icon-1024.png` alpha
   > (RGBA→RGB, App Store validation); verified via `plutil -lint`, `xcodebuild -list`,
   > and `xcrun actool` (catalog compiles with zero warnings).
2. Fix design-file defects D1–D7 from §1.2 directly in `ui-design.pen` (the file is the spec — it must be accurate).
   > ✅ **Done 2026-09-25** — all 7 bottom navs now flush (bottom gap 0) and standardized to
   > **101px height** (D1–D5); resized navs re-centered via existing `alignItems: center` + padding;
   > `uDLHe` (Feedback nav container) switched to `fill_container` to match the new height;
   > "USD/BTC" chip (D6) repositioned to fit its card. Verified per-screen screenshots
   > (Dashboard + Asset Performance + Feedback) and a full-document clip sweep.
   > **Skipped intentionally (D7 + badge `f05sM`)**: both are geometric-only flags — `clip`
   > defaults to `false` in the schema, nothing is visually cut (D7 text overhangs 5px into
   > empty space; the badge is an intentional corner overhang). D8 sub-pixel rounding ignored.
3. Commit the §1.3 token tables as the design spec of record.

### Phase 1 — Design system in `shared`
- Theme rewrite from tokens; bundle fonts.
- Build §3.5 components; unit-render each in a `@Preview`-style gallery route (dev-only).
- Rebuild bottom nav to 3 destinations (component-level; wiring comes in Phase 2).
- **Touch policy**: theme + new components only; existing screens untouched.
  > ◐ **In progress 2026-09-27** — done: full Ledger palette rewrite (`theme/Color.kt`),
  > bundled Manrope 600/700/800 + Inter 400/500/600 as fontTools-instantiated statics
  > (`composeResources/font/` + OFL licenses), Ledger typography (`Type.kt`), radii
  > (`Shape.kt`), dark-only `LedgerDarkColors` scheme (signature of
  > `ExchangeCurrencyAppTheme` unchanged), `material-icons-extended` dependency,
  > and all §3.5 components: `BentoCard`, `GlassCard`, `SectionHeader`, `LedgerButton`
  > (4 variants), `LedgerInput`, `LedgerTopAppBar`, `LedgerBottomNav` +
  > `LedgerDestination` (3 tabs, unwired), `Sparkline`, `AccordionRow`, `EmptyState`,
  > plus dev-only `ComponentGallery`. Verified: `:shared:compileDebugKotlinAndroid` ✅,
  > `:shared:compileKotlinIosSimulatorArm64` ✅, and the full `:shared:build` gate ✅
  > (2026-09-27 — all 4 targets compiled, 3 iOS frameworks + both AARs assembled).
  > Gradle speedups added (`org.gradle.parallel/caching=true`).

### Phase 2 — Navigation migration (no visual change)
- Introduce `NavHost` + routes `home→(current Converter tab)`, `history→(current Rates tab)`,
  `converter`, plus placeholder routes for the new screens.
- Delete `SharedDestination`/`when` switch. Verify both platforms, no visual regressions.

### Phase 3 — Account cluster (static screens; zero data risk)
- `AccountScreen`, `FaqScreen`, `SupportScreen`, `ContactSupportScreen`, `FeedbackScreen`.
- `ISessionStorage`: add preference keys (notifications toggle, default pair) — additive only.
- Forms: local success state only (no backend endpoint exists — open item §6.3).

### Phase 4 — History cluster (the one data phase)
- Models, `transactions.sq` + queries, `IRepository` extensions (§3.3), conversion
  recording hook in `SharedViewModel.convert()` success path.
- `HistoryViewModel` + `HistoryScreen`: balance cards, `/historical` chart with range
  chips (Canvas), best-rate comparison card, transactions list.
- `commonTest`: repository + VM tests for the new surface.

### Phase 5 — Home / Dashboard
- `HomeViewModel` + `HomeScreen`: top pairs + sparklines (from short-window `/historical`
  or live deltas), Quick Exchange card wired to the **existing** `SharedViewModel`,
  recent transactions from Phase 4 table.
- Retire `RatesScreen` + old-generation mapping.

### Phase 6 — Android wiring & legacy retirement
- `MainActivity` hosts `ExchangeCurrencyApp`; `ExchangeApplication` calls `initSharedKoin`
  (note §14 known issue: Android actuals need `Context` for session storage / DB driver).
- Delete `app` module local pages/components/theme once parity is verified.
- Separate follow-up decision: retire `domain`/`core`/`database` modules entirely
  (§9.2 rule 3 says don't delete yet — keep that decision isolated).

### Phase 7 — QA vs design
- Per-screen checklist against `ui-design.pen` (spacing, type scale, colors, states).
- Screenshot comparison (design export vs device screenshots).
- Full regression: both platforms, dark theme only (design is dark-only — open item §6.4).

---

## 5. Guardrails (the "keep business logic" rules)

1. **Additive-only**: new repository methods, new tables, new ViewModels. No edits to
   existing behavior — the single exception is the conversion-recording hook (§3.3).
2. **Public API freeze**: `IRepository` and `SharedViewModel` signatures that exist today
   must not change shape during UI phases.
3. **Phase isolation**: a commit either touches `ui/**` or `data|domain/**`, never both
   (except the Phase 4 hook).
4. **No hardcoded hex/strings in screens** — everything through theme tokens and a
   `LedgerStrings` file (copy from the design).
5. **Compile+test gate after every phase**; smoke test both platforms before commit.
6. **The .pen file is the spec** — when code and design disagree, fix design defects
   first (D1–D8), then implement; never silently "improve" the design in code.

---

## 6. Open items / decisions needed

0. **Guest mode / login** (added 2026-10-01): Profile should support a guest mode
   (browse + convert without an account) alongside a future login. Decision parked —
   affects AccountScreen persona fields, transaction ledger ownership, and backend
   auth. Do NOT build accounts before this is decided.

1. **App display name**: keep `Exchange Convert App` (launcher) while in-app copy says
   "Sovereign Ledger", or rename everywhere? (Info.plist `CFBundleDisplayName`, Android label, store listings.)
   Brand logo added 2026-10-01 (`design/logo.svg` — abstract gold rate-line mark): in-app top bars,
   Android adaptive icon + splash, full iOS AppIcon set, landing header (needs `exchange-api` deploy ≥ v0.4.3-dev.17).
   Launcher *label* still undecided.
2. **Conversion recording**: record *every* conversion (recommend yes) and whether
   failed/offline conversions are recorded (recommend no).
3. **Contact/Feedback submit**: no backend endpoint exists in `exchange-api` — ship with
   local confirmation stub now, wire later (matches design intent).
4. **Light theme**: design is dark-only. Decision: dark-only v1 (theme token structure
   supports adding a light palette later).
5. **Flag icons**: design uses initial-circles, not flag images — adopt initials
   (no image downloads), consistent with `CurrenciesCatalog`.
