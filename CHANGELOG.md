## 0.3.0

### Fixed

* *Breaking*: `to-sql-date` converts anything convertible to `LocalDate` into a `java.sql.Date`.
  Previously `to-sql-date` worked the same as `to-java-date`.

## 0.2.2

### Fixed

* Wrong primitive type annotation on `to-millis-from-epoch`, see
  [Eastwood](https://github.com/jonase/eastwood/blob/master/doc/README-warn-about-bad-tags.txt)
  docs for the explanation.

## 0.2.1

### Fixed

* [#1](https://github.com/dm3/clojure.java-time/issues/1): Reflection warnings in two-field time entity constructors
* [#2](https://github.com/dm3/clojure.java-time/issues/2): `Ordered` implementation for `java.time.Instant`

## 0.2.0

### Breaking changes

* `zoned-date-time` doesn't accept the zone id as the last argument
* `offset-date-time/offset-time` doesn't accept offset id as the last argument

### New features

* `with-offset`/`with-offset-same-instant` for offset manipulation
* `with-zone`/`with-zone-same-instant` for zone manipulation
