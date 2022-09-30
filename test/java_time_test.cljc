(ns java-time-test
  "Tests live in java-time.api-test, but we rename the
  alias j to point to java-time here."
  (:require [java-time :as j]))

;; prefer over ()
(with-open [rdr (-> (slurp "test/java_time/api_test.cljc")
                    java.io.StringReader.
                    clojure.lang.LineNumberingPushbackReader.)]
  (let [opts {:read-cond :allow
              :eof (Object.)
              :features (-> #{:clj} #?(:bb (conj :bb)
                                       ;for linter
                                       :default identity))}
        ns-form (read opts rdr) ;;rm ns form
        _ (assert (= '(ns java-time.api-test
                        (:require [java-time.api :as j]))
                     (remove string? ns-form))
                  (pr-str ns-form))] 
    (loop []
      (let [form (read opts rdr)]
        (when-not (identical? (:eof opts) form)
          ;(println "DEBUG\n" form)
          (eval form)
          (recur))))))
(assert (= *ns* (the-ns 'java-time-test)) *ns*)
(assert (= #'java-time-test/constructors-test
           (resolve 'constructors-test))
        (resolve 'constructors-test))
(require 'java-time.api-test)
(assert (= (set (keys (ns-publics 'java-time-test)))
           (set (keys (ns-publics 'java-time.api-test)))))
