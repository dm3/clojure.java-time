(ns java-time.jdbc-test
  (:require
   [java-time.jdbc]
   [java-time]
   [clojure.java.jdbc :as jdbc]
   [clojure.test :as t :refer :all]))

(deftest test-jdbc-extend-protocal

  (testing "result-set-read-column"
    (is (= (jdbc/result-set-read-column (java-time/sql-timestamp 2018 11 12 15 00 00) nil nil)
           (java-time/local-date-time 2018 11 12 15 00 00)))
    (is (= (jdbc/result-set-read-column (java-time/sql-date 2018 11 12) nil nil)
           (java-time/local-date 2018 11 12)))
    (is (= (jdbc/result-set-read-column (java-time/sql-time 15 00 00) nil nil)
           (java-time/local-time 15 00 00))))

  (testing "sql-value"
    (is (= (jdbc/sql-value (java-time/local-date-time 2018 11 12 15 00 00))
           (java-time/sql-timestamp 2018 11 12 15 00 00)))
    (is (= (jdbc/sql-value (java-time/local-date 2018 11 12))
           (java-time/sql-date 2018 11 12)))
    (is (= (jdbc/sql-value (java-time/local-time 15 00 00))
           (java-time/sql-time 15 00 00)))))
