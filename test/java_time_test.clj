(ns java-time-test
  (:require [clojure.test :refer :all]
            [java-time.util :as jt.u]
            [java-time :as j]))

(def clock (j/fixed-clock "2015-11-26T10:20:30.000000040Z" "UTC"))

(deftest constructors
  (testing "clocks"
    (testing ", with-clock"
      (are [f] (= (j/with-clock clock (f)) (f clock))
           j/zoned-date-time
           j/offset-date-time
           j/offset-time
           j/local-date-time
           j/local-time
           j/local-date
           j/zone-offset
           j/zone-id))

    (testing ", system"
      (let [now-millis (j/value (j/system-clock))]
        (is (<= now-millis
                (j/value (j/system-clock "UTC"))))
        (is (= (j/system-clock "UTC")
               (j/with-zone (j/system-clock "Europe/Zurich") "UTC")))))

    (testing ", fixed"
      (is (= (j/value (j/fixed-clock "2015-01-01T10:20:30Z" "UTC"))
             (j/value (j/fixed-clock "2015-01-01T10:20:30Z")))))

    (testing ", offset"
      (is (= (str (-> (j/fixed-clock "2015-01-01T10:20:30Z" "UTC")
                      (j/offset-clock (j/minutes 30))))
             "OffsetClock[FixedClock[2015-01-01T10:20:30Z,UTC],PT30M]")))

    (testing ", tick"
      (is (= (str (-> (j/fixed-clock "2015-01-01T10:20:30Z" "UTC")
                      (j/tick-clock (j/minutes 10))))
             "TickClock[FixedClock[2015-01-01T10:20:30Z,UTC],PT10M]"))))

  (testing "offsets"
    (is (= (j/zone-offset +0)
           (j/zone-offset "+00:00")
           (j/zone-offset -0)
           (j/zone-offset 0 0)))

    (is (= (j/zone-offset 1 30)
           (j/zone-offset "+01:30")
           (j/zone-offset 1 30 0)
           (j/zone-offset +1.5))))

  (testing "enums"
    (is (= (j/month 11)
           (j/month :november)
           (j/month (j/local-date clock))
           (j/month "MM" "11")))

    (is (j/month? (j/month 7)))

    (is (= (j/day-of-week 4)
           (j/day-of-week :thursday)
           (j/day-of-week (j/local-date clock))
           (j/day-of-week "ee" "05")))

    (is (j/day-of-week? (j/day-of-week 4))))

  (testing "multi field"
    (is (= (j/month-day (j/local-date clock))
           (j/month-day 11 26)
           (j/month-day "dd-MM" "26-11")))

    (is (= (j/month-day 1)
           (j/month-day (j/month 1))
           (j/month-day 1 1)))

    (is (j/month-day? (j/month-day 1 1)))

    (is (= (j/year-month (j/local-date clock))
           (j/year-month 2015 11)
           (j/year-month "yy-MM" "15-11")))

    (is (= (j/year-month 1)
           (j/year-month (j/year 1))
           (j/year-month 1 1)))

    (is (j/year-month? (j/year-month 1 1))))

  (testing "years"
    (is (= (j/year clock)
           (j/year "2015")
           (j/year 2015)
           (j/year "yy" "15")))

    (is (= (j/year "UTC")
           (j/year (j/zone-id "UTC"))))

    (is (j/year? (j/year 2015))))

  (testing "local date"
    (is (= (j/local-date clock)
           (j/local-date 2015 11 26)
           (j/local-date "2015-11-26")
           (j/local-date "yyyy/MM/dd" "2015/11/26")
           (j/local-date (j/local-date 2015 11 26))
           (j/local-date (j/local-date-time clock))
           (j/local-date (j/zoned-date-time clock))
           (j/local-date (j/offset-date-time clock))
           (j/local-date (j/instant clock) "UTC")
           (j/local-date (j/to-java-date clock) "UTC")))

    (is (j/local-date? (j/local-date)))

    (is (= (j/local-date 2015)
           (j/local-date 2015 1)
           (j/local-date 2015 1 1)
           (j/local-date (j/year 2015) (j/month 1))
           (j/local-date (j/year 2015) (j/month 1) (j/day-of-week 1)))))

  (testing "local time"
    (is (= (j/local-time clock)
           (j/local-time 10 20 30 40)
           (j/local-time "10:20:30.000000040")
           (j/local-time "HH:mm,ss:SSSSSSSSS" "10:20,30:000000040")
           (j/local-time (j/local-time clock))
           (j/local-time (j/local-date-time clock))
           (j/local-time (j/zoned-date-time clock))
           (j/local-time (j/offset-date-time clock))
           (j/local-time (j/instant clock) "UTC")))

    (is (= (j/truncate-to (j/local-time clock) :millis)
          (j/local-time (j/to-java-date clock) "UTC")))

    (is (j/local-time? (j/local-time)))

    (is (= (j/local-time 10)
           (j/local-time 10 0)
           (j/local-time 10 0 0)
           (j/local-time 10 0 0 0)))

    (is (= (j/truncate-to (j/local-time 10 20 30 40) :minutes)
           (j/local-time 10 20))))

  (testing "local date time"
    (is (= (j/local-date-time clock)
           (j/local-date-time 2015 11 26 10 20 30 40)
           (j/local-date-time "2015-11-26T10:20:30.000000040")
           (j/local-date-time "yyyy/MM/dd'T'SSSSSSSSS,HH:mm:ss" "2015/11/26T000000040,10:20:30")
           (j/local-date-time (j/local-date 2015 11 26) (j/local-time 10 20 30 40))
           (j/local-date-time (j/local-date-time clock))
           (j/local-date-time (j/zoned-date-time clock))
           (j/local-date-time (j/offset-date-time clock))
           (j/local-date-time (j/instant clock) "UTC")))

    (is (= (j/truncate-to (j/local-date-time clock) :millis)
          (j/local-date-time (j/to-java-date clock) "UTC")))

    (is (j/local-date-time? (j/local-date-time)))

    (is (= (j/local-date-time 2015)
           (j/local-date-time 2015 1)
           (j/local-date-time 2015 1 1)
           (j/local-date-time (j/year 2015) (j/month 1))
           (j/local-date-time (j/year 2015) (j/month 1) (j/day-of-week 1))
           (j/local-date-time 2015 1 1 0)
           (j/local-date-time 2015 1 1 0 0)
           (j/local-date-time 2015 1 1 0 0 0)
           (j/local-date-time 2015 1 1 0 0 0 0)))

    (is (= (j/truncate-to (j/local-date-time 2015 1 1 10 20 30 40) :minutes)
           (j/local-date-time 2015 1 1 10 20))))

  (testing "zoned date time"
    (is (= (j/zoned-date-time clock)
           (j/zoned-date-time (j/zoned-date-time clock))
           (j/zoned-date-time "2015-11-26T10:20:30.000000040+00:00[UTC]")
           (j/zoned-date-time "2015-11-26T10:20:30.000000040Z[UTC]")
           (j/zoned-date-time "yyyy/MM/dd'T'HH:mm:ss-SSSSSSSSS'['VV']'" "2015/11/26T10:20:30-000000040[UTC]")
           (j/zoned-date-time (j/local-date clock) (j/local-time clock) "UTC")
           (j/zoned-date-time (j/local-date-time clock) "UTC")
           (j/zoned-date-time (j/offset-date-time clock) "UTC")
           (j/zoned-date-time 2015 11 26 10 20 30 40 "UTC")
           (j/zoned-date-time (j/instant clock) "UTC")))

    (is (= (j/truncate-to (j/zoned-date-time clock) :millis)
          (j/zoned-date-time (j/to-java-date clock) "UTC")))

    (is (j/zoned-date-time? (j/zoned-date-time (j/zone-id "UTC"))))

    (is (= (j/zoned-date-time 2015 "UTC")
           (j/zoned-date-time 2015 1 "UTC")
           (j/zoned-date-time 2015 1 1 "UTC")
           (j/zoned-date-time (j/year 2015) "UTC")
           (j/zoned-date-time (j/year 2015) (j/month 1) "UTC")
           (j/zoned-date-time (j/year 2015) (j/month 1) (j/day-of-week 1) "UTC")
           (j/zoned-date-time 2015 1 1 0 "UTC")
           (j/zoned-date-time 2015 1 1 0 0 "UTC")
           (j/zoned-date-time 2015 1 1 0 0 0 "UTC")
           (j/zoned-date-time 2015 1 1 0 0 0 0 "UTC")))

    (is (= (j/truncate-to (j/zoned-date-time 2015 1 1 10 20 30 40 "UTC") :minutes)
           (j/zoned-date-time 2015 1 1 10 20 "UTC"))))

  (testing "offset date time"
    (is (= (j/offset-date-time clock)
           (j/offset-date-time (j/offset-date-time clock))
           (j/offset-date-time "2015-11-26T10:20:30.000000040+00:00")
           (j/offset-date-time "2015-11-26T10:20:30.000000040Z")
           (j/offset-date-time "yyyy/MM/dd'T'HH:mm:ss-SSSSSSSSS'['X']'" "2015/11/26T10:20:30-000000040[Z]")
           (j/offset-date-time (j/local-date clock) (j/local-time clock) +0)
           (j/offset-date-time (j/local-date-time clock) +0)
           (j/offset-date-time (j/zoned-date-time clock) +0)
           (j/offset-date-time 2015 11 26 10 20 30 40 +0)
           (j/offset-date-time (j/instant clock) "UTC")))

    (is (= (j/truncate-to (j/offset-date-time clock) :millis)
          (j/offset-date-time (j/to-java-date clock) "UTC")))

    (is (j/offset-date-time? (j/offset-date-time)))

    (is (= (j/offset-date-time 2015 +0)
           (j/offset-date-time 2015 1 +0)
           (j/offset-date-time 2015 1 1 +0)
           (j/offset-date-time (j/year 2015) +0)
           (j/offset-date-time (j/year 2015) (j/month 1) +0)
           (j/offset-date-time (j/year 2015) (j/month 1) (j/day-of-week 1) +0)
           (j/offset-date-time 2015 1 1 0 +0)
           (j/offset-date-time 2015 1 1 0 0 +0)
           (j/offset-date-time 2015 1 1 0 0 0 +0)
           (j/offset-date-time 2015 1 1 0 0 0 0 +0)))

    (is (= (j/truncate-to (j/offset-date-time 2015 1 1 10 20 30 40 0) :minutes)
           (j/offset-date-time 2015 1 1 10 20 0))))

  (testing "offset time"
    (is (= (j/offset-time clock)
           (j/offset-time (j/offset-time clock))
           (j/offset-time (j/zoned-date-time clock))
           (j/offset-time "10:20:30.000000040+00:00")
           (j/offset-time "10:20:30.000000040Z")
           (j/offset-time "HH:mm:ss-SSSSSSSSS'['X']'" "10:20:30-000000040[Z]")
           (j/offset-time (j/local-time clock) +0)
           (j/offset-time (j/instant clock) "UTC")
           (j/offset-time 10 20 30 40 +0)
           (j/offset-time 10 20 30 40 +0)
           (j/offset-time (j/instant clock) "UTC")))

    (is (= (j/truncate-to (j/offset-time clock) :millis)
          (j/offset-time (j/to-java-date clock) "UTC")))

    (is (j/offset-time? (j/offset-time (j/zone-id "UTC"))))
    (is (j/offset-time? (j/offset-time +0)))

    (is (= (j/offset-time 0 +0)
           (j/offset-time 0 0 +0)
           (j/offset-time 0 0 0 +0)
           (j/offset-time 0 0 0 0 +0)))

    (is (= (j/truncate-to (j/offset-time 10 20 30 40 0) :minutes)
           (j/offset-time 10 20 0))))

   (testing "instant"
     (is (= (j/instant clock)
            (j/instant "2015-11-26T10:20:30.000000040Z")
            (j/instant "yyyy/MM/dd'T'HH:mm:ss-SSSSSSSSS'['X']'" "2015/11/26T10:20:30-000000040[Z]")
            (j/instant 1448533230 40)))

     (is (= (j/truncate-to (j/instant clock) :millis)
            ;; (.toEpochMilli instant)
            (j/instant 1448533230000))))

   (testing "duration"
     (is (= (j/duration 100)
            (j/duration (j/duration 100))
            (j/duration "PT0.1S")
            (j/duration (j/local-time 0 0 0 0) (j/local-time 0 0 0 (* 100 1000 1000)))
            (j/duration 100 :millis)))

     (is (j/duration? (j/duration))))

   (testing "period"
     (is (= (j/period 10 20 30)
            (j/period "P10Y20M30D")))

     (is (= (j/period 11 9)
            (j/period (j/local-date 2001 1 1) (j/local-date 2012 10 1))))

     (is (= (j/period)
            (j/period 0)
            (j/period 0 0)
            (j/period 0 0 0)
            (j/period 0 :years)
            (j/period 0 :months)
            (j/period 0 :days)))

     (is (j/period? (j/period)))))

