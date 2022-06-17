(ns java-time.dev.gen
  (:require [clojure.string :as str]
            [clojure.set :as set]))

(defn normalize-argv [argv]
  {:post [(or (empty? %)
              (apply distinct? %))]}
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

(defn import-fn [sym]
  (let [vr (find-var sym)
        m (meta vr)
        n (:name m)
        arglists (:arglists m)
        protocol (:protocol m)
        forward-meta (into (sorted-map) (select-keys m [:doc :tag :deprecated]))
        forward-meta (cond-> forward-meta
                       (nil? (:tag forward-meta)) (dissoc :tag))
        impl '+impl+]
    (when (:macro m)
      (throw (IllegalArgumentException.
               (str "Calling import-fn on a macro: " sym))))
    (list 'let [impl (list 'delay
                            (list 'load-java-time)
                            (list 'deref (list 'resolve (list 'quote sym))))]
               (list* 'defn n
                      (concat
                        (some-> (not-empty forward-meta) list)
                        (map (fn [argv] 
                               (let [argv (normalize-argv argv)]
                                 (list argv
                                       (if (some #{'&} argv)
                                         (list* 'apply (list 'deref impl) (remove #{'&} argv))
                                         (list* (list 'deref impl) argv)))))
                             arglists))))))

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
             (map (fn [argv]
                    (let [argv (normalize-argv argv)]
                      (list argv
                            (if (some #{'&} argv)
                              (list* 'list* (list 'quote sym) (remove #{'&} argv))
                              (list* 'list (list 'quote sym) argv)))))
                  arglists)))))

(defn import-vars
  "Imports a list of vars from other namespaces."
  [& syms]
  (let [unravel (fn unravel [x]
                  (if (sequential? x)
                    (->> x
                         rest
                         (mapcat unravel)
                         (map
                           #(symbol
                              (str (first x)
                                   (when-let [n (namespace %)]
                                     (str "." n)))
                              (name %))))
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

         '[java-time.pre-java8 java-date sql-date sql-timestamp instant->sql-timestamp sql-time]

         '[java-time.interval
           move-start-to move-end-to move-start-by move-end-by
           start end contains? overlaps? abuts? overlap gap]]})

(defn gen-java-time-ns-forms []
  (let [require-macros (into #{} (map first) (:macros impl-info))
        require-fns #_(set/difference (into #{} (map first)
                                          (concat (:threeten-extra-fns impl-info) (:fns impl-info)))
                                    require-macros)
        ;;FIXME implementations must be loaded in this order for a stable graph traversal (I think)
        (into #{} (map #(symbol (str "java-time." %)))
              '[core properties temporal amount zone single-field local chrono
                convert sugar seqs adjuster interval format joda clock pre-java8 mock])]
    (concat
      [";; NOTE: This namespace is generated by java-time.dev.gen"
       `(~'ns ~'java-time
          (:refer-clojure :exclude ~'(zero? range iterate max min contains? format abs))
          (:require ~@(sort require-macros)))
       (format "(let [lock (the-ns 'java-time) do-load (delay (locking lock (require %s #?@(:bb [] :default ['java-time.mock]))))]\n  (defn load-java-time \"Load java-time implementation\" [] @do-load))"
               (str/join " " (map #(str "'" %) (sort (disj require-fns 'java-time.mock)))))
       '(if *compile-files*
          (load-java-time)
          (when-not (= "true" (System/getProperty "java-time.no-async-load"))
            (.start (Thread. (fn [] (load-java-time))))))]
      (apply import-vars (:macros impl-info))
      (apply import-vars (:fns impl-info))
      (map #(list 'java-time.utils/when-threeten-extra %) (apply import-vars (:threeten-extra-fns impl-info))))))

(defn print-form [form]
  (cond
    (string? form) (println form)
    :else (println (pr-str form)))
  nil)

(defn print-java-time-ns []
  (run! print-form (gen-java-time-ns-forms)))

(defn spit-java-time-ns []
  (spit "src/java_time.cljc" (with-out-str (print-java-time-ns))))

(comment
  (print-java-time-ns)
  (spit-java-time-ns)
  )
