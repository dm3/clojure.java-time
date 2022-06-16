(ns java-time.fields
  (:import (java.time.temporal IsoFields ChronoField
                               #?@(:bb []
                                   :default [JulianFields]))))

(defonce iso
  {:week-based-year IsoFields/WEEK_BASED_YEAR
   :quarter-of-year IsoFields/QUARTER_OF_YEAR
   :day-of-quarter  IsoFields/DAY_OF_QUARTER
   :week-of-year    IsoFields/WEEK_OF_WEEK_BASED_YEAR})

(defonce chrono
  {:era                          ChronoField/ERA
   :year                         ChronoField/YEAR
   :year-of-era                  ChronoField/YEAR_OF_ERA
   :proleptic-month              ChronoField/PROLEPTIC_MONTH
   :day-of-year                  ChronoField/DAY_OF_YEAR
   :day-of-month                 ChronoField/DAY_OF_MONTH
   :day-of-week                  ChronoField/DAY_OF_WEEK
   :month-of-year                ChronoField/MONTH_OF_YEAR
   :hour-of-day                  ChronoField/HOUR_OF_DAY
   :hour-of-ampm                 ChronoField/HOUR_OF_AMPM
   :second-of-day                ChronoField/SECOND_OF_DAY
   :second-of-minute             ChronoField/SECOND_OF_MINUTE
   :milli-of-day                 ChronoField/MILLI_OF_DAY
   :milli-of-second              ChronoField/MILLI_OF_SECOND
   :micro-of-day                 ChronoField/MICRO_OF_DAY
   :micro-of-second              ChronoField/MICRO_OF_SECOND
   :nano-of-day                  ChronoField/NANO_OF_DAY
   :nano-of-second               ChronoField/NANO_OF_SECOND
   :offset-seconds               ChronoField/OFFSET_SECONDS
   :aligned-day-of-week-in-month ChronoField/ALIGNED_DAY_OF_WEEK_IN_MONTH
   :aligned-day-of-week-in-year  ChronoField/ALIGNED_DAY_OF_WEEK_IN_YEAR
   :aligned-week-of-month        ChronoField/ALIGNED_WEEK_OF_MONTH
   :aligned-week-of-year         ChronoField/ALIGNED_WEEK_OF_YEAR
   :am-pm-of-day                 ChronoField/AMPM_OF_DAY
   :clock-hour-of-am-pm          ChronoField/CLOCK_HOUR_OF_AMPM
   :clock-hour-of-day            ChronoField/CLOCK_HOUR_OF_DAY
   :epoch-day                    ChronoField/EPOCH_DAY
   :instant-seconds              ChronoField/INSTANT_SECONDS
   :minute-of-day                ChronoField/MINUTE_OF_DAY
   :minute-of-hour               ChronoField/MINUTE_OF_HOUR})

(defonce julian
  #?(:bb {} ;; JulianFields not available in bb
     :default {:julian-day          JulianFields/JULIAN_DAY
               :modified-julian-day JulianFields/MODIFIED_JULIAN_DAY
               :rata-die            JulianFields/RATA_DIE}))
