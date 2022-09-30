(ns java-time.mock
  (:require [java-time
             [amount :as amount]
             [core :as core]
             [temporal :as temporal]
             [zone :as zone]])
  (:import (java.time Clock)))

(definterface IMockClock
  (^void advanceClock [amount]))

(defn ^Clock mock-clock
  "Returns a mock implementation of the `java.time.Clock`. The mock supports
  `advance-clock!` operation which allows to move the time in the clock, e.g.:

  ```
  (let [clock (mock-clock 0 \"UTC\")]
    (with-clock clock
      (is (= (value clock) 0))
      (is (= (instant) (instant 0)))
      (advance-clock! clock (j/millis 1))
      (is (= (value clock) 1))
      (is (= (instant) (instant 1)))))
  ```

  You can move the clock back via advancing by a negative temporal amount.

  Creates a clock at epoch in the default time zone when called without arguments.
  
  Not available in babashka."
  ([] (mock-clock 0))
  ([instant] (mock-clock instant (zone/zone-id)))
  ([instant zone]
   (let [!instant (atom (temporal/instant instant))
         zone (zone/zone-id zone)]
     (proxy [Clock IMockClock] []
       (advanceClock [amount]
         (swap! !instant core/plus amount)
         nil)

       (getZone [] zone)
       (withZone [zone']
         (mock-clock @!instant zone'))
       (instant [] @!instant)

       (equals [other]
         (if (and (instance? IMockClock other) (zone/clock? other))
           (let [^Clock clock other]
             (and (= zone (.getZone clock))
                  (= @!instant (.instant clock))))
           false))
       (hashCode []
         (bit-xor (hash @!instant) (hash zone)))
       (toString []
         (str "MockClock[" @!instant "," zone "]"))))))

(defn advance-clock!
  "Advances the `clock` by the given time `amount`.

  This mutates the mock clock.
  
  Not available in babashka."
  [^IMockClock clock amount]
  (.advanceClock clock amount))

(defn set-clock!
  "Sets the `clock` to the given `time`.

  This mutates the mock clock.

  Not available in babashka."
  [^Clock clock time]
  (let [current (.instant clock)]
    (->> (core/time-between :millis current (temporal/instant time))
         (amount/millis)
         (advance-clock! clock))))
