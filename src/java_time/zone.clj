(ns java-time.zone
  (:require [java-time.core :as jt.c :refer (value)]
            [java-time.temporal :as jt.t]
            [java-time.util :as jt.u]
            [java-time.amount :as jt.a]
            [java-time.format :as jt.f]
            [java-time.clock :as jt.clock]
            [java-time.properties :as jt.p :refer (get-unit-checked)]
            [java-time.defconversion :refer (conversion! deffactory)])
  (:import [java.time.temporal TemporalAccessor]
           [java.time.format DateTimeFormatter]
           [java.time Clock Instant LocalDate LocalTime LocalDateTime
            ZoneId ZoneOffset OffsetDateTime OffsetTime ZonedDateTime]))

;;;;; Zone Id

(defn- to-hms [n]
  (if (integer? n)
    [n 0 0]
    (let [h (int n)
          m-n (* 60 (- n h))
          m (int m-n)
          s (int (* 60 (- m-n m)))]
      [h m s])))

(conversion! java.util.TimeZone ZoneId
  (fn [^java.util.TimeZone z]
    (.toZoneId z)))

(conversion! CharSequence ZoneId
  (fn [^CharSequence s]
    (ZoneId/of s)))

(conversion! [CharSequence ZoneOffset] ZoneId
  (fn [^CharSequence s, ^ZoneOffset zo]
    (ZoneId/ofOffset s zo)))

(defn- ^ZoneOffset clock->zone-offset [^Clock c]
  (-> (.getZone c)
      (.getRules)
      (.getOffset (.instant c))))

(deffactory zone-offset
  "Creates a `ZoneOffset` from a string identifier (e.g. \"+01\"), a number of
  hours/hours and minutes/hours, minutes and seconds or extracts from another
  temporal entity.

  Returns default system zone offset if no arguments provided."
  :returns ZoneOffset
  :implicit-arities []
  ([] (jt.clock/make clock->zone-offset))
  ([o] (cond (instance? ZoneOffset o)
             o

             (instance? Clock o)
             (clock->zone-offset o)

             (instance? java.time.temporal.TemporalAccessor o)
             (ZoneOffset/from ^java.time.temporal.TemporalAccessor o)

             (string? o)
             (ZoneOffset/of ^String o)

             (number? o)
             (let [[h m s] (to-hms o)]
               (zone-offset h m s))

             :else (throw (java.time.DateTimeException.
                            (format "Could not convert %s to a ZoneOffset!" o)))))
  ([h m] (ZoneOffset/ofHoursMinutes h m))
  ([h m s] (ZoneOffset/ofHoursMinutesSeconds h m s)))

(deffactory zone-id
  "Creates a `ZoneId` from a string identifier, `java.util.TimeZone` or extracts
  from another temporal entity.

  Returns default system zone id if no arguments provided.

  Given two arguments will use the second as the offset."
  :returns ZoneId
  :implicit-arities [1 2]
  ([] (jt.clock/make (fn [^Clock c] (.getZone c)))))

(defn available-zone-ids []
  (ZoneId/getAvailableZoneIds))

;; offset date/time

(deffactory offset-date-time
  "Creates an `OffsetDateTime`. The following arguments are supported:

    * no arguments - current date-time with the default offset
    * one argument
      + clock
      + zone id
      + another temporal entity
      + string representation
      + offset
    * two arguments
      + formatter (format) and a string
      + local date-time and an offset
      + another temporal entity and an offset (preserves local time)
      + year and an offset
    * three arguments
      + local date, local time and an offset
      + year, month and an offset
    * four up to eight arguments - last is always the offset"
  :returns OffsetDateTime
  :implicit-arities [1 2 3]
  ([] (jt.clock/make (fn [^Clock c] (OffsetDateTime/now c))))
  ([y m d o] (offset-date-time y m d 0 o))
  ([y m d h o] (offset-date-time y m d h 0 o))
  ([y mo d h m o] (offset-date-time y mo d h m 0 o))
  ([y mo d h m s o] (offset-date-time y mo d h m s 0 o))
  ([y mo d h m s n o]
   (OffsetDateTime/of
     (int (value y)) (int (value mo)) (int (value d))
     (int h) (int m) (int s) (int n) (zone-offset o))))

