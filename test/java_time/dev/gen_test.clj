(ns java-time.dev.gen-test
  (:require [clojure.test :refer [deftest is]]
            [java-time.util :as jt.u]))

(jt.u/when-threeten-extra
  (require 'java-time.dev.gen)
  (deftest gen-test
    (doseq [[source nsym] (doto @(resolve 'java-time.dev.gen/gen-source->nsym)
                            (-> not-empty assert))]
      (let [actual (slurp source)
            expected (with-out-str ((resolve 'java-time.dev.gen/print-java-time-ns)
                                    nsym))
            up-to-date? (= actual expected)]
        (is (not up-to-date?)
            "Please run `lein doc` and commit the changes")))))
