(ns java-time.graph
  (:require [clojure.set :as sets]
            [clojure.string :as string]
            [java-time.potemkin.util :as u]
            [java-time.util :as jt.u])
  #?@(:bb []
      :default [(:import [java.util PriorityQueue])]))

;; Concept heavily inspired by Zach Tellman's ByteStreams
;; https://github.com/ztellman/byte-streams/blob/master/src/byte_streams/graph.clj

(deftype Conversion [f ^double cost]
  Object
  #?@(:bb [] :default [
  (equals [_ x]
    (and
      (instance? Conversion x)
      (identical? f (.f ^Conversion x))
      (== cost (.cost ^Conversion x))))
  (hashCode [_]
    (bit-xor (System/identityHashCode f) (unchecked-int cost)))
  ])
  (toString [_]
    (str "Cost:" cost)))

(deftype Types [types ^int arity]
  Object
  #?@(:bb [] :default [
  (equals [_ o]
    (and (instance? Types o)
         (and (= arity (.arity ^Types o))
              (loop [idx 0]
                (if (= (nth types idx) (nth (.types ^Types o) idx))
                  (if (> arity (inc idx))
                    (recur (inc idx))
                    true)
                  false)))))
  (hashCode [o]
    (bit-xor (hash types) arity))
  ])
  (toString [_]
    (pr-str types)))

(defn arity [^Types t]
  (.arity t))

(defn types->str [^Types t]
  (.toString t))

(def max-arity 3)
(def max-cost 8)
(def max-path-length 4)
(def max-extent 2)

(defn types [ts]
  (let [cnt (count ts)]
    (when (> cnt max-arity)
      (throw (ex-info (format "Maximum arity supported by conversion graph is %s!" max-arity)
                      {:types ts})))
    (Types. (vec ts) cnt)))

(defn- assignable-type? [a b]
  (or (= a b) (.isAssignableFrom ^Class b a)))

(def assignable?
  ^{:doc "True if `a` is assignable to `b`, e.g. Integer is assignable to Number."}
  (u/fast-memoize
    (fn [^Types a ^Types b]
      (or (= a b)
          (and (= (.arity a) (.arity b))
               (let [ta (.types a), tb (.types b)]
                 (loop [idx 0]
                   (when (assignable-type? (nth ta idx) (nth tb idx))
                     (if (> (.arity a) (inc idx))
                       (recur (inc idx))
                       true)))))))))

(defprotocol IConversionGraph
  (get-conversion [_ src dst])
  (assoc-conversion [_ src dst f cost])
  (equivalent-targets [_ dst])
  (possible-sources [_])
  (possible-conversions [_ src]))

(defn- expand [[a b]]
  (when-not (empty? b)
    (cons
      [(conj a (first b)) (rest b)]
      (expand [a (rest b)]))))

(defn- combinations [n s]
  (letfn [(combos [n s]
            (if (zero? n)
              (list (vector (vector) s))
              (mapcat expand (combos (dec n) s))))]
    (map first (combos n s))))

