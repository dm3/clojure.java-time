(ns java-time.dev.gen-test
  (:require [clojure.test :refer [deftest is]]
            [java-time.util :as jt.u]))

(jt.u/when-threeten-extra
  (require 'java-time.dev.gen)
  (deftest gen-test
    (doseq [[source nsym] (doto @(resolve 'java-time.dev.gen/gen-source->nsym)
                            (-> not-empty assert))]
      (is (= (slurp source)
             (with-out-str ((resolve 'java-time.dev.gen/print-java-time-ns)
                            nsym)))
          (format "%s namespace is out of date -- call (java-time.dev.gen/spit-java-time-ns) or `$ lein doc`"
                  nsym)))))
