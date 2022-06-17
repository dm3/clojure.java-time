(ns user
  (:require [clojure.tools.namespace.repl :as repl]
            [criterium.core :as crit]
            [taoensso.timbre :as timbre]
            [taoensso.tufte :as profiling :refer (pspy profile defnp p)]))

(defn go []
  (set! *warn-on-reflection* false)
  (repl/refresh-all)
  (require '[java-time :as j])
  (eval `(profile :info :local-date-time (j/local-date-time 1 2 3)))
  (eval `(profile :info :zoned-date-time (j/zoned-date-time 1 2 3)))
  (eval `(profile :info :fail (try (j/zoned-date-time 1 2 "a") (catch Exception e# nil)))))

(defn bench []
  (repl/refresh-all)
  (require '[java-time :as j])
  (eval `(crit/bench (j/local-date-time 1 2 3))))

(defn print-reflection-warnings []
  (set! *warn-on-reflection* true)
  (repl/refresh-all)
  (set! *warn-on-reflection* false))
