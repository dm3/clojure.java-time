(ns java-time.joda
  (:require [java-time.util :as jt.u]
            [java-time.local :as jt.l :refer (local-date local-time local-date-time)]
            [java-time.zone :as jt.z :refer (zoned-date-time offset-date-time offset-time)]
            [java-time.temporal :as jt.t :refer (instant)]
            [java-time.defconversion :refer (conversion! deffactory)])
  (:import [java.time LocalDate LocalTime LocalDateTime ZoneId Instant
            ZonedDateTime OffsetDateTime OffsetTime]))

(jt.u/when-joda
  (defn joda-fields [o fields]
    (mapv (fn [[^org.joda.time.DateTimeFieldType f mul]]
            (* mul (if (instance? org.joda.time.ReadableInstant o)
                     (.get ^org.joda.time.ReadableInstant o f)
                     (.get ^org.joda.time.ReadablePartial o f)))) fields))

  (defn from-joda-fields [o fields constructor]
    (apply constructor (joda-fields o fields)))

  (def joda-date-fields
    [[(org.joda.time.DateTimeFieldType/year) 1]
     [(org.joda.time.DateTimeFieldType/monthOfYear) 1]
     [(org.joda.time.DateTimeFieldType/dayOfMonth) 1]])

  (def joda-time-fields
    [[(org.joda.time.DateTimeFieldType/hourOfDay) 1]
     [(org.joda.time.DateTimeFieldType/minuteOfHour) 1]
     [(org.joda.time.DateTimeFieldType/secondOfMinute) 1]
     [(org.joda.time.DateTimeFieldType/millisOfSecond) (* 1000 1000)]])

  (def joda-date-time-fields
    (concat joda-date-fields joda-time-fields))

  ;; local
  (conversion! org.joda.time.ReadablePartial LocalDateTime
    (fn [^org.joda.time.ReadablePartial i]
      (from-joda-fields i joda-date-time-fields local-date-time)))

  (conversion! org.joda.time.ReadablePartial LocalDate
    (fn [^org.joda.time.ReadablePartial i]
      (from-joda-fields i joda-date-fields local-date)))

  (conversion! org.joda.time.ReadablePartial LocalTime
    (fn [^org.joda.time.ReadablePartial i]
      (from-joda-fields i joda-time-fields local-time)))

  (defn ^ZoneId from-joda-zone [^org.joda.time.DateTimeZone dtz]
    (ZoneId/of (.getID dtz)))

  (conversion! org.joda.time.DateTime [Instant ZoneId]
    (fn [^org.joda.time.ReadableInstant i]
      [(instant (.getMillis i))
       (from-joda-zone (.getZone i))]))

  (conversion! org.joda.time.Instant Instant
    (fn [^org.joda.time.Instant i]
      (instant (.getMillis i))))

  (conversion! org.joda.time.DateTimeZone java.time.ZoneId
    from-joda-zone))