(deftest operations
  (testing "duration"
    (testing "plus"
      (is (= (j/duration 100000001)
             (j/plus (j/standard-days 1) (j/hours 3) (j/minutes 46) (j/seconds 40) (j/millis 1) (j/nanos 0))
             (j/plus (j/duration 1 :days)
                     (j/duration 3 :hours)
                     (j/duration 46 :minutes)
                     (j/duration 40 :seconds)
                     (j/duration 1 :millis)
                     (j/duration 0 :nanos)))))

    (testing "minus"
      (is (= (j/duration "PT22H58M58.998999999S")
             (j/minus (j/standard-days 1) (j/hours 1) (j/minutes 1) (j/seconds 1) (j/millis 1) (j/nanos 1)))))

    (testing "multiply"
      (is (= (j/hours 2)
             (j/multiply-by (j/hours 1) 2))))

    (testing "number ops"
      (is (j/zero? (j/duration 0)))
      (is (= (j/duration 10) (j/abs (j/duration -10))))
      (is (= (j/duration -10) (j/negate (j/duration 10))))
      (is (j/negative? (j/duration -10)))))

  (testing "period"
    (testing "plus"
      (is (= (j/period 10 20 30)
             (j/plus (j/years 10) (j/months 20) (j/days 30))
             (j/plus (j/period 10)
                     (j/period 0 20)
                     (j/period 0 0 30))
             (j/plus (j/period 10 :years)
                     (j/period 20 :months)
                     (j/period 30 :days)))))

    (testing "minus"
      (is (= (j/period 0 0 0)
             (j/minus (j/period 10 20 30)
                      (j/years 10) (j/months 20) (j/days 30)))))

    (testing "multiply"
      (is (= (j/days 2)
             (j/multiply-by (j/days 1) 2))))

    (testing "number ops"
      (is (j/zero? (j/period 0)))
      (is (= (j/period -10 10) (j/negate (j/period 10 -10))))
      (is (j/negative? (j/period -10)))
      (is (j/negative? (j/period -10 10)))))

  (testing "year"
    (testing "plus"
      (is (= (j/year 5)
             (j/plus (j/year 2) (j/years 3)))))

    (testing "minus"
      (is (= (j/year 0)
             (j/minus (j/year 5) (j/years 5))))))

  (testing "month"
    (testing "plus"
      (is (= (j/month :may)
             (j/plus (j/month 2) 3)
             (j/plus (j/month 2) (j/months 3)))))

    (testing "minus"
      (is (= (j/month :january)
             (j/minus (j/month 5) 4)
             (j/minus (j/month 5) (j/months 4))))))

  (testing "day of week"
    (testing "plus"
      (is (= (j/day-of-week :sunday)
             (j/plus (j/day-of-week 1) 6)
             (j/plus (j/day-of-week 1) (j/days 6)))))

    (testing "minus"
      (is (= (j/day-of-week :monday)
             (j/minus (j/day-of-week 6) 5)
             (j/minus (j/day-of-week 6) (j/days 5)))))))

