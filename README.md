# Clojure.Java-Time

[![Clojars Project](https://img.shields.io/clojars/v/clojure.java-time.svg)](https://clojars.org/clojure.java-time)

A Clojure wrapper for Java 8 Date-Time API.

## Rationale

Main goals:

* Provide a consistent API for common operations with
  instants, date-times, zones and periods.
* Provide an escape hatch from Java types to clojure data structures.
* Avoid reflective calls.
* Provide an entry point into Java-Time by freeing the user from importing most
  of the Java-Time classes.

Why use Clojure.Java-Time over [clj-time](https://github.com/clj-time/clj-time)
or [Clojure.Joda-Time](https://github.com/dm3/clojure.joda-time)?

* You don't want to have a dependency on the Joda-Time library
* You already use Java 8
* You prefer as little Java interop code as possible

This library employs a structured and comprehensive approach to exposing the
Java 8 Date-Time API to the Clojure world. It's very similar to
Clojure.Joda-Time in its design goals and overall feeling, so if you ever used
that you will feel at home!

Why use Clojure.Java-Time over [cljc.java-time](https://github.com/henryw374/cljc.java-time) with [tick](https://github.com/juxt/tick)?

* You only plan on running on the JVM
* You prefer a single `require` over multiple ones

I don't see any reasons except for aesthetical pleasure and existing knowledge to choose one 
over the other. However, I have neither used or benchmarked Cljc.Java-Time and Tick so my endorsement 
is purely on the merits of a broader feature set.

## Documentation

* [API](http://dm3.github.io/clojure.java-time/)
* [![cljdoc badge](https://cljdoc.org/badge/clojure.java-time/clojure.java-time)](https://cljdoc.org/d/clojure.java-time/clojure.java-time/CURRENT)

## What's different in Java Time API?

If you already used Joda Time before you might think: "What in the world could
they do better?". After all, Joda-Time already provides a pretty comprehensive
set of tools for dealing with time-related concepts. Turns out, it's a tad more
complicated than it has to be. Also, a few concepts have faulty designs which
lead to hard to fix bugs and misuse. You can see the birds-eye view of changes
and some of the rationale on the author's (Stephen Colebourne) blog:

* [what's wrong with Joda-Time](http://blog.joda.org/2009/11/why-jsr-310-isn-joda-time_4941.html),
* [when you should use Java-Time](http://blog.joda.org/2014/07/threeten-backport-vs-joda-time.html)
* [what's different in Java-Time](http://blog.joda.org/2014/11/converting-from-joda-time-to-javatime.html).

You can also take a look at a [comprehensive comparison](http://time4j.net/tutorial/appendix.html) by the
[Time4J](http://time4j.net/) authors.

## Usage

Add the following dependency to your `deps.edn`
```clj
clojure.java-time/clojure.java-time {:mvn/version "0.3.3"}
```

or to your `project.clj` or `build.boot`:

```clj
[clojure.java-time "0.3.3"]
```

The [API](https://dm3.github.io/clojure.java-time/) of the Clojure.Java-Time
consists of one namespace, namely `java-time`.  For the purposes of this guide,
we will `require` the main namespace:

```clj
(require '[java-time :as jt]
         ;; for REPL experimentation
         java-time.repl)
```

### Concept run-through

Java Time API may seem daunting. Instead of a single `java.util.Date` you have
a `ZonedDateTime`, `OffsetDateTime`, `LocalDateTime`, `Instant`, and other
types. You would be well served by reading the official documentation for the
[Java Time API](https://docs.oracle.com/javase/tutorial/datetime/iso/index.html), 
but we'll also do a quick run-through here.

#### Local Dates and/or Times

`LocalDate`, `LocalTime` and `LocalDateTime` are used to represent a date, time
and date-time respectively without an offset or a timezone. The local time entities
are used to represent human-based dates/times. They are a good fit for representing
the time of various events:

* `LocalDate` - birthday, holiday
  * see [`jt/local-date`](http://dm3.github.io/clojure.java-time/java-time.html#var-local-date)
* `LocalTime` - bus schedule, opening time of a shop
  * see [`jt/local-time`](http://dm3.github.io/clojure.java-time/java-time.html#var-local-time)
* `LocalDateTime` - start of a competition
  * see [`jt/local-date-time`](http://dm3.github.io/clojure.java-time/java-time.html#var-local-date-time)

Example usage:

```clj
(jt/local-date 2015 10)
;=> #<java.time.LocalDate 2015-10-01>

(jt/local-time 10)
;=> #<java.time.LocalTime 10:00>

(jt/local-date-time 2015 10)
:=> #<java.time.LocalDateTime 2015-10-01T00:00>
```

#### Zoned Dates

There are two types which deal with zones: 
* `OffsetDateTime`
  * see [`jt/offset-date-time`](https://dm3.github.io/clojure.java-time/java-time.html#var-offset-date-time)
* `ZonedDateTime`
  * see [`jt/zoned-date-time`](https://dm3.github.io/clojure.java-time/java-time.html#var-zoned-date-time)

They do pretty much what you would expect from their name.
You can think of the `Offset` time as a more concrete version of the `Zoned`
time. For example, the same timezone can have different offsets throughout the
year due to DST or governmental regulations.

```clj
(jt/offset-time 10)
;=> #<java.time.OffsetTime 10:00+01:00>

(jt/offset-date-time 2015 10)
;=> #<java.time.OffsetDateTime 2015-10-01T10:00+01:00>

(jt/zoned-date-time 2015 10)
;=> #<java.time.ZonedDateTime 2015-10-01T10:00+01:00[Europe/London]>
```

Offset/Zone times only take the offset/zone as the last arguments for the
maximum arity constructor. You can influence the zone/offset by using the
[`jt/with-zone`](https://dm3.github.io/clojure.java-time/java-time.html#var-with-zone)
or [`jt/with-offset`](https://dm3.github.io/clojure.java-time/java-time.html#var-with-offset) functions, like so:

```clj
(jt/with-zone (jt/zoned-date-time 2015 10) "UTC")
;=> #<java.time.ZonedDateTime 2015-10-01T00:00Z[UTC]>

(jt/with-zone-same-instant (jt/zoned-date-time 2015 10) "UTC")
;=> #<java.time.ZonedDateTime 2015-09-30T23:00Z[UTC]>

(jt/with-clock (jt/system-clock "UTC")
  (jt/zoned-date-time 2015 10))
;=> #<java.time.ZonedDateTime 2015-10-01T00:00Z[UTC]>
```

#### Instant

An `Instant` is used to generate a time stamp representing machine time. It
doesn't have an offset or a time zone. You can think of it as of a number of
milliseconds since epoch (`1970-01-01T00:00:00Z`). An instant is directly
analogous to `java.util.Date`:

```clj
(jt/instant)
;=> #<java.time.Instant "2015-09-26T05:25:48.667Z">

(java.util.Date.)
;=> #inst "2015-09-26T05:25:50.118-00:00"
```

Every other date entity can be converted to an instant (local ones will require
an additional zone information).

#### Period and Duration

Java Time Period entities are considerably simpler than the Joda-Time periods.
They are fixed containers of years, months and days. You can use them to
represent any period of time with a granularity larger or equal to a single day.
Duration, on the other hand, represents a standard duration less than or equal
to a single standard (24-hour) day.

### Caution
The current incarnation of the library is relatively slow while calling the 2-3
arity `zoned-date-time/offset-time/offset-date-time` constructors for the
*first time* in a given Clojure runtime. If you need predictable latency at the
time of the first call in your business logic, please warm the
constructors you are going to use up by calling them beforehand, e.g.:

```clj
(defn warm-up []
  (jt/zoned-date-time 2015 1 1)
  (jt/zoned-date-time 2015 1)
  (jt/zoned-date-time 2015))
```

The "constructor" here refers to an arity of a function together with its type
signature. For example, a `(jt/zoned-date-time 2015)` and `(jt/zoned-date-time (jt/system-clock))`
are different constructors.

### An appetizer

First, let's do a quick run through common use cases.

What is the current date?

```clj
(def now (jt/local-date))
;=> #object[java.time.LocalDate "2015-09-27"]
```

What's the next day?

```clj
(plus now (jt/days 1))
;=> #object[java.time.LocalDate "2015-09-28"]
```

The previous day?

```clj
(jt/minus now (jt/days 1))
;=> #object[java.time.LocalDate "2015-09-26"]
```

Three days starting at `now`?

```clj
(take 3 (jt/iterate jt/plus now (jt/days 1)))
;=> (#object[java.time.LocalDate "2015-09-27"]
;    #object[java.time.LocalDate "2015-09-28"]
;    #object[java.time.LocalDate "2015-09-29"])
```

When is the first Monday in month?

```clj
(jt/adjust now :first-in-month :monday)
;=> #object[java.time.LocalDate "2015-09-07"]
```

Date with some of its fields truncated:

```clj
(jt/truncate-to (jt/local-date-time 2015 9 28 10 15) :days)
;=> #object[java.time.LocalDateTime "2015-09-28T00:00"]
```

Date-time adjusted to the given hour:

```clj
(jt/adjust (jt/local-date-time 2015 9 28 10 15) (jt/local-time 6))
;=> #object[java.time.LocalDateTime "2015-09-28T06:00"]
```

The latest of the given dates?

```clj
(jt/max (jt/local-date 2015 9 20) (jt/local-date 2015 9 28) (jt/local-date 2015 9 1))
;=> #object[java.time.LocalDate "2015-09-28"]
```

The shortest of the given durations?

```clj
(jt/min (jt/duration 10 :seconds) (jt/duration 5 :hours) (jt/duration 3000 :millis))
;=> #object[java.time.Duration "PT3S"]
```

Get the year field out of the date:

```clj
(jt/as (jt/local-date 2015 9 28) :year)
;=> 2015
```

Get multiple fields:

```clj
(jt/as (jt/local-date 2015 9 28) :year :month-of-year :day-of-month)
;=> (2015 9 28)
```

Get the duration in a different unit:

```clj
(jt/plus (jt/hours 3) (jt/minutes 2))
;=> #object[java.time.Duration "PT3H2M"]

(jt/as *1 :minutes)
;=> 182
```

Format a date:

```clj
(jt/format "MM/dd" (jt/zoned-date-time 2015 9 28))
;=> "09/28"
```

Parse a date:

```clj
(jt/local-date "MM/yyyy/dd" "09/2015/28")
;=> #object[java.time.LocalDate "2015-09-28"]
```

Zoned date-times and offset date-times/times always take the zone/offset as the
last argument. Offsets can be specified as float values:

```clj
(jt/zone-offset +1.5)
;=> #<java.time.ZoneOffset +01:30>

(jt/zone-offset -1.5)
;=> #<java.time.ZoneOffset -01:30>
```

Compare dates:

```clj
(jt/before? (jt/year 2020) (jt/year 2021))
;=> true

(jt/after? (jt/year 2021) (jt/year 2021))
;=> false

(let [expiration-date (jt/year 2010)
      purchase-date (jt/year 2010)]
  (jt/not-before? expiration-date purchase-date))
;=> true

(let [start-date (jt/year 2011)
      cutoff-date (jt/year 2010)]
  (jt/not-after? start-date cutoff-date))
;=> false
```

#### Conversions

Time entities can be converted to other time entities if the target contains
less information, e.g. (assuming we're in UTC timezone):

```clj
(jt/zoned-date-time (jt/offset-date-time 2015 9 28 1))
;=> #object[java.time.ZonedDateTime "2015-09-28T01:00Z"]

(jt/instant (jt/offset-date-time 2015 9 28 1))
;=> #object[java.time.Instant "2015-09-28T01:00:00Z"]

(jt/offset-time (jt/offset-date-time 2015 9 28 1))
;=> #object[java.time.OffsetTime "01:00Z"]

(jt/local-date-time (jt/offset-date-time 2015 9 28 1))
;=> #object[java.time.LocalDateTime "2015-09-28T01:00"]

(jt/local-time (jt/offset-time 1))
;=> #object[java.time.LocalTime 0x3a3cd6d5 "01:00"]
```

Converting an Instant to ZonedDateTime requires a time zone:

```clojure
(jt/zoned-date-time (jt/instant 100) "UTC)
;=> #object[java.time.ZonedDateTime 0x291777c0 "1970-01-01T00:00:00.100Z[UTC]"]
```

#### Legacy Date-Time Types

Any date which can be converted to an instant, can also be converted to a
`java.util.Date`:

```clojure
(jt/java-date (jt/zoned-date-time 2015 9 28))
;=> #inst "2015-09-27T22:00:00.000-00:00"

(jt/java-date 50000)
;=> #inst "1970-01-01T00:00:50.000-00:00"
```

An instance of `java.util.Date` serves the same purpose as the new
`java.time.Instant`. It's a machine timestamp which isn't aware of the
timezone. Please, do not get confused by the way it is printed by the Clojure
printer - the UTC timezone is applied during formatting.

Sometimes you'll have to work with the legacy `java.sql.{Date,Time,Timestamp}`
types. The correspondence between the legacy types and the new Date-Time
entities is as follows:

  * `java.sql.Date`  <-> `java.time.LocalDate`
  * `java.sql.Timestamp` <-> `java.time.LocalDateTime` 
  * `java.sql.Time` <-> `java.time.LocalTime`

```clojure
(jt/sql-date 2015 9 28)
;=> #inst "2015-09-27T22:00:00.000-00:00"

(jt/sql-timestamp 2015 9 28 10 20 30 4000000)
;=> #inst "2015-09-28T09:20:30.004-00:00"

(jt/sql-time 10 20 30)
;=> #inst "1970-01-01T09:20:30.000-00:00"
```

The results of the above calls get printed as `#inst` because all of the
`java.sql.{Date,Time,Timestamp}` are subtypes of `java.util.Date`.
Coincidentally, this makes it impossible to plug the `java.sql.*` types into
the Clojure.Java-Time conversion graph.

Conversions to the legacy types also go the other way around:

```clojure
(jt/local-date (jt/sql-date 2015 9 28))
;=> #object[java.time.LocalDate "2015-09-28"]

(jt/local-date-time (jt/sql-timestamp 2015 9 28 10 20 30 4000000))
;=> #object[java.time.LocalDateTime "2015-09-28T10:20:30.004"]

(jt/local-time (jt/sql-time 10 20 30))
;=> #object[java.time.LocalTime "10:20:30"]
```

#### Three-Ten Extra

If you add an optional `[org.threeten/threeten-extra "1.2"]` dependency to the
project, you will get an `Interval`, `AmPm`, `DayOfMonth`, `DayOfYear`,
`Quarter` and `YearQuarter` data types as well as a couple more adjusters.

An interval can be constructed from two entities that can be converted to
instants:

```clojure
(jt/interval (jt/offset-date-time 2015 1 1) (jt/zoned-date-time 2016 1 1))
;=> #<org.threeten.extra.Interval 2015-01-01T00:00:00Z/2016-01-01T00:00:00Z>

(jt/move-start-by *1 (jt/duration 5 :days))
;=> #<org.threeten.extra.Interval 2015-01-06T00:00:00Z/2016-01-01T00:00:00Z>

(jt/move-end-by *1 (jt/duration 5 :days))
;=> #<org.threeten.extra.Interval 2015-01-06T00:00:00Z/2016-01-06T00:00:00Z>

(jt/contains? *1 (jt/offset-date-time 2015 1 1))
;=> false
```

#### Joda-Time

Bonus! If you have Joda Time on the classpath (either directly, or via
`clj-time`), you can seamlessly convert from Joda Time to Java Time types:

```clojure
(java-time.repl/show-path org.joda.time.DateTime java.time.OffsetTime)
;=> {:cost 2.0,
;    :path [[#<java_time.graph.Types@15e43c24 [org.joda.time.DateTime]>
;            #<java_time.graph.Types@78a2235c [java.time.Instant java.time.ZoneId]>]
;           [#<java_time.graph.Types@6d8ded1a [java.time.Instant java.time.ZoneId]>
;            #<java_time.graph.Types@5360f6ae [java.time.OffsetTime]>]]}

(jt/offset-time (org.joda.time.DateTime/now))
;=> #<java.time.OffsetTime 22:00:00.000000000-00:00>
```

Clojure 1.9 added an [Inst](https://clojuredocs.org/clojure.core/inst_q)
protocol which is implemented for `java.util.Date` and `java.time.Instant` by
default. If you're stuck on Joda-Time, you can extend the
`org.joda.time.ReadableInstant`, which includes both `Instant` and `DateTime`
using the following:

```clojure
(jt/when-joda-time-loaded
  (extend-type org.joda.time.ReadableInstant
    Inst (inst-ms* [inst] (.getMillis inst))))
```

This snippet isn't included in the Clojure.Java-Time code by default as both
the `Inst` protocol and the Joda-Time types are external to the library.

#### Clocks

Java Time introduced a concept of `Clock` - a time entity which can seed the
dates, times and zones. However, there's no built-in facility which would allow
you to influence the date-times created using default constructors ala Joda's
`DateTimeUtils/setCurrentMillisSystem`. Clojure.Java-Time tries to fix that with
the `with-clock` macro and the corresponding `with-clock-fn` function:

```clojure
(jt/zone-id)
;=> #<java.time.ZoneRegion Europe/London>

(jt/with-clock (jt/system-clock "UTC")
  (jt/zone-id))
;=> #<java.time.ZoneRegion UTC>
```

In addition to the built-in `java.time` clocks, we provide a Mock clock which
can be very handy in testing:

```clojure
(def clock (jt/mock-clock 0 "UTC"))
;=> #'user/clock

(jt/with-clock clock
  (jt/instant))
;=> #object[java.time.Instant "1970-01-01T00:00:00Z"]

(jt/advance-clock! clock (jt/plus (jt/hours 5) (jt/minutes 20)))
;=> nil

(jt/with-clock clock
  (jt/instant))
;=> #object[java.time.Instant "1970-01-01T05:20:00Z"]

(jt/set-clock! clock 0)
;=> nil

(jt/with-clock clock
  (jt/instant))
;=> #object[java.time.Instant "1970-01-01T00:00:00Z"]
```

Clock overrides works for all of the date-time types.

#### Fields, Units and Properties

Date-Time entities are composed of date fields, while Duration entities are
composed of time units. You can see all of the predefined fields and units
via the `java-time.repl` ns:

```clojure
(java-time.repl/show-fields)
;=> (:aligned-day-of-week-in-month
;    :aligned-day-of-week-in-year
;    :aligned-week-of-month
;    :aligned-week-of-year
;    :am-pm-of-day
;    :clock-hour-of-am-pm
;    ...)
```

```clojure
(java-time.repl/show-units)
;=> (:centuries
;    :days
;    :decades
;    :eras
;    :forever
;    :half-days
;    ...)
```

You can obtain any field/unit like this:

```clojure
(jt/field :year)
;=> #object[java.time.temporal.ChronoField "Year"]

(jt/unit :days)
;=> #object[java.time.temporal.ChronoUnit "Days"]

(jt/field (local-date 2015) :year)
;=> #object[java.time.temporal.ChronoField "Year"]
```

You can obtain all of the fields/units of the temporal entity:

```clojure
(jt/fields (jt/local-date))
;=> {:proleptic-month #object[java.time.temporal.ChronoField ...}

(jt/units (jt/duration))
;=> {:seconds #object[java.time.temporal.ChronoUnit "Seconds"],
;    :nanos #object[java.time.temporal.ChronoUnit "Nanos"]}
```

By themselves the fields and units aren't very interesting. You can get the
range of valid values for a field and a duration between two dates, but that's
about it:

```clojure
(jt/range (jt/field :year))
;=> #object[java.time.temporal.ValueRange "-999999999 - 999999999"]

(jt/range (jt/field :day-of-month))
;=> #object[java.time.temporal.ValueRange "1 - 28/31"]

(jt/time-between (jt/local-date 2015 9) (jt/local-date 2015 9 28) :days)
;=> 27
```

Fields and units become interesting in conjunction with properties. Java-Time
doesn't support the concept of properties which is present in Joda-Time. There
are reasons for that which I feel are only valid in a statically-typed API like
Java's. In Clojure, properties allow expressing time entity modifications and
queries uniformly across all of the entity types.

```clojure
(def prop (jt/property (jt/local-date 2015 2 28) :day-of-month))
;=> #java_time.temporal.TemporalFieldProperty{...}

(jt/value prop)
;=> 28

(jt/with-min-value prop)
;=> #object[java.time.LocalDate "2015-02-01"]

(jt/with-value prop 20)
;=> #object[java.time.LocalDate "2015-02-20"]

(jt/with-max-value prop)
;=> #object[java.time.LocalDate "2015-02-28"]

(jt/properties (jt/local-date 2015 9 28))
;=> {:proleptic-month #java_time.temporal.TemporalFieldProperty{...}, ...}
```

## Implementation Details

Most of the temporal entity constructors with arities 1 to 3 use the conversion
graph underneath. This provides for a very flexible way of defining the
conversions while avoiding huge conditional statements and multiple definitions
of the identical conversion logic. However, the flexibility comes with a cost:

1. The first call to a constructor will take a _long_ time as it will try to
   find a path in the conversion graph. Subsequent calls will reuse the path.
2. It's not trivial to evaluate the impact of adding and removing conversions
   both on the performance and the conversion path chosen for certain arguments.
3. You might get nonsensical results for some of the paths in the graph that
   you might expect would make sense.

Hopefully, the performance issue will be resolved in the future...

You can play with the conversion graph using the following helpers:

```clojure
(java-time.repl/show-path org.joda.time.DateTime java.time.OffsetTime)
;=> {:cost 2.0,
;    :path [[#<java_time.graph.Types@15e43c24 [org.joda.time.DateTime]>
;            #<java_time.graph.Types@78a2235c [java.time.Instant java.time.ZoneId]>]
;           [#<java_time.graph.Types@6d8ded1a [java.time.Instant java.time.ZoneId]>
;            #<java_time.graph.Types@5360f6ae [java.time.OffsetTime]>]]}

(java-time.repl/show-graph)
;=> {1
;     {org.threeten.extra.DayOfYear
;      [[#object[java_time.graph.Types "[java.lang.Number]"]
;        #object[java_time.graph.Conversion "Cost:1.0"]]],
;      java.lang.Number
;      [[#object[java_time.graph.Types "[java.time.Instant]"]
;        #object[java_time.graph.Conversion "Cost:1.0"]]
;        ...}}
```
