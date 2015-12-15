(defproject clojure.java-time "0.2.0-SNAPSHOT"
  :description "Idiomatic Clojure wrapper for Java 8 Time API"
  :url "http://github.com/dm3/clojure.java-time"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :scm {:name "git"
        :url "http://github.com/dm3/clojure.java-time"}
  :dependencies [[clj-tuple "0.2.2"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0"]
                                  [criterium "0.4.2"]
                                  [com.taoensso/timbre "4.1.4"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [joda-time/joda-time "2.9"]
                                  [org.threeten/threeten-extra "0.9"]]
                   :plugins [[lein-codox "0.9.0"]]
                   :codox {:namespaces [java-time]}
                   :source-paths ["dev"]
                   :global-vars {*warn-on-reflection* true}}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.7-three-ten-joda {:dependencies [[org.clojure/clojure "1.7.0"]
                                                 [org.threeten/threeten-extra "0.9"]
                                                 [joda-time/joda-time "2.9"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0-RC3"]]}
             :1.8-three-ten-joda {:dependencies [[org.clojure/clojure "1.8.0-RC3"]
                                                 [org.threeten/threeten-extra "0.9"]
                                                 [joda-time/joda-time "2.9"]]}}
  :aliases {"test-all" ["do"
                        ["with-profile" "1.7:1.8:1.7-three-ten-joda:1.8-three-ten-joda" "test"]]})
