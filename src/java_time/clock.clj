(ns java-time.clock
  (:require [java-time.core :as jt.c])
  (:import [java.time Clock Instant]))

(def ^:dynamic ^Clock *clock* nil)

(defn make [f]
  (if *clock*
    (f *clock*)
    (f (Clock/systemDefaultZone))))

(defn with-clock-fn
  "Executes the given function in the scope of the provided clock. All the
  temporal entities that get created without parameters will inherit their
  values from the clock."
  [^Clock c f]
  (binding [*clock* c]
    (f)))

(defmacro with-clock
  "Executes the given `forms` in the scope of the provided `clock`.

  All the temporal entities that get created without parameters will inherit
  their values from the clock:

    (with-clock (system-clock \"Europe/London\")
      (zone-id))
    => #<java.time.ZoneRegion Europe/London>"
  [c & forms]
  `(with-clock-fn ~c (fn [] ~@forms)))