(deftest ordering

  (testing "times"
    (is (j/after? (j/local-date-time clock) (j/minus (j/local-date-time clock) (j/days 5))))
    (is (j/before? (j/local-date-time clock) (j/plus (j/local-date-time clock) (j/days 5))))

    (is (j/after? (j/local-date clock) (j/minus (j/local-date clock) (j/days 5))))
    (is (j/before? (j/local-date clock) (j/plus (j/local-date clock) (j/days 5))))

    (is (j/after? (j/local-time clock) (j/minus (j/local-time clock) (j/minutes 5))))
    (is (j/before? (j/local-time clock) (j/plus (j/local-time clock) (j/minutes 5))))

    (is (j/after? (j/zoned-date-time clock) (j/minus (j/zoned-date-time clock) (j/minutes 5))))
    (is (j/before? (j/zoned-date-time clock) (j/plus (j/zoned-date-time clock) (j/minutes 5))))

    (is (j/after? (j/offset-date-time clock) (j/minus (j/offset-date-time clock) (j/minutes 5))))
    (is (j/before? (j/offset-date-time clock) (j/plus (j/offset-date-time clock) (j/minutes 5))))

    (is (j/after? (j/offset-time clock) (j/minus (j/offset-time clock) (j/minutes 5))))
    (is (j/before? (j/offset-time clock) (j/plus (j/offset-time clock) (j/minutes 5)))))

  (testing "clocks"
    (is (j/after? (j/fixed-clock 1000) (j/fixed-clock 0)))
    (is (j/before? (j/fixed-clock 1000) (j/fixed-clock 5000))))

  (testing "fields"
    (is (j/after? (j/day-of-week :saturday) :thursday))
    (is (j/before? (j/day-of-week :saturday) :sunday))

    (is (j/after? (j/month :february) :january))
    (is (j/before? (j/month :february) :march))

    (is (j/after? (j/year 2010) 2009))
    (is (j/before? (j/year 2010) 2011))

    (is (j/after? (j/month-day 5 1) (j/month-day 4 1)))
    (is (j/before? (j/month-day 1 1) (j/month-day 4 1)))))

