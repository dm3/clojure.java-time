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
  (let [vr (find-var sym)
        m (meta vr)
        n (:name m)
        arglists (:arglists m)
        protocol (:protocol m)
        when-class (-> sym meta :when-class)
        forward-meta (into (sorted-map) (select-keys m [:tag]))
        _ (assert (not= n impl-local-sym))]
    (when (:macro m)
      (throw (IllegalArgumentException.
               (str "Calling import-fn on a macro: " sym))))
    (cond->> (list 'def (with-meta n forward-meta)
                   (list 'java-time.impl.load/slow-path (list 'quote sym)))
      when-class (list 'java-time.impl.load/when-class when-class))))

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
           (let [_ (require (-> sym namespace symbol))
                 vr (resolve sym)
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

(defn gen-java-time-ns-forms []
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
       `(~'ns ~'java-time
          (:refer-clojure :exclude ~'(zero? range iterate max min contains? format abs))
          (:require java-time.impl.load))]
      (apply import-vars (:macros impl-info))
      (apply import-vars (:fns impl-info))
      
      [(list* 'java-time.impl.load/when-class "org.threeten.extra.Temporals" (apply import-vars (:threeten-extra-fns impl-info)))
       (list 'let ['t (list 'Thread. 'java-time.impl.load/async-load-fast-path)]
             (list 'if '*compile-files*
                   (list '.join 't)
                   (list 'do (list '.start 't)
                         ;; 70% of the runtime, would be nice to (carefully) eliminate
                         (list '.join 'java-time.impl.load/serialized-load-thread))))])))

(defn print-form [form]
  (binding [*print-namespace-maps* false
            *print-meta* true
            *print-length* nil
            *print-level* nil]
    (cond
      (string? form) (println form)
      :else (println (pr-str (walk/postwalk
                               (fn [v]
                                 (if (meta v)
                                   (if (symbol? v)
                                     (vary-meta v #(cond-> nil
                                                     (some? (:tag %)) (assoc :tag (:tag %))))
                                     (with-meta v nil))
                                   v))
                               form)))))
  nil)

(defn print-java-time-ns []
  (run! print-form (gen-java-time-ns-forms)))

(defn spit-java-time-ns []
  (spit "src/java_time.cljc" (with-out-str (print-java-time-ns))))

(comment
  (print-java-time-ns)
  (spit-java-time-ns)
  )
