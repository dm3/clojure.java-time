(ns java-time.repl
  (:require [java-time.adjuster :as j.adj]
            [java-time.properties :as j.p]
            [java-time.format :as jt.f]
            [java-time.zone :as jt.z]
            [java-time.defconversion :as jt.dc]
            [clojure.pprint :as pprint]))

(defn show-adjusters []
  (pprint/pprint (sort (keys j.adj/predefined-adjusters))))

(defn show-units []
  (pprint/pprint (sort (keys (:predefined j.p/unit-groups)))))

(defn show-fields []
  (pprint/pprint (sort (keys (:predefined j.p/field-groups)))))

(defn show-formatters []
  (pprint/pprint (sort (keys jt.f/predefined-formatters))))

(defn show-timezones []
  (pprint/pprint (sort (jt.z/available-zone-ids))))

(defn show-graph []
  (pprint/pprint (.m-by-arity @jt.dc/graph)))

(defn show-path [from to]
  (jt.dc/get-path from to))
