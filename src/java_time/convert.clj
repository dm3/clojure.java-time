(ns java-time.convert
  (:require [java-time.core :as jt.c]
            [java-time.util :as jt.u]
            [java-time.local :as jt.l]
            [java-time.properties :as jt.p]
            [java-time.temporal :as jt.t])
  (:import [java.time.temporal TemporalUnit ChronoUnit]
           [java.time Instant]
           [java.util Date]
           [java.lang Math]))

(defn as-map
  "Converts a time entity to a map of property key -> value as defined by the
  passed in `value-fn`. By default the actual value of the unit/field is
  produced.

  ```
  (as-map (duration))
  => {:nanos 0, :seconds 0}

  (as-map (local-date 2015 1 1))
  => {:year 2015, :month-of-year 1, :day-of-month 1, ...}
  ```"
  ([e] (as-map e jt.c/value))
  ([e value-fn] (jt.u/map-vals value-fn (jt.c/properties e))))

(defn- convert-unit [^long amount ^long from-x, ^long to-x]
  ;; from and to `x` will always be positive
  (if (> from-x to-x)
    {:whole (Math/multiplyExact amount (quot from-x to-x))
     :remainder 0}
    (let [m (quot to-x from-x)]
      {:whole (quot amount m)
       :remainder (rem amount m)})))

(defn- month-month-factor [unit]
  (case unit
    :months 1
    :quarter-years 3
    :years 12
    :decades 120
    :centuries 1200
    :millenia 12000))

(defn- precise? [^TemporalUnit unit]
  (and (instance? ChronoUnit unit)
       (<= (.compareTo ^ChronoUnit unit ChronoUnit/WEEKS) 0)))

;; Implementation inspired by org.threeten.extra.Temporals/convertAmount
;; BSD Licence
(defn convert-amount
  "Converts an amount from one unit to another. Returns a map of:

  * `:whole` - the whole part of the conversion in the `to` unit
  * `:remainder` - the remainder in the `from` unit

  Arguments may be keywords or instances of `TemporalUnit`.

  Converts between precise units--nanos up to weeks---treating days as exact
  multiples of 24 hours. Also converts between imprecise units---months up to
  millennia. See `ChronoUnit` and `IsoFields` for all of the supported units.
  Does not convert between precise and imprecise units.

  Throws `ArithmeticException` if long overflow occurs during computation.

  ```
  (convert-amount 10000 :seconds :hours)
  => {:remainder 2800 :whole 2}
  ```"
  [amount from-unit to-unit]
  (let [^TemporalUnit from-unit (jt.p/unit from-unit)
        ^TemporalUnit to-unit (jt.p/unit to-unit)]
    (if (= from-unit to-unit)
      {:whole amount, :remainder 0}
      (let [from-precise? (precise? from-unit)
            to-precise? (precise? to-unit)]
        (cond (and from-precise? to-precise?)
              (convert-unit (long amount)
                            (-> from-unit .getDuration .toNanos)
                            (-> to-unit .getDuration .toNanos))

              (and (not from-precise?) (not to-precise?))
              (convert-unit (long amount)
                            (month-month-factor (jt.p/unit-key from-unit))
                            (month-month-factor (jt.p/unit-key to-unit)))

              :else (-> (format "Cannot convert between precise (nanos up to weeks) and imprecise units, got: %s to %s!"
                                from-unit to-unit)
                        IllegalArgumentException.
                        throw))))))

(defn ^:deprecated ^java.util.Date to-java-date
  "Converts a date entity to a `java.util.Date`.

  *Deprecated*:
  This function only has a single arity and works for entities directly
  convertible to `java.time.Instant`. Please consider using [[java-date]]`
  instead."
  [o]
  (cond-> o
    (not (instance? Date o)) (-> jt.t/instant Date/from)))

(defn ^:deprecated ^java.sql.Date to-sql-date
  "Converts a local date entity to a `java.sql.Date`.

  *Deprecated*:
  This function only has a single arity and works for entities directly
  convertible to `java.time.LocalDate`. Please consider using [[sql-date]]
  instead."
  [o] (java.sql.Date/valueOf (jt.l/local-date o)))

(defn ^:deprecated ^java.sql.Timestamp to-sql-timestamp
  "Converts a date entity to a `java.sql.Timestamp`.

  *Deprecated*:
  This function only has a single arity and works for entities directly
  convertible to `java.time.Instant`. Please consider using [[sql-timestamp]]
  instead."
  [o]
  (java.sql.Timestamp/valueOf (jt.l/local-date-time o)))

(defn to-millis-from-epoch
  "Converts a date entity to a `long` representing the number of milliseconds
  from epoch."
  ^long [o]
  (if (number? o) (long o)
    (.toEpochMilli (jt.t/instant o))))
