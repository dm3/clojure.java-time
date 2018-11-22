(defproject clojure.java-time "0.3.3-SNAPSHOT"
  :description "Clojure wrapper for Java 8 Time API"
  :url "http://github.com/dm3/clojure.java-time"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :scm {:name "git"
        :url "http://github.com/dm3/clojure.java-time"}
  :dependencies [[clj-tuple "0.2.2"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [criterium "0.4.4"]
                                  [com.taoensso/timbre "4.1.4"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [joda-time/joda-time "2.9.4"]
                                  [org.clojure/java.jdbc "0.7.8"]
                                  [org.threeten/threeten-extra "1.2"]]
                   :plugins [[lein-codox "0.10.3"]]
                   :codox {:namespaces [java-time java-time.repl]
                           :doc-files ["README.md" "CHANGELOG.md"]}
                   :source-paths ["dev"]
                   :global-vars {*warn-on-reflection* true}}
             :1.7 {:dependencies [[org.clojure/java.jdbc "0.7.8"]
                                  [org.clojure/clojure "1.7.0"]]}
             :1.7-three-ten-joda {:dependencies [[org.clojure/java.jdbc "0.7.8"]
                                                 [org.clojure/clojure "1.7.0"]
                                                 [org.threeten/threeten-extra "1.2"]
                                                 [joda-time/joda-time "2.9.9"]]}
             :1.9 {:dependencies [[org.clojure/java.jdbc "0.7.8"]
                                  [org.clojure/clojure "1.9.0-alpha17"]]}
             :1.9-three-ten-joda {:dependencies [[org.clojure/java.jdbc "0.7.8"]
                                                 [org.clojure/clojure "1.9.0-RC2"]
                                                 [org.threeten/threeten-extra "1.2"]
                                                 [joda-time/joda-time "2.9.9"]]}}
  :aliases {"test-all" ["do"
                        ["with-profile" "1.7:1.9:1.7-three-ten-joda:1.9-three-ten-joda" "test"]]})
