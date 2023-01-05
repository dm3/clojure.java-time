(ns java-time.dev.gen
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.walk :as walk]))

(def impl-local-sym '+impl+)

(defn normalize-argv [argv]
  {:post [(or (empty? %)
              (apply distinct? %))
          (not-any? #{impl-local-sym} %)]}
  (into [] (map-indexed (fn [i arg]
                          (if (symbol? arg)
                            (do (assert (not (namespace arg)))
                                (if (some #(Character/isDigit (char %)) (name arg))
                                  (symbol (apply str (concat
                                                       (remove #(Character/isDigit (char %)) (name arg))
                                                       [i])))
                                  arg))
                            (symbol (str "arg" i)))))
        argv))

(defn normalize-arities [arities]
  (cond-> arities
    (= 1 (count arities)) first))

(defn import-fn [sym]
  {:pre [(namespace sym)]}
  (let [vr (find-var sym)
        m (meta vr)
        n (:name m)
        arglists (:arglists m)
        protocol (:protocol m)
        when-class (-> sym meta :when-class)
        forward-meta (into (sorted-map) (select-keys m [:tag :arglists :doc :deprecated]))
        _ (assert (not= n impl-local-sym))
        _ (when (:macro m)
            (throw (IllegalArgumentException.
                     (str "Calling import-fn on a macro: " sym))))
        form (if protocol
               (list* 'defn (with-meta n (dissoc forward-meta :arglists))
                      (map (fn [argv]
                             {:pre [(not-any? #{'&} argv)]}
                             (list argv (list* sym argv)))
                           arglists))
               (list 'def (with-meta n forward-meta) sym))]
    (cond->> form
      when-class (list 'java-time.util/when-class when-class))))

(defn import-macro [sym]
  (let [vr (find-var sym)
        m (meta vr)
        _ (when-not (:macro m)
            (throw (IllegalArgumentException.
                     (str "Calling import-macro on a non-macro: " sym))))
        n (:name m)
        arglists (:arglists m)]
    (list* 'defmacro n 
           (concat
             (some-> (not-empty (into (sorted-map) (select-keys m [:doc :deprecated])))
                     list)
             (normalize-arities
               (map (fn [argv]
                      (let [argv (normalize-argv argv)]
                        (list argv
                              (if (some #{'&} argv)
                                (list* 'list* (list 'quote sym) (remove #{'&} argv))
                                (list* 'list (list 'quote sym) argv)))))
                    arglists))))))

(defn import-vars
  "Imports a list of vars from other namespaces."
  [& syms]
  (let [unravel (fn unravel [x]
                  (if (sequential? x)
                    (->> x
                         rest
                         (mapcat unravel)
                         (map
                           #(with-meta
                              (symbol
                                (str (first x)
                                     (when-let [n (namespace %)]
                                       (str "." n)))
                                (name %))
                              (meta %))))
                    [x]))
        syms (mapcat unravel syms)]
    (map (fn [sym]
           (let [vr (if-some [rr (resolve 'clojure.core/requiring-resolve)]
                      (rr sym)
                      (do (require (-> sym namespace symbol))
                          (resolve sym)))
                 _ (assert vr (str sym " is unresolvable"))
                 m (meta vr)]
             (if (:macro m)
               (import-macro sym)
               (import-fn sym))))
         syms)))

(def impl-info
  {:macros '[[java-time.clock with-clock]
             [java-time.util when-joda-time-loaded]]
   :threeten-extra-fns ['[java-time.interval interval interval?]

                        '[java-time.single-field
                          am-pm am-pm? quarter quarter? day-of-month day-of-month?
                          day-of-year day-of-year? year-quarter year-quarter?]]
   :fns ['[java-time.clock with-clock-fn]
         '[java-time.core
           zero? negative? negate abs max min 
           before? not-after? after? not-before?
           supports?
           fields units properties property
           as value range min-value max-value largest-min-value smallest-max-value
           truncate-to time-between with-zone 
           plus minus multiply-by
           ;; TODO below here needs unit tests
           chronology leap? with-value with-min-value with-max-value with-largest-min-value with-smallest-max-value]

         '[java-time.amount
           duration period period? duration?
           nanos micros millis seconds minutes hours standard-days
           days weeks months years]

         '[java-time.properties
           unit? unit field? field]

         '[java-time.temporal
           value-range instant instant?]

         '[java-time.local
           local-date local-date-time local-time
           local-date? local-date-time? local-time?]

         '[java-time.single-field
           year year? month month? day-of-week day-of-week? month-day month-day?
           year-month year-month?]

         '[java-time.zone
           available-zone-ids zone-id zone-offset
           offset-date-time offset-time zoned-date-time
           system-clock fixed-clock offset-clock tick-clock clock?
           zone-id? zoned-date-time? offset-date-time? offset-time?
           with-zone-same-instant with-offset with-offset-same-instant]

         '[java-time.mock mock-clock advance-clock! set-clock!]

         '[java-time.convert
           as-map convert-amount to-java-date to-sql-date to-sql-timestamp
           to-millis-from-epoch]

         '[java-time.sugar
           monday? tuesday? wednesday? thursday? friday? saturday? sunday?
           weekend? weekday?]

         '[java-time.seqs iterate]

         '[java-time.adjuster adjust]

         '[java-time.format format formatter]

         '[java-time.pre-java8 java-date sql-date sql-timestamp instant->sql-timestamp
           ^{:when-class "java.sql.Time"} sql-time]

         '[java-time.interval
           move-start-to move-end-to move-start-by move-end-by
           start end contains? overlaps? abuts? overlap gap]]})

(defn gen-java-time-ns-forms [nsym]
  (let [require-macros (into #{} (map first) (:macros impl-info))
        require-fns #_(set/difference (into #{} (map first)
                                          (concat (:threeten-extra-fns impl-info)
                                                  (:fns impl-info)))
                                    require-macros)
        ;;FIXME implementations must be loaded in this order for a stable graph traversal (I think)
        (into #{} (map #(symbol (str "java-time." %)))
              '[core properties temporal amount zone single-field local chrono
                convert sugar seqs adjuster interval format joda clock pre-java8 mock])]
    (concat
      [";; NOTE: This namespace is generated by java-time.dev.gen"
       `(~'ns ~nsym
          (:refer-clojure :exclude ~'(zero? range iterate max min contains? format abs))
          (:require ~'[java-time core properties temporal amount zone single-field local chrono
                       convert sugar seqs adjuster interval format joda clock pre-java8 mock]))]
      (apply import-vars (:macros impl-info))
      (apply import-vars (:fns impl-info))
      
      [(list* 'java-time.util/when-class "org.threeten.extra.Temporals" (apply import-vars (:threeten-extra-fns impl-info)))])))

(defn print-form [form]
  (with-bindings
    (cond-> {#'*print-meta* true
             #'*print-length* nil
             #'*print-level* nil}
      (resolve '*print-namespace-maps*)
      (assoc (resolve '*print-namespace-maps*) false))
    (cond
      (string? form) (println form)
      :else (println (pr-str (walk/postwalk
                               (fn [v]
                                 (if (meta v)
                                   (if (symbol? v)
                                     (vary-meta v #(not-empty
                                                     (cond-> (sorted-map)
                                                       (some? (:tag %)) (assoc :tag (:tag %))
                                                       (some? (:doc %)) (assoc :doc (:doc %))
                                                       ((some-fn true? string?) (:deprecated %)) (assoc :deprecated (:deprecated %))
                                                       (string? (:superseded-by %)) (assoc :superseded-by (:superseded-by %))
                                                       (string? (:supercedes %)) (assoc :supercedes (:supercedes %))
                                                       (some? (:arglists %)) (assoc :arglists (list 'quote (doall (map normalize-argv (:arglists %))))))))
                                     (with-meta v nil))
                                   v))
                               form)))))
  nil)

(defn print-java-time-ns [nsym]
  (run! print-form (gen-java-time-ns-forms nsym)))

(def java-time-nsym
  (with-meta
    'java-time
    {:superseded-by "java-time.api"
     :deprecated "1.1.0"
     :doc
     "This namespace has been deprecated due to [#91](https://github.com/dm3/clojure.java-time/issues/91).
     Please migrate to [[java-time.api]]

     This namespace will continue to exist and be updated. For
     maximum JVM compatibility, please migrate to `java-time.api`,
     which provides the same interface. `java-time` and `java-time.api`
     and can be freely intermixed within the same library, so you can
     safely migrate your own code even if your dependencies use the old namespace.
     
     Migration steps:

     1. rename all references from `java-time` to [[java-time.api]].
        eg., `(:require [java-time :as jt])` => `(:require [java-time.api :as jt])`"}))
(def java-time-api-nsym
  (with-meta
    'java-time.api
    {:supercedes "java-time"}))

(def gen-source->nsym
  {"src/java_time.clj" java-time-nsym
   "src/java_time/api.clj" java-time-api-nsym})

(defn spit-java-time-ns []
  (doseq [[source nsym] gen-source->nsym]
    (spit source (with-out-str (print-java-time-ns nsym)))))

(comment
  (print-java-time-ns java-time-nsym)
  (spit-java-time-ns)
  )
