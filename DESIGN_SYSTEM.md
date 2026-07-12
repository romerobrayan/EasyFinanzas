# DESIGN_SYSTEM.md — Tinto ("Vino Tinto")

A dark, wine-toned identity. Wine is the brand color (active bar, FAB, primary actions); muted gold is the accent (selected states, highlighted icons, links); expenses read in bordeaux and income in sage green. The app is **dark-first** — this is the default and primary theme. A light theme is out of scope for v1.

## Color tokens

Define these in `core/designsystem/theme/Color.kt` and wire them into the Material 3 `darkColorScheme`. Names below are the semantic tokens; the M3 mapping follows.

| Token | Hex | Role |
|---|---|---|
| `VtBackground` | `#17090E` | App background (near-black wine) |
| `VtSurface` | `#24121A` | Cards, sheets, bottom bar |
| `VtSurfaceVariant` | `#301823` | Chips, elevated tiles, inputs |
| `VtPrimary` | `#B23A5E` | Brand / interactive wine — active bar, FAB, primary button |
| `VtPrimaryContainer` | `#5A2A3C` | Inactive bars, primary-tinted containers |
| `VtSecondary` | `#8E2C4D` | Deep wine — secondary emphasis |
| `VtAccentGold` | `#C9A961` | Accent — selected tab, highlighted icon, links, "Ver todos" |
| `VtExpense` | `#D8567A` | Expense amounts (bordeaux) |
| `VtIncome` | `#5FB894` | Income amounts (sage green) |
| `VtError` | `#E5484D` | Errors / destructive |
| `VtOnBackground` | `#F5E9EC` | Primary text on background/surface |
| `VtOnSurfaceVariant` | `#B99CA6` | Secondary text |
| `VtMuted` | `#7D6069` | Tertiary text, inactive icons, captions |
| `VtOutline` | `#3A2029` | Hairline dividers, borders |

### Category accent palette

Each category has one accent, shown as a colored glyph on a `VtSurfaceVariant` rounded tile. Never fill the whole tile with the accent — glyph-on-tint keeps the statement calm.

| Category | Accent hex | Icon key (Tabler-style) |
|---|---|---|
| Comida | `#E08AA3` | `tools-kitchen-2` |
| Entretenimiento | `#C9A961` | `device-tv` |
| Gasto hormiga | `#D8567A` | `ant` |
| Salud | `#5FB894` | `heartbeat` |
| Pasajes / Transporte | `#7F9BD1` | `bus` |
| Servicios / Suscripciones | `#C79A6B` | `repeat` |
| Mercado | `#9DC97E` | `shopping-cart` |
| Otros | `#B99CA6` | `dots` |

### Material 3 mapping

```kotlin
val TintoDarkColors = darkColorScheme(
    primary            = VtPrimary,
    onPrimary          = VtOnBackground,
    primaryContainer   = VtPrimaryContainer,
    onPrimaryContainer = VtOnBackground,
    secondary          = VtSecondary,
    tertiary           = VtAccentGold,     // gold accent surfaces as "tertiary"
    background         = VtBackground,
    onBackground       = VtOnBackground,
    surface            = VtSurface,
    onSurface          = VtOnBackground,
    surfaceVariant     = VtSurfaceVariant,
    onSurfaceVariant   = VtOnSurfaceVariant,
    outline            = VtOutline,
    error              = VtError,
)
```

`VtExpense`, `VtIncome`, `VtAccentGold`, and the category accents are **not** part of the M3 scheme — they are semantic extras. Expose them via a small `TintoColors` object provided through a `CompositionLocal` (`LocalTintoColors`) so composables read `LocalTintoColors.current.expense` etc. This keeps semantic finance colors explicit rather than abusing M3 slots.

## Typography

Two families, bundled as Android font resources under `res/font/`:

- **Display / brand:** Fraunces (variable serif) — screen titles, brand moments. Editorial, warm, pairs with the wine tone.
- **Body + numbers:** Inter — everything else. Enable **tabular figures** (`fontFeatureSettings = "tnum"`) everywhere money or aligned numbers appear, so digits don't jitter.

