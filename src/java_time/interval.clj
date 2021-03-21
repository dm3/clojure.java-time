(ns java-time.interval
  (:refer-clojure :exclude [contains?])
  (:require [clojure.string :as string]
            [java-time.util :as jt.u]
            [java-time.core :as jt.c]
            [java-time.temporal :as jt.t]
            [java-time.amount :as jt.a])
  (:import [java.time Instant Duration]))

(defprotocol ^:private AnyInterval
  (seq-move-start-by [i os]
    "Use `move-start-by` with vararags")
  (seq-move-end-by [i os]
    "Use `move-end-by` with vararags")
  (move-start-to [i new-start]
   "Moves the start instant of the interval to the given instant (or something
   convertible to an instant):

   (move-start-to (interval 0 10000) (instant 5000))
   => #<Interval ...:05Z/...:10Z>

  Fails if the new start instant falls after the end instant:

   (move-start-to (interval 0 10000) (millis 15000))
   => DateTimeException...")
  (move-end-to [i new-end]
   "Moves the end of the interval to the given instant (or something
   convertible to an instant):

   (move-end-to (interval 0 10000) (instant 15000))
   => #<Interval ...:00Z/...:15Z>

  Fails if the new end instant falls before the start instant:

   (move-end-to (interval 0 10000) (millis -1))
   => DateTimeException...")
  (start [i] "Gets the start instant of the interval")
  (end [i] "Gets the end instant of the interval")
  (contains? [i o] "True if the interval contains the given instant or interval")
  (overlaps? [i oi] "True if this interval overlaps the other one")
  (abuts? [i oi] "True if this interval abut with the other one")
  (overlap [i oi] "Gets the overlap between this interval and the other one or `nil`")
  (gap [i oi] "Gets the gap between this interval and the other one or `nil`"))

;;;;;;;;;;;;; ReadableInterval

(jt.u/when-threeten-extra
  (import [org.threeten.extra Interval])
  (defn interval?
    "True if `Interval`"
    [o] (instance? Interval o))

  (defn ^Interval interval
    "Constructs an interval out of a string, start and end instants or a start
    + duration:

    (j/interval \"2010-01-01T00:00:00Z/2013-01-01T00:00:00Z\")
    => #<Interval 2010-01-01T00:00:00Z/2013-01-01T00:00:00Z>

    (j/interval (j/instant 100000) (j/instant 1000000))
    => #<Interval 1970-01-01T00:01:40Z/1970-01-01T00:16:40Z>

    (j/interval (j/instant 100000) (j/duration 15 :minutes))
    => #<Interval 1970-01-01T00:01:40Z/1970-01-01T00:16:40Z>

    Requires the optional `threeten-extra` dependency."
    ([^String o] (Interval/parse o))
    ([a b]
     (cond (and (jt.t/instant? a) (jt.t/instant? b))
           (Interval/of ^Instant a ^Instant b)

           (jt.a/duration? b)
           (Interval/of (jt.t/instant a) ^Duration b)

           :else (Interval/of (jt.t/instant a) (jt.t/instant b)))))

  (defn- with-start [^Interval i ^Instant s]
    (.withStart i s))

  (defn- with-end [^Interval i ^Instant e]
    (.withEnd i e))

  (extend-type Interval
    AnyInterval
    (seq-move-start-by [i os]
      (let [^Instant s (jt.c/seq-plus (.getStart i) os)]
        (with-start i s)))
    (seq-move-end-by [i os]
      (let [^Instant e (jt.c/seq-plus (.getEnd i) os)]
        (with-end i e)))
    (move-start-to [i new-start]
      (with-start i (jt.t/instant new-start)))
    (move-end-to [i new-end]
      (with-end i (jt.t/instant new-end)))
    (start [i] (.getStart i))
    (end [i] (.getEnd i))
    (contains? [i o] (if (interval? o)
                       (.encloses i ^Interval o)
                       (.contains i (jt.t/instant o))))
    (overlaps? [i oi] (.overlaps i oi))
    (abuts? [i oi] (.abuts i oi))

    (overlap [self ^Interval i]
      (when (overlaps? self i)
        (interval (jt.c/max (start self) (start i))
                  (jt.c/min (end self) (end i)))))
    (gap [self ^Interval i]
      (cond (.isAfter (.getStart self) (.getEnd i))
            (interval (.getEnd i) (.getStart self))

            (.isBefore (.getEnd self) (.getStart i))
            (interval (.getEnd self) (.getStart i))))

    jt.c/Ordered
    (single-before? [i o] (if (jt.t/instant? o)
                            (.isBefore (.getEnd i) o)
                            (.isBefore (.getEnd i) (.getStart ^Interval o))))
    (single-after? [i o] (if (jt.t/instant? o)
                           (.isAfter (.getStart i) o)
                           (.isAfter (.getStart i) (.getEnd ^Interval o))))

    jt.c/As
    (as* [o k]
      (jt.c/as (.toDuration o) k))))

(defn move-start-by
  "Moves the start instant of the interval by the sum of given
  periods/durations/numbers of milliseconds:

    (move-start-by (interval 0 10000) (millis 1000) (seconds 1))
    => #<Interval ...:02Z/...:10Z>

  Fails if the new start instant falls after the end instant.

    (move-start-by (interval 0 10000) (millis 11000))
    ; => DateTimeException..."
  [i & os] (seq-move-start-by i os))

(defn move-end-by
  "Moves the end instant of the interval by the sum of given
  periods/durations/numbers of milliseconds.

    (move-start-by (interval 0 10000) (millis 1000) (seconds 1))
    => #<Interval ...:00Z/...:12Z>

  Fails if the new end instant falls before the start instant.

    (move-end-by (interval 0 10000) (millis -11000))
    => DateTimeException..."
  [i & os] (seq-move-end-by i os))
