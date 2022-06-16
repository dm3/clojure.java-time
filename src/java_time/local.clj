(ns java-time.local
  (:require [java-time.core :as jt.c :refer (value)]
            [java-time.properties :as jt.p]
            [java-time.temporal :as jt.t]
            [java-time.format :as jt.f]
            [java-time.clock :as jt.clock]
            [java-time.defconversion :refer (conversion! deffactory)])
  (:import [java.time ZoneId Clock LocalDate LocalTime LocalDateTime Instant
            ZonedDateTime OffsetDateTime OffsetTime]
           [java.time.temporal TemporalAccessor]))

(deffactory local-date
  "Creates a `LocalDate`. The following arguments are supported:

    * no arguments - current local-date
    * one argument
      + clock
      + another temporal entity
      + string representation
      + year
    * two arguments
      + formatter (format) and a string
      + an instant and a zone id
      + another temporal entity and an offset (preserves local time)
      + year and month
    * three arguments
      + year, month and date"
  :returns LocalDate
  :implicit-arities [1 2 3]
  ([] (jt.clock/make (fn [^Clock c] (LocalDate/now c)))))

(deffactory local-time
  "Creates a `LocalTime`. The following arguments are supported:

    * no arguments - current local time
    * one argument
      + clock
      + another temporal entity
      + string representation
      + hours
    * two arguments
      + formatter (format) and a string
      + an instant and a zone id
      + hours and minutes
    * three/four arguments - hour, minute, second, nanos"
  :returns LocalTime
  :implicit-arities [1 2 3]
  ([] (jt.clock/make (fn [^Clock c] (LocalTime/now c))))
  ([h m s nn]
   (LocalTime/of (int h) (int m) (int s) (int nn))))

(deffactory local-date-time
  "Creates a `LocalDateTime`. The following arguments are supported:

    * no arguments - current local date-time
    * one argument
      + clock
      + another temporal entity
      + string representation
      + year
    * two arguments
      + local date and local time
      + an instant and a zone id
      + formatter (format) and a string
      + year and month

    three and more arguments - year/month/day/..."
  :returns LocalDateTime
  :implicit-arities [1 2 3]
  ([] (jt.clock/make (fn [^Clock c] (LocalDateTime/now c))))
  ([y m d h]
   (local-date-time y m d h 0))
  ([y m d h mm]
   (local-date-time y m d h mm 0))
  ([y m d h mm ss]
   (local-date-time y m d h mm ss 0))
  ([y m d h mm ss n]
   (LocalDateTime/of (int (value y)) (int (value m)) (int (value d))
                     (int h) (int mm) (int ss) (int n))))

(extend-type LocalTime
  jt.c/Truncatable
  (truncate-to [o u]
    (.truncatedTo o (jt.p/get-unit-checked u)))

  jt.c/Ordered
  (single-after? [t o]
    (.isAfter t o))
  (single-before? [t o]
    (.isBefore t o)))

(extend-type LocalDateTime
  jt.c/Truncatable
  (truncate-to [o u]
    (.truncatedTo o (jt.p/get-unit-checked u))))

(conversion! Clock LocalDate
  (fn [^Clock c]
    (LocalDate/now c)))

(conversion! Clock LocalTime
  (fn [^Clock c]
    (LocalTime/now c)))

(conversion! Clock LocalDateTime
  (fn [^Clock c]
    (LocalDateTime/now c)))

(conversion! CharSequence LocalDate
  (fn [^CharSequence s]
    (LocalDate/parse s)))

(conversion! CharSequence LocalTime
  (fn [^CharSequence s]
    (LocalTime/parse s)))

(conversion! CharSequence LocalDateTime
  (fn [^CharSequence s]
    (LocalDateTime/parse s)))

(conversion! LocalTime LocalDateTime
  (fn [^LocalTime lt]
    (.atDate lt (local-date)))
  2)

(conversion! LocalDate LocalDateTime
  (fn [^LocalDate ld]
    (.atTime ld (local-time)))
  2)

(conversion! [java.time.format.DateTimeFormatter CharSequence] LocalDate
  #(LocalDate/from (jt.f/parse %1 %2)))

(conversion! [java.time.format.DateTimeFormatter CharSequence] LocalTime
  #(LocalTime/from (jt.f/parse %1 %2)))

(conversion! [java.time.format.DateTimeFormatter CharSequence] LocalDateTime
  #(LocalDateTime/from (jt.f/parse %1 %2)))

(conversion! Number LocalDate
  #(LocalDate/of (int %) 1 1))

(conversion! Number LocalTime
  #(LocalTime/of (int %) 0))

(conversion! Number LocalDateTime
  #(LocalDateTime/of (int %) 1 1 0 0))

(conversion! [Number Number] LocalDate
  #(LocalDate/of (int %1) (int %2) 1))

(conversion! [Number Number] LocalTime
  #(LocalTime/of (int %1) (int %2)))

(conversion! [Number Number] LocalDateTime
  #(LocalDateTime/of (int %1) (int %2) 1 0 0))

(conversion! [Number Number Number] LocalDate
  #(LocalDate/of (int %1) (int %2) (int %3)))

(conversion! [Number Number Number] LocalTime
  #(LocalTime/of (int %1) (int %2) (int %3)))

(conversion! [Number Number Number] LocalDateTime
  #(LocalDateTime/of (int %1) (int %2) (int %3) 0 0))

(conversion! [Instant ZoneId] LocalDateTime
  (fn [^Instant i, ^ZoneId z]
    (LocalDateTime/ofInstant i z)))

(conversion! [LocalDate LocalTime] LocalDateTime
  (fn [^LocalDate dt, ^LocalTime tm]
    (LocalDateTime/of dt tm)))

(conversion! LocalDateTime [LocalDate LocalTime]
  (fn [^LocalDateTime ldt]
    [(.toLocalDate ldt) (.toLocalTime ldt)]))

(conversion! CharSequence java.time.format.DateTimeFormatter
  jt.f/formatter)