Define in `Type.kt`. Suggested scale (Compose `sp`):

| Style | Family | Size / weight | Use |
|---|---|---|---|
| `moneyHero` | Inter (tnum) | 30sp / Medium | Dashboard total |
| `moneyRow` | Inter (tnum) | 14sp / Medium | Amounts in statement rows |
| `screenTitle` | Fraunces | 22sp / Medium | Screen headers, brand |
| `sectionTitle` | Inter | 15sp / Medium | "Movimientos", section headers |
| `body` | Inter | 14sp / Regular | Row primary text |
| `caption` | Inter | 12sp / Regular | Row secondary text (category · card) |
| `meta` | Inter | 11sp / Regular | Chart axis labels, overlines |

Rule: only two weights — Regular (400) and Medium (500). No bold-heavy type; the wine palette carries the weight.

## Spacing, shape, elevation

- **Spacing grid:** 4dp base. Use `4 / 8 / 12 / 16 / 20 / 24`. Screen horizontal padding = 18dp. Standard vertical rhythm between sections = 16dp.
- **Corner radii:** chips / pills = fully rounded (`50%`); cards & sheets = 18dp; buttons = 12dp; category tile = 11dp; FAB = 16dp (rounded square, not circular — matches the mockup).
- **Elevation:** flat. No drop shadows. Separate surfaces by fill color (`VtBackground` → `VtSurface` → `VtSurfaceVariant`) and 0.5dp `VtOutline` hairlines, not by shadow. The FAB gets a 4dp ring of `VtSurface` (a knockout) where it overlaps the bottom bar, per the mockup — not a shadow.

## Core components (build in `core/designsystem/component`)

1. **`TintoBarChart`** — the hero. Custom `Canvas` bar chart. Inactive bars `VtPrimaryContainer`; the selected/current bar `VtPrimary` with a 1.5dp `VtAccentGold` ring; rounded top corners (6dp). Axis labels in `meta` style, `VtMuted` (selected label `VtOnBackground`/Medium). Bars are tappable to drill into that bucket.
2. **`PeriodSelector`** — pill row Día / Semana / Mes / Año. Selected pill is `VtAccentGold` fill with `VtBackground` text; unselected are text-only in `VtMuted`.
3. **`MonthSelector`** — the `VtSurfaceVariant` pill with month label + gold chevron that opens the period bottom sheet (mirrors Nubank's "Selecciona un Extracto").
4. **`StatementRow`** — category tile (colored glyph on `VtSurfaceVariant`) + title/subtitle stack + right-aligned amount. Amount uses `VtExpense` (with a `−` sign) or `VtIncome` (with `+`). Rows separated by 0.5dp `VtOutline`, not cards.
5. **`MoneyText`** — formats `Money` to grouped COP (`$1.842.500`), tabular figures, sign + color by `TransactionType`. Single source of currency formatting — never format money ad hoc in a screen.
6. **`CategoryIcon`** — the rounded tile + accent glyph, driven by `Category.iconKey` + `colorHex`.
7. **`RecurringBadge`** — small pill marking detected recurring charges ("recurrente"), gold text on `VtSurfaceVariant`.
8. **`TintoScaffold` / `TintoBottomBar`** — 5-slot bottom bar (Inicio, Movimientos, +FAB center, Recordatorios, Perfil). Active destination icon `VtAccentGold`, inactive `VtMuted`. Center FAB is `VtPrimary`.

## Voice / microcopy

Spanish UI, sentence case, no shouting. Verb-first CTAs ("Agregar gasto", "Ver todos"). Amounts always signed and grouped. Never surface raw parser output to the user — captured items are phrased as "Detectamos un movimiento" pending review.

## Do / Don't

- Do lead each screen with a single dominant number or chart (Nubank's one-hero-per-screen hierarchy).
- Do keep the statement list flat and calm — hairlines, not cards per row.
- Don't fill category tiles with their accent color; glyph-on-tint only.
- Don't introduce a third font or a fourth type weight.
- Don't use green/red as the *brand* — wine is the brand; green and bordeaux are reserved for income vs expense semantics.
- Don't add shadows to fake depth; use fill-color layering.
