(ns java-time.adjuster
  (:require [java-time.util :as jt.u]
            [java-time.single-field :as jt.sf])
  (:import [java.time.temporal TemporalAdjusters TemporalAdjuster]))

(def base-adjusters {:first-day-of-month [(TemporalAdjusters/firstDayOfMonth) 0]
                     :last-day-of-month [(TemporalAdjusters/lastDayOfMonth) 0]
                     :first-day-of-next-month [(TemporalAdjusters/firstDayOfNextMonth) 0]
                     :first-day-of-year [(TemporalAdjusters/firstDayOfYear) 0]
                     :last-day-of-year [(TemporalAdjusters/lastDayOfYear) 0]
                     :first-day-of-next-year [(TemporalAdjusters/firstDayOfNextYear) 0]
                     :first-in-month [#(TemporalAdjusters/firstInMonth (jt.sf/day-of-week %)) 1]
                     :last-in-month [#(TemporalAdjusters/lastInMonth (jt.sf/day-of-week %)) 1]
                     :day-of-week-in-month [#(TemporalAdjusters/dayOfWeekInMonth
                                               (int %1) (jt.sf/day-of-week %2)) 2]
                     :next-day-of-week [#(TemporalAdjusters/next (jt.sf/day-of-week %)) 1]
                     :next-or-same-day-of-week [#(TemporalAdjusters/nextOrSame (jt.sf/day-of-week %)) 1]
                     :previous-day-of-week [#(TemporalAdjusters/previous (jt.sf/day-of-week %)) 1]
                     :previous-or-same-day-of-week [#(TemporalAdjusters/previousOrSame (jt.sf/day-of-week %)) 1]})

(def extra-adjusters
  (jt.u/if-threeten-extra
    {:next-working-day [(org.threeten.extra.Temporals/nextWorkingDay) 0]
     :previous-working-day [(org.threeten.extra.Temporals/previousWorkingDay) 0]}
    {}))

(def predefined-adjusters (merge base-adjusters extra-adjusters))

(defn- ^TemporalAdjuster get-adjuster [kw args]
  (if-let [[adj nargs] (get predefined-adjusters kw)]
    (if (zero? nargs)
      adj
      (if (= (count args) nargs)
        (apply adj args)
        (throw (java.time.DateTimeException.
                 (str "Adjuster: " (name kw) " cannot be created from" args "!")))))
    (throw (java.time.DateTimeException.
             (str "Adjuster: " (name kw) " not found!")))))

(defn adjust
  "Adjusts the temporal `entity` using the provided `adjuster` with optional `args`.

  The adjuster should either be a keyword which resolves to one of the
  predefined adjusters (see `java-time.repl/show-adjusters`) an instance of
  `TemporalAdjuster` or a function which returns another temporal entity when
  applied to the given one:

    (adjust (local-date 2015 1 1) :next-working-day)
    => #<LocalDate 2015-1-2>

    (adjust (local-date 2015 1 1) :first-in-month :monday)
    => #<LocalDate 2015-1-5>

    (adjust (local-date 2015 1 1) plus (days 1))
    => #<LocalDate 2015-1-2>"
  [entity adjuster & args]
  (cond (instance? TemporalAdjuster adjuster)
        (.adjustInto ^TemporalAdjuster adjuster ^Temporal entity)

        (fn? adjuster)
        (apply adjuster entity args)

        (keyword? adjuster)
        (.adjustInto (get-adjuster adjuster args) ^Temporal entity)))
