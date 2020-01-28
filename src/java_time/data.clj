(ns java-time.data
  "A pure-data view of all the important/data-holding java.time objects."
  (:require [clojure.core.protocols :as p]
            [clojure.datafy :as d])
  (:import (java.time YearMonth Month DayOfWeek Instant
                      LocalTime LocalDate LocalDateTime
                      ZonedDateTime OffsetDateTime
                      ZoneOffset ZoneId)
           (java.time.format DateTimeFormatter)
           (java.time.temporal IsoFields JulianFields TemporalAccessor)))

(defn- dt-formatter
  ^DateTimeFormatter [x]
  (cond-> x
          (string? x)
          (DateTimeFormatter/ofPattern)))

(defn- zone-id
  ^ZoneId [x]
  (cond-> x
          (string? x)
          (ZoneId/of)))

(defn- zone-offset
  ^ZoneId [x]
  (cond-> x
          (string? x)
          (ZoneOffset/of)))

(defn- julian-field
  [^TemporalAccessor o x]
  (case x
    :day          (.get o JulianFields/JULIAN_DAY)
    :modified-day (.get o JulianFields/MODIFIED_JULIAN_DAY)
    :rata-die     (.get o JulianFields/RATA_DIE)
    nil))

(defonce ^:private system-zone
  (delay (ZoneId/systemDefault)))

(defonce ^:private system-offset
  (delay (ZoneOffset/systemDefault)))

