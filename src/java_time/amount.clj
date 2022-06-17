(ns java-time.amount
  (:require [clojure.string :as string]
            [java-time.core :as jt.c]
            [java-time.local :as jt.l]
            [java-time.util :as jt.u]
            [java-time.properties :as jt.p]
            [java-time.convert :as jt.convert]
            [java-time.defconversion :refer (conversion! deffactory)])
  (:import [java.time Duration Period]
           [java.time.temporal ChronoUnit TemporalAmount Temporal TemporalUnit]))

(defn- ^Duration d-plus [^Duration cp, ^TemporalAmount o]
  (.plus cp o))

(defn- ^Duration d-minus [^Duration cp, ^TemporalAmount o]
  (.minus cp o))

(extend-type Duration
  jt.c/Plusable
  (seq-plus [d tas]
    (reduce d-plus d tas))

  jt.c/Minusable
  (seq-minus [d tas]
    (reduce d-minus d tas))

  jt.c/Multipliable
  (multiply-by [d v]
    (.multipliedBy d v))

  jt.c/Amount
  (zero? [d]
    (.isZero d))

  (negative? [d]
    (.isNegative d))

  (negate [d]
    (.negated d))

  (abs [d]
    (.abs d))

  jt.c/As
  (as* [o k]
    (-> (.toNanos o)
        (jt.convert/convert-amount :nanos k)
        :whole)))

(deffactory duration
  "Creates a duration - a temporal entity representing standard days, hours,
  minutes, millis, micros and nanos. The duration itself contains only seconds
  and nanos as properties.

  Given one argument will
    * interpret as millis if a number
    * try to parse from the standard format if a string
    * extract supported units from another `TemporalAmount`
    * convert from a Joda Period/Duration

  Given two arguments will
    * get a duration between two `Temporal`s
    * get a duration of a specified unit, e.g. `(duration 100 :seconds)`"
  :returns Duration
  :implicit-arities [1 2]
  ([] (Duration/ofMillis 0)))

(defn ^java.time.Duration micros
  "Duration of a specified number of microseconds."
  [micros]
  (Duration/ofNanos (Math/multiplyExact (long micros) 1000)))

(defmacro gen-duration-methods
  []
  (conj (map (fn [[fn-name method-name]]
               (let [fn-name (with-meta fn-name {:tag Duration})]
                 `(defn ~fn-name
                    ~(str "Duration of a specified number of " method-name ".")
                    [v#]
                    (. Duration ~(symbol (str 'of (string/capitalize (str method-name)))) (long v#)))))
             [['standard-days 'days]
              ['hours 'hours]
              ['minutes 'minutes]
              ['millis 'millis]
              ['seconds 'seconds]
              ['nanos 'nanos]])
        'do))

(gen-duration-methods)

(defmacro gen-period-methods
  []
  (conj (map (fn [fn-name]
               (let [fn-name (with-meta fn-name {:tag Period})]
                 `(defn ~fn-name
                    [v#]
                    (. Period ~(symbol (str 'of (string/capitalize (str fn-name)))) (int v#)))))
             ['years
              'months
              'days
              'weeks])
        'do))

(gen-period-methods)

(deffactory period
  "Creates a period - a temporal entity consisting of years, months and days.

  Given one argument will
    * interpret as years if a number
    * try to parse from the standard format if a string
    * extract supported units from another `TemporalAmount`
    * convert from a Joda Period

  Given two arguments will
    * get a period of a specified unit, e.g. `(period 10 :months)`
    * get a period between two temporals by converting them to local dates
    * get a period of a specified number of years and months

  Given three arguments will create a year/month/day period."
  :returns Period
  :implicit-arities [1 2 3]
  ([] (Period/of 0 0 0)))

(jt.u/when-joda-time-loaded
  (defn ^Period joda-period->period [^org.joda.time.Period p]
    (if-not (zero? (+ (.getMillis p) (.getSeconds p) (.getMinutes p) (.getHours p)))
      (throw (ex-info "Cannot convert a Joda Period containing non year/month/days to a Java-Time Period!"
               {:period p}))
      (Period/of (.getYears p) (.getMonths p) (+ (* 7 (.getWeeks p)) (.getDays p)))))

  (defn ^Duration joda-period->duration [^org.joda.time.Period p]
    (if-not (zero? (+ (.getMonths p) (.getYears p)))
      (throw (ex-info "Cannot convert a Joda Period containing months/years to a Java-Time Duration!"
               {:period p}))
      (jt.c/plus
        (duration (.getMillis p) :millis)
        (duration (.getSeconds p) :seconds)
        (duration (.getMinutes p) :minutes)
        (duration (.getHours p) :hours)
        (duration (+ (* 7 (.getWeeks p)) (.getDays p)) :days))))

  (conversion! org.joda.time.Duration Duration
    (fn [^org.joda.time.Duration d]
      (Duration/ofMillis (.getMillis d))))

  (conversion! org.joda.time.Period Duration
    joda-period->duration)

  (conversion! org.joda.time.Duration Period
    (fn [^org.joda.time.Duration d]
      (Period/ofDays (.getStandardDays ^org.joda.time.Duration d))))

  (conversion! org.joda.time.Period Period
    joda-period->period))

(conversion! CharSequence Duration
  (fn [^CharSequence s]
    (Duration/parse s)))

(conversion! CharSequence Period
  (fn [^CharSequence s]
    (Period/parse s)))

(conversion! Number Duration
  (fn [^Number millis]
    (Duration/ofMillis millis)))

(conversion! Number Period
  #(Period/of (int %1) 0 0))

(conversion! [Number Number] Period
  #(Period/of (int %1) (int %2) 0))

(conversion! [Number Number Number] Period
  #(Period/of (int %1) (int %2) (int %3)))

(conversion! [Temporal Temporal] Duration
  (fn [^Temporal a, ^Temporal b]
    (Duration/between a b)))

(conversion! [Temporal Temporal] Period
  (fn [^Temporal a, ^Temporal b]
    (Period/between a b)))

(conversion! [Number clojure.lang.Keyword] Period
  (fn [value k]
    (case k
      :years (years value)
      :months (months value)
      :days (days value))))

(conversion! [Number TemporalUnit] Duration
  (fn [value ^TemporalUnit unit]
    (Duration/of (long value) unit)))

(conversion! clojure.lang.Keyword TemporalUnit
  jt.p/get-unit-checked)

(conversion! clojure.lang.Keyword TemporalAmount
  jt.p/get-unit-checked)

(extend-type Period
  jt.c/As
  (as* [o k]
    (if (<= (.compareTo ^ChronoUnit (jt.p/unit k) ChronoUnit/WEEKS) 0)
      (if (and (zero? (.getYears o))
               (zero? (.getMonths o)))
        (-> (.getDays o)
            (jt.convert/convert-amount :days k)
            :whole)
        (throw (java.time.DateTimeException. "Period contains years or months")))
      (if (zero? (.getDays o))
        (-> (.toTotalMonths o)
            (jt.convert/convert-amount :months k)
            :whole)
        (throw (java.time.DateTimeException. "Period contains days"))))))
