(ns java-time.pre-java8
  (:require [java-time
             [local :as jt.l]
             [temporal :as jt.t]
             [defconversion :refer [conversion!]]]))

(defn ^java.util.Date java-date
  "Creates a `java.util.Date` out of any combination of arguments valid for
  `java-time/instant` or the Instant itself.

  A `java.util.Date` represents an instant in time. It's a direct analog of the
  `java.time.Instant` type introduced in the JSR-310. Please consider using the
  `java.time.Instant` (through `java-time/instant`) directly."
  ([] (java.util.Date/from (jt.t/instant)))
  ([a] (java.util.Date/from (jt.t/instant a)))
  ([a b] (java.util.Date/from (jt.t/instant a b))))

(defn- arities [type ctor n-args]
  (for [i (range (inc n-args))]
    (let [arg-vec (vec (take i (repeatedly gensym)))
          type-ctor (symbol (name type) "valueOf")]
      `(~arg-vec (~type-ctor (~ctor ~@arg-vec))))))

(defmacro defsqldate [type name ctor n-args doc]
  (let [fn-name (with-meta name {:tag type})]
    `(defn ~fn-name ~doc
       ~@(arities type ctor n-args))))

(conversion! java.sql.Date java.time.LocalDate
  (fn [^java.sql.Date dt]
    (.toLocalDate dt)))

(conversion! java.sql.Timestamp java.time.LocalDateTime
  (fn [^java.sql.Timestamp dt]
    (.toLocalDateTime dt)))

(conversion! java.sql.Time java.time.LocalTime
  (fn [^java.sql.Time dt]
    (.toLocalTime dt)))

(defsqldate java.sql.Date sql-date jt.l/local-date 3
  "Creates a `java.sql.Date` out of any combination of arguments valid for
  `java-time/local-date` or the LocalDate itself.

  Please consider using the JSR-310 Java Time types instead of `java.sql.Date`
  if your drivers support them.

  Even though `java.sql.Date` extends a `java.util.Date`, it's supposed to be
  used as a local date (no time component or timezone) for the purposes of
  conversion from/to native JDBC driver DATE types.")

(defsqldate java.sql.Timestamp sql-timestamp jt.l/local-date-time 7
  "Creates a `java.sql.Timestamp` out of any combination of arguments valid for
  `java-time/local-date-time` or the LocalDateTime itself.

  Please consider using the JSR-310 Java Time types instead of
  `java.sql.Timestamp` if your drivers support them.

  `java.sql.Timestamp` is a version of a `java.util.Date` supposed to be used
  as a local date-time (no timezone) for the purposes of conversion from/to native
  JDBC driver TIMESTAMP types.")

(defsqldate java.sql.Time sql-time jt.l/local-time 3
  "Creates a `java.sql.Time` out of any combination of arguments valid for
  `java-time/local-time` (except the nanos constructor) or the LocalTime
  itself.

  Please consider using the JSR-310 Java Time types instead of `java.sql.Time`
  if your drivers support them.

  Even though `java.sql.Time` extends a `java.util.Date`, it's supposed to be
  used as a local time (no date component or timezone) for the purposes of
  conversion from/to native JDBC driver TIME types.")
