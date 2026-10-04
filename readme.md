# Sovereign Ledger

> Live exchange rates with a private, on-device conversion ledger.
> Android + iOS from one Kotlin Multiplatform codebase.

A currency converter redesigned as a "sovereign" financial instrument: check
live rates, convert with the full converter, and keep a precise ledger of every
exchange — all without an account. Built Kotlin-first with Compose
Multiplatform sharing everything (domain, data, UI) between platforms.

![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS-4edea3)
![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-e9c349)
![Store ID](https://img.shields.io/badge/applicationId-com.sovereignledger.app-b9c7e4)

## What's inside

| Area | Highlights |
|---|---|
| **Dashboard (Home)** | Market overview with 7-day sparklines & change badges, Quick Exchange, recent activity from your ledger |
| **History** | 30-day charts (1W/1M/3M/1Y/ALL), all-time best rate with comparison bar, per-pair transaction list |
| **Account (guest mode)** | Display-name profile, hourly data plan, notifications preference, Clear Local Ledger — login ships soon (Sovereign tier → realtime rates) |
| **Converter** | Exchange Rates page + full converter with swap; every executed conversion is recorded |
| **Support cluster** | FAQ with live search, contact & feedback forms (local success state) |
| **Backend** | Own open-source [exchange-api](https://github.com/liodali/OpenExchangeRate) — provider waterfall, Postgres cache, flags, metrics |

**Privacy posture:** no accounts, no tracking, no ads. The conversion ledger and
preferences never leave the device. See the
[privacy policy](https://api.exchange.dev.adetify.com/privacy).

## Brand

The mark — three ascending rate bars on a baseline (`design/logo.svg`) — drives
the adaptive launcher icon, the in-app top bars, the iOS icon set, and the
[store assets](docs/store/) (feature graphic + showcase images, generated from
the bundled Manrope/Inter fonts).

## Architecture

```
┌─────────────────────────┐   ┌──────────────────────────┐
│ app (Android host)      │   │ iosApp (SwiftUI shell)   │
│ MainActivity + Koin     │   │ SwiftPM → shared framework│
└───────────┬─────────────┘   └────────────┬─────────────┘
            └────────────┬─────────────────┘
                  ┌──────▼───────────────────────────┐
                  │ shared (Kotlin Multiplatform)     │
                  │  domain/   models + repository    │
                  │  data/     Ktor client, SQLDelight│
                  │            (rates + transactions) │
                  │  ui/       Compose MP screens,    │
                  │            Ledger design system    │
                  └──────┬───────────────────────────┘
                         │ HTTPS
                ┌────────▼─────────┐
                │ exchange-api     │  provider waterfall → Postgres cache
                │ (own backend)    │  /latest /historic /currencies /flags
                └──────────────────┘
```

- **UI:** Compose Multiplatform (dark-only "Sovereign Ledger" design system,
  tokens in `shared/ui/theme`), Navigation-Compose MP
- **DI:** Koin multiplatform (`shared/di/SharedKoin.kt`)
- **Data:** Ktor client + SQLDelight (`currencies.sq`, `transactions.sq` + `1.sqm`)
- **Data tiers:** `DataTier.GUEST` (hourly refresh) / `SOVEREIGN` (realtime,
  with login) gated in `CurrencyRepositoryImpl`
- **Legacy modules** (`domain`, `core`, `database`) are deprecated and kept only
  until final retirement — see `agents.md` §9.2

## Build & run

```bash
# Android (debug installs as com.sovereignledger.app.debug)
./gradlew :app:assembleDebug
./gradlew :app:installDebug

# iOS — open the Xcode project (scheme pre-builds the shared framework)
open iosApp/ExchangeConvertApp.xcodeproj

# Tests (commonTest: dates, ledger math, ViewModels, data tiers)
./gradlew :shared:testDebugUnitTest
./gradlew :shared:iosSimulatorArm64Test
```

The app talks to our own backend by default
(`api.exchange.dev.adetify.com`, set in `SharedKoin.kt`).

## Pipelines

| Target | Flow |
|---|---|
| **Android → Play** | tag `app-vX.Y.Z+BUILD[-dev]` (version + build from the tag) → GitHub Actions → signed AAB → internal testing ([guide](docs/play-release.md)) |
| **Backend** | tag `vX.Y.Z` in `exchange-api` → Actions → Gitea registry → Jenkins blue/green on the VPS |

## Documentation

- [`agents.md`](agents.md) — full project map & conventions
- [`plans/redesign-migration-strategy.md`](plans/redesign-migration-strategy.md) — the Sovereign Ledger redesign (phases, guardrails)
- [`docs/play-release.md`](docs/play-release.md) · [`docs/play-listing.md`](docs/play-listing.md) — store pipeline & copy
