(ns java-time-test
  "Tests live in java-time.api-test, but we rename the
  alias j to point to java-time here."
  (:require [java-time :as j]))

(with-open [rdr (-> (slurp "test/java_time/api_test.clj")
                  java.io.StringReader.
                  clojure.lang.LineNumberingPushbackReader.)]
  (let [ns-form (read rdr) ;;rm ns form
        _ (assert (= '(ns java-time.api-test
                        (:require [java-time.api :as j]))
                     (remove string? ns-form))
                  (pr-str ns-form))
        s (str (slurp rdr)
               "\n(assert (= *ns* (the-ns 'java-time-test)) *ns*)")] 
    #_
    (println "DEBUG\n" s)
    (load-string s)))
(assert (= *ns* (the-ns 'java-time-test)) *ns*)
(assert (= #'java-time-test/constructors-test
           (resolve 'constructors-test))
        (resolve 'constructors-test))
(require 'java-time.api-test)
(assert (= (set (keys (ns-publics 'java-time-test)))
           (set (keys (ns-publics 'java-time.api-test)))))
