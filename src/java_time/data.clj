(ns java-time.data
  "A pure-data view of all the important/data-holding java.time objects."
  (:require [clojure.core.protocols :as p]
            [clojure.datafy :as d])
  (:import (java.time YearMonth Month DayOfWeek Instant
                      LocalTime LocalDateTime
                      ZonedDateTime OffsetDateTime LocalDate)
           (java.time.format DateTimeFormatter)))

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
    {:year (-> (d/datafy (.getMonth ym))
               (assoc-in [:month :length] (.lengthOfMonth ym)) ;; correct the length here
               (merge {:length (.lengthOfYear ym)              ;; 365 or 366 depending on `.isLeapYear()`
                       :leap?  (.isLeapYear ym)
                       :value  (.getYear ym)}))})

  LocalTime
  (datafy [lt]
    (let [nanos (.getNano lt)]
      {:iso    {:local-time (.format DateTimeFormatter/ISO_TIME lt)}
       :day    {:hour   (.getHour   lt)}
       :hour   {:minute (.getMinute lt)}
       :minute {:second (.getSecond lt)}
       :second {:nano nanos
                :milli (long (/ nanos 1e6))
                :micro (long (/ nanos 1e3))}}))

  LocalDate
  (datafy [ld]
    (let [weekday (.getDayOfWeek  ld)
          ym      (YearMonth/of (.getYear ld)
                                (.getMonth ld))]

      (merge (d/datafy weekday)
             (d/datafy ym)
             {:iso {:local-date (.format DateTimeFormatter/ISO_DATE ld)}})))

  LocalDateTime
  (datafy [ldt]
    (let [lt (.toLocalTime ldt)
          d  (.toLocalDate ldt)]
      (-> (merge-with merge
                      (d/datafy lt)
                      (d/datafy d))
          (assoc-in [:year :day] (.getDayOfYear  ldt))
          (assoc-in [:year :month :day] (.getDayOfMonth ldt)))))

  OffsetDateTime
  (datafy [odt]
    (let [ldt         (.toLocalDateTime odt)
          offset      (.getOffset odt)
          off-seconds (.getTotalSeconds offset)]
      (-> (d/datafy ldt)
          (assoc-in [:iso :offset-datetime] (.format DateTimeFormatter/ISO_OFFSET_DATE_TIME odt))
          (assoc :offset {:id    (.getId offset)
                          :hours (/ off-seconds 60)
                          :seconds off-seconds}))))

  ZonedDateTime
  (datafy [zdt]
    (let [odt  (.toOffsetDateTime zdt)
          zone (.getZone zdt)]
      (-> (d/datafy odt)
          (assoc-in [:iso :zoned-datetime] (.format DateTimeFormatter/ISO_ZONED_DATE_TIME zdt))
          (assoc-in [:zone :id] (.getId zone)))))

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
      {:epoch {:second epoch-second
               :milli  epoch-milli
               :micro  epoch-micro
               :nano   epoch-nano}}))
  )

(defn datafy+
  "Calls `d/datafy`, and attaches Object <o>  in the
   metadata of the result (under the `:object` key)."
  [o]
  (some-> (d/datafy o)
          (with-meta {:object o})))

(comment
  ;; example invocation
  (d/datafy (ZonedDateTime/now))
  ;;=>
  {:day {:hour 20},
   :hour {:minute 9},
   :week {:day {:name "WEDNESDAY", :value 3}},
   :second {:nano 11914000, :milli 11, :micro 11914},
   :offset {:id "Z", :hours 0, :seconds 0},
   :zone {:id "Europe/London"},
   :iso {:local-time "20:09:34.011914",
         :local-date "2020-01-08",
         :offset-datetime "2020-01-08T20:09:34.011914Z",
         :zoned-datetime "2020-01-08T20:09:34.011914Z[Europe/London]"},
   :year {:month {:name "JANUARY", :value 1, :length 31, :day 8}, :length 366, :leap? true, :value 2020, :day 8},
   :minute {:second 34}}

  (d/datafy (Instant/now))
  ;;=>
   {:epoch {:second 1578514227,
            :milli 1578514227681,
            :micro 1578514227681499,
            :nano 1.57851422768149888E18}}

  )