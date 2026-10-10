# Server-Push Rate Alerts — Decision & Implementation Plan (Oct 2026)

## Status

Proposed — not started. Companion to [`plans/rate-alerts.md`](rate-alerts.md)
(local engine, shipped Oct 2026). Supersedes the caps in that doc when the
client changes land (local cap 2 → 3, tier matrix below).

## Goal

Move rate-alert evaluation and delivery **to the server for every tier** —
guests included, via an **anonymous temporary session** created on first app
open — so notifications arrive even when the app is killed /
background-restricted, with tiered budgets. Logged-in users present a real
JWT. The paid **base** tier (higher budgets) is reserved for a future version;
the design just parameterizes it.

### Tier matrix (the product spec)

| Tier | Identity | Server alerts | Server notifications | Local alerts |
|---|---|---|---|---|
| **Guest** (no login) | anonymous session JWT (first app open) | 1 | **1 per rolling 2h** (digest) | 3 |
| **Logged** (free) | login JWT | 3 | **2 per rolling 2h** (digest) | 3 |
| **Base** (paid, *future*) | login JWT | 10 | more (TBD) | 3 |

Budget semantics (decided): **digest at window end** — when the budget is
exhausted, further fires are *held*; the next allowed push coalesces all held
fires into one notification (`USD/EUR ▲0.62% · XAU/USD ▼1.05%`). No fire is
lost; a guest's 1/2h budget behaves like a reliable 2h digest.

Client cap changes: `RateAlert.maxAlertsForTier` guest local 2 → **3** for
guest and logged alike (`DataTier` needs the BASE tier added; current
`SOVEREIGN` maps to logged).

---

## 1. The question: separate service (ntfy?) vs. inside exchange-api?

### What we actually need

1. **Identity** — an anonymous session per install + JWT for logged users.
2. **Evaluation** — periodically check rates vs. alert rules. This is the real
   work: scheduling, state, tier caps, notification budgets.
3. **Last-mile delivery** — wake the device: FCM on Android, **APNs on iOS**.

### Why ntfy is not the right fit