(deffactory offset-time
  "Creates an `OffsetTime`. The following arguments are supported:

    * no arguments - current time with the default offset
    * one argument
      + clock
      + zone id
      + another temporal entity
      + string representation
      + offset - produces local time with the given offset
    * two arguments
      + formatter (format) and a string
      + local time and an offset
      + instant and an offset
      + hour and an offset
    * three up to five arguments - last is always the offset"
  :returns OffsetTime
  :implicit-arities [1 2 3]
  ([] (jt.clock/make (fn [^Clock c] (OffsetTime/now c))))
  ([h m s o] (offset-time h m s 0 o))
  ([h m s n o]
   (OffsetTime/of h m s n (zone-offset o))))

(deffactory zoned-date-time
  "Creates a `ZonedDateTime`. The following arguments are supported:

    * no arguments - current date-time in the default zone
    * one argument
      + clock
      + zone id
      + another temporal entity
      + string representation
      + year
    * two arguments
      + formatter and a string
      + local date-time and a zone id
      + year and a zone id
      + year and month
    * three arguments
      + local date, local time and a zone id
      + year, month and a zone id
      + year, month and day
    * four up to eight arguments - last is always the zone id

  If zone id is not specified, default zone id will be used. You can check the
  default zone by invoking `(zone-id)`."
  :returns ZonedDateTime
  :implicit-arities [1 2 3]
  ([] (jt.clock/make (fn [^Clock c] (ZonedDateTime/now c))))
  ([y m d o] (zoned-date-time y m d 0 o))
  ([y m d h o] (zoned-date-time y m d h 0 o))
  ([y mo d h m o] (zoned-date-time y mo d h m 0 o))
  ([y mo d h m s o] (zoned-date-time y mo d h m s 0 o))
  ([y mo d h m s n o]
   (ZonedDateTime/of
     (int (value y)) (int (value mo)) (int (value d))
     (int h) (int m) (int s) (int n) (zone-id o))))

(conversion! Clock ZonedDateTime
  (fn [^Clock c]
    (ZonedDateTime/now c)))

(conversion! Clock OffsetDateTime
  (fn [^Clock c]
    (OffsetDateTime/now c)))

(conversion! Clock OffsetTime
  (fn [^Clock c]
    (OffsetTime/now c)))

(conversion! ZoneId ZonedDateTime
  (fn [^ZoneId z]
    (ZonedDateTime/now z))
  2)

(conversion! ZoneId OffsetDateTime
  (fn [^ZoneId z]
    (OffsetDateTime/now z))
  2)

(conversion! ZoneId OffsetTime
  (fn [^ZoneId z]
    (OffsetTime/now z))
  2)

(conversion! CharSequence ZonedDateTime
  (fn [^CharSequence s]
    (ZonedDateTime/parse s))
  2)

(conversion! CharSequence OffsetDateTime
  (fn [^CharSequence s]
    (OffsetDateTime/parse s))
  2)

(conversion! CharSequence OffsetTime
  (fn [^CharSequence s]
    (OffsetTime/parse s))
  2)

(conversion! ZoneOffset OffsetDateTime
  (fn [^ZoneOffset zo]
    (.withOffsetSameLocal (offset-date-time) zo))
  2)

(conversion! ZoneOffset OffsetTime
  (fn [^ZoneOffset zo]
    (.withOffsetSameLocal (offset-time) zo))
  2)

(conversion! ZonedDateTime [Instant ZoneId]
  (fn [^ZonedDateTime zdt]
    [(.toInstant zdt) (.getZone zdt)]))

(conversion! OffsetDateTime [Instant ZoneOffset]
  (fn [^OffsetDateTime odt]
    [(.toInstant odt) (.getOffset odt)]))

(conversion! OffsetTime [LocalTime ZoneOffset]
  (fn [^OffsetTime odt]
    [(.toLocalTime odt) (.getOffset odt)]))

(conversion! OffsetTime OffsetDateTime
  (fn [^OffsetTime ot]
    (.atDate ot (LocalDate/now)))
  2)

(conversion! [LocalDateTime ZoneOffset] OffsetDateTime
  (fn [^LocalDateTime ldt, ^ZoneOffset zo]
    (OffsetDateTime/of ldt zo)))

(conversion! [LocalDateTime ZoneId] ZonedDateTime
  (fn [^LocalDateTime ldt, ^ZoneId z]
    (ZonedDateTime/of ldt z)))

(conversion! [LocalTime ZoneOffset] OffsetTime
  (fn [^LocalTime lt, ^ZoneOffset zo]
    (OffsetTime/of lt zo)))

