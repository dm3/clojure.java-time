(defproject test/no-deps "0.1.0-SNAPSHOT"
  :description "Clojure.Java-Time with no external dependencies"
  :dependencies [[clojure.java-time "0.1.0-SNAPSHOT" :exclusions [org.threeten/threeten-extra]]
                 [org.clojure/clojure "1.7.0"]])
