
# Project Summary: dashcraft

**Description:**
Dashcraft is a ClojureScript component library for building interactive dashboards. It is built on top of the `replicant` UI library and uses `shadow-cljs` for ClojureScript compilation and hot-reloading. The project uses `portfolio` for component development and visualization in isolation.

## Key Technologies

| Technology | Role |
|---|---|
| ClojureScript | Primary language |
| Replicant (2025.12.1) | UI library (hiccup-based, `defalias` components) |
| shadow-cljs (3.3.8) | CLJS compilation, dev server, hot-reload |
| Portfolio (2026.03.1) | Component showcase / visual testing |
| Malli (0.20.1) | Schema validation (forms, EDN editor) |
| ECharts (npm) | Charting engine |
| phosphor-clj (2025.04.16) | Icons (loading spinner, error warning) |
| Babashka | Task runner (`bb.edn`) |
| Bun | npm package manager |

## Project Structure

```
src/robertluo/dashcraft/
  chart.cljs          # ECharts wrapper: ch/chart, ch/drill-down, ch/bread-crumb
  data_table.cljc     # Sortable/groupable data table
  edn_editor.cljc     # Schema-driven EDN editor (Malli)
  form.cljc           # Form generation from Malli schemas
  loading.cljc        # Loading spinner container
  error_aware.cljc    # Error overlay container
  util.cljc           # Custom DOM event helper
  main.cljs           # Demo app (unused, legacy)
portfolio/dashcraft/
  scenes.cljs         # Portfolio scenes for all components
dev/
  user.clj            # REPL helpers: (start!), (stop!), (cljs-repl!)
shadow-cljs.edn       # Single :portfolio build, dev-http on port 3000
deps.edn              # Clojure deps, :dev/:test/:outdated aliases
bb.edn                # Babashka tasks
```

## Core Components

All components use Replicant's `defalias` and follow the `::namespace/key` attribute convention.

- **`ch/chart`** — ECharts wrapper. Accepts `::ch/data` (with `:columns`/`:rows` for dataset format, or raw ECharts options for inline series data). Supports `::ch/notify` for chart event forwarding via custom DOM events.
- **`ch/drill-down`** — Drill-down chart with breadcrumb navigation. Uses `ch/chart` internally with notify mechanism for click-to-drill.
- **`dt/table`** — Data table with sortable columns, grouping, and custom cell rendering.
- **`ee/editor`** — Schema-driven EDN editor. Supports maps, vectors, tuples, enums, multi-dispatch, sequential, set, `:or`, `:orn`, and optional fields.
- **`form/form`** — Generates HTML forms from Malli schemas with validation.
- **`loading/loading-container`** — Wraps content with a loading spinner overlay.
- **`error-aware/error-aware-container`** — Wraps content with a dismissible error overlay.

## Development Workflow

```bash
bb dev            # Install deps + start nREPL (auto-picks port, writes .nrepl-port)
                  # Then in REPL: (start!) -> shadow-cljs + portfolio on http://localhost:3000
                  # (stop!), (cljs-repl!), :cljs/quit

bb watch          # Standalone shadow-cljs watch (no REPL), same port 3000
bb test           # Run RCT tests (rich comment tests from source files)
bb watch-tests    # File watcher that re-runs tests on save
bb outdated       # Show outdated deps (antq)
bb clean          # Remove target, .shadow-cljs, .cpcache, compiled JS
```

## Key Implementation Details

### `data->chart`
Converts `:columns`/`:rows` to ECharts dataset format. When both are nil (inline series data like pie, wordcloud), passes data through unchanged.

### Chart event mechanism (`::ch/notify`)
Charts forward ECharts events as custom DOM events. The `::ch/notify` map specifies `{event-name [echarts-event-type query-filter]}`. The corresponding `:on {event-name handler}` catches the custom event.

### Known limitation: drill-down + dispatch-as-data
`ch/drill-down` uses raw callbacks (`::ch/on-drill fn`), which is incompatible with Replicant's effects-as-data dispatch pattern. Projects using dispatch-as-data need to implement drill-down directly with ECharts.