(ns java-time.util
  (:require [clojure.string :as string]))

(defn ^:deprecated get-static-fields-of-type
  [^Class klass, ^Class of-type]
  (->> (seq (.getFields klass))
       (map (fn [^java.lang.reflect.Field f]
              (when (.isAssignableFrom of-type (.getType f))
                [(.getName f) (.get f nil)])) )
       (keep identity)
       (into {})))

(defn dashize [camelcase]
  (let [words (re-seq #"([^A-Z]+|[A-Z]+[^A-Z]*)" camelcase)]
    (string/join "-" (map (comp string/lower-case first) words))))

(defmacro if-class [clstr then else]
  (let [loaded (try (Class/forName clstr)
                    (catch Throwable e))]
    (if #?(:bb (and (resolve (symbol clstr)) loaded)
           :default loaded)
      then
      else)))

(defmacro when-class [clstr & body]
  `(if-class ~clstr (do ~@body) nil))

(defmacro if-threeten-extra [then-body else-body]
  `(if-class "org.threeten.extra.Temporals"
     ~then-body
     ~else-body))

(defmacro when-threeten-extra [& body]
  `(if-threeten-extra (do ~@body) nil))

(defmacro when-joda-time-loaded
  "Execute the `body` when Joda-Time classes are found on the classpath.

  Take care - when AOT-compiling code using this macro, the Joda-Time classes
  must be on the classpath at compile time!"
  [& body]
  (if (try (Class/forName "org.joda.time.DateTime")
           (catch Throwable e))
    `(do ~@body)))

;; From Medley, C Weavejester
(defn editable? [coll]
  (instance? clojure.lang.IEditableCollection coll))

(defn reduce-map [f coll]
  (if (editable? coll)
    (persistent! (reduce-kv (f assoc!) (transient (empty coll)) coll))
    (reduce-kv (f assoc) (empty coll) coll)))

(defn map-vals
  "Maps a function over the values of an associative collection."
  [f coll]
  (reduce-map (fn [xf] (fn [m k v] (xf m k (f v)))) coll))

(defn map-kv
  "Maps a function over the key/value pairs of an associate collection. Expects
  a function that takes two arguments, the key and value, and returns the new
  key and value as a collection of two elements."
  [f coll]
  (reduce-map (fn [xf] (fn [m k v] (let [[k v] (f k v)] (xf m k v)))) coll))

(defn class->TemporalAccessor-static-fields [^Class klass]
  {:pre [(class? klass)]}
  (case (.getName klass)
    "java.time.Month" {"JUNE" java.time.Month/JUNE
                       "DECEMBER" java.time.Month/DECEMBER
                       "SEPTEMBER" java.time.Month/SEPTEMBER
                       "OCTOBER" java.time.Month/OCTOBER
                       "FEBRUARY" java.time.Month/FEBRUARY
                       "MAY" java.time.Month/MAY
                       "AUGUST" java.time.Month/AUGUST
                       "MARCH" java.time.Month/MARCH
                       "JANUARY" java.time.Month/JANUARY
                       "JULY" java.time.Month/JULY
                       "APRIL" java.time.Month/APRIL
                       "NOVEMBER" java.time.Month/NOVEMBER}
    "java.time.DayOfWeek" {"MONDAY" java.time.DayOfWeek/MONDAY
                           "TUESDAY" java.time.DayOfWeek/TUESDAY
                           "WEDNESDAY" java.time.DayOfWeek/WEDNESDAY
                           "THURSDAY" java.time.DayOfWeek/THURSDAY
                           "FRIDAY" java.time.DayOfWeek/FRIDAY
                           "SATURDAY" java.time.DayOfWeek/SATURDAY
                           "SUNDAY" java.time.DayOfWeek/SUNDAY}
    "org.threeten.extra.AmPm" (if-threeten-extra
                                {"AM" org.threeten.extra.AmPm/AM, "PM" org.threeten.extra.AmPm/PM}
                                (throw (ex-info "org.threeten.extra.AmPm not found"))) 
    "org.threeten.extra.Quarter" (if-threeten-extra
                                   {"Q1" org.threeten.extra.Quarter/Q1, "Q2" org.threeten.extra.Quarter/Q2, "Q3" org.threeten.extra.Quarter/Q3, "Q4" org.threeten.extra.Quarter/Q4}
                                   (throw (ex-info "org.threeten.extra.Quarter not found"))) 
    (throw (ex-info (str "TODO class->TemporalAccessor-static-fields " klass)
                    {}))))
