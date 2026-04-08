(ns user
  "REPL helpers for dashcraft development.

   Start:   (start!)
   Stop:    (stop!)
   CLJS:    (cljs-repl!)
   Quit:    :cljs/quit"
  (:require [shadow.cljs.devtools.server :as server]
            [shadow.cljs.devtools.api :as shadow]))

(defn start!
  "Start shadow-cljs server + watch :portfolio build."
  []
  (server/start!)
  (shadow/watch :portfolio)
  :started)

(defn stop!
  "Stop shadow-cljs server."
  []
  (server/stop!)
  :stopped)

(defn cljs-repl!
  "Connect to browser CLJS REPL. Exit with :cljs/quit"
  []
  (shadow/repl :app))

(comment
  (start!)
  (stop!)
  (cljs-repl!))