(deftest properties
  (testing "units"
    (is (= (j/unit :seconds)
           (j/unit (j/duration) :seconds)
           (j/unit (j/duration) (j/unit :seconds))))

    (is (j/unit? (j/unit :seconds)))

    (is (j/supports? (j/duration) :seconds))
    (is (j/supports? :seconds (j/local-date-time)))
    (is (not (j/supports? :seconds (j/local-date))))

    (is (= 60
           (j/time-between (j/local-time "15:40") (j/local-time "15:41") :seconds)
           (j/time-between :seconds (j/local-time "15:40") (j/local-time "15:41"))
           (j/time-between (j/unit :seconds) (j/local-time "15:40") (j/local-time "15:41")))))

  (testing "fields"
    (is (= (j/field :second-of-day)
           (j/field (j/local-date-time) :second-of-day)
           (j/field (j/local-date-time) (j/field :second-of-day))))

    (is (j/field? (j/field :second-of-day)))

    (is (j/supports? (j/local-date-time) :second-of-day))
    (is (j/supports? :second-of-day (j/local-date-time)))
    (is (j/supports? :rata-die (j/local-date-time)))
    (is (not (j/supports? :second-of-day (j/local-date))))

    (testing ", ranges"
      (is (= (j/value-range 0 86399)
             (j/range :second-of-day)))

      (is (= (j/value-range {:min-smallest 1, :min-largest 1, :max-smallest 28, :max-largest 31})
             (j/range :day-of-month)
             (j/range (j/field :day-of-month))))

      (is (= 1 (j/min-value :day-of-month)))
      (is (= 1 (j/largest-min-value :day-of-month)))
      (is (= 28 (j/smallest-max-value :day-of-month)))
      (is (= 31 (j/max-value :day-of-month)))))

  (testing "duration"
    (let [d (j/duration 100000001)]
      (is (= (j/properties d)
             {:nanos (j/property d :nanos)
              :seconds (j/property d :seconds)}))
      (is (= (j/units d)
             {:nanos (j/unit :nanos)
              :seconds (j/unit :seconds)}))))

  (testing "period"
    (let [p (j/period 10 5 1)]
      (is (= (j/properties p)
             {:days (j/property p :days)
              :months (j/property p :months)
              :years (j/property p :years)}))
      (is (= (j/units p)
             {:days (j/unit :days)
              :months (j/unit :months)
              :years (j/unit :years)}))))

  (testing "temporals"
    (doseq [e [(j/local-date)
               (j/local-time)
               (j/local-date-time)
               (j/offset-date-time)
               (j/offset-time)
               (j/zoned-date-time)]]
      (is (seq (j/properties e)))
      (is (seq (j/fields e)))))

  (testing "single fields"
    (doseq [e [(j/month :february)
               (j/day-of-week :monday)
               (j/year 100)
               (j/zone-offset 5 20)]]
      (is (seq (j/properties e)))
      (is (seq (j/fields e)))
      (is (j/range e))))

  (testing "multi fields"
    (doseq [e [(j/month-day :january 1)
               (j/year-month 10 10)]]
      (is (seq (j/properties e)))
      (is (seq (j/fields e))))))

