(ns java-time.single-field
  (:require [clojure.string :as string]
            [java-time.zone :as jt.z]
            [java-time.format :as jt.f]
            [java-time.core :as jt.c]
            [java-time.util :as jt.u]
            [java-time.clock :as jt.clock]
            [java-time.defconversion :refer (conversion!)])
  (:import [java.time.temporal TemporalAccessor TemporalAmount TemporalUnit ChronoUnit]
           [java.time.format DateTimeFormatter]
           [java.time Clock Year Month YearMonth MonthDay DayOfWeek ZoneId Instant]))

(defn- resolve-tag [tag]
  (if (symbol? tag)
    (let [cls (resolve tag)]
      (if (var? cls)
        ;;primitive
        tag
        (symbol (.getName ^Class cls))))
    tag))

(defn- get-only-unit-value ^long [^TemporalAmount a, ^TemporalUnit u]
  (let [non-zero-units
        (->> (.getUnits a)
             (map (fn [^TemporalUnit tu] (vector tu (.get a tu))))
             (filter (fn [[_ uv]] (not (zero? uv)))))
        [our-unit our-value] (first (filter (fn [[tu]] (= tu u)) non-zero-units))]
    (when-not our-unit
      (let [msg (format "No unit: %s found in %s!" u a)]
        (throw #?(:bb (ex-info msg {})
                  :default (java.time.temporal.UnsupportedTemporalTypeException. msg)))))
    (when (> (count non-zero-units) 1)
      (let [msg (format "Cannot use: %s, expected only %s to be non-zero!" a u)]
        (throw #?(:bb (ex-info msg {})
                  :default (java.time.temporal.UnsupportedTemporalTypeException. msg)))))
    (long our-value)))

