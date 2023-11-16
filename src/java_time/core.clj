(ns java-time.core
  (:refer-clojure :exclude (zero? range max min abs < > <= >= * - + neg? =))
  (:require [clojure.core :as cc])
  (:import [java.time.temporal ValueRange]
           [java.time.chrono Chronology]))

(defprotocol Amount
  (zero? [a]
    "True if the amount is zero")
  (negative? [a]
    "True if the amount is negative")
  (negate [a]
    "Negates a temporal amount:

      (negate (negate x)) == x")
  (abs [a]
    "Returns the absolute value of a temporal amount:

      (abs (negate x)) == (abs x)"))

(defprotocol Supporting
  (supports? [o p]
    "True if the `o` entity supports the `p` property"))

(defprotocol HasChronology
  (^java.time.chrono.Chronology chronology [o]
    "The `Chronology` of the entity"))

(defprotocol HasFields
  (fields [o]
    "Fields present in this temporal entity")
  (field* [o k]
    "Internal use"))

(defprotocol HasUnits
  (units [o]
    "Units present in this temporal entity.")
  (unit* [o k]
    "Internal use"))

(defprotocol HasProperties
  (properties [o]
    "Map of properties present in this temporal entity")
  (property [o k]
    "Property of this temporal entity under key `k`"))

(defprotocol As
  (as* [o k]
    "Value of property/unit identified by key/object `k` of the temporal
    entity `o`"))

(defprotocol ReadableProperty
  (value [p]
    "Value of the property"))

(defprotocol ReadableRangeProperty
  (range [p]
    "Range of values for this property")
  (min-value [p]
    "Minimum value of this property")
  (largest-min-value [p]
    "Largest minimum value of this property")
  (smallest-max-value [p]
    "Smallest maximum value of this property, e.g. 28th of February for months")
  (max-value [p]
    "Maximum value of this property, e.g. 29th of February for months"))

(defprotocol WritableProperty
  (with-value [p v]
    "Underlying temporal entity with the value of this property set to `v`"))

(defprotocol WritableRangeProperty
  (with-min-value [p]
    "Underlying temporal entity with the value set to the minimum available for
    this property")
  (with-largest-min-value [p]
    "Underlying temporal entity with the value set to the largest minimum
    available for this property")
  (with-smallest-max-value [p]
    "Underlying temporal entity with the value set to the smallest maximum
    available for this property")
  (with-max-value [p]
    "Underlying temporal entity with the value set to the maximum
    available for this property"))

(defprotocol KnowsTimeBetween
  (time-between [o e u]
    "Time between temporal entities `o` and `e` in unit `u`.

    ```
    (j/time-between (j/local-date 2015) (j/local-date 2016) :days)
    => 365

    (j/time-between :days (j/local-date 2015) (j/local-date 2016))
    => 365
    ```"))

(defprotocol KnowsIfLeap
  (leap? [o]
    "True if the year of this entity is a leap year."))

(defprotocol Truncatable
  (truncate-to [o u]
    "Truncates this entity to the specified time unit. Only works for units that
    divide into the length of standard day without remainder (up to `:days`)."))

(defprotocol HasZone
  (with-zone [o z]
    "Returns this temporal entity with the specified `ZoneId`"))

(defprotocol Plusable
  "Internal"
  (seq-plus [o os]))

(defprotocol Minusable
  "Internal"
  (seq-minus [o os]))

(defprotocol Multipliable
  (multiply-by [o v]
    "Entity `o` multiplied by the value `v`"))

(defprotocol Ordered
  (single-before? [a b]
    "Internal use")
  (single-after? [a b]
    "Internal use"))

(defprotocol Convert
  (-convert [a b] "Internal use.
                  Convert `b` to a value compatible with `a`'s implementation
                  of single-after? etc, and one that is useable as the first argument
                  to single-after? etc."))

(extend-type Object
  Convert
  (-convert [a b] b))

(defn as
  "Values of property/unit identified by keys/objects `ks` of the temporal
  entity `o`, e.g.

  ```
  (as (duration 1 :hour) :minutes)
  => 60

  (as (local-date 2015 9) :year :month-of-year)
  => [2015 9]
  ```"
  ([o k]
   (as* o k))
  ([o k1 k2]
   [(as* o k1) (as* o k2)])
  ([o k1 k2 & ks]
   (into (as o k1 k2) (map #(as* o %)) ks)))

(defn before?
  "Returns `true` if time entities are ordered from the earliest to the
  latest (same semantics as `<`), otherwise `false`.

  ```
  (before? (local-date 2009) (local-date 2010) (local-date 2011))
  => true

  (before? (interval (instant 10000) (instant 1000000))
           (instant 99999999))
  => true
  ```"
  ([x] true)
  ([x y] (single-before? x (-convert x y)))
  ([x y & more]
   (let [y (-convert x y)]
     (if (single-before? x y)
       (if-some [n (next more)]
         (recur y (first more) n)
         (single-before? y (-convert y (first more))))
       false))))

(defn after?
  "Returns `true` if time entities are ordered from the latest to the
  earliest (same semantics as `>`), otherwise `false`.

  ```
  (after? (local-date 2011) (local-date 2010) (local-date 2009))
  => true

  (after? (instant 99999999)
          (interval (instant 10000) (instant 1000000)))
  => true
  ```"
  ([x] true)
  ([x y] (single-after? x (-convert x y)))
  ([x y & more]
   (let [y (-convert x y)]
     (if (single-after? x y)
       (if-some [n (next more)]
         (recur y (first more) n)
         (single-after? y (-convert y (first more))))
       false))))

(defn ^:private single-not-after? [x y]
  (or (cc/= x y)
      (single-before? x y)))

(defn not-after?
  "Returns `true` if time entities are ordered from the earliest to the
  latest (same semantics as `<=`), otherwise `false`.

  ```
  (not-after? (local-date 2009) (local-date 2010) (local-date 2011))
  ;=> true

  (not-after? (interval (instant 10000) (instant 1000000))
              (instant 99999999))
  ;=> true
  ```"
  ([x] true)
  ([x y] (single-not-after? x (-convert x y)))
  ([x y & more]
   (let [y (-convert x y)]
     (and (single-not-after? x y)
          (if-some [n (next more)]
            (recur y (first more) n)
            (single-not-after? y (-convert y (first more))))))))

(defn ^:private single-not-before? [x y]
  (or (cc/= x y)
      (single-after? x y)))

(defn not-before?
  "Returns `true` if time entities are ordered from the latest to the
  earliest (same semantics as `>=`), otherwise `false`.

  ```
  (not-before? (local-date 2011) (local-date 2010) (local-date 2009))
  ;=> true

  (not-before? (instant 99999999)
               (interval (instant 10000) (instant 1000000)))
  ;=> true
  ```"
  ([x] true)
  ([x y] (single-not-before? x (-convert x y)))
  ([x y & more]
   (let [y (-convert x y)]
     (and (single-not-before? x y)
          (if-some [n (next more)]
            (recur y (first more) n)
            (single-not-before? y (-convert y (first more))))))))

(defn plus
  "Adds all of the `os` to the time entity `o`. `plus` is not commutative, the
  first argument is always the entity which will accumulate the rest of the
  arguments.

  ```
  (j/plus (j/local-date 2015) (j/years 1))
  => <java.time.LocalDate \"2016-01-01\">
  ```"
  [o & os]
  (seq-plus o os))

(defn minus
  "Subtracts all of the `os` from the time entity `o`

  ```
  (j/minus (j/local-date 2015) (j/years 1))
  => <java.time.LocalDate \"2014-01-01\">
  ```"
  [o & os]
  (if (seq os)
    (seq-minus o os)
    (negate o)))

;;;;; Clojure types

(extend-type Number
  ReadableProperty
  (value [n] n)

  WritableProperty
  (with-value [n v] v)

  Plusable
  (seq-plus [n xs]
    (apply cc/+ n xs))

  Minusable
  (seq-minus [n xs]
    (apply cc/- n xs))

  Multipliable
  (multiply-by [n v]
    (cc/* n (value v)))

  Amount
  (zero? [n] (cc/zero? n))
  (negative? [n] (cc/neg? n))
  (negate [n] (cc/- n))
  (abs [n] (Math/abs (long n))))

(extend-type nil
  ReadableProperty
  (value [_] nil)

  WritableProperty
  (with-value [_ v] v))

(def readable-range-property-fns
  {:min-value (fn [p] (.getMinimum ^ValueRange (range p)))
   :largest-min-value (fn [p] (.getLargestMinimum ^ValueRange (range p)))
   :smallest-max-value (fn [p] (.getSmallestMaximum ^ValueRange (range p)))
   :max-value (fn [p] (.getMaximum ^ValueRange (range p)))})

;; vars named after excluded clojure.core vars

(def ^{:arglists '([x] [x y] [x y & more])}
  <=
  "Returns `true` if time entities are ordered from the earliest to the
  latest (same semantics as `<=`), otherwise `false`.

  ```
  (j/<= (local-date 2009) (local-date 2010) (local-date 2011))
  ;=> true

  (j/<= (interval (instant 10000) (instant 1000000))
        (instant 99999999))
  ;=> true
  ```"
  not-after?)

(def ^{:arglists '([x] [x y] [x y & more])}
  >=
  "Returns `true` if time entities are ordered from the latest to the
  earliest (same semantics as `>=`), otherwise `false`.

  ```
  (j/>= (local-date 2011) (local-date 2010) (local-date 2009))
  ;=> true

  (j/>= (instant 99999999)
        (interval (instant 10000) (instant 1000000)))
  ;=> true
  ```"
  not-before?)

(def ^{:arglists '([x] [x y] [x y & more])}
  < 
  "Returns `true` if time entities are ordered from the earliest to the
  latest (same semantics as `<`), otherwise `false`.

  ```
  (j/< (local-date 2009) (local-date 2010) (local-date 2011))
  => true

  (j/< (interval (instant 10000) (instant 1000000))
       (instant 99999999))
  => true
  ```"
  before?)

(def ^{:arglists '([x] [x y] [x y & more])}
  > 
  "Returns `true` if time entities are ordered from the latest to the
  earliest (same semantics as `>`), otherwise `false`.

  ```
  (j/> (local-date 2011) (local-date 2010) (local-date 2009))
  => true

  (j/> (instant 99999999)
       (interval (instant 10000) (instant 1000000)))
  => true
  ```"
  after?)

(def ^{:arglists '([o & os])}
  +
  "Adds all of the `os` to the time entity `o`. `+` is not commutative, the
  first argument is always the entity which will accumulate the rest of the
  arguments.

  ```
  (j/+ (j/local-date 2015) (j/years 1))
  => <java.time.LocalDate \"2016-01-01\">
  ```"
  plus)

(def ^{:arglists '([o & os])}
  -
  "Subtracts all of the `os` from the time entity `o`

  ```
  (j/- (j/local-date 2015) (j/years 1))
  => <java.time.LocalDate \"2014-01-01\">
  ```"
  minus)

(defn max
  "Latest/longest of the given time entities. Entities should be of the same
  type"
  [o & os]
  (first (sort #(compare %2 %1) (cons o os))))

(defn min
  "Earliest/shortest of the given time entities. Entities should be of the same
  type"
  [o & os]
  (first (sort (cons o os))))

(defn =
  "Returns true if all time entities represent the same time, otherwise false.

  `j/=` is not commutative, the first argument is always the entity which will
  accumulate the rest of the arguments.
  
  e.g., (j/= (j/day-of-week :thursday) :thursday) => true"
  ([x] true)
  ([x y] (cc/= x (-convert x y)))
  ([x y & more]
   (let [y (-convert x y)]
     (and (cc/= x y)
          (if-some [n (next more)]
            (recur y (first more) n)
            (cc/= y (-convert y (first more))))))))

;; these can't be aliased since they're protocol methods

(defn neg?
  "True if the amount is negative"
  [a]
  (negative? a))

(defn *
  "Entity `o` multiplied by the value `v`"
  [o v]
  (multiply-by o v))