(extend-protocol p/Datafiable

  Month
  (datafy [m]
    {:month {:name   (.name m)
             :value  (.getValue m)
             ;; ignoring leap-year precision on plain Month objects
             :length (.length m false)}}) 

  DayOfWeek
  (datafy [d]
    {:week {:day {:name  (.name d)
                  :value (.getValue d)}}})

  YearMonth
  (datafy [ym]
    (let [month (-> (d/datafy (.getMonth ym))
                    ;; correct the length here
                    (assoc-in [:month :length] (.lengthOfMonth ym)))]
      (with-meta
        {:year (merge month
                      {:length (.lengthOfYear ym) ;; 365 or 366 depending on `.isLeapYear()`
                       :leap?  (.isLeapYear ym)
                       :value  (.getYear ym)})}

        {`p/nav (fn [_ k v]
                  (case k
                    :before? (.isBefore ym v)
                    :after?  (.isAfter  ym v)
                    :instant (-> (.atDay ym 1)
                                 (.atStartOfDay)
                                 (.toInstant (or (zone-offset v) @system-offset)))
                    :format (.format (dt-formatter (or v "yyyy-MM")) ym)
                    nil))})))

  LocalTime
  (datafy [lt]
    (let [nanos (.getNano lt)]
      (with-meta
        {:day    {:hour   (.getHour   lt)}
         :hour   {:minute (.getMinute lt)}
         :minute {:second (.getSecond lt)}
         :second {:nano  nanos
                  :milli (long (/ nanos 1e6))
                  :micro (long (/ nanos 1e3))}}

        {`p/nav  (fn [_ k v]
                   (case k
                     :before? (.isBefore lt v)
                     :after?  (.isAfter  lt v)
                     :iso-format (.format DateTimeFormatter/ISO_TIME lt)
                     :format     (.format (dt-formatter v) lt)
                     nil))})))

  LocalDate
  (datafy [ld]
    (let [weekday (.getDayOfWeek  ld)
          ym      (YearMonth/of (.getYear ld)
                                (.getMonth ld))]
      (with-meta
        (merge (d/datafy weekday)
               (assoc-in (d/datafy ym) [:year :week]
                         (.get ld IsoFields/WEEK_OF_WEEK_BASED_YEAR)))

        {`p/nav (fn [_ k v]
                  (case k
                    :before?    (.isBefore ld v)
                    :after?     (.isAfter  ld v)
                    :iso-format (.format DateTimeFormatter/ISO_DATE ld)
                    :format     (.format (dt-formatter v) ld)
                    :julian     (julian-field ld v)
                    :instant  (-> (.atStartOfDay ld)
                                  (.toInstant (or (zone-offset v) @system-offset)))
                    :weekday    weekday
                    :year-month ym
                    nil))})))


  LocalDateTime
  (datafy [ldt]
    (let [lt (.toLocalTime ldt)
          d  (.toLocalDate ldt)]
      (with-meta
        (-> (merge-with merge (d/datafy lt) (d/datafy d))
            (assoc-in [:year :day]        (.getDayOfYear  ldt))
            (assoc-in [:year :month :day] (.getDayOfMonth ldt)))

        {`p/nav (fn [_ k v]
                  (case k
                    :before?    (.isBefore ldt v)
                    :after?     (.isAfter  ldt v)
                    :iso-format (.format DateTimeFormatter/ISO_LOCAL_DATE_TIME ldt)
                    :format     (.format (dt-formatter v) ldt)
                    :instant    (.toInstant ldt (or (zone-offset v) @system-offset))
                    :julian     (julian-field ldt v)
                    :local-time lt
                    :local-date d
                    nil))})))

  OffsetDateTime
  (datafy [odt]
    (let [ldt         (.toLocalDateTime odt)
          offset      (.getOffset odt)
          off-seconds (.getTotalSeconds offset)]
      (with-meta
        (-> (d/datafy ldt)
            (assoc :offset {:id      (.getId offset)
                            :hours   (/ off-seconds 60)
                            :seconds off-seconds}))

        {`p/nav (fn [_ k v]
                  (case k
                    :before?    (.isBefore odt v)
                    :after?     (.isAfter  odt v)
                    :iso-format (.format DateTimeFormatter/ISO_OFFSET_DATE_TIME odt)
                    :format     (.format (dt-formatter v) ldt)
                    :instant    (.toInstant odt)
                    :julian     (julian-field odt v)
                    :local-datetime ldt
                    nil))})))

  ZonedDateTime
  (datafy [zdt]
    (let [odt  (.toOffsetDateTime zdt)
          zone (.getZone zdt)]
      (with-meta
        (-> (d/datafy odt)
            (assoc-in [:zone :id] (.getId zone)))

        {`p/nav (fn [_ k v]
                  (case k
                    :before?    (.isBefore zdt v)
                    :after?     (.isAfter  zdt v)
                    :iso-format (.format DateTimeFormatter/ISO_ZONED_DATE_TIME zdt)
                    :format     (.format (dt-formatter v) zdt)
                    :instant    (.toInstant zdt)
                    :julian     (julian-field zdt v)
                    :offset-datetime odt
                    nil))})))

  Instant
  ;; a count of nanoseconds since the epoch of the first moment of 1970 in UTC
  (datafy [inst]
    (let [epoch-second (.getEpochSecond inst) ;; total seconds *until* <inst>
          nanos        (.getNano inst)        ;; total nanoseconds *since* <inst>
          epoch-nano   (-> epoch-second
                           (* 1e9)
                           (+ nanos))         ;; total nanoseconds *until* <inst>
          epoch-milli (long (/ epoch-nano 1e6))
          epoch-micro (long (/ epoch-nano 1e3))]
      (with-meta
        {:epoch {:second epoch-second
                 :milli  epoch-milli
                 :micro  epoch-micro
                 :nano   epoch-nano}}
        {`p/nav (fn [_ k v]
                  (case k
                    :before?    (.isBefore inst v)
                    :after?     (.isAfter  inst v)
                    :iso-format (.format DateTimeFormatter/ISO_INSTANT inst)
                    :format     (.format ^DateTimeFormatter v inst)
                    :local-time      (LocalTime/ofInstant      inst  (or (zone-id v) @system-zone))
                    :local-date      (LocalDate/ofInstant      inst  (or (zone-id v) @system-zone))
                    :local-datetime  (LocalDateTime/ofInstant  inst  (or (zone-id v) @system-zone))
                    :offset-datetime (OffsetDateTime/ofInstant inst  (or (zone-id v) @system-zone))
                    :zoned-datetime  (ZonedDateTime/ofInstant  inst  (or (zone-id v) @system-zone))
                    nil))})))
  )

(comment
  ;; example invocation
  (d/datafy (ZonedDateTime/now))
  ;;=>
  {:day  {:hour 20},
   :hour {:minute 9},
   :week {:day {:name "WEDNESDAY",
                :value 3}},
   :second {:nano 11914000,
            :milli 11,
            :micro 11914},
   :offset {:id "Z",
            :hours 0,
            :seconds 0},
   :zone {:id "Europe/London"},
   :year {:month {:name "JANUARY",
                  :value 1,
                  :length 31,
                  :day 8},
          :length 366,
          :leap? true,
          :value 2020,
          :day 8
          :week 2},
   :minute {:second 34}}

  (d/datafy (Instant/now))
  ;;=>
   {:epoch {:second 1578514227,
            :milli 1578514227681,
            :micro 1578514227681499,
            :nano 1.57851422768149888E18}}

  )