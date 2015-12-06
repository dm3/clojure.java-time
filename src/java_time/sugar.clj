(ns java-time.sugar
  (:require [java-time.core :as jt.c]
            [java-time.util :as jt.u]))

(doseq [[day-name day-number]
        [['monday 1] ['tuesday 2] ['wednesday 3] ['thursday 4] ['friday 5]
         ['saturday 6] ['sunday 7]]]
  (eval `(defn ~(symbol (str day-name '?))
           ~(str "Returns true if the given time entity with the\n"
                 "  `day-of-week` property falls on a " day-name ".")
           [o#] (if-let [p# (jt.c/property o# :day-of-week)]
                  (= (jt.c/value p#) ~day-number)
                  (throw (java.time.DateTimeException. (str "Day of week unsupported for: " (type o#))))))))

(defn weekend? [dt]
  (or (saturday? dt) (sunday? dt)))

(defn weekday? [dt]
  (not (weekend? dt)))
