(ns java-time-test
  (:require [clojure.test :refer :all]
            [java-time.util :as jt.u]
            [java-time :as j])
  (:import java.util.Locale))

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
           (j/day-of-week (j/local-date clock))))

    (is (j/day-of-week? (j/day-of-week 4))))

  (testing "multi field"
    (is (= (j/month-day (j/local-date clock))
           (j/month-day 11 26)
           (j/month-day "dd-MM" "26-11")))

    (is (= (j/month-day 1)
           (j/month-day (j/month 1))
           (j/month-day 1 1)
           (j/month-day 1 (j/day-of-week 1))))

    (is (j/month-day? (j/month-day 1 1)))

    (is (= (j/year-month (j/local-date clock))
           (j/year-month 2015 11)
           (j/year-month "yy-MM" "15-11")))

    (is (= (j/year-month 1)
           (j/year-month (j/year 1))
           (j/year-month 1 1)
           (j/year-month 1 (j/month 1))))

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

    (j/with-clock (j/system-clock "UTC")
      (is (= (j/zoned-date-time 2015)
             (j/zoned-date-time 2015 1)
             (j/zoned-date-time 2015 1 1)
             (j/zoned-date-time (j/year 2015))
             (j/zoned-date-time (j/year 2015) (j/month 1))
             (j/zoned-date-time (j/year 2015) (j/month 1) (j/day-of-week 1))
             (j/zoned-date-time 2015 1 1 0)
             (j/zoned-date-time 2015 1 1 0 0)
             (j/zoned-date-time 2015 1 1 0 0 0)
             (j/zoned-date-time 2015 1 1 0 0 0 0))))

    (let [zone-id (j/zone-id)]
      (is (= (j/zoned-date-time 2015 1 1 0 0 0 0)
             (j/zoned-date-time 2015 1 1 0 0 0 0 zone-id))))

    (let [utc (j/zoned-date-time 2015 1 1 0 0 0 0 "UTC")]
      (is (= (j/zoned-date-time 2014 12 31 19 0 0 0 "America/New_York")
             (j/with-zone-same-instant utc "America/New_York")
             (j/with-zone (j/zoned-date-time 2014 12 31 19 0 0 0 "UTC") "America/New_York"))))

    (is (= (j/truncate-to (j/zoned-date-time 2015 1 1 10 20 30 40) :minutes)
           (j/zoned-date-time 2015 1 1 10 20))))

  (testing "offset date time"
    (is (= (j/offset-date-time clock)
           (j/offset-date-time (j/offset-date-time clock))
           (j/offset-date-time "2015-11-26T10:20:30.000000040+00:00")
           (j/offset-date-time "2015-11-26T10:20:30.000000040Z")
           (j/offset-date-time "yyyy/MM/dd'T'HH:mm:ss-SSSSSSSSS'['X']'" "2015/11/26T10:20:30-000000040[Z]")
           (j/offset-date-time (j/local-date clock) (j/local-time clock) (j/zone-offset +0))
           (j/offset-date-time (j/local-date-time clock) (j/zone-offset +0))
           (j/offset-date-time (j/zoned-date-time clock) (j/zone-offset +0))
           (j/offset-date-time 2015 11 26 10 20 30 40 (j/zone-offset +0))
           (j/offset-date-time (j/instant clock) "UTC")))

    (is (= (j/truncate-to (j/offset-date-time clock) :millis)
          (j/offset-date-time (j/to-java-date clock) "UTC")))

    (is (j/offset-date-time? (j/offset-date-time)))

    (j/with-clock (j/system-clock "UTC")
      (is (= (j/offset-date-time 2015)
             (j/offset-date-time 2015 1)
             (j/offset-date-time 2015 1 1)
             (j/offset-date-time (j/year 2015))
             (j/offset-date-time (j/year 2015) (j/month 1))
             (j/offset-date-time (j/year 2015) (j/month 1) (j/day-of-week 1))
             (j/offset-date-time 2015 1 1 0)
             (j/offset-date-time 2015 1 1 0 0)
             (j/offset-date-time 2015 1 1 0 0 0)
             (j/offset-date-time 2015 1 1 0 0 0 0 (j/zone-offset +0)))))

    (is (= (j/truncate-to (j/offset-date-time 2015 1 1 10 20 30 40) :minutes)
           (j/offset-date-time 2015 1 1 10 20)))

    (let [utc (j/offset-date-time 2015 1 1 0 0 0 0 +0)]
      (is (= (j/offset-date-time 2014 12 31 19 0 0 0 -5)
             (j/with-offset-same-instant utc -5)
             (j/with-offset (j/offset-date-time 2014 12 31 19 0 0 0 +0) -5)))))

  (testing "offset time"
    (is (= (j/offset-time clock)
           (j/offset-time (j/offset-time clock))
           (j/offset-time (j/zoned-date-time clock))
           (j/offset-time "10:20:30.000000040+00:00")
           (j/offset-time "10:20:30.000000040Z")
           (j/offset-time "HH:mm:ss-SSSSSSSSS'['X']'" "10:20:30-000000040[Z]")
           (j/offset-time (j/local-time clock) (j/zone-offset +0))
           (j/offset-time (j/instant clock) "UTC")
           (j/offset-time 10 20 30 40 +0)
           (j/offset-time (j/instant clock) "UTC")))

    (is (= (j/truncate-to (j/offset-time clock) :millis)
          (j/offset-time (j/to-java-date clock) "UTC")))

    (is (j/offset-time? (j/offset-time (j/zone-id "UTC"))))
    (is (j/offset-time? (j/offset-time +0)))

    (j/with-clock (j/system-clock "UTC")
      (is (= (j/offset-time 0)
             (j/offset-time 0 0)
             (j/offset-time 0 0 0)
             (j/offset-time 0 0 0 0))))

    (is (= (j/truncate-to (j/offset-time 10 20 30 40) :minutes)
           (j/offset-time 10 20)))

    (let [utc (j/offset-time 15 0 0 0 +0)]
      (is (= (j/offset-time 10 0 0 0 -5)
             (j/with-offset-same-instant utc -5)
             (j/with-offset (j/offset-time 10 0 0 0 +0) -5)))))

   (testing "instant"
     (is (= (j/instant clock)
            (j/instant "2015-11-26T10:20:30.000000040Z")
            (j/instant "yyyy/MM/dd'T'HH:mm:ss-SSSSSSSSS'['X']'" "2015/11/26T10:20:30-000000040[Z]")))

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
    (let [ldt (j/local-date-time clock)
          ldt+5 (j/plus ldt (j/days 5))]
      (is (j/after? ldt+5 ldt))
      (is (not (j/after? ldt ldt+5)))
      (is (j/before? ldt ldt+5))
      (is (not (j/before? ldt+5 ldt)))
      (is (j/not-after? ldt ldt))
      (is (j/not-after? ldt ldt+5))
      (is (not (j/not-after? ldt+5 ldt)))
      (is (j/not-before? ldt ldt))
      (is (j/not-before? ldt+5 ldt))
      (is (not (j/not-before? ldt ldt+5))))

    (let [ld (j/local-date clock)
          ld+5 (j/plus ld (j/days 5))]
      (is (j/after? ld+5 ld))
      (is (not (j/after? ld ld+5)))
      (is (j/before? ld ld+5))
      (is (not (j/before? ld+5 ld)))
      (is (j/not-after? ld ld))
      (is (j/not-after? ld ld+5))
      (is (not (j/not-after? ld+5 ld)))
      (is (j/not-before? ld ld))
      (is (j/not-before? ld+5 ld))
      (is (not (j/not-before? ld ld+5))))

    (let [lt (j/local-time clock)
          lt+5 (j/plus lt (j/minutes 5))]
      (is (j/after? lt+5 lt))
      (is (not (j/after? lt lt+5)))
      (is (j/before? lt lt+5))
      (is (not (j/before? lt+5 lt)))
      (is (j/not-after? lt lt))
      (is (j/not-after? lt lt+5))
      (is (not (j/not-after? lt+5 lt)))
      (is (j/not-before? lt lt))
      (is (j/not-before? lt+5 lt))
      (is (not (j/not-before? lt lt+5))))

    (let [zdt (j/zoned-date-time clock)
          zdt+5 (j/plus zdt (j/minutes 5))]
      (is (j/after? zdt+5 zdt))
      (is (not (j/after? zdt zdt+5)))
      (is (j/before? zdt zdt+5))
      (is (not (j/before? zdt+5 zdt)))
      (is (j/not-after? zdt zdt))
      (is (j/not-after? zdt zdt+5))
      (is (not (j/not-after? zdt+5 zdt)))
      (is (j/not-before? zdt zdt))
      (is (j/not-before? zdt+5 zdt))
      (is (not (j/not-before? zdt zdt+5))))

    (let [odt (j/offset-date-time clock)
          odt+5 (j/plus odt (j/minutes 5))]
      (is (j/after? odt+5 odt))
      (is (not (j/after? odt odt+5)))
      (is (j/before? odt odt+5))
      (is (not (j/before? odt+5 odt)))
      (is (j/not-after? odt odt))
      (is (j/not-after? odt odt+5))
      (is (not (j/not-after? odt+5 odt)))
      (is (j/not-before? odt odt))
      (is (j/not-before? odt+5 odt))
      (is (not (j/not-before? odt odt+5))))

    (let [ot (j/offset-time clock)
          ot+5 (j/plus ot (j/minutes 5))]
      (is (j/after? ot+5 ot))
      (is (not (j/after? ot ot+5)))
      (is (j/before? ot ot+5))
      (is (not (j/before? ot+5 ot)))
      (is (j/not-after? ot ot))
      (is (j/not-after? ot ot+5))
      (is (not (j/not-after? ot+5 ot)))
      (is (j/not-before? ot ot))
      (is (j/not-before? ot+5 ot))
      (is (not (j/not-before? ot ot+5))))

    (let [i (j/instant clock)
          i+5 (j/plus i (j/minutes 5))]
      (is (j/after? i+5 i))
      (is (not (j/after? i i+5)))
      (is (j/before? i i+5))
      (is (not (j/before? i+5 i)))
      (is (j/not-after? i i))
      (is (j/not-after? i i+5))
      (is (not (j/not-after? i+5 i)))
      (is (j/not-before? i i))
      (is (j/not-before? i+5 i))
      (is (not (j/not-before? i i+5)))))

  (testing "clocks"
    (let [fc (j/fixed-clock 0)
          fc+1000 (j/fixed-clock 1000)]
      (is (j/after? fc+1000 fc))
      (is (not (j/after? fc fc+1000)))
      (is (j/before? fc fc+1000))
      (is (not (j/before? fc+1000 fc)))
      (is (j/not-after? fc fc))
      (is (j/not-after? fc fc+1000))
      (is (not (j/not-after? fc+1000 fc)))
      (is (j/not-before? fc fc))
      (is (j/not-before? fc+1000 fc))
      (is (not (j/not-before? fc fc+1000)))))

  (testing "fields"
    (let [thursday (j/day-of-week :thursday)
          saturday (j/day-of-week :saturday)
          sunday (j/day-of-week :sunday)]
      (is (j/after? saturday :thursday))
      (is (not (j/after? thursday :saturday)))
      (is (j/before? saturday :sunday))
      (is (not (j/before? sunday :saturday)))
      (is (j/not-after? saturday saturday))
      (is (j/not-after? saturday sunday))
      (is (not (j/not-after? sunday saturday)))
      (is (j/not-before? saturday saturday))
      (is (j/not-before? sunday saturday))
      (is (not (j/not-before? saturday sunday))))
    
    (let [january (j/month :january)
          february (j/month :february)
          march (j/month :march)]
      (is (j/after? february :january))
      (is (not (j/after? january :february)))
      (is (j/before? february :march))
      (is (not (j/before? march :february)))
      (is (j/not-after? january january))
      (is (j/not-after? february march))
      (is (not (j/not-after? march february)))
      (is (j/not-before? january january))
      (is (j/not-before? february january))
      (is (not (j/not-before? january february))))

    (let [year-2009 (j/year 2009)
          year-2010 (j/year 2010)]
      (is (j/after? year-2010 2009))
      (is (not (j/after? year-2009 2010)))
      (is (j/before? year-2009 2010))
      (is (not (j/before? year-2010 2009)))
      (is (j/not-after? year-2010 year-2010))
      (is (j/not-after? year-2009 year-2010))
      (is (not (j/not-after? year-2010 year-2009)))
      (is (j/not-before? year-2010 year-2010))
      (is (j/not-before? year-2010 year-2009))
      (is (not (j/not-before? year-2009 year-2010))))

    (let [jan-1 (j/month-day 1 1)
          apr-1 (j/month-day 4 1)]
      (is (j/after? apr-1 jan-1))
      (is (not (j/after? jan-1 apr-1)))
      (is (j/before? jan-1 apr-1))
      (is (not (j/before? apr-1 jan-1)))
      (is (j/not-after? jan-1 jan-1))
      (is (j/not-after? jan-1 apr-1))
      (is (not (j/not-after? apr-1 jan-1)))
      (is (j/not-before? jan-1 jan-1))
      (is (j/not-before? apr-1 jan-1))
      (is (not (j/not-before? jan-1 apr-1))))))

