#!/usr/bin/env bb
(ns sync-readme-version
  (:require [clojure.string :as str]))

(def project-version-prefix "(defproject clojure.java-time \"")
(def project-version-suffix "\"")
(def project-version (some #(when (and (str/starts-with? % project-version-prefix)
                                       (str/ends-with? % project-version-suffix))
                              (subs % (count project-version-prefix) (- (count %) (count project-version-suffix))))
                           (str/split-lines (slurp "project.clj"))))
(assert project-version "Could not determine project version")
(def replace-version-around
  (volatile!
    #{{:prefix "clojure.java-time/clojure.java-time {:mvn/version \""
       :suffix "\"}"}
      {:prefix "[clojure.java-time \""
       :suffix "\"]"}}))
(spit "README.md"
      (str/join "\n"
                (map (fn [s]
                       (if-some [matching (first (filter #(and (str/starts-with? s (:prefix %))
                                                               (str/ends-with? s (:suffix %)))
                                                         @replace-version-around))]
                         (do (vswap! replace-version-around disj matching)
                             (str (:prefix matching) project-version (:suffix matching)))
                         s))
                     (str/split-lines (slurp "README.md")))))
(assert (empty? @replace-version-around)
        (str "Failed to replace: " @replace-version-around))
