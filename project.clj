(defproject clojure.java-time "0.1.0-SNAPSHOT"
  :description "Idiomatic Clojure wrapper for Java 8 Time API"
  :url "http://github.com/dm3/clojure.java-time"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :scm {:name "git"
        :url "http://github.com/dm3/clojure.java-time"}
  :dependencies [[org.threeten/threeten-extra "0.9"]
                 [clj-tuple "0.2.2"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0"]
                                  [org.clojure/test.check "0.5.8"]
                                  [criterium "0.4.2"]
                                  [com.taoensso/timbre "4.1.4"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [joda-time/joda-time "2.9"]]
                   :plugins [[codox "0.8.13"]]
                   :codox {:include [java-time]}
                   :source-paths ["dev"]
                   :global-vars {*warn-on-reflection* true}}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0-RC1"]]}}
  :aliases {"test-all" ["with-profile" "dev,default:dev,1.6,default:dev,1.8,default" "test"]}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :sign-releases false}]])