(deftest mock-clock
  (testing "constructors"
    (is (= (j/mock-clock)
           (j/mock-clock 0)
           (j/mock-clock 0 (j/zone-id)))))

  (let [utc-clock #(j/mock-clock % "UTC")]
    (testing "accessors"
      (let [clock (utc-clock 0)]
        (is (= 0 (j/value clock)))
        (is (= (j/zone-id "UTC") (j/zone-id clock)))))

    (testing "equality"
      (is (= (utc-clock 0) (utc-clock 0)))
      (is (= (hash (utc-clock 0)) (hash (utc-clock 0))))
      (is (not= (utc-clock 0) (utc-clock 1)))
      (is (not= (utc-clock 0) (j/mock-clock 0 "GMT"))))

    (testing "advance"
      (let [clock (utc-clock 0)]
        (testing "by positive amount"
          (j/advance-clock! clock (j/millis 1))
          (is (= 1 (j/value clock))))

        (testing "by negative amount"
          (j/advance-clock! clock (j/millis -1))
          (is (= 0 (j/value clock))))))

    (testing "clone with a different zone"
      (let [clock (utc-clock 0)
            cloned-clock (j/with-zone clock "GMT")]
        (is (not (identical? clock cloned-clock)))
        (is (= (j/zone-id "GMT") (j/zone-id cloned-clock)))
        (is (= (j/value cloned-clock) (j/value clock)))

        (j/advance-clock! cloned-clock (j/seconds 1))
        (is (= 1000 (j/value cloned-clock)))
        (is (not= (j/value cloned-clock) (j/value clock)))))

    (testing "set"
      (let [clock (utc-clock 0)]
        (testing "into future"
          (j/set-clock! clock 100)
          (is (= 100 (j/value clock))))

        (testing "into past"
          (j/set-clock! clock 0)
          (is (= 0 (j/value clock))))))))

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
    (is (j/wednesday? (j/zoned-date-time 2015 1 7)))
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

    (testing "temporal-accessor"
      (let [month-day-under-test (j/month-day 3 31)]
       (is (= 3 (j/as month-day-under-test :month-of-year)))
       (is (= 31 (j/as month-day-under-test :day-of-month)))
       (is (thrown? Exception (j/as month-day-under-test :year))))
      (let [year-month-under-test (j/year-month 2018 3)]
       (is (= 2018 (j/as year-month-under-test :year)))
       (is (= 3 (j/as year-month-under-test :month-of-year)))
       (is (thrown? Exception (j/as year-month-under-test :day-of-month)))))

    (testing "multiple"
      (is (= [2015 1 1] (j/as (j/local-date 2015 1 1) :year :month-of-year :day-of-month))))

    (testing "throws"
      (is (thrown? Exception (j/as (j/local-time 0) :year))))))

