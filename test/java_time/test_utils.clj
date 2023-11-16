(ns java-time.test-utils
  (:refer-clojure :exclude [boolean?])
  (:require [clojure.math.combinatorics :as comb]
            [clojure.test :refer [deftest is]]))

(def boolean? #(or (true? %) (false? %)))

(defn ^:private validate-_is-args [msg]
  (assert (string? msg) (str "is: message must be a string: " (pr-str msg))))

(defn ^:dynamic _is [f args msg]
  (validate-_is-args msg)
  (is (apply f args) msg))

(defn ^:private reorder-vector [v order]
  (assert ((every-pred vector?) v order))
  (assert (= (count v) (count order)))
  (assert (next order))
  (assert (apply distinct? order))
  (reduce (fn [v' [from to]]
            (assoc v' to (nth v from)))
          v (map vector (range (count v)) order)))

(deftest ^:private reorder-vector-test
  (is (= '[a b c d]
         (reorder-vector '[a b c d] [0 1 2 3])))
  (is (= '[a b d c]
         (reorder-vector '[a b c d] [0 1 3 2])))
  (is (= '[d c b a]
         (reorder-vector '[a b c d] [3 2 1 0]))))

; aRb => !bRa
; aRb && bRc => !bRa && !cRb && !cRa && aRc
(defn is-asymmetric* [prop R args R-syn args-syn]
  {:pre [((every-pred vector?) args args-syn)
         (#{:asymmetric :antisymmetric} prop)]
   :post [(boolean? %)]}
  (let [nargs (count args)
        _ (assert (= nargs (count args-syn)))
        _ (assert (<= 2 nargs) "Must provide at least 2 arguments")
        split-args+syn (fn [args+syn]
                         [(mapv first args+syn)
                          (mapv second args+syn)])
        args+syn (mapv vector args args-syn)
        continue (volatile! true)
        _ (doseq [;; for each combination of 2 or more args
                  nargs (range 2 (inc nargs))
                  :while @continue
                  args+syn (map vec (comb/combinations args+syn nargs))
                  :let [[args args-syn] (split-args+syn args+syn)]
                  ;; (R args...) is true
                  :while (or (_is R args (pr-str (list* R-syn args-syn)))
                             (vreset! continue false))
                  :let [original-order (range nargs)]
                  order (comb/permutations original-order)
                  :while @continue
                  :when (not= order original-order)
                  :let [[args args-syn] (split-args+syn (reorder-vector args+syn order))]
                  :while (or (case prop
                               ;; and (R args..) is false for every *other* permutation of args
                               :asymmetric (_is (complement R) args (pr-str (list 'not (list* R-syn args-syn))))
                               ;; and (R args..) is false for every *other* permutation of args, or one of the
                               ;; out-of-order arguments is equal
                               :antisymmetric (let [out-of-order-args (loop [out []
                                                                             order order]
                                                                        (if (next order)
                                                                          (let [[l r] order]
                                                                            (recur (cond-> out
                                                                                     (> l r) (conj [l r]))
                                                                                   (next order)))
                                                                          out))
                                                    _ (assert (seq out-of-order-args))
                                                    f (fn [& args]
                                                        (let [args (vec args)]
                                                          (if (apply R args)
                                                            (every? (fn [[l r]]
                                                                      (= (nth args l)
                                                                         (nth args r)))
                                                                    out-of-order-args)
                                                            true)))
                                                    conjunction-syn (mapv (fn [[l r]]
                                                                            (list '= (nth args-syn l) (nth args-syn r)))
                                                                          out-of-order-args)]
                                                (_is f args (pr-str (list 'or (list 'not (list* R-syn args-syn))
                                                                          (if (= 1 (count conjunction-syn))
                                                                            (first conjunction-syn)
                                                                            (list* 'and conjunction-syn)))))))
                             (vreset! continue false))])]
    @continue))

(defmacro is-asymmetric
  "With two arguments, tests that (R a b) is true and (R b a) is false.
  With three arguments (R a b c):
  1. tests the previous property for (R a b), (R b c), and (R a c)
  2. tests that (R a b c) is true.
  3. tests that these are false: (R a c b), (R b a c), (R b c a), (R c a b), (R c b a).
  
  Similar for four or more arguments."
  [[R a b & args :as all]]
  (assert (seq? all))
  (assert (<= 3 (count all))
          (str "Must provide 2 or more arguments: " (pr-str all)))
  `(is-asymmetric* :asymmetric
                   ~R (into [~a ~b] ~(vec args))
                   '~R (into '~[a b] '~(vec args))))

; aRb && bRa => a=b
(defmacro is-antisymmetric
  "With two arguments (R a b), tests that:
  1. (R a b) is true
  2. if (R b a) then a=b
  With three arguments (R a b c):
  1. tests the previous property for (R a b), (R b c), and (R a c)
  2. tests that (R a b c) is true.
  3. tests that
     - if (R a c b) then b=c
     - if (R b a c) then a=b
     - if (R b c a) then c=a
     - if (R c a b) then c=a
     - if (R c b a) then a=b and b=c
  
  Similar for four or more arguments."
  [[R a b & args :as all]]
  (assert (seq? all))
  (assert (<= 3 (count all))
          (str "Must provide 2 or more arguments: " (pr-str all)))
  `(is-asymmetric* :antisymmetric
                   ~R (into [~a ~b] ~(vec args))
                   '~R (into '~[a b] '~(vec args))))

(defn ^:private with-expected-results*
  [expected-results f]
  (assert (even? (count expected-results)))
  (let [actual-results (atom [])
        _ (binding [_is (fn [f args msg]
                          (validate-_is-args msg)
                          (let [res (apply f args)]
                            (swap! actual-results conj (read-string msg) (if res :pass :fail))
                            res))]
            (f))
        actual-results @actual-results]
    (or (= expected-results actual-results)
        (throw (ex-info (str "Expected result: "
                             (pr-str expected-results)
                             "\n Actual result: "
                             (pr-str actual-results))
                        {:expected-result expected-results
                         :actual-results actual-results})))))

(defmacro ^:private with-expected-results
  [results & body]
  `(with-expected-results* '~results #(do ~@body)))

(defn ^:private is-ex-data* [expected-ex-data f]
  (try (f)
       (is false (str "No error thrown"))
       (catch Exception e
         (is (= expected-ex-data (ex-data e))))))

(defmacro ^:private is-ex-data [expected-ex-data body]
  `(is-ex-data* ~expected-ex-data #(do ~body)))

(deftest ^:private _is-test
  (is (with-expected-results [(< 2 1) :pass]
        (_is (constantly true) [] "(< 2 1)")))
  (is-ex-data
    '{:expected-result [(< 2 1) :fail]
      :actual-results  [(< 2 1) :pass]}
    (with-expected-results [(< 2 1) :fail]
      (_is (constantly true) [] "(< 2 1)")))
  (is-ex-data
    '{:expected-result [(< 2 1) :pass]
      :actual-results  [(< 2 1) :pass
                        (< 2 1) :pass]}
    (with-expected-results [(< 2 1) :pass]
      (_is (constantly true) [] "(< 2 1)")
      (_is (constantly true) [] "(< 2 1)"))))

(deftest ^:private is-asymmetric-test
  (is (with-expected-results [(< 1 2) :pass
                              (not (< 2 1)) :pass]
        (is-asymmetric (< 1 2))))
  (let [a 1
        b 2]
    (is (with-expected-results [(< a b) :pass
                                (not (< b a)) :pass]
          (is-asymmetric (< a b)))))
  (is (with-expected-results [(> 1 2) :fail]
        (is-asymmetric (> 1 2))))
  (is (with-expected-results [(<= 1 1) :pass
                              (not (<= 1 1)) :fail]
        (is-asymmetric (<= 1 1))))
  (is (with-expected-results [(<= 1 1) :pass
                              (not (<= 1 1)) :fail]
        (is-asymmetric (<= 1 1 1 1 1 1))))
  (is (with-expected-results 
        [(< 1 2) :pass
         (not (< 2 1)) :pass
         (< 1 3) :pass
         (not (< 3 1)) :pass
         (< 2 3) :pass
         (not (< 3 2)) :pass
         (< 1 2 3) :pass
         (not (< 1 3 2)) :pass
         (not (< 2 1 3)) :pass
         (not (< 3 1 2)) :pass
         (not (< 2 3 1)) :pass
         (not (< 3 2 1)) :pass]
        (is-asymmetric (< 1 2 3)))))

(deftest ^:private is-antisymmetric-test
  (is (with-expected-results [(<= 1 1) :pass
                              (or (not (<= 1 1)) (= 1 1)) :pass]
        (is-antisymmetric (<= 1 1))))
  (let [a 1
        b 1]
    (is (with-expected-results [(<= a b) :pass
                                (or (not (<= b a)) (= a b)) :pass]
          (is-antisymmetric (<= a b)))))
  (is (with-expected-results [(<= 1 2) :pass
                              (or (not (<= 2 1)) (= 1 2)) :pass]
        (is-antisymmetric (<= 1 2))))
  (is (with-expected-results [(> 1 2) :fail]
        (is-antisymmetric (> 1 2))))
  (is (with-expected-results [(> 2 1) :pass
                              (or (not (> 1 2)) (= 2 1)) :pass]
        (is-antisymmetric (> 2 1))))
  (is (with-expected-results
        [(<= 1 2) :pass
         (or (not (<= 2 1)) (= 1 2)) :pass
         (<= 1 3) :pass
         (or (not (<= 3 1)) (= 1 3)) :pass
         (<= 2 3) :pass
         (or (not (<= 3 2)) (= 2 3)) :pass
         (<= 1 2 3) :pass
         (or (not (<= 1 3 2)) (= 2 3)) :pass
         (or (not (<= 2 1 3)) (= 1 2)) :pass
         (or (not (<= 3 1 2)) (= 2 3)) :pass
         (or (not (<= 2 3 1)) (= 1 2)) :pass
         (or (not (<= 3 2 1)) (and (= 1 2) (= 2 3))) :pass]
        (is-antisymmetric (<= 1 2 3)))))
