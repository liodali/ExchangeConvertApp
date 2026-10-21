# Tablet / iPad UI (from `design/ui-design.pen`)

Source frames: `iPad — *` (11 frames, 1194×834 landscape). Extracted via `pen` CLI (headless interactive).

## Window modes (dp width, cross-platform — BoxWithConstraints)
| Mode | Width | Shell |
|---|---|---|
| COMPACT | < 600 | current bottom nav (phone) — untouched |
| MEDIUM | 600–839 | rail + single column (foldable unfolded) |
| EXPANDED | ≥ 840 | rail + two-column workspaces (iPad) |

## Navigation rail (design `Tablet Navigation Rail`)
- 72dp wide, `Canvas` bg, radius 16–20, padding 8h/15v, space-between, center
- Brand mark 38×38 accent radius 13, "D"
- 5 destinations, 54×57 pill radius 11–15, icon 19 + label 10sp:
  - **Overview** `chart-no-axes-combined` → Routes.HOME
  - **Rates** `arrow-left-right` → Routes.CONVERTER (app's rates surface; full Rates screen = Phase 3)
  - **History** `history` → Routes.HISTORY
  - **Account** `user-round` → Routes.ACCOUNT
  - **Support** `life-buoy` → Routes.SUPPORT (+ FAQ/Contact/Feedback/Chat/Terms/Privacy/About map here)
  - selected: bg accentSoft, icon+label accent (design #20372A/#4EDEA3); unselected muted
- Bottom: settings icon + avatar 34dp radius 17 initials

## Home — expanded layout (`iPad — Sovereign Market Dashboard`)
- Header 50dp: title 23sp + subtitle + sync chip "Updated Xm ago" + search/bell
- Snapshot strip 86dp: 3 cards (pair + Δ% 12sp, price 20sp), radius 14, stroke border
- Columns (gap 16):
  - Market column (fill): chart panel 300dp + asset explorer (fill)
  - **Tools column 292dp**: Quick convert card + Recent conversions (fill)

## Rates — expanded (`iPad — Rates Display`, Phase 3)
- Header 70dp + LIVE badge; main column 676dp + Available Rates panel (fill)

## Phases
1. ✅ Breakpoints + `LedgerNavRail` + shell swap (rail ≥600dp, bottom nav compact-only) — all screens get rail; phone untouched
2. ✅ Home two-column + snapshot strip (expanded)
3. Rates screen from design + two-pane
4. Polish: hover states, iPad keyboard, sheets → side panels

Gotcha: design hardcodes Sovereign hex — implementation uses `LedgerColors` semantic tokens so Summer/Orchoid/Light palettes work.
