(ns nodeps.test
  (:require [java-time :as j]
            [clojure.test :refer :all]))

(def now (j/fixed-clock (j/zoned-date-time 2015 1 1)))

(deftest works
  (is (nil? (resolve 'j/interval)))
  (is (nil? (resolve 'j/am-pm)))
  (is (= 180 (j/as (j/duration 3 :hours) :minutes))))
