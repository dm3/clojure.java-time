(ns java-time.jdbc
  (:require [clojure.java.jdbc :as jdbc]
            [java-time]))

(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Timestamp
  (result-set-read-column [v _2 _3]
    (java-time/local-date-time v))
  java.sql.Date
  (result-set-read-column [v _2 _3]
    (java-time/local-date v))
  java.sql.Time
  (result-set-read-column [v _2 _3]
    (java-time/local-time v))
  )

(extend-protocol jdbc/ISQLValue
  java.time.LocalDateTime
  (sql-value [v]
    (java.sql.Timestamp/valueOf v))
  java.time.LocalDate
  (sql-value [v]
    (java.sql.Date/valueOf v))
  java.time.LocalTime
  (sql-value [v]
    (java.sql.Time/valueOf v))
  )