[ntfy](https://github.com/binwiederhier/ntfy) only replaces piece **3** — and
only partially:

| Problem | Detail |
|---|---|
| **iOS own-app push is impossible with ntfy** | Push into *our* app must terminate at APNs with *our* credentials. ntfy's iOS delivery works only for the **ntfy app itself**: a self-hosted server relays a "poll request" through `upstream-base-url: https://ntfy.sh` (rate-limited ~250 msgs/day), which wakes the *ntfy* app via ntfy.sh's APNs/FCM connection. Using it for our own app would mean shipping a forked ntfy iOS client as our app. |
| **It doesn't remove the hard part** | We'd still build the sessions, evaluator, Postgres state, budgets and caps somewhere. ntfy is just an HTTP pub/sub in front of that. |
| **Another stateful service on the VPS** | Own container, auth.db/cache.db volumes, public exposure, monitoring — for zero capability we can't build in ~200 lines. |
| **No tier concept** | Topics are public-ish strings; mapping per-install quotas onto ntfy auth is fighting the model. |

Where ntfy *would* make sense: "install the ntfy app and subscribe to a topic"
as a power-user delivery channel. That is not in-app notification and has no
tiering — out of scope, noted as a future add-on only.

### Android transport: why FCM (alternatives considered)

On Android the **only OS-level push channel is FCM** (via Play Services) —
the mirror image of iOS where APNs is the only door. Alternatives and their
costs:

| Alternative | Why rejected as primary |
|---|---|
| Aggregators (OneSignal, Expo Push, SNS) | Wrap FCM anyway **plus** add a third party — strictly worse |
| Own connection (WebSocket/UnifiedPush/ntfy-embedded) | Foreground service killed by Doze/OEM battery managers; permanent "alive" notification; battery drain — even the ntfy Android app defaults to FCM |
| WorkManager polling | That *is* the existing local engine — the fallback, not the upgrade |
| Email/webhook digest | No notification-shelf presence; opt-in extra channel, not a replacement |

Mitigations for the Google dependency: **minimal payloads** (public market
data + alert id only, no PII — nothing sensitive transits Google), and the
local engine remains the no-Google mode for users who refuse Firebase.
ntfy/UnifiedPush as an opt-in distributor stays on the future list.

### Verdict: a module inside exchange-api (Ktor), direct FCM + APNs

exchange-api already has every building block:

- **Auth hook**: `auth-jwt` (HMAC) installed, `POST /auth/verify` exists,
  `GuestRateLimit` already implements the "valid Bearer JWT = bypass"
  convention — the anonymous-session issuer slots straight in.
- **Rates**: `CurrencyService` cache-first reads + `ServiceManager` provider
  rotation (no new provider work).
- **Scheduler pattern**: `HistoricPreloaderWorker` (`while(isActive) { …;
  delay }` on the application scope) — copy the shape.
- **Postgres**: Ktorm + idempotent boot DDL in `DataBasePostgres.init` —
  add tables there.
- **Ops**: Railway staging / VPS prod pipelines already deploy the fat jar —
  no new service to host.

Estimated size: ~900–1200 lines server-side (session issuance + routes +
tables + budgeted evaluator + 2 dispatchers) plus small client wiring on each
platform.

### Options considered

| Option | iOS own-app push | New ops burden | Removes evaluation work? | Verdict |
|---|---|---|---|---|
| Self-hosted ntfy | ❌ (ntfy app only) | +1 stateful service | ❌ | Rejected |
| Separate notify microservice (FCM+APNs) | ✅ | +1 repo/deploy + inter-service auth | ❌ (still need rates + rules) | Deferred — extraction path kept open |
| **Module in exchange-api** | ✅ | none | n/a (already here) | **Chosen** |

Keep the seam clean: everything behind an `AlertDispatcher` interface so the
module can be lifted into its own service later if push volume ever demands it.

---

## 2. Architecture

```
first app open ─► POST /auth/session {installId, platform} ─► anon JWT
                  (sub = "anon:<installId>", tier=guest, exp 30d, refreshable)
login (future) ─► login JWT (sub = user id, tier=logged|base) + merge of the
                  anonymous session's devices/alerts into the user identity

shared KMP app ─► POST /alerts/devices  (FCM/APNs token registration, Bearer)
                  CRUD /alerts[…]?     (server-alert caps per tier, dedupe pair)

                ┌──────────────────── exchange-api (Ktor, existing) ────────────────────┐
                │  AlertEvaluatorWorker (app-scope coroutine, 5 min tick)               │
                │    enabled alerts → group by base → CurrencyService.getLatest(TTL)    │
                │    → same rules as shared RateAlertsEngine                            │
                │        THRESHOLD: |Δ%| ≥ threshold, rebase baseline, 15 min min-gap   │
                │        PERIODIC : interval elapsed since lastNotifiedAt               │
                │    → budget check (rolling 2h window, per tier)                       │
                │        slot free  → dispatch now (digest if >1 held event)            │
                │        exhausted → alert_events.state = HELD                          │
                │    → sweeper: when a user's window frees a slot, flush their HELD      │
                │      events as ONE digest push                                        │
                │    → AlertDispatcher ──┬── FcmDispatcher (HTTP v1, OAuth2 svc account) │
                │                        └── ApnsDispatcher (pushy 0.15.x, ES256 token)  │
                │  Postgres (Ktorm): rate_alerts · alert_devices · alert_events         │
                └──────────────────────────────────────────────────────────────────────┘
                                  │ FCM (Android)                    │ APNs (iOS)
                                  ▼                                  ▼
FirebaseMessagingService → LocalNotifier    UNUserNotificationCenter delegate
(channel `rate_alerts` reused)              (RateAlertsBackground.swift AppDelegate)
```

Rule of thumb: **every alert lives in exactly one place** — local SQLDelight
(guests' and logged users' up-to-3 local alerts, current engine) *or* the
server (tier-capped, push-delivered). Never both → no double notifications.

---

## 3. Identity: temporary sessions (new section, the core addition)

### 3.1 Anonymous session lifecycle

- **On first app open**, the shared client generates and persists an
  `installId` (UUID, in `ISessionStorage` next to currency/lastUpdate), then
  calls `POST /auth/session` — no credentials.
- Server mints an **anonymous JWT** with the existing HMAC verifier config:
  claims `sub = "anon:<installId>"`, `tier = guest`, `typ = anon`,
  `exp = now + 30d`. Stateless — **no sessions table**; same secret and
  verifier as `/auth/verify`, so every existing authed path just works.
- Idempotent: posting the same `installId` again returns a fresh token.
  The client refreshes whenever `exp < now + 7d` (checked on app open),
  keeping `sub` stable → alerts/devices survive refreshes.
- **Login (when the auth service lands)**: client exchanges credentials for a
  user JWT (`tier = logged|base`) and calls the merge endpoint — the server
  re-points the current device token and migrates the anon session's alerts
  to the user `sub` (respecting the higher tier's caps; no dedupe conflicts
  expected, conflicts resolved by keeping the user's existing alert).
  Install-level state (installId) is kept for telemetry only.

### 3.2 Anti-abuse invariants (stateless ≠ abusable)

- **One device token ⇄ one active session.** `alert_devices.token` stays
  UNIQUE: registering a token under a new `sub` **detaches** it from the old
  one. Minting extra anonymous sessions cannot farm push quota — the physical
  install is the scarce resource.
- `POST /auth/session` is rate-limited per IP (reuse the `GuestRateLimit`
  sliding-window infra) and de-duplicated per `installId`.
- **Rate-limit bypass changes**: today any valid Bearer bypasses
  `GuestRateLimit`. Anon JWTs must **not** get the full bypass (mint-and-
  bypass loop) — logged/base tiers keep it; anon sessions get a modest
  per-session allowance (e.g. 60/min, counted against `sub`).
- Anonymous rows are purged: devices inactive 60d, their alerts and events
  shortly after (alerts of an anon session that never returns are garbage).

### 3.3 Endpoints (auth group)

| Method | Path | Auth | Notes |
|---|---|---|---|
| `POST` | `/auth/session` | none (IP-limited) | body `{installId, platform, appVersion}` → `{sessionToken, expiresAt}`; refresh = same call |
| `POST` | `/auth/session/merge` | login JWT | migrate anon session's device+alerts to the user `sub` (Phase 4) |
| `POST` | `/alerts/devices` | any session JWT | body `{platform, token, appVersion, bundleId}` → upsert under `sub`; **detaches token from any other session**; `bundleId` is stored per device and used as the APNs topic (debug/TestFlight/App Store builds differ) |
| `DELETE` | `/alerts/devices` | any session JWT | body `{token}` — sign-out cleanup |
| `GET` | `/alerts` | any session JWT | list + server `lastRate`/`lastNotifiedAt` (drives UI state) |
| `POST` | `/alerts` | any session JWT | caps: guest 1 / logged 3 / base 10; dedupe per `(base, quote)`; 403 over cap |
| `PATCH` | `/alerts/{id}` | any session JWT | toggle `enabled`, edit threshold/interval |
| `DELETE` | `/alerts/{id}` | any session JWT | |

The verifier's `validate` reads `tier` (default guest). Add `/auth/session`
to `GuestRateLimit` with a small allowance; see §3.2 for the bypass change.

---

## 4. Backend design (exchange-api)

### 4.1 Schema (Ktorm, added to `db/Schemas.kt` + boot DDL in `db/Initialization.kt`)

```kotlin
object RateAlerts : Table<RateAlertEntity>("rate_alerts") {
    val id = long("id").primaryKey().autoInc(true)
    val userId = varchar("user_id")            // JWT `sub` — "anon:<uuid>" or user id
    val base = varchar("base")                 // VARCHAR(10), same as currencyrates
    val quote = varchar("quote")
    val mode = varchar("mode")                 // PERIODIC | THRESHOLD
    val thresholdPercent = double("threshold_percent")
    val intervalMinutes = long("interval_minutes")
    val enabled = boolean("enabled")
    val lastRate = double("last_rate")                 // nullable — baseline
    val lastEvaluatedAt = long("last_evaluated_at")    // epoch millis
    val lastNotifiedAt = long("last_notified_at")      // epoch millis
    val createdAt = long("created_at")
    // UNIQUE(user_id, base, quote) + index on (enabled, base)
}

object AlertDevices : Table<AlertDeviceEntity>("alert_devices") {
    val id = long("id").primaryKey().autoInc(true)
    val userId = varchar("user_id")            // owning session `sub`
    val platform = varchar("platform")         // ANDROID | IOS
    val token = varchar("token")               // UNIQUE — the single-session invariant
    val appVersion = varchar("app_version")
    val createdAt = long("created_at")
    val lastSeenAt = long("last_seen_at")      // refreshed on every app open
    val active = boolean("active")             // false after 404/410 cleanup
}

object AlertEvents : Table<AlertEventEntity>("alert_events") {
    val id = long("id").primaryKey().autoInc(true)
    val alertId = long("alert_id")             // FK rate_alerts.id
    val userId = varchar("user_id")            // denormalized — budget + purge queries
    val firedAt = long("fired_at")
    val rateBefore = double("rate_before")
    val rateAfter = double("rate_after")
    val changePercent = double("change_percent")
    val state = varchar("state")               // HELD | SENT | FAILED:<reason> | SKIPPED_NO_DEVICE
    val digestId = varchar("digest_id")        // nullable — groups events sent in one push
    val notifiedAt = long("notified_at")       // nullable — when the push actually went out
    // index on (user_id, notified_at) — the 2h budget window count
}
```

No `users` table — the principal is the JWT `sub`. Login issuance lives in
the future auth service; this module only verifies JWTs and mints anon ones.

### 4.2 Evaluator worker (`services/AlertEvaluatorWorker.kt`)

Shape copied from `HistoricPreloaderWorker`:

```kotlin
fun start(applicationScope: CoroutineScope) = applicationScope.launch {
    while (isActive) {
        evaluateAll()          // rules + budget + dispatch
        sweepHeldDigests()     // flush windows that freed a slot
        delay(evalIntervalMs)  // ALERTS_EVAL_INTERVAL_MS, default 5 min
    }
}
```

**Rules** (parity with `shared/.../RateAlertsEngine.kt`):

- **Baseline seeding**: first evaluation stores `lastRate`, never fires.
- **THRESHOLD**: fire when `|Δ%| ≥ thresholdPercent`; after firing, rebase
  `lastRate` to the firing rate; 15-min min-gap per alert.
- **PERIODIC**: fire when `intervalMinutes` elapsed since `lastNotifiedAt`.
- One rate fetch per **distinct base** per tick — user count is free.

**Notification budget (rolling 2h window, per user)**:

```
budget(tier): guest = 1, logged = 2, base = TBD(>2)
delivered(user) = count(alert_events where userId, state = SENT, notifiedAt > now - 2h)

on fire:
    if delivered < budget → dispatch now (if >1 HELD event for the user,
                              send them as ONE digest in this push)
    else                  → insert event with state = HELD
sweep (each tick, per user with HELD events):
    nextEligibleAt = (k-th most recent SENT notifiedAt) + 2h   // k = budget
    if now ≥ nextEligibleAt → flush ALL held events as one digest push
```

For a guest this degrades to exactly "one notification each 2h": first fire
pushes instantly, everything after digests every 2h. Digest payload:

```json
{
  "title": "USD/EUR ▲ +0.62%",                       // single fire
  "body":   "1 USD = 0.9134 EUR"
}
{                                                     // digest (≥2 held)
  "title": "2 pairs moved",
  "body":   "USD/EUR ▲ +0.62% · XAU/USD ▼ 1.05%",
  "data":   { "alerts": "[{id,base,quote,changePercent,rate,firedAt}, …]" }
}
```

- Evaluation skips users with **no active device** (events still recorded as
  `SKIPPED_NO_DEVICE` so history stays truthful).
- **Freshness caveat**: `currencyrates` buckets `latest` rows by hour
  (`formatterWithHourOnly`) — a plain cache read can be ~59 min stale, too
  coarse for ±0.5% thresholds. The evaluator passes a freshness TTL (~10
  min): if the current-hour row for the base is older, force one provider
  rotation (which re-warms the shared cache). Distinct bases are bounded by
  the supported fiat set, so provider quota impact is capped; `ServiceManager`
  quota-exclusion still applies. Counters (`alerts_provider_fetches_total`)
  to watch it — guests now broaden the alert base mix.
- **Single-replica assumption**: Railway 1 replica, VPS one container — an
  in-process worker is safe. If replicas ever scale, claim ticks with
  `SELECT … FOR UPDATE SKIP LOCKED` before extracting the module.

### 4.3 Dispatchers

```kotlin
interface AlertDispatcher {
    suspend fun send(device: AlertDeviceEntity, events: List<AlertEventEntity>): DeliveryResult
}
```

**FcmDispatcher** — FCM **HTTP v1** (`POST fcm.googleapis.com/v1/projects/{id}/messages:send`,
legacy API is gone). OAuth2 access token minted from a service-account JSON
(`google-auth-library-oauth2-http` or hand-rolled JWT → token exchange).
Android `data` message + `notification` with
`notification_channel_id = "rate_alerts"` (existing channel),
`collapse_key = "alerts"`, TTL 1h.

**ApnsDispatcher** — [pushy](https://pushy-apns.org/) `com.eatthepath:pushy:0.15.x`
(HTTP/2, token-based ES256 auth: `.p8` key + key id + team id). Host by env
(`DEVELOPMENT` vs `PRODUCTION` — matches how the app build was installed).
**Topic is per device**: the client's `bundleId` from `/alerts/devices`
registration (`.debug` builds ⇄ sandbox host + debug bundle id; TestFlight /
App Store ⇄ production host + release bundle id — never cross them, APNs
rejects mismatched tokens).

**Token hygiene**: FCM `UNREGISTERED` (404) and APNs `BadDeviceToken` /
`Unregistered` (410) → mark `alert_devices.active = false`; nightly job
purges rows inactive > 30 days, anon sessions inactive > 60d (§3.2).
`"validate_only": true` FCM flag for the staging smoke test.

Payload privacy: only public market data + pair + alert ids. No PII, no
account data. (Direct APNs on iOS keeps Google out of the iOS stack entirely —
matches the Sovereign positioning.)

### 4.4 Env / secrets

| Var | Purpose |
|---|---|
| `ALERTS_EVAL_INTERVAL_MS` | evaluator tick (default 300000) |
| `ALERTS_QUIET_NIGHT_START_HOUR` / `ALERTS_QUIET_NIGHT_END_HOUR` / `ALERTS_QUIET_TZ` / `ALERTS_QUIET_WEEKENDS` | quiet hours (v0.5.3): night 20–8 Europe/Berlin silences ALL pairs; weekends silence non-crypto only (crypto trades 24/7); fully-quiet bases skip provider fetches; baselines survive so Monday 08:00 emits at most one notification per moved pair |
| `ALERTS_FRESHNESS_TTL_MS` | rate cache TTL for evaluation (default 600000) |
| `ALERTS_ENABLED` | feature kill-switch (worker + routes + session issuer) |
| `SESSION_JWT_TTL_DAYS` | anon JWT lifetime (default 30) |
| `FCM_PROJECT_ID` + `FCM_SERVICE_ACCOUNT` (inline JSON) | FCM v1 credentials |
| `APNS_TEAM_ID`, `APNS_KEY_ID`, `APNS_PRIVATE_KEY_P8` (base64 of the `.p8` — decoded in `ApnsDispatcher` before `loadFromInputStream`, to avoid multiline env pain), `APNS_HOST` | APNs credentials (topic is per device from registration, not an env var) |

Railway: staging env vars (service **Variables** UI). VPS/prod: secrets in
`deploy/ansible/group_vars/all/vault.yml` → rendered into `db/.env` via
`roles/exchange/templates/env/app.env.j2` (add matching lines there) — the
container picks them up on the next Jenkins deploy. **Not** the Jenkins
credential file: `exchange-api-deploy-env` carries only deploy plumbing
(Gitea login, `PROMETHEUS_TOKEN`, webhook). Non-secret knobs
(`ALERTS_*`, `APNS_HOST`) go in `group_vars/all/main.yml`.

### 4.5 Metrics (existing Prometheus registry)

`alerts_active`, `alerts_evaluated_total`, `alerts_fired_total`,
`alerts_held_total` (budget pressure per tier), `alerts_digest_size` histogram,
`alerts_push_{sent,failed}_total{platform}`, `alerts_provider_fetches_total`,
`auth_sessions_issued_total`, evaluator tick duration histogram.

---

## 5. Client design

### 5.1 shared (KMP)

- **SessionManager (new)**: generate/persist `installId`; obtain + refresh the
  anon JWT (`exp < now + 7d` check on app open); hold the login JWT when the
  auth service lands; expose `Bearer` for all API calls. Storage rides
  `ISessionStorage` (DataStore / NSUserDefaults actuals already exist).
- `RateAlert` gets `source: LOCAL | SERVER` — the add-alert dialog gains a
  delivery toggle ("Server push" default / "On device"), with per-list caps
  shown. The Rate Alerts screen renders **two sections** (Server push / On
  device), each with its own counter (`1 of 1`, `2 of 3`).
- `IRepository` gains server-alert operations backed by a new
  `RateAlertsApi` (Ktor client, reuses `HttpClientFactory`): CRUD + device
  registration.
- Local cap change: `RateAlert.maxAlertsForTier` → 3 for guest and logged
  (`LIMIT_MESSAGE` copy + tests updated); add `DataTier.BASE` (10) for later.
- Local engine scope: `RateAlertScheduler.update(hasActiveAlerts)` now means
  "has **local** alerts" regardless of tier — logged users keep the local
  engine for their on-device alerts. An alert exists in exactly one engine,
  so no double notifications, no tier gating needed.
- Tier transitions:
  - **anon → login**: `POST /auth/session/merge` migrates the device + the 1
    guest server alert into the user identity (cap grows to 3); local alerts
    untouched.
  - **login lapse → anon**: the user JWT expires; client falls back to a
    fresh anon session; server alerts over the guest cap render disabled
    with a re-login CTA; server keeps rows 30 days grace before cleanup.

### 5.2 Android app

- Add Firebase (`google-services` plugin + `google-services.json`, currently
  none in the repo) + `firebase-messaging`.
- `SovereignMessagingService : FirebaseMessagingService` — `onMessageReceived`
  maps the data payload to the **existing** `LocalNotifier.notify()`
  (channel `rate_alerts`), so look & feel stay identical; digest payloads
  render as an expanded-style summary line; `onNewToken` re-POSTs to
  `/alerts/devices`.
- Token registration on app start (after session ensure) and on login.

### 5.3 iOS app

**APNs configuration checklist (one-time, Apple Developer + Xcode):**

1. **Signing key**: Apple Developer → Keys → create an **APNs auth key**
   (`.p8`). Token-based auth: no yearly cert renewal, one key works for both
   sandbox and production. Note its **Key ID** and the membership **Team ID**.
2. **Capability**: Xcode → target → Signing & Capabilities → **+
   Push Notifications** (adds the `aps-environment` entitlement). With
   Xcode-managed signing the provisioning profile picks push up automatically.
3. **Background Modes** (optional, silent updates): `remote-notification`.
4. **Code**: `RateAlertsBackground.swift`'s `AppDelegate` —
   `registerForRemoteNotifications()` on launch;
   `didRegisterForRemoteNotificationsWithDeviceToken` → hex string →
   `POST /alerts/devices{platform: IOS}`; implement
   `UNUserNotificationCenter.delegate` for foreground presentation +
   `didReceiveRemoteNotification` if silent updates are used.
5. **Environment split**: debug builds (`com.sovereignledger.app.debug`)
   receive via **APNs sandbox** (`api.sandbox.push.apple.com`) →
   `APNS_HOST=DEVELOPMENT` on staging; TestFlight/App Store builds use the
   production host — validate both, tokens are not interchangeable.
6. **Backend**: `APNS_TEAM_ID`, `APNS_KEY_ID`, `APNS_PRIVATE_KEY_P8`,
   `APNS_HOST` via env/Ansible vault (§4.4); the push topic is the
   **`bundleId` the app sends at device registration** — `.debug` builds
   register `com.sovereignledger.app.debug`, release builds
   `com.sovereignledger.app`.

Other iOS notes:

- Background `BGAppRefresh` registration stays — now it serves **local-tier
  alerts** (and token refresh) rather than being disabled for subs.
- The `.p8` never touches the app binary — it lives only on the server.

---

## 6. Phases

| # | Scope | Exit criteria |
|---|---|---|
| 0 | This decision record reviewed & accepted (tier matrix + digest semantics) | — |
| 1 | Backend foundation: Ktorm tables + boot DDL, `POST /auth/session` issuer, `/alerts` CRUD + device reg behind session JWTs, evaluator worker **dry-run** (logs fires/budget decisions, no push), unit tests mirroring `RateAlertsEngineTest` fixtures + budget-window tests | ✅ **DONE Oct 9 2026** — verified locally end-to-end (anon session mint → CRUD 201/409/403/401 → device reg 204 → evaluator: baseline seed → fire/rebase → SENT → HELD (budget exhausted) → digest flush after window aged out). 12/12 new unit tests; full suite green. See "Phase 1 gotchas" (§7) |
| 2 | Android delivery: FCM dispatcher + Firebase wiring + app SessionManager/installId; FCM `validate_only` smoke test, then live on staging | kill app on emulator → push arrives < 1 min after a manipulated-threshold fire; second fire within 2h is held then flushed as digest |
| 3 | iOS delivery: pushy APNs dispatcher + AppDelegate wiring; sandbox E2E | ✅ **DONE Oct 10 2026** — verified on a real device (Dali's iPhone, iPhone 12 mini): Xcode debug build → session + APNs sandbox token auto-registered (`platform=IOS`, bundleId topic) → hair-trigger alert via API → notification delivered through `api.sandbox.push.apple.com`. Two deploy fixes found on the way: `preferIPv4Stack` (Railway containers are IPv4-only; prod Dockerfile already had it) and the retry loop absorbing Railway's transient DNS failures (netty resolver watch item below) |
| 4 | Tier integration & UI: local cap 3 (client), `source` toggle + two-section Rate Alerts screen, budget copy ("1 push / 2h · guest"), anon→login merge endpoint stub, lapse fallback | guest flow exercises both server and local alerts without duplication |
| 5 | Hardening: metrics dashboard, token hygiene + anon purge jobs, quota counters, load test evaluator (1k alerts across tiers), docs | dashboards in the existing Prometheus/Uptime Kuma stack |
| ✱ later | Base tier budgets (10 alerts, richer cadence), extract `alert-rules` multiplatform module shared by commonMain + exchange-api (rules now duplicated — keep pinned by shared test fixtures), quiet hours, absolute price targets, ntfy/webhook/email as opt-in channels | — |

Phase 1–2 land the user-visible win (guest Android push with the 2h digest);
3–4 complete the story; nothing blocks on the others. The login JWT path is
only *stubbed* until the auth service exists — anon sessions carry the whole
feature meanwhile.

---

## 7. Risks / gotchas

### Phase 1 gotchas (discovered while implementing, all fixed)

- **Ktorm `totalRecords` ≠ count value**: `select(count()).totalRecords` returns the
  number of result *rows* (always 1 for an aggregate) — read the aggregate via
  `row.getInt(1)` instead. This made the guest cap reject everything.
- **Ktorm DSL aliases selected columns** (`alert_events_notified_at`): reading a
  result by plain `Column.name` yields null — use the `row[Column]` indexer (or
  `.label`). This silently zeroed budget timestamps, so the window never blocked.
- **Zero-arg `toCreateTable()` extension self-recurses** (StackOverflow at boot)
  when it shadows the generic from `Extensions.kt` — only define the
  `unique`-taking variant, let no-arg calls resolve to the generic.
- **Ktor unnamed `jwt {}` registers as provider "default"**: routes using
  `authenticate("auth-jwt")` crash at boot — name the provider explicitly.
- Gson bypasses Kotlin defaults → alert DTOs use nullable fields + explicit
  validation, never non-null data-class fields.
- **Netty DNS on Railway (watch item, Phase 3 E2E)**: pushy's netty resolver
  intermittently fails to resolve `api.*.push.apple.com` inside Railway
  containers (search-domain + UDP quirks) while the JVM resolver works fine.
  The HELD-retry loop absorbs it — if it ever gets noisy, tune netty via
  `io.netty.resolver.dns.defaultNameServerFallback` or force the JDK resolver
  on the APNs client. Also: containers without IPv6 routes need
  `-Djava.net.preferIPv4Stack=true` (both Dockerfiles set it now).
- **POST /alerts returns 204 instead of 201** when the immediate
  `findAlert(id, userId)` after insert races the connection — cosmetic,
  alert is created and evaluated; fix by returning the entity from
  `createAlert` directly (Phase 4 cleanup).

- **Stateless anon sessions can't be individually revoked** (no denylist) —
  acceptable; the one-device-one-session invariant (§3.2) is the real quota
  enforcement. Rotating `installId` mints a new identity but the device token
  invariant still pins the install to one active session.
- **APNs host mismatch**: sandbox tokens ⇄ `api.sandbox.push.apple.com`.
  Debug builds silently fail against production — gate by env, test both.
- **FCM token rotation**: tokens rotate on app reinstall/clear-data;
  re-register on every start + `onNewToken`, rely on `lastSeenAt` + inactive
  cleanup for strays.
- **iOS entitlements**: push capability + `aps-environment` must be in the
  provisioning profile — Xcode-managed profiles handle it; watch CI archive.
- **Provider quota**: guests broaden the base mix (any fiat base, not the
  preload set). Freshness TTL < eval interval multiplies provider hits per
  base (TTL 10 min → ≤6/hour/base). Counters + `ServiceManager` exclusion
  guard it; raise TTL before raising eval frequency.
- **Budget window is server-clock only** — clients never compute eligibility;
  the app's "last notification" label for server alerts renders from
  `GET /alerts` state.
- **Ktor client engines**: pushy brings its own Netty HTTP/2 — do not route
  APNs through the CIO client. FCM v1 is plain HTTPS (any engine).
- **Gson parity**: server keeps Gson content negotiation — alert DTOs are
  trivial, but pin field names with tests (client uses kotlinx-serialization).
- **Rules duplication**: evaluator logic is a JVM re-implementation of the
  KMP engine. Same fixtures in both test suites until the `alert-rules`
  extraction.

## 8. Open decisions

1. **iOS transport**: direct APNs via pushy (**recommended** — no Google SDK
   in the iOS app) vs FCM-for-both (one backend integration, fastest Phase 3).
2. **Base tier budgets**: alert cap confirmed 10; notifications/window TBD
   (e.g. 6 per 2h? unlimited with quiet hours?) — future version.
3. **Logged-tier naming**: keep `DataTier.SO VEREIGN` as the logged tier or
   rename to `LOGGED`/`BASE` vocabulary now, while the change is cheap.
4. **Add-alert default source**: "Server push" default with "On device"
   opt-in (recommended — push is strictly more reliable), or vice versa.
5. **JWT issuer for login**: `/auth/session/merge` requires a real user JWT —
   depends on where the Sovereign login lands. Until then Phases 1–3 run
   entirely on anonymous sessions.
