(defproject clojure.java-time "0.3.4-SNAPSHOT"
  :description "Clojure wrapper for Java 8 Time API"
  :url "http://github.com/dm3/clojure.java-time"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :scm {:name "git"
        :url "http://github.com/dm3/clojure.java-time"}
  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]]
  :profiles {:dev {:dependencies [[criterium "0.4.4"]
                                  [com.taoensso/timbre "4.1.4"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [joda-time/joda-time "2.9.4"]
                                  [org.threeten/threeten-extra "1.2"]]
                   :plugins [[lein-codox "0.10.3"]]
                   :codox {:namespaces [java-time java-time.repl]
                           :doc-files ["README.md" "CHANGELOG.md"]}
                   :source-paths ["dev"]
                   :global-vars {*warn-on-reflection* true}}
             :async-profiler
             {:jvm-opts ["-Djdk.attach.allowAttachSelf" "-XX:+UnlockDiagnosticVMOptions" "-XX:+DebugNonSafepoints"]
              :dependencies [[com.clojure-goes-fast/clj-async-profiler "0.3.1"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.8-three-ten-joda {:dependencies [[org.clojure/clojure "1.8.0"]
                                                 [org.threeten/threeten-extra "1.4"]
                                                 [joda-time/joda-time "2.10.1"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.9-three-ten-joda {:dependencies [[org.clojure/clojure "1.9.0"]
                                                 [org.threeten/threeten-extra "1.4"]
                                                 [joda-time/joda-time "2.10.1"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.0"]]}
             :1.10-three-ten-joda {:dependencies [[org.clojure/clojure "1.10.0"]
                                                  [org.threeten/threeten-extra "1.4"]
                                                  [joda-time/joda-time "2.10.1"]]}}
  :aliases {"test-all" ["do"
                        ["with-profile" "1.8:1.9:1.10:1.8-three-ten-joda:1.9-three-ten-joda:1.10-three-ten-joda" "test"]]})
