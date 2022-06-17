# Changelog

## 0.3.4

### Fixed

* #78(liquidz): Add missing chrono fields (v0.3.3 has breaking changes)
* #81(terop): Remove clj-tuple - no advantages over Clojure vector anymore
* Remove java-time.util/get-static-fields-of-type
  * set Java property `java-time.util.get-static-fields-of-type=true` to revert

## 0.3.3

### New Features

* #71(brettrowberry): not-after? and not-before?
* #40(davidneu)/#61(puredanger): added/fixed deps.edn

### Fixed

* #72(FieryCod): working with GraalVM
* #29(danieldroit): conversion graph construction edge case
* #60(robdaemon): locales can mess up predefined-formatters
* #51(jimpil): remove reflection during load - improves load time

### Docs

* #27(emlin)
* #35(thobbs)
* #36(holyjak)
* #38(ProjectFrank)
* #39(sashary)
* #48(bpringe)
* #56,#57(green-coder)
* #63(vandr0iy)
* #54,#31,#24

## 0.3.2

### New Features

* `zone-id?` predicate
* `set-clock!` - sets the mocked clock value to the supplied instant
* `when-joda-time-loaded` - macro which runs code when Joda-Time is on the classpath
* `instant->sql-timestamp` - produce a `java.sql.Timestamp` from an Instant-like object
* `as` support for two-field Time entities #21, courtesy Larry Jones

## 0.3.1

### New Features

* `clock?` predicate
* `mock-clock` - returns a mocked instance of `java.time.Clock`.

## 0.3.0

### Breaking changes

* `to-sql-date` converts anything convertible to `LocalDate` into a `java.sql.Date`.
  Previously `to-sql-date` returned a `java.util.Date` (bug).
* [#10](https://github.com/dm3/clojure.java-time/issues/10) `to-sql-timestamp`
  stopped accepting an instant and starting accepting local date time. I
  mistakenly assumed that `java.sql.Timestamp/from(Instant)` was deprecated.

### New Features

Please see a new section within README - [Legacy Date-Time
Types](https://github.com/dm3/clojure.java-time#legacy-date-time-types)

* `java-date`, `sql-date`, `sql-timestamp`, `sql-time` - functions which
  produce the `java.util.*` date-time objects.
* [#5](https://github.com/dm3/clojure.java-time/issues/5) automatic conversions:
    - `java.sql.Date` -> `java.time.LocalDate`
    - `java.sql.Timestamp` -> `java.time.LocalDateTime`
    - `java.sql.Time` -> `java.time.LocalTime`
* deprecated `to-java-date`/`to-sql-date`/`to-sql-timestamp`

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