(deftest legacy-conversion
  (testing "deprecated"
    (testing "converts through instant"
      (is (= (j/instant 1000) (j/instant (java.util.Date. 1000))))
      (is (= (java.util.Date. 1000) (j/to-java-date 1000)))
      (is (= (java.sql.Date/valueOf (j/local-date 1000)) (j/to-sql-date 1000)))
      (is (= (java.sql.Timestamp/valueOf (j/local-date-time 1000)) (j/to-sql-timestamp 1000)))
      (is (= 1000
             (j/to-millis-from-epoch 1000)
             (j/to-millis-from-epoch (java.util.Date. 1000))
             (j/to-millis-from-epoch (j/offset-date-time (j/instant 1000) +0)))))

    (testing "converts to java.util/sql Dates"
      (is (= (java.util.Date. 1000) (j/to-java-date (j/instant 1000))))
      (is (= (java.sql.Date/valueOf (j/local-date 2016)) (j/to-sql-date (j/local-date 2016))))))

  (testing "pre-java8"
    (is (= (j/java-date (j/instant 1000))
           (java.util.Date. 1000)
           (j/java-date 1000)))
    (is (= (java.sql.Date/valueOf (j/local-date 2000 10 5))
           (j/sql-date 2000 10 5)
           (j/sql-date (j/local-date 2000 10 5))))
    (is (= (java.sql.Timestamp/valueOf (j/local-date-time 2000 10 5 20 30 40))
           (j/sql-timestamp 2000 10 5 20 30 40)
           (j/sql-timestamp (j/local-date-time 2000 10 5 20 30 40))))
    (is (= (java.sql.Timestamp/from (j/instant 1))
           (java.sql.Timestamp. 1)
           (j/instant->sql-timestamp (j/instant 1))
           (j/instant->sql-timestamp 1)))
    (is (= (java.sql.Time/valueOf (j/local-time 20 30 40))
           (j/sql-time 20 30 40)
           (j/sql-time (j/local-time 20 30 40))))

    (is (= (j/local-date 2000 10 5) (j/local-date (j/sql-date 2000 10 5))))
    (is (= (j/local-date-time 2000 10 5 20 30 40 1000)
           (j/local-date-time (j/sql-timestamp 2000 10 5 20 30 40 1000))))
    (is (= (j/instant 1) (j/instant (j/instant->sql-timestamp 1))))
    (is (= (j/local-time 20 30 40) (j/local-time (j/sql-time 20 30 40)))))

  (testing "from java.util Date types"
    (is (= (j/zone-id "UTC") (j/zone-id (java.util.TimeZone/getTimeZone "UTC"))))))

