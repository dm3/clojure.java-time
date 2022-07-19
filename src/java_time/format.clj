(ns java-time.format
  (:refer-clojure :exclude (format))
  (:require [java-time.util :as jt.u])
  (:import [java.time.temporal TemporalAccessor]
           [java.time.format DateTimeFormatter DateTimeFormatterBuilder ResolverStyle]
           java.util.Locale))

(defonce predefined-formatters
  (->>
    {"BASIC_ISO_DATE"       DateTimeFormatter/BASIC_ISO_DATE
     "ISO_LOCAL_TIME"       DateTimeFormatter/ISO_LOCAL_TIME
     "RFC_1123_DATE_TIME"   DateTimeFormatter/RFC_1123_DATE_TIME
     "ISO_OFFSET_DATE"      DateTimeFormatter/ISO_OFFSET_DATE
     "ISO_OFFSET_DATE_TIME" DateTimeFormatter/ISO_OFFSET_DATE_TIME
     "ISO_ZONED_DATE_TIME"  DateTimeFormatter/ISO_ZONED_DATE_TIME
     "ISO_LOCAL_DATE_TIME"  DateTimeFormatter/ISO_LOCAL_DATE_TIME
     "ISO_TIME"             DateTimeFormatter/ISO_TIME
     "ISO_WEEK_DATE"        DateTimeFormatter/ISO_WEEK_DATE
     "ISO_LOCAL_DATE"       DateTimeFormatter/ISO_LOCAL_DATE
     "ISO_OFFSET_TIME"      DateTimeFormatter/ISO_OFFSET_TIME
     "ISO_ORDINAL_DATE"     DateTimeFormatter/ISO_ORDINAL_DATE
     "ISO_DATE"             DateTimeFormatter/ISO_DATE
     "ISO_DATE_TIME"        DateTimeFormatter/ISO_DATE_TIME
     "ISO_INSTANT"          DateTimeFormatter/ISO_INSTANT}
       (jt.u/map-kv
        (fn [^String n fmt]
          [(.. (.replace n \_ \-) toString (toLowerCase (Locale/US))) fmt]))))

(defn- get-resolver-style [s]
  (cond-> s
    (not (instance? ResolverStyle s))
    (case
      :strict ResolverStyle/STRICT
      :smart ResolverStyle/SMART
      :lenient ResolverStyle/LENIENT)))

(defn- ^DateTimeFormatterBuilder get-case-formatter [c]
  (let [fmt-builder (DateTimeFormatterBuilder.)]
    (if (= c :sensitive)
      (.parseCaseSensitive fmt-builder)
      (.parseCaseInsensitive fmt-builder))
    fmt-builder))

(defn ^java.time.format.DateTimeFormatter formatter
  "Constructs a DateTimeFormatter out of a

  * format string - \"yyyy/MM/dd\", \"HH:mm\", etc.
  * formatter name - :iso-date, :iso-time, etc.

  Accepts a map of options as an optional second argument:

  * `resolver-style` - either `:strict`, `:smart `or `:lenient`
  * `case` - either `:insensitive` or `:sensitive` (defaults to :sensitive)"
  ([fmt]
   (formatter fmt {}))
  ([fmt {:keys [resolver-style case] :or {case :sensitive}}]
   (let [^DateTimeFormatter fmt
         (cond (instance? DateTimeFormatter fmt) fmt
               (string? fmt) (.. (get-case-formatter case)
                                 (appendPattern fmt)
                                 toFormatter)
               :else (get predefined-formatters (name fmt)))]
     (cond-> fmt
       resolver-style (.withResolverStyle (get-resolver-style resolver-style))))))

(defn format
  "Formats the given time entity as a string.

  Accepts something that can be converted to a `DateTimeFormatter` or a
  formatter key, e.g. `:iso-offset-time`, as a first argument. Given one
  argument uses the default format.

  ```
  (format (zoned-date-time))
  => \"2015-03-21T09:22:46.677800+01:00[Europe/London]\"

  (format :iso-date (zoned-date-time))
  \"2015-03-21+01:00\"
  ```"
  ([o] (str o))
  ([fmt o]
   (.format (formatter fmt) o)))

(defn ^TemporalAccessor parse
  ([fmt o] (parse fmt o {}))
  ([fmt o opts]
   (.parse (formatter fmt opts) o)))
