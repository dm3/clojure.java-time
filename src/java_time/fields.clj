(ns java-time.fields
  (:import (java.time.temporal IsoFields ChronoField JulianFields)))

(defonce iso
  {:week-based-year IsoFields/WEEK_BASED_YEAR
   :quarter-of-year IsoFields/QUARTER_OF_YEAR
   :day-of-quarter  IsoFields/DAY_OF_QUARTER
   :week-of-year    IsoFields/WEEK_OF_WEEK_BASED_YEAR})

(defonce chrono
  {:era               ChronoField/ERA
   :year              ChronoField/YEAR
   :year-of-era       ChronoField/YEAR_OF_ERA
   :proleptic-month   ChronoField/PROLEPTIC_MONTH
   :day-of-year       ChronoField/DAY_OF_YEAR
   :day-of-month      ChronoField/DAY_OF_MONTH
   :day-of-week       ChronoField/DAY_OF_WEEK
   :month-of-year     ChronoField/MONTH_OF_YEAR
   :hour-of-day       ChronoField/HOUR_OF_DAY
   :hour-of-ampm      ChronoField/HOUR_OF_AMPM
   :second-of-day     ChronoField/SECOND_OF_DAY
   :second-of-minute  ChronoField/SECOND_OF_MINUTE
   :milli-of-day      ChronoField/MILLI_OF_DAY
   :milli-of-second   ChronoField/MILLI_OF_SECOND
   :micro-of-day      ChronoField/MICRO_OF_DAY
   :micro-of-second   ChronoField/MICRO_OF_SECOND
   :nano-of-day       ChronoField/NANO_OF_DAY
   :nano-of-second    ChronoField/NANO_OF_SECOND
   :offset-seconds    ChronoField/OFFSET_SECONDS})

(defonce julian
  {:julian-day          JulianFields/JULIAN_DAY
   :modified-julian-day JulianFields/MODIFIED_JULIAN_DAY
   :rata-die            JulianFields/RATA_DIE})