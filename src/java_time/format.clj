(ns java-time.format
  (:refer-clojure :exclude (format))
  (:require [clojure.string :as string]
            [java-time.core :as jt.c]
            [java-time.util :as jt.u])
  (:import [java.time.temporal TemporalAccessor]
           [java.time.format DateTimeFormatter DateTimeFormatterBuilder ResolverStyle]
           java.util.Locale))

(def predefined-formatters
  (->> (jt.u/get-static-fields-of-type DateTimeFormatter DateTimeFormatter)
       (jt.u/map-kv
        (fn [^String n fmt]
          [(.. (.replace n \_ \-) toString (toLowerCase (Locale/US))) fmt]))))

(defn- get-resolver-style [s]
  (if (instance? ResolverStyle s) s
    (case s
      :strict ResolverStyle/STRICT
      :smart ResolverStyle/SMART
      :lenient ResolverStyle/LENIENT)))

(defn ^DateTimeFormatter formatter
  "Constructs a DateTimeFormatter out of a

  * format string - \"YYYY/mm/DD\", \"YYY HH:MM\", etc.
  * formatter name - :iso-date, :iso-time, etc.

  Accepts a map of options as an optional second argument:

  * `resolver-style` - either `:strict`, `:smart `or `:lenient`"
  ([fmt]
   (formatter fmt {}))
  ([fmt {:keys [resolver-style]}]
   (let [^DateTimeFormatter fmt
         (cond (instance? DateTimeFormatter fmt) fmt
               (string? fmt) (DateTimeFormatter/ofPattern fmt)
               :else (get predefined-formatters (name fmt)))
         fmt (if resolver-style
               (.withResolverStyle fmt (get-resolver-style resolver-style))
               fmt)]
     fmt)))

(defn format
  "Formats the given time entity as a string.

  Accepts something that can be converted to a `DateTimeFormatter` as a first
  argument. Given one argument uses the default format."
  ([o] (str o))
  ([fmt o]
   (.format (formatter fmt) o)))

(defn ^TemporalAccessor parse
  ([fmt o] (parse fmt o {}))
  ([fmt o opts]
   (.parse (formatter fmt opts) o)))