(deftest seq-test
  (is (= [(j/local-date 2015) (j/local-date 2016)]
         (take 2 (j/iterate j/plus (j/local-date 2015) (j/years 1))))))

(deftest adjuster-test
  (testing "predefined adjusters"
    (is (= (j/adjust (j/local-date 2015 1 1) :first-in-month :monday)
           (j/local-date 2015 1 5)))

    (is (= (j/adjust (j/local-date 2015 1 1) :day-of-week-in-month 1 :monday)
           (j/local-date 2015 1 5)))

    (is (= (j/adjust (j/local-date 2015 1 1) :day-of-week-in-month 2 :monday)
           (j/local-date 2015 1 12)))

    (is (= (j/adjust (j/local-date 2015 1 1) :first-day-of-next-year)
           (j/local-date 2016 1 1))))

  (testing "functions as adjusters"
    (is (= (j/adjust (j/local-date 2015 1 1) j/plus (j/days 1))
           (j/local-date 2015 1 2)))))

(deftest sugar-test
  (testing "weekdays"
    (is (j/monday? (j/local-date 2015 1 5)))
    (is (j/tuesday? (j/offset-date-time 2015 1 6 0)))
    (is (j/wednesday? (j/zoned-date-time 2015 1 7 "UTC")))
    (is (j/thursday? (j/local-date-time 2015 1 8)))
    (is (j/friday? (j/day-of-week 5)))
    (is (j/saturday? (j/day-of-week :saturday)))
    (is (j/sunday? (j/day-of-week 7))))

  (testing "predicates"
    (is (j/weekday? (j/local-date 2015 1 5)))
    (is (not (j/weekday? (j/local-date 2015 1 4))))
    (is (j/weekend? (j/local-date 2015 1 4)))
    (is (not (j/weekend? (j/local-date 2015 1 5))))))

