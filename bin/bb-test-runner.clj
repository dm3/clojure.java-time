;; run via ./bin/bb-test-runner.sh
(ns bb-test-runner
  (:require [clojure.test :as t]
            [babashka.classpath :as cp]))

(def require-nsyms ['java-time.potemkin.util
                    'java-time.potemkin.namespaces
                    'java-time.joda
                    ;'java-time
                    ])
;;TODO
(def test-nsyms [#_'java-time-test
                 #_'java-time.graph-test])

(some->> (seq (concat require-nsyms test-nsyms))
         (apply require))

(def test-results
  (apply t/run-tests test-nsyms))

(def failures-and-errors
  (let [{:keys [:fail :error]} test-results]
    (+ fail error)))

(System/exit failures-and-errors)