(jt.u/when-threeten-extra
  (testing "adjusters"
    (is (= (j/adjust (j/local-date 2015 1 1) :next-working-day)
           (j/local-date 2015 1 2))))

  (testing "interval"
    (is (= (j/interval "1970-01-01T00:00:00Z/1970-01-01T00:00:01Z")
           (j/interval 0 1000)
           (j/interval (j/offset-date-time 1970 1 1 0 0 0 0 +0)
                       (j/offset-date-time 1970 1 1 0 0 1 0 +0))))

    (is (= 1 (j/as (j/interval (j/instant 0) (j/instant 1)) :millis)))

    (is (thrown-with-msg? IllegalArgumentException #"Cannot convert between.*"
          (j/as (j/interval (j/instant 0) (j/instant 1)) :months))))

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
    (let [interval-1-2 (j/interval 1 2)
          interval-3-4 (j/interval 3 4)
          instant-1 (j/instant 1)
          instant-3 (j/instant 3)]
      (is (j/before? interval-1-2 interval-3-4))
      (is (not (j/before? interval-3-4 interval-1-2)))
      (is (j/before? interval-1-2 instant-3))
      (is (not (j/before? interval-3-4 instant-1)))

      (is (j/after? interval-3-4 interval-1-2))
      (is (not (j/after? interval-1-2 interval-3-4)))
      (is (j/after? interval-3-4 instant-1))
      (is (not (j/after? interval-1-2 instant-3)))

      (is (j/not-before? interval-3-4 interval-3-4))
      (is (j/not-before? interval-1-2 interval-3-4))
      (is (not (j/not-before? interval-1-2 interval-3-4)))
      (is (j/not-before? interval-3-4 instant-3))
      (is (j/not-before? interval-3-4 instant-1))
      (is (not (j/not-before? interval-1-2 instant-3)))

      (is (j/not-after? interval-1-2 interval-1-2))
      (is (j/not-after? interval-1-2 interval-3-4))
      (is (not (j/not-after? interval-3-4 interval-1-2)))
      (is (j/not-after? interval-1-2 instant-1))
      (is (j/not-after? interval-1-2 instant-3))
      (is (not (j/not-after? interval-3-4 instant-1))))))

