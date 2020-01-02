(ns java-time.data
  "A pure-data view of all the important/data-holding java.time objects."
  (:require [clojure.core.protocols :as p])
  (:import (java.time YearMonth Month DayOfWeek Instant
                      LocalTime LocalDateTime
                      ZonedDateTime OffsetDateTime)))

(extend-protocol p/Datafiable

  Month
  (datafy [m]
    {:month-name (.name m)})

  DayOfWeek
  (datafy [d]
    {:day-name (.name d)})

  YearMonth
  (datafy [ym]
    (merge
      {:year         (.getYear       ym)
       :year-length  (.lengthOfYear  ym) ;; 365 or 366 depending on `.isLeapYear()`
       :leap-year?   (.isLeapYear    ym)
       :month        (.getMonthValue ym)
       :month-length (.lengthOfMonth ym)}
      (p/datafy (.getMonth ym))))

  LocalTime
  (datafy [lt]
    (let [nanos (.getNano lt)]
      {:hour-of-day      (.getHour   lt)
       :minute-of-hour   (.getMinute lt)
       :second-of-minute (.getSecond lt)
       :milli-of-second  (long (/ nanos 1000000))
       :micro-of-second  (long (/ nanos 1000))
       :nano-of-second   nanos}))

  LocalDateTime
  (datafy [ldt]
    (let [lt      (.toLocalTime ldt)
          weekday (.getDayOfWeek  ldt)
          ym (YearMonth/of (.getYear  ldt)
                           (.getMonth ldt))]
      (merge
        {:day-of-month (.getDayOfMonth ldt)
         :day-of-year  (.getDayOfYear  ldt)
         :day-of-week  (.getValue weekday)}
        (p/datafy ym)
        (p/datafy weekday)
        (p/datafy lt))))

  OffsetDateTime
  (datafy [odt]
    (let [ldt         (.toLocalDateTime odt)
          offset      (.getOffset odt)
          off-seconds (.getTotalSeconds offset)]
      (merge
        {:offset-id      (.getId offset)
         :offset-hours   (/ off-seconds 60)
         :offset-seconds off-seconds}
        (p/datafy ldt))))

  ZonedDateTime
  (datafy [zdt]
    (let [odt (.toOffsetDateTime zdt)]
      (merge
        {:zone-id (.getId (.getZone zdt))}
        (p/datafy odt))))

  Instant
  ;; a count of nanoseconds since the epoch of the first moment of 1970 in UTC
  (datafy [inst]
    (let [epoch-second (.getEpochSecond inst) ;; total seconds *until* <inst>
          nanos        (.getNano inst)        ;; total nanoseconds *since* <inst>
          epoch-nano   (-> epoch-second
                           (* 1000000000)
                           (+ nanos))         ;; total nanoseconds *until* <inst>
          epoch-milli (long (/ epoch-nano 1000000))
          epoch-micro (long (/ epoch-nano 1000))]
      {:epoch-second epoch-second
       :epoch-milli  epoch-milli
       :epoch-micro  epoch-micro
       :epoch-nano   epoch-nano}))
  )

(defn datafy
  "Main entry point for turning certain java.time Objects
   into plain Clojure maps. See `datafy+` if you want to remember
   the object <o> (e.g. for formatting etc)."
  [o]
  (p/datafy o))

(defn datafy+
  "Calls `datafy`, and attaches Object <o>  in the
   metadata of the result (under the `:object` key)."
  [o]
  (some-> (datafy o)
          (with-meta {:object o})))

(comment
  ;; example invocation
  (datafy (ZonedDateTime/now))
  ;; =>
  {:leap-year? true,
   :month-name "JANUARY",
   :nano-of-second 288112000,
   :offset-seconds 0,
   :day-of-week 4,
   :hour-of-day 10,
   :month 1,
   :offset-id "Z",
   :milli-of-second 288,
   :month-length 31,
   :micro-of-second 288112,
   :day-of-month 2,
   :day-name "THURSDAY",
   :year 2020,
   :day-of-year 2,
   :zone-id "Europe/London",
   :second-of-minute 11,
   :offset-hours 0,
   :minute-of-hour 10,
   :year-length 366}
  )