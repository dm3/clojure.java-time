#!/usr/bin/env bb
(ns -release-readme+changelog
  "Update README.md and CHANGELOG.md with the current version in project.clj.
  
  Usage: ./bin/-release-readme+changelog.clj"
  (:require [clojure.string :as str]))

(defn get-project-version []
  {:post [(string? %)
          (seq %)]}
  (let [project-version-prefix "(defproject clojure.java-time \""
        project-version-suffix "\""]
    (some #(when (and (str/starts-with? % project-version-prefix)
                      (str/ends-with? % project-version-suffix))
             (subs % (count project-version-prefix) (- (count %) (count project-version-suffix))))
          (str/split-lines (slurp "project.clj")))))

(defn update-readme [project-version slurped-readme]
  (let [replace-version-around (volatile!
                                 #{{:prefix "clojure.java-time/clojure.java-time {:mvn/version \""
                                    :suffix "\"}"}
                                   {:prefix "[clojure.java-time \""
                                    :suffix "\"]"}})
        res (str/join "\n"
                      (map (fn [s]
                             (if-some [matching (first (filter #(and (str/starts-with? s (:prefix %))
                                                                     (str/ends-with? s (:suffix %)))
                                                               @replace-version-around))]
                               (do (vswap! replace-version-around disj matching)
                                   (str (:prefix matching) project-version (:suffix matching)))
                               s))
                           (str/split-lines slurped-readme)))]
    (assert (empty? @replace-version-around)
            (str "Failed to replace: " @replace-version-around))
    res))

(assert (= "# Readme\n\n## Dependency\n\nclojure.java-time/clojure.java-time {:mvn/version \"1.0.0\"}\n\n[clojure.java-time \"1.0.0\"]"
           (update-readme "1.0.0"
                          "# Readme\n\n## Dependency\n\nclojure.java-time/clojure.java-time {:mvn/version \"0.5.0\"}\n\n[clojure.java-time \"0.5.0\"]")))

(defn update-changelog [project-version slurped-changelog]
  (str/replace slurped-changelog "## NEXT\n" (format "## NEXT\n\n## %s\n" project-version)))

(assert (= "# Changelog\n\n## NEXT\n\n## 1.0.0\n\n- a release note"
           (update-changelog "1.0.0" "# Changelog\n\n## NEXT\n\n- a release note")))

(defn -main []
  (let [project-version (get-project-version)]
    (spit "README.md" (update-readme project-version (slurp "README.md")))
    (spit "CHANGELOG.md" (update-changelog project-version (slurp "CHANGELOG.md")))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
