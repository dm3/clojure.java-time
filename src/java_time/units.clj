(ns java-time.units
  (:import (java.time.temporal IsoFields ChronoUnit)))

(defonce iso
  {:week-based-years IsoFields/WEEK_BASED_YEARS
   :quarter-years    IsoFields/QUARTER_YEARS})

(defonce chrono
  {:millis     ChronoUnit/MILLIS
   :weeks      ChronoUnit/WEEKS
   :centuries  ChronoUnit/CENTURIES
   :minutes    ChronoUnit/MINUTES
   :days       ChronoUnit/DAYS
   :years      ChronoUnit/YEARS
   :seconds    ChronoUnit/SECONDS
   :nanos      ChronoUnit/NANOS
   :decades    ChronoUnit/DECADES
   :forever    ChronoUnit/FOREVER
   :hours      ChronoUnit/HOURS
   :micros     ChronoUnit/MICROS
   :millenia   ChronoUnit/MILLENNIA
   :months     ChronoUnit/MONTHS
   :half-days  ChronoUnit/HALF_DAYS
   :eras       ChronoUnit/ERAS})
