(ns java-time.sugar
  (:require [clojure.string :as str]
            [java-time.core :as jt.c]
            [java-time.util :as jt.u]))

(defmacro gen-sugar-fns
  []
  (conj (map (fn [[day-name day-number]]
               `(defn ~(symbol (str day-name '?))
                 ~(str "Returns true if the given time entity with the\n"
                       "  `day-of-week` property falls on a " (str/capitalize day-name) ", otherwise false.")
                 {:arglists '[[~'i]]}
                 [o#]
                 (if-let [p# (jt.c/property o# :day-of-week)]
                   (= (jt.c/value p#) ~day-number)
                   (throw (java.time.DateTimeException. (str "Day of week unsupported for: " (type o#)))))))
             [['monday 1]
              ['tuesday 2]
              ['wednesday 3]
              ['thursday 4]
              ['friday 5]
              ['saturday 6]
              ['sunday 7]])
        'do))

(gen-sugar-fns)

(defn weekend? 
  "Returns true if argument satisfies [[saturday?]] and [[sunday?]],
  otherwise false."
  [dt]
  (or (saturday? dt) (sunday? dt)))

(defn weekday?
  "Complement of [[weekend?]]."
  [dt]
  (not (weekend? dt)))
