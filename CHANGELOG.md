# Changelog

## NEXT

## 1.4.3

- [#114](https://github.com/dm3/clojure.java-time/pull/114): fix doc typo ([@metayan](https://github.com/metayan))
- [#111](https://github.com/dm3/clojure.java-time/issues/111): docstrings of java-time.api contain literal \n ([@devurandom](https://github.com/devurandom)

## 1.4.2

- [#105](https://github.com/dm3/clojure.java-time/issues/105): fix `not-{before,after}?` on unconverted values
  - a consequence of fixing [#104](https://github.com/dm3/clojure.java-time/issues/104)
- add `java-time.api/=` for equality of times (with conversions)
- add support to intermix unconverted values in comparison operations after first argument
  - supported by `{before,after}?`, `not-{before,after}?`, `j/=`, and aliases of those ops
  - examples:
    - `(j/< (j/day-of-week :thursday) :saturday :sunday)`
    - `(j/<= (j/day-of-week :thursday) :thursday (j/day-of-week :saturday) :sunday)`
    - `(j/= (j/day-of-week :thursday) :thursday (j/day-of-week :thursday) :thursday)`

## 1.4.1

- [#104](https://github.com/dm3/clojure.java-time/issues/104): fix transivitity of `not-{before,after}?` when called with intervals

## 1.4.0

- [#98](https://github.com/dm3/clojure.java-time/issues/98): fix `not-before?` and `not-after?` with one or more than two arguments
- new aliases
  - `java-time.api/+` aliases `java-time.api/plus`
  - `java-time.api/-` aliases `java-time.api/minus`
  - `java-time.api/neg?` aliases `java-time.api/negative?`
  - `java-time.api/<` aliases `java-time.api/before?`
  - `java-time.api/>` aliases `java-time.api/after?`
  - `java-time.api/<=` aliases `java-time.api/not-after?`
  - `java-time.api/>=` aliases `java-time.api/not-before?`

## 1.3.0

- remove `:redef` on protocol methods, they are never direct-linked: https://ask.clojure.org/index.php/10967/are-protocol-methods-guaranteed-to-not-be-directly-linked?show=10990#a10990
- [#100](https://github.com/dm3/clojure.java-time/issues/100): respect `*clock*` when only providing a zone id in constructors

## 1.2.0

- [#95](https://github.com/dm3/clojure.java-time/issues/95) work around [CLJ-1796](https://clojure.atlassian.net/browse/CLJ-1796)

## 1.1.0

Due to [#91](https://github.com/dm3/clojure.java-time/issues/91), the main `java-time`
namespace has been deprecated. A new namespace `java-time.api` has been created

Note that this change is entirely optional---`java-time` and `java-time.api` will continue to be in sync and may coexist.

See [the docstring for `java-time`](https://dm3.github.io/clojure.java-time/java-time.html) for more details.

## 1.0.0

Released 2022-11-24.

### New Features

* Previously, Intervals were only allowed to the left of Instants in {before,after}?. Now they can be freely intermixed.

### Fixed

* #78(liquidz): Add missing chrono fields (v0.3.3 has breaking changes)
* #81(terop): Remove clj-tuple - no advantages over Clojure vector anymore
* #52: Fix before/after on Intervals
* #83(imrekoszo): Exclude clojure.core/abs

### Enhancements
* add docstrings to all `java-time` fns
* support clj-kondo in `java-time` ns by adding `:arglists` to all vars

### Internal
* Deprecate `java-time.util/get-static-fields-of-type`
* Remove `java-time.potemkin.namespaces`

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
