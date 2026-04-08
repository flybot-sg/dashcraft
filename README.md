# Dashcraft

A ClojureScript component library using [replicant](https://replicant.fun) with a development environment powered by shadow-cljs and Portfolio for component development and visualization.

## Prerequisites

- [Clojure CLI](https://clojure.org/guides/install_clojure) (for deps.edn dependencies)
- [Babashka](https://babashka.org)
- [Bun](https://bun.sh/) (for JavaScript dependencies)

## Development

### Starting the REPL

```bash
bb dev
```

This starts an nREPL with shadow-cljs middleware. Connect your editor, then in the REPL:

```clojure
(start!)       ; start shadow-cljs + portfolio on http://localhost:3000
(stop!)        ; stop shadow-cljs
(cljs-repl!)   ; connect to browser CLJS REPL (exit with :cljs/quit)
```

### Standalone watch (no REPL)

```bash
bb watch
```

### Tests

```bash
bb test           # run RCT tests
bb watch-tests    # re-run on file changes
```

### Other tasks

```bash
bb outdated    # show outdated dependencies
bb clean       # remove build artifacts
```

### Component Development with Portfolio

Component examples and demos are managed through Portfolio. Add your component examples in the `portfolio/` directory to see them at http://localhost:3000.

## Project Structure

```
.
├── src/          # Core ClojureScript source files
├── portfolio/    # Component examples and demos (Portfolio scenes)
├── dev/          # REPL helpers (user.clj)
└── resources/    # Static assets and public files
```