(jt.u/when-joda-time-loaded

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

(deftest locale-test
  (let [current-locale (Locale/getDefault)
        test-langs ["en" "tr" "cn"]]
    (testing "locale specific rules for lower-case can cause formatters to not be found"
      (doseq [lang test-langs]
       (testing lang
         (try
           (Locale/setDefault (Locale/forLanguageTag lang))
           (is (some? (j/formatter :rfc-1123-date-time)))
           (finally
             (Locale/setDefault current-locale))))))))

(deftest formatter-test
  (testing "case-insensitive formatter"
    (let [fmt (java-time/formatter "hh:mma" {:case :insensitive})]
      (is (= (j/local-time 0 34 0 0)
             (j/local-time fmt "00:34am")
             (j/local-time fmt "00:34AM")))))

  (testing "case-sensitive formatter"
    ;; Java version detection:
    ;; java.lang.Runtime.Version class exists from Java 9 onwards, hence
    ;; the need to detect the Java version from the java.version string returned
    ;; by System/getProperty. In versions before 9, java.version has the format
    ;; 1.x.y, e.g., 1.8.0_255, but from version 9 onwards, the initial 1 element
    ;; is dropped.
    ;; Refer to JEP 223 for information on the change.
    ;; https://openjdk.java.net/jeps/223
    (let [java-version (->> (System/getProperty "java.version")
                            (re-find #"^\d+")
                            Integer/parseInt)]
      (if (> java-version 11)
        (testing "Java 13 and above treats AM as invalid"
          (let [fmt (java-time/formatter "hh:mma" {:case :sensitive})]
            (is (= (j/local-time 0 34 0 0)
                   (j/local-time fmt "12:34am")))
            (is (thrown? Exception (j/local-time fmt "12:34AM")))))

        (testing "Java 8 and 11 treats am as invalid"
          (let [fmt (java-time/formatter "hh:mma" {:case :sensitive})]
            (is (= (j/local-time 0 34 0 0)
                   (j/local-time fmt "12:34AM")))
            (is (thrown? Exception (j/local-time fmt "12:34am")))))))))
