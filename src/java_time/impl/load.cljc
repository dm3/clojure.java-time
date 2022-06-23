;; internal!
(ns java-time.impl.load)

;; all macro deps should be loaded by the end of `java-time` load. this takes
;; about the same time as `java-time` itself to load, so there isn't much penalty.
(def ^Thread serialized-load-thread
  (let [^Runnable f (bound-fn [] (require 'java-time.clock 'java-time.util))]
    (doto (Thread. f) .start)))

(let [lock (the-ns 'java-time.impl.load)
      do-load (delay (locking lock
                       (.join serialized-load-thread)
                       (require 'java-time.core 'java-time.adjuster 'java-time.amount 'java-time.chrono 'java-time.clock
                                'java-time.convert 'java-time.format 'java-time.interval 'java-time.joda 'java-time.local
                                'java-time.pre-java8 'java-time.properties 'java-time.seqs 'java-time.single-field 'java-time.sugar
                                'java-time.temporal 'java-time.zone #?@(:bb [] :default ['java-time.mock]))))]
  (def load-java-time (fn [] @do-load)))

(def slow-path-vars (volatile! []))

(def slow-path*
  (fn [sym]
    (vswap! slow-path-vars conj sym)
    (fn [& args]
      (load-java-time)
      (apply (resolve sym) args))))

(defmacro slow-path [vsym]
  (if *compile-files*
    `(do (vswap! slow-path-vars conj ~vsym)
         (load-java-time)
         @(resolve ~vsym))
    `(slow-path* ~vsym)))

(def ^Runnable async-load-fast-path
  (bound-fn []
    (load-java-time)
    (run! (fn [sym]
            (let [api (resolve (symbol "java-time" (name sym)))
                  impl (resolve sym)]
              (alter-var-root api (constantly @impl))
              (alter-meta! api vary-meta (fnil into {}) (select-keys (meta impl) [:doc :arglists :deprecated]))))
          @slow-path-vars)))

(defmacro when-class [clstr & body]
  (let [loaded (try (Class/forName clstr)
                    (catch Throwable e))]
    (when #?(:bb (and (resolve (symbol clstr)) loaded)
             :default loaded)
      (list* 'do body))))
