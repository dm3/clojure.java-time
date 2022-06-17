(ns java-time
  (:refer-clojure :exclude (zero? range iterate max min contains? format abs))
  (:require [java-time.potemkin.namespaces :as potemkin]
            [java-time.util :as jt.u]
            [java-time core properties temporal amount zone single-field local chrono
             convert sugar seqs adjuster interval format joda clock pre-java8 #?@(:bb [] :default [mock])]))

(potemkin/import-vars
  [java-time.clock
   with-clock with-clock-fn]

  [java-time.core
   zero? negative? negate abs max min 
   before? not-after? after? not-before?
   supports?
   fields units properties property
   as value range min-value max-value largest-min-value smallest-max-value
   truncate-to time-between with-zone 
   plus minus multiply-by
   ;; TODO below here needs unit tests
   chronology leap? with-value with-min-value with-max-value with-largest-min-value with-smallest-max-value]

  [java-time.amount
   duration period period? duration?
   nanos micros millis seconds minutes hours standard-days
   days weeks months years]

  [java-time.properties
   unit? unit field? field]

  [java-time.temporal
   value-range instant instant?]

  [java-time.local
   local-date local-date-time local-time
   local-date? local-date-time? local-time?]

  [java-time.single-field
   year year? month month? day-of-week day-of-week? month-day month-day?
   year-month year-month?]

  [java-time.zone
   available-zone-ids zone-id zone-offset
   offset-date-time offset-time zoned-date-time
   system-clock fixed-clock offset-clock tick-clock clock?
   zone-id? zoned-date-time? offset-date-time? offset-time?
   with-zone-same-instant with-offset with-offset-same-instant]

  #?@(:bb []
      :default [[java-time.mock
                 mock-clock advance-clock! set-clock!]])

  [java-time.convert
   as-map convert-amount to-java-date to-sql-date to-sql-timestamp
   to-millis-from-epoch]

  [java-time.sugar
   monday? tuesday? wednesday? thursday? friday? saturday? sunday?
   weekend? weekday?]

  [java-time.seqs
   iterate]

  [java-time.adjuster
   adjust]

  [java-time.format
   format formatter]

  [java-time.pre-java8
   java-date sql-date sql-timestamp instant->sql-timestamp #?@(:bb [] :default [sql-time])]

  [java-time.interval
   move-start-to move-end-to move-start-by move-end-by
   start end contains? overlaps? abuts? overlap gap]

  [java-time.util
   when-joda-time-loaded])

(jt.u/when-threeten-extra
  (potemkin/import-vars
    [java-time.interval interval interval?]

    [java-time.single-field
     am-pm am-pm? quarter quarter? day-of-month day-of-month?
     day-of-year day-of-year? year-quarter year-quarter?]))