(defmacro enumerated-entity [tp doc & {:keys [unit]}]
  (assert (string? doc))
  (let [tp (resolve-tag tp)
        fname (with-meta (symbol (jt.u/dashize (-> (str tp) (string/split #"\.") peek))) {:tag tp})
        fields (symbol (str fname "-fields"))]
    `(do
       (def ~fields
         (->> (jt.u/class->TemporalAccessor-static-fields ~tp)
              vals
              (into {} (map (fn [m#] [(keyword (string/lower-case (str m#))) m#])))))

       (defn ~(symbol (str fname "?"))
         {:arglists '~'[[o]]
          :doc ~(str "True if `" tp "`.")}
         [o#]
         (instance? ~tp o#))

       (conversion! ~tp Number jt.c/value)

       (defn ~fname
         {:arglists '~'[[] [v] [fmt arg]]
          :doc ~doc}
         ([] (. ~tp from (jt.z/zoned-date-time)))
         ([v#] (cond (keyword? v#)
                     (v# ~fields)

                     (number? v#)
                     (. ~tp of (int v#))

                     (instance? TemporalAccessor v#)
                     (. ~tp from v#)))
         ([fmt# arg#]
          (~fname (jt.f/parse fmt# arg#))))

       (extend-type ~tp
         jt.c/Ordered
         (single-after? [d# o#]
           (> (.getValue d#) (.getValue (~fname o#))))
         (single-before? [d# o#]
           (< (.getValue d#) (.getValue (~fname o#)))))

        ;; Enum-based entities do not implement `Temporal`, thus we don't have an easy
        ;; option to add/subtract a TemporalAmount.
        ~@(when unit
            (for [[protocol proto-op op] [[`jt.c/Plusable 'seq-plus 'plus]
                                          [`jt.c/Minusable 'seq-minus 'minus]]]
              (let [typed-arg (with-meta (gensym) {:tag tp})]
                `(extend-type ~tp ~protocol
                   (~proto-op [o# os#]
                     (reduce
                       (fn [~typed-arg v#]
                         (cond (number? v#)
                               (. ~typed-arg ~op (long v#))

                               (instance? TemporalAmount v#)
                               (. ~typed-arg ~op (get-only-unit-value v# ~unit))))
                       o# os#)))))))))

(defmacro single-field-entity [tp doc & {:keys [parseable?]}]
  (assert (string? doc))
  (let [^Class tpcls (resolve tp)
        tp (symbol (.getName tpcls))
        fname (with-meta (symbol (jt.u/dashize (-> (str tp) (string/split #"\.") peek))) {:tag tp})
        arg (gensym)]
    `(do
       (defn ~(symbol (str fname "?"))
         {:arglists '~'[[o]]
          :doc ~(str "Returns true if `o` is `" tp "`, otherwise false.")}
         [o#]
         (instance? ~tp o#))

       (conversion! ~tp Number jt.c/value)

       (defn ~fname ~doc
         {:arglists '~'[[] [arg] [fmt arg]]}
         ([] (. ~tp from (jt.z/zoned-date-time)))
         ([~arg] (cond (number? ~arg)
                       (. ~tp of (int ~arg))

                       (instance? TemporalAccessor ~arg)
                       (. ~tp from ~arg)

                       (instance? Clock ~arg)
                       (. ~tp now ~(with-meta arg {:tag `Clock}))

                       (instance? ZoneId ~arg)
                       (. ~tp now ~(with-meta arg {:tag `ZoneId}))

                       ~@(when parseable?
                           `[(string? ~arg)
                             (try (. ~tp parse ~arg)
                                  (catch java.time.format.DateTimeParseException _#
                                    (. ~tp now (jt.z/zone-id ~arg))))])))
         ([fmt# arg#]
          (~fname (jt.f/parse fmt# arg#))))

       (extend-type ~tp
         jt.c/Ordered
         (single-after? [d# o#]
           (> (.getValue d#) (.getValue (~fname o#))))
         (single-before? [d# o#]
           (< (.getValue d#) (.getValue (~fname o#))))))))

(defmacro two-field-entity [tp doc & {:keys [major-field-types major-field-ctor
                                             minor-field-ctor minor-field-default]}]
  (assert (string? doc))
  (let [[major-field-ctor major-field-type] major-field-ctor
        [minor-field-ctor minor-field-type] minor-field-ctor
        major-field-type (resolve-tag major-field-type)
        minor-field-type (resolve-tag minor-field-type)
        tp (resolve-tag tp)
        fname (with-meta (symbol (jt.u/dashize (-> (str tp) (string/split #"\.") peek))) {:tag tp})
        arg (gensym)
        tmp-major (with-meta (gensym) {:tag major-field-type})
        tmp-minor (with-meta (gensym) {:tag minor-field-type})]
    `(do
       (defn ~(symbol (str fname "?"))
         {:arglists '~'[[o]]
          :doc ~(str "Returns true if `o` is `" tp "`, otherwise false.")}
         [o#]
         (instance? ~tp o#))

       (defn ~fname ~doc
         {:arglists '~'[[] [arg] [a b]]}
         ([] (. ~tp from (jt.z/zoned-date-time)))
         ([~arg] (cond (some (fn [x#] (instance? x# ~arg)) ~major-field-types)
                       (let [~tmp-major (~major-field-ctor ~arg)]
                         (. ~tp of ~tmp-major ~minor-field-default))

                       (instance? TemporalAccessor ~arg)
                       (. ~tp from ~arg)

                       (instance? Clock ~arg)
                       (. ~tp now ~(with-meta arg {:tag `Clock}))

                       (instance? ZoneId ~arg)
                       (. ~tp now ~(with-meta arg {:tag `ZoneId}))

                       (string? ~arg)
                       (try (. ~tp parse ~arg)
                            (catch java.time.format.DateTimeParseException _#
                              (. ~tp now (jt.z/zone-id ~arg))))

                       :else (let [~tmp-major (~major-field-ctor ~arg)]
                               (. ~tp of ~tmp-major ~minor-field-default))))
         ([a# b#]
          (if (and (or (instance? DateTimeFormatter a#) (string? a#)) (string? b#))
            (~fname (jt.f/parse a# b#))
            (let [~tmp-major (~major-field-ctor a#)
                  ~tmp-minor (~minor-field-ctor b#)]
              (. ~tp of ~tmp-major ~tmp-minor)))))

       (extend-type ~tp
         jt.c/Ordered
         (single-after? [d# o#]
           (.isAfter d# o#))
         (single-before? [d# o#]
           (.isBefore d# o#))
         jt.c/As
         (as* [o# k#]
            (jt.c/value (jt.c/property o# k#)))))))

(enumerated-entity DayOfWeek
  "Returns the `DayOfWeek` for the given day keyword name (e.g. `:monday`),
  ordinal or entity. Current day if no arguments given."
  :unit ChronoUnit/DAYS)

(enumerated-entity Month
  "Returns the `Month` for the given month keyword name (e.g. `:january`),
  ordinal or entity. Current month if no arguments given."
  :unit ChronoUnit/MONTHS)

(single-field-entity Year
 "Returns the `Year` for the given entity, string, clock, zone or number.
 Current year if no arguments given."
 :parseable? true)

(two-field-entity MonthDay
  "Returns the `MonthDay` for the given entity, string, clock, zone or
  month/day combination. Current month-day if no arguments given."
  :major-field-ctor [month Month]
  :major-field-types [Month Number]
  :minor-field-ctor [(comp int jt.c/value) int]
  :minor-field-default 1)

(two-field-entity YearMonth
  "Returns the `YearMonth` for the given entity, string, clock, zone or
  month/day combination. Current year-month if no arguments given."
  :major-field-ctor [(comp int jt.c/value) int]
  :major-field-types [Year Number]
  :minor-field-ctor [month Month]
  :minor-field-default 1)

;;;;;;;;;; Threeten Extra

;; Do not use Months/Days/Weeks/Years as already covered by java.time.Period
(jt.u/when-threeten-extra
  (import [org.threeten.extra AmPm DayOfMonth DayOfYear Quarter YearQuarter])

  (enumerated-entity AmPm
    "Returns the `AmPm` for the given keyword name (`:am` or `:pm`),
    ordinal or entity. Current AM/PM if no arguments given.")

  (enumerated-entity Quarter
    "Returns the `Quarter` for the given quarter keyword name (e.g. `:q1`),
    ordinal or entity. Current quarter if no arguments given."
    :unit java.time.temporal.IsoFields/QUARTER_YEARS)

  (single-field-entity DayOfMonth
    "Returns the `DayOfMonth` for the given entity, clock, zone or day of month.
    Current day of month if no arguments given.")

  (single-field-entity DayOfYear
    "Returns the `DayOfYear` for the given entity, clock, zone or day of year.
    Current day of year if no arguments given.")

  (defn ^Integer year-to-int [x]
    (if (number? x) (int x)
      (.getValue ^Year x)))

  (two-field-entity YearQuarter
    "Returns the `YearQuarter` for the given entity, clock, zone or year with quarter.
    Current year quarter if no arguments given."
    :major-field-ctor [year-to-int int]
    :major-field-types [Year Number]
    :minor-field-ctor [quarter Quarter]
    :minor-field-default 1))