(deftest convert-test
  (testing "amount"
    (is (= {:remainder 10, :whole 0}
           (j/convert-amount 10 :seconds :minutes)))
    (is (= {:remainder 323200, :whole 16}
           (j/convert-amount 10000000 :seconds :weeks)))
    (is (thrown? Exception
          (j/convert-amount 10 :seconds :years)))
    (is (thrown? Exception
          (j/convert-amount 10 :years :forever))))

  (testing "as"
    (testing "duration"
      (is (= 0 (j/as (j/duration 10 :seconds) :minutes)))
      (is (= 10 (j/as (j/duration 10 :seconds) :seconds)))
      (is (= 10000 (j/as (j/duration 10 :seconds) :millis)))
      (is (thrown? Exception (j/as (j/duration 10 :seconds) :months))))

    (testing "period"
      (is (= 0 (j/as (j/days 1) :weeks)))
      (is (= (* 24 60) (j/as (j/days 1) :minutes)))
      (is (thrown? Exception (j/as (j/months 1) :minutes)))
      (is (thrown? Exception (j/as (j/period 1 1 1) :months)))
      (is (= 13 (j/as (j/period 1 1) :months))))

    (testing "temporal"
      (is (= 1 (j/as (j/local-date 2015 1 1) :day-of-month)))
      (is (= 2015 (j/as (j/local-date 2015 1 1) :year))))

    (testing "multiple"
      (is (= [2015 1 1] (j/as (j/local-date 2015 1 1) :year :month-of-year :day-of-month))))

    (testing "throws"
      (is (thrown? Exception (j/as (j/local-time 0) :year))))))