(def continuous-combinations
  (u/fast-memoize
    (fn [n]
      (let [rng (range n)]
        (->> (map inc rng)
             (map #(combinations % rng))
             (apply concat)
             (filterv #(apply = 1 (map - (rest %) %))))))))

(defn- as-source [types-so-far t [dst c]]
  (vector
    (vector (types (conj types-so-far t)) dst)
    c))

(defn- search-for-possible-sources
  [vresult m types-so-far k more-arity-steps]
  (u/doit [[t r] m]
    (when (assignable-type? k t)
      (if-not more-arity-steps
        (vswap! vresult concat (mapv #(as-source types-so-far t %) r))
        (search-for-possible-sources vresult r
                                     (conj types-so-far t)
                                     (first more-arity-steps)
                                     (next more-arity-steps))))))

(defn- collect-targets [v]
  (reduce
    (fn [r [k v]]
      (if (map? v)
        (concat r (collect-targets v))
        (concat r v)))
    (vector) v))

(defn- add-conversion [m ^Types src dst conversion]
  (let [add #(update % (peek (.types src))
                     (fnil conj (vector))
                     (vector dst conversion))]
    (if (> (.arity src) 1)
      (update-in m (pop (.types src)) add)
      (add m))))

(deftype ConversionGraph [m-by-arity srcs]
  IConversionGraph
  (get-conversion [_ src dst]
    (let [m (get m-by-arity (.arity ^Types src))]
      (->> (get-in m (.types ^Types src))
           (some #(= dst (first %))))))
  (assoc-conversion [_ src dst f cost]
    (ConversionGraph.
      (update m-by-arity (.arity ^Types src)
              add-conversion src dst (Conversion. f cost))
      (conj srcs src)))
  (possible-sources [_] srcs)
  (equivalent-targets [_ dst]
    (->> (vals m-by-arity)
         (mapcat collect-targets)
         (map first)
         (filter #(assignable? % dst))
         (set)))
  (possible-conversions [_ src]
    (let [result (volatile! (vector))]
      (search-for-possible-sources
        result
        (get m-by-arity (.arity ^Types src))
        (vector)
        (first (.types ^Types src))
        (next (.types ^Types src)))
      @result)))

(defn conversion-graph []
  (ConversionGraph.
    (zipmap (map inc (range max-arity)) (repeat {})) #{}))

(defrecord ConversionPath [path fns visited? cost]
  #?@(:bb [] :default [
  Comparable
  (compareTo [_ x]
    (let [cmp (compare cost (.cost ^ConversionPath x))]
      (if (zero? cmp)
        (compare (count path) (count (.path ^ConversionPath x)))
        cmp)))
  ])
  Object
  (toString [_]
    (str path cost)))

(defn- conj-path [^ConversionPath p src dst ^Conversion c]
  (ConversionPath.
    (conj (.path p) (vector src dst))
    (conj (.fns p) (.f c))
    (conj (.visited? p) dst)
    (+ (.cost p) (.cost c))))

(def graph-conversion-path
  (fn [g src dst]
    (let [path (ConversionPath. (vector) (vector) #{src} 0)]
      (if (assignable? src dst)
        path
        (let [q #?(:bb (atom ())
                   :default (doto (PriorityQueue.) (.add path)))
              add #?(:bb #(swap! q (fn [prev]
                                     (sort-by (fn [^ConversionPath p]
                                                [(.cost p) (count (.path p))])
                                              #(compare %2 %1)
                                              (conj prev %))))
                     :default #(.add q %))
              poll #?(:bb #(-> (swap-vals! q next) first first)
                      :default #(.poll q))
              dsts (equivalent-targets g dst)]
          (loop []
            (when-let [^ConversionPath p (poll)]
              (let [curr (or (-> p .path last second) src)]
                (if (some #(assignable? curr %) dsts)
                  p
                  (do
                    (u/doit [[[src dst] c] (possible-conversions g curr)]
                      (when (and (> max-path-length (count (.path p)))
                                 (not ((.visited? p) dst)))
                        (add (conj-path p src dst c))))
                    (recur)))))))))))

(defn- replace-range [v replacement idxs]
  (concat (subvec v 0 (first idxs))
          replacement
          (subvec v (inc (last idxs)) (count v))))

(defn- index-conversions [^Types src idxs [[_ ^Types replacement] ^Conversion conv]]
  (vector src (types (replace-range (.types src) (.types replacement) idxs))
          (fn [vs]
            (let [vs (vec vs)]
              (replace-range vs
                             ((.f conv) (subvec vs (first idxs) (inc (last idxs))))
                             idxs)))
          (.cost conv)))

(defn- sub-conversions
  "Given an `src` types, generate all of the conversions from these types that
  are possible to achieve in one step in the provided conversion graph `g`.

  For example:

    g = [[String -> Integer] [Integer -> Keyword] [[String Integer] -> String]
    src = [String Integer]

    result = [[src -> [String  Keyword]]
              [src -> [Integer Integer]]
              [src -> [Integer Keyword]]
              [src -> String]"
  [g ^Types src]
  (if (> (.arity src) max-arity)
    (vector)
    (->> (continuous-combinations (.arity src))
         (mapcat
           (fn [idxs]
             (let [^Types input (types (subvec (.types src) (first idxs) (inc (last idxs))))]
               (->> (possible-conversions g input)
                    (filter (fn [[[_ ^Types replacement] _]]
                              (>= max-arity (+ (.arity replacement)
                                               (- (.arity src) (.arity input))))))
                    (map #(index-conversions src idxs %)))))))))

(defn- with-conversions [g convs]
  (loop [g g, new-conversions (vector)
         [src dst f cost :as con] (first convs)
         convs (next convs)]
    (if con
      (if (get-conversion g src dst)
        (recur g new-conversions
               (first convs) (next convs))
        (recur (assoc-conversion g src dst f cost)
               (conj new-conversions con)
               (first convs) (next convs)))
      [new-conversions g])))

;; Heuristic:
;; we want to skip the branches that contain the destination types as their part.
;; In our conversion world it's very unlikely that a value will be reduced to
;; the value of the same type.
(def contains-types?
  "True if `a` contains `b` as its part."
  (fn [^Types a, ^Types b]
    (and (not= (.arity a) (.arity b))
         (let [ta (.types a), tb (.types b)]
           (loop [idx 0]
             (when (>= (.arity a) (+ idx (.arity b)))
               (if (= tb (subvec ta idx (+ idx (.arity b))))
                 true
                 (recur (inc idx)))))))))

;; if a graph's sources do not contain all of the types present in the
;; requested source and the destination doesn't contain them either we conclude
;; that it's impossible to convert the source to the destination.
(defn- has-source-type? [g ^Types src, ^Types dst]
  (let [src-types (map (comp types vector) (.types src))
        contains-src-types? (fn [s] (some #(or (assignable? % s)
                                               (contains-types? s %)) src-types))]
    (or (contains-src-types? dst)
        (->> (possible-sources g)
             (some contains-src-types?)))))

(defn- expand-frontier [g ^Types src max-extent]
  (loop [g g, q (-> (clojure.lang.PersistentQueue/EMPTY) (conj [src 0]))]
    (if-let [[next-src step] (peek q)]
      (if (> step max-extent)
        g
        (let [more-conversions (sub-conversions g next-src)
              [new-conversions g'] (with-conversions g more-conversions)
              accepted-conversions (filter (fn [[conv-src _ _ cost]]
                                             (>= max-cost cost)) new-conversions)]
          (recur g' (reduce (fn [q [_ dst _ _]] (conj q (vector dst (inc step))))
                            (pop q) accepted-conversions))))
      g)))

(def conversion-path
  (u/fast-memoize
    (fn [^ConversionGraph g, ^Types src, ^Types dst]
      (when (has-source-type? g src dst)
        (let [g' (expand-frontier g src max-extent)]
          (graph-conversion-path g' src dst))))))

(defn- convert-via [path]
  (condp = (count (:path path))
    0 (vector path (fn [x] x))
    1 (vector path (->> path :fns first))
    (let [fns (->> path :fns (apply vector))]
      (vector path (fn [v] (reduce (fn [v f] (f v)) v fns))))))

(defn conversion-fn
  "Create a function which will convert between the `src` and the `dst`
  `Types`."
  [g src dst]
  (when-let [path (conversion-path g src dst)]
    (convert-via path)))
