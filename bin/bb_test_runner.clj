;; run via ./bin/bb-test-runner.sh
(ns bb-test-runner
  (:require [clojure.test :as t]
            [babashka.classpath :as cp]))

(def require-nsyms ['java-time.joda
                    'java-time.core
                    'java-time.properties
                    'java-time.util
                    'java-time.temporal
                    'java-time.amount
                    'java-time.zone
                    'java-time.single-field
                    'java-time.local
                    'java-time.chrono
                    'java-time.convert
                    'java-time.sugar
                    'java-time.seqs
                    'java-time.adjuster
                    'java-time.interval
                    'java-time.format
                    'java-time.clock
                    'java-time.pre-java8
                    'java-time
                    ])

(def test-nsyms ['java-time-test
                 'java-time.graph-test])

(some->> (seq (concat require-nsyms test-nsyms))
         (apply require))

(def test-results
  (apply t/run-tests test-nsyms))

(def failures-and-errors
  (let [{:keys [fail error]} test-results]
    (min 1 (+ fail error))))

(System/exit failures-and-errors)