(deftest legacy-conversion
  (testing "converts through instant"
    (is (= (j/instant 1000) (j/instant (java.util.Date. 1000))))
    (is (= (java.util.Date. 1000) (j/to-java-date 1000)))
    (is (= (java.sql.Date. 1000) (j/to-sql-date 1000)))
    (is (= (java.sql.Timestamp. 1000) (j/to-sql-timestamp 1000)))
    (is (= 1000
           (j/to-millis-from-epoch 1000)
           (j/to-millis-from-epoch (java.util.Date. 1000))
           (j/to-millis-from-epoch (j/offset-date-time (j/instant 1000) +0)))))

  (testing "from java.util Date types"
    (is (= (j/zone-id "UTC") (j/zone-id (java.util.TimeZone/getTimeZone "UTC"))))))

(jt.u/when-threeten-extra
  (testing "adjusters"
    (is (= (j/adjust (j/local-date 2015 1 1) :next-working-day)
           (j/local-date 2015 1 2))))

  (testing "interval"
    (is (= (j/interval "1970-01-01T00:00:00Z/1970-01-01T00:00:01Z")
           (j/interval 0 1000)
           (j/interval (j/offset-date-time 1970 1 1 +0)
                       (j/offset-date-time 1970 1 1 0 0 1 +0))))

    (is (= 1 (j/as (j/interval (j/instant 0) (j/instant 1)) :millis))))

  (testing "operations"
    (is (= (j/interval 5000 10000)
           (j/move-end-by (j/interval 5000 6000) (j/seconds 4))
           (j/move-start-by (j/interval 0 10000) (j/seconds 5))
           (j/move-end-to (j/interval 5000 6000) 10000)
           (j/move-start-to (j/interval 0 10000) 5000)))

    (is (= (j/instant 0) (j/start (j/interval 0 1000))))
    (is (= (j/instant 1000) (j/end (j/interval 0 1000))))

    (testing "contains"
      (is (j/contains? (j/interval 0 1000) 500))
      (is (not (j/contains? (j/interval 0 1000) 1500)))
      (is (j/contains? (j/interval 0 1000) (j/interval 100 900)))
      (is (j/contains? (j/interval 0 1000) (j/interval 0 900)))
      (is (j/contains? (j/interval 0 1000) (j/interval 0 1000)))
      (is (j/contains? (j/interval 0 1000) (j/interval 1000 1000)))
      (is (not (j/contains? (j/interval 0 1000) (j/interval 1000 1001)))))

    (testing "overlaps"
      (is (j/overlaps? (j/interval 0 1000) (j/interval 0 500)))
      (is (j/overlaps? (j/interval 0 1000) (j/interval 0 1500)))
      (is (j/overlaps? (j/interval 500 1000) (j/interval 0 1500)))
      (is (not (j/overlaps? (j/interval 0 1000) (j/interval 1500 2000))))

      (is (= (j/interval 500 1000) (j/overlap (j/interval 500 1000) (j/interval 0 1500))))
      (is (nil? (j/overlap (j/interval 0 1000) (j/interval 1500 2000)))))

    (testing "abuts"
      (is (j/abuts? (j/interval 0 1000) (j/interval 1000 2000)))
      (is (not (j/abuts? (j/interval 0 1000) (j/interval 900 2000)))))

    (testing "gap"
      (is (= (j/interval 1000 2000) (j/gap (j/interval 0 1000) (j/interval 2000 3000))))
      (is (nil? (j/gap (j/interval 0 1000) (j/interval 500 1500))))))

  (testing "ordering"
    (is (j/before? (j/interval 1000 2000) (j/instant 5000)))
    (is (not (j/before? (j/interval 1000 5000) (j/instant 5000))))
    (is (j/before? (j/interval 1000 5000) (j/interval 5001 6000)))

    (is (j/after? (j/interval 1000 5000) (j/instant 100)))
    (is (not (j/after? (j/interval 1000 5000) (j/instant 2000))))
    (is (j/after? (j/interval 1000 5000) (j/interval 100 999)))))