(conversion! [Instant ZoneId] ZonedDateTime
  (fn [^Instant i, ^ZoneId z]
    (ZonedDateTime/ofInstant i z)))

(conversion! [Instant ZoneId] OffsetDateTime
  (fn [^Instant i, ^ZoneId z]
    (OffsetDateTime/ofInstant i z)))

(conversion! [Instant ZoneId] OffsetTime
  (fn [^Instant i, ^ZoneId z]
    (OffsetTime/ofInstant i z)))

(conversion! [java.time.format.DateTimeFormatter CharSequence] ZonedDateTime
  #(ZonedDateTime/from (jt.f/parse %1 %2)))

(conversion! [java.time.format.DateTimeFormatter CharSequence] OffsetDateTime
  #(OffsetDateTime/from (jt.f/parse %1 %2)))

(conversion! [java.time.format.DateTimeFormatter CharSequence] OffsetTime
  #(OffsetTime/from (jt.f/parse %1 %2)))

(conversion! [Number ZoneId] ZonedDateTime
  (fn [value ^ZoneId z]
    (zoned-date-time value 1 1 z)))

(conversion! [Number ZoneOffset] OffsetDateTime
  (fn [value ^ZoneOffset zo]
    (offset-date-time value 1 1 zo)))

(conversion! [Number ZoneOffset] OffsetTime
  (fn [value ^ZoneOffset zo]
    (offset-time value 0 zo)))

(conversion! [Number Number ZoneId] ZonedDateTime
  (fn [y m ^ZoneId z]
    (zoned-date-time y m 1 z)))

(conversion! [Number Number ZoneOffset] OffsetDateTime
  (fn [y m ^ZoneOffset zo]
    (offset-date-time y m 1 zo)))

(conversion! [Number Number ZoneOffset] OffsetTime
  (fn [h m ^ZoneOffset zo]
    (offset-time h m 0 zo)))

(conversion! java.util.GregorianCalendar ZonedDateTime
  (fn [^java.util.GregorianCalendar cal]
    (.toZonedDateTime cal)))

(extend-type OffsetDateTime
  jt.c/Truncatable
  (truncate-to [o u]
    (.truncatedTo o (get-unit-checked u)))

  jt.c/Ordered
  (single-after? [d o]
    (.isAfter d o))
  (single-before? [d o]
    (.isBefore d o)))

(extend-type OffsetTime
  jt.c/Truncatable
  (truncate-to [o u]
    (.truncatedTo o (get-unit-checked u)))

  jt.c/Ordered
  (single-after? [d o]
    (.isAfter d o))
  (single-before? [d o]
    (.isBefore d o)))

(extend-type ZonedDateTime
  jt.c/Truncatable
  (truncate-to [o u]
    (.truncatedTo o (get-unit-checked u))))

;;;;; Clock

(defn ^Clock system-clock
  "Creates a system clock. In the default timezone if called without arguments,
  otherwise accepts a Zone Id."
  ([] (Clock/systemDefaultZone))
  ([k] (Clock/system (zone-id k))))

(defn ^Clock fixed-clock
  "Creates a fixed clock either at the current instant or at the supplied
  instant/instant + zone."
  ([] (Clock/fixed (Instant/now) (zone-id)))
  ([i] (Clock/fixed (jt.t/instant i) (zone-id)))
  ([i z] (Clock/fixed (jt.t/instant i) (zone-id z))))

(defn ^Clock offset-clock
  "Creates a clock offset from the current/provided clock by a given
  `duration`."
  ([d] (Clock/offset (system-clock) (jt.a/duration d)))
  ([^Clock c, d] (Clock/offset c (jt.a/duration d))))

(defn ^Clock tick-clock
  "Creates a clock wrapping system/provided clock that only ticks as per
  specified duration."
  ([d] (Clock/tick (system-clock) (jt.a/duration d)))
  ([^Clock c, d] (Clock/tick c (jt.a/duration d))))

(extend-type Clock
  jt.c/ReadableProperty
  (value [c] (.millis c))

  jt.c/HasZone
  (with-zone [c z]
    (.withZone c (zone-id z)))

  jt.c/Ordered
  (single-after? [c o]
    (> (.millis c) (.millis ^Clock o)))
  (single-before? [c o]
    (< (.millis c) (.millis ^Clock o))))

;; Avoid cyclic dep
(extend-type java.time.format.DateTimeFormatter
  jt.c/HasZone
  (with-zone [dtf zone]
    (.withZone dtf (zone-id zone))))
