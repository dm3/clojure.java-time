(ns java-time.graph-test
  (:require [java-time.graph :as sut]
            [clojure.test :as t :refer :all]))

(deftest types
  (testing "identical"
    (is (= (sut/types [Object]) (sut/types [Object])))
    (is (= (sut/types [Object String]) (sut/types [Object String]))))

  (testing "assignable"
    (testing "single arity"
      (is (sut/assignable? (sut/types [Object]) (sut/types [Object])))
      (is (sut/assignable? (sut/types [String]) (sut/types [Object])))
      (is (not (sut/assignable? (sut/types [Object]) (sut/types [String]))))
      (is (not (sut/assignable? (sut/types [String]) (sut/types [Number])))))

    (testing "multi arity"
      (is (sut/assignable? (sut/types [Object Number]) (sut/types [Object Object])))
      (is (not (sut/assignable? (sut/types [Object Number]) (sut/types [Object String]))))
      (is (not (sut/assignable? (sut/types [Object Object]) (sut/types [Object]))))
      (is (not (sut/assignable? (sut/types [Object]) (sut/types [Object Object])))))))

(defn mk-graph [& conversions]
  (loop [g (sut/conversion-graph)
         [[in out f cost] & conversions] conversions]
    (let [g (sut/assoc-conversion g (sut/types in) (sut/types out) f (or cost 1))]
      (if (seq conversions)
        (recur g conversions)
        g))))

(defn conversion-fn [g in out]
  (sut/conversion-fn g (sut/types in) (sut/types out)))

(deftest empty-graph
  (let [graph (sut/conversion-graph)
        t (sut/types [Object])]
    (is (empty? (sut/equivalent-targets graph t)))
    (is (empty? (sut/possible-conversions graph t)))))

(deftest single-arity-conversions
  (testing "implicit"
    (is (= [11] ((second (sut/conversion-fn (sut/conversion-graph)
                                            (sut/types [Number])
                                            (sut/types [Number]))) [11]))))

  (testing "success"
    (let [g (mk-graph [[String] [Number] (fn [v] [(Integer/parseInt (first v))])])]
      (is (= [11] ((second (conversion-fn g [String] [Number])) ["11"])))))

  (testing "failure"
    (let [g (mk-graph [[String] [Number] (fn [v] [(Integer/parseInt (first v))])])]
      (is (nil? (conversion-fn g [Object] [Number])))))

  (testing "chain"
    (let [g (mk-graph
              [[Number] [String] (comp vector str first)]
              [[String] [clojure.lang.Keyword] (comp vector keyword first)])]
      (is (= [:11] ((second (conversion-fn g [Number] [clojure.lang.Keyword])) [11]))))))

(deftest multi-arity-conversions
  (testing "implicit"
    (is (= [1 1] ((second (sut/conversion-fn (sut/conversion-graph)
                                             (sut/types [Number Number])
                                             (sut/types [Number Number]))) [1 1]))))

  (testing "simple"
    (let [g (mk-graph [[String String] [String] (fn [[a b]] [(str a "," b)])])]
      (is (= ["a,b"] ((second (conversion-fn g [String String] [String])) ["a" "b"])))))

  (testing "chain"
    (let [g (mk-graph
              [[Number Number] [String] (fn [[a b]] [(str (+ a b))])]
              [[String] [clojure.lang.Keyword] (comp vector keyword first)])]
      (is (= [:9] ((second (conversion-fn g [Number Number] [clojure.lang.Keyword])) [4 5])))))

  (testing "same arity"
    (let [g (mk-graph [[Number] [String] (comp vector str first)])]
      (is (= ["4" "5"] ((second (conversion-fn g [Number Number] [String String])) [4 5])))))

  (testing "same arity chain"
    (let [g (mk-graph [[Number] [String] (comp vector str first)]
                      [[String] [clojure.lang.Keyword] (comp vector keyword first)])]
      (is (= ["4" "x" :5] ((second (conversion-fn g [Number String Number] [String String clojure.lang.Keyword]))
                           [4 "x" 5])))))

  (testing "different arity"
    (let [g (mk-graph
              [[Number] [String] (fn [x] (vector (str "number-" (first x))))]
              [[String String] [clojure.lang.Keyword] (fn [[n v]] (vector (keyword n v)))])]
      (is (= [:number-4/hi] ((second (conversion-fn g [Number String] [clojure.lang.Keyword])) [4 "hi"]))))))

(deftest multi-arity-multi-step
  (testing "multiple steps"
    (with-redefs [sut/max-arity 4
                  sut/max-extent 3
                  sut/max-path-length 6]
      (let [g (mk-graph
                [[Number] [String] (fn [x] (vector (str "number-" (first x))))]
                [[String String] [clojure.lang.Keyword] (fn [[n v]] [(keyword n v)])]
                [[clojure.lang.Keyword clojure.lang.Keyword] [String] (fn [[k1 k2]] [(str k1 "!" k2)])])]
        (is (= [":number-4/hi!:number-5/ho"]
               ((second (conversion-fn g [Number String Number String] [String])) [4 "hi" 5 "ho"])))))))

(deftest non-empty-graph
  (let [g (mk-graph [[String] [Number] (fn [v] (Integer/parseInt v))]
                    [[String] [Long] (fn [v] (Long/parseLong v))]
                    [[clojure.lang.Keyword] [String] str])]
    (is (= #{(sut/types [Number]) (sut/types [Long])} (set (sut/equivalent-targets g (sut/types [Number])))))
    (is (= 2 (count (sut/possible-conversions g (sut/types [String])))))))