(jt.u/when-joda

  (def joda-clock (j/fixed-clock "2015-11-26T10:20:30.040Z" "UTC"))

  (import '[org.joda.time Duration Period DateTimeZone
            LocalDate LocalTime LocalDateTime DateTime Instant])
  (deftest joda
    (testing "duration from duration and period"
      (is (= (j/duration 1 :millis)
            (j/duration (Duration/millis 1))
            (j/duration (Period/millis 1))))
      (is (= (j/duration 1 :seconds)
            (j/duration (Duration/standardSeconds 1))
            (j/duration (Period/seconds 1))))
      (is (= (j/duration 1 :minutes)
            (j/duration (Duration/standardMinutes 1))
            (j/duration (Period/minutes 1))))
      (is (= (j/duration 1 :hours)
            (j/duration (Duration/standardHours 1))
            (j/duration (Period/hours 1))))
      (is (= (j/duration 1 :days)
            (j/duration (Duration/standardDays 1))
            (j/duration (Period/days 1))))

      (is (= (j/plus (j/millis 1) (j/seconds 1) (j/minutes 1) (j/hours 1)
               (j/standard-days 1))
            (j/duration (.plus (Duration/millis 1)
                          (.plus (Duration/standardSeconds 1)
                            (.plus (Duration/standardMinutes 1)
                              (.plus (Duration/standardHours 1)
                                (Duration/standardDays 1)))))))))

    (testing "duration from period"
      (is (= (j/duration 7 :days) (j/duration (Period/weeks 1)))))

    (testing "period from duration"
      (is (= (j/period 1 :days) (j/period (Duration/standardHours 24)))))

    (testing "period from joda period"
      (is (= (j/period 1 :days) (j/period (Period/days 1))))
      (is (= (j/period 7 :days) (j/period (Period/weeks 1))))
      (is (= (j/period 1 :months) (j/period (Period/months 1))))
      (is (= (j/period 1 :years) (j/period (Period/years 1))))

      (is (= (j/plus (j/days 1) (j/months 1) (j/years 1))
            (j/period (.plus (Period/days 1)
                        (.plus (Period/months 1)
                          (Period/years 1)))))))

    #_(testing "instant"
      (is (= (j/instant joda-clock)
             (j/instant (Instant. (DateTime. 2015 11 26 10 20 30))))))

    (testing "local date"
      (is (= (j/local-date joda-clock)
             (j/local-date (LocalDate. 2015 11 26))
             (j/local-date (LocalDateTime. 2015 11 26 10 20 30))
             (j/local-date (DateTime. 2015 11 26 10 20 30)))))

    (testing "local date-time"
      (is (= (j/local-date-time joda-clock)
             (j/local-date-time (LocalDateTime. 2015 11 26 10 20 30 40))
             (j/local-date-time (DateTime. 2015 11 26 10 20 30 40)))))

    (testing "local time"
      (is (= (j/local-time joda-clock)
            (j/local-time (LocalTime. 10 20 30 40))
            (j/local-time (LocalDateTime. 2015 11 26 10 20 30 40))
            (j/local-time (DateTime. 2015 11 26 10 20 30 40)))))

    (testing "zoned date-time"
      (is (= (j/zoned-date-time joda-clock)
             (j/zoned-date-time (DateTime. 2015 11 26 10 20 30 40 (DateTimeZone/forID "UTC"))))))

    (testing "offset date-time"
      (is (= (j/offset-date-time joda-clock)
             (j/offset-date-time (DateTime. 2015 11 26 10 20 30 40 (DateTimeZone/forID "UTC"))))))

    (testing "offset time"
      (is (= (j/offset-time joda-clock)
             (j/offset-time (DateTime. 2015 11 26 10 20 30 40 (DateTimeZone/forID "UTC"))))))))

