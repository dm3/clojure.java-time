(ns java-time.chrono
  (:require [java-time.core :as jt.c])
  (:import [java.time.chrono ChronoPeriod ChronoLocalDate ChronoLocalDateTime ChronoZonedDateTime]
           [java.time.temporal TemporalAmount]))

(defn- ^ChronoPeriod cp-plus [^ChronoPeriod cp, ^TemporalAmount o]
  (.plus cp o))

(defn- ^ChronoPeriod cp-minus [^ChronoPeriod cp, ^TemporalAmount o]
  (.minus cp o))

(extend-type ChronoPeriod
  jt.c/Plusable
  (seq-plus [cp tas]
    (reduce cp-plus cp tas))

  jt.c/Minusable
  (seq-minus [cp tas]
    (reduce cp-minus cp tas))

  jt.c/Multipliable
  (multiply-by [cp v]
    (.multipliedBy cp (int v)))

  jt.c/Amount
  (zero? [cp]
    (.isZero cp))

  (negative? [cp]
    (.isNegative cp))

  (negate [cp]
    (.negated cp)))

(extend-protocol jt.c/Ordered
  ChronoLocalDate
  (single-after? [d o]
    (.isAfter d o))
  (single-before? [d o]
    (.isBefore d o))

  ChronoLocalDateTime
  (single-after? [d o]
    (.isAfter d o))
  (single-before? [d o]
    (.isBefore d o))

  ChronoZonedDateTime
  (single-after? [d o]
    (.isAfter d o))
  (single-before? [d o]
    (.isBefore d o)))

(extend-protocol jt.c/HasChronology
  ChronoPeriod
  (chronology [o]
    (.getChronology o))

  ChronoLocalDate
  (chronology [o]
    (.getChronology o))

  ChronoLocalDateTime
  (chronology [o]
    (.getChronology o))

  ChronoZonedDateTime
  (chronology [o]
    (.getChronology o)))
