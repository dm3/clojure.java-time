(ns java-time.defconversion
  (:require [java-time.graph :as g]))

(def graph (atom (g/conversion-graph)))

(defn- check-arity [t vs]
  (when-not (= (g/arity t) (count vs))
    (throw (ex-info (format "Arity of %s doesn't match the values %s!" t vs)
                    {:types t, :values vs})))
  vs)

(defn- to-seq [in]
  (cond-> in
    (not (sequential? in)) vector))

(defn- wrap-validation [from to f]
  (fn [vs]
    (let [result (apply f (check-arity from vs))]
      (check-arity to (to-seq result)))))

(defn- combinations [xs f cost]
  (let [idxs (g/continuous-combinations (count xs))]
    (for [combo idxs]
      (vector (fn [& vs]
                (let [res (to-seq (apply f vs))]
                  (subvec res (first combo) (inc (last combo)))))
              (g/types (subvec xs (first combo) (inc (last combo))))
              (cond-> cost
                ;; TODO: mark as lossy conversion
                ;; currently we just incur a 0.5*number of types dropped penalty
                (not= (count idxs) (count xs))
                (+ (* 0.5 (- (count xs) (count combo)))))))))

(def ^:dynamic *fail-on-duplicate-conversion?* true)

(defn conversion!
  ([from to f] (conversion! from to f 1))
  ([from-type-vec to-type-vec f cost]
   (let [from (g/types (to-seq from-type-vec))
         tos (combinations (to-seq to-type-vec) f cost)]
     (doseq [[f to cost] tos]
       (swap! graph
              (fn [g]
                (if-let [existing (g/get-conversion g from to)]
                  (if *fail-on-duplicate-conversion?*
                    (throw (ex-info (format "Conversion %s -> %s already exists: %s!" from to existing)
                                    {:from from, :to to, :existing existing}))
                    g)
                  (let [f (wrap-validation from to f)]
                    (g/assoc-conversion g from to f cost)))))))))

(defn types-of [xs]
  (g/types (map type xs)))

(defn ^:internal call-conversion [nm tp args]
  (if-let [[path fn] (g/conversion-fn
                       @graph
                       (types-of args)
                       (g/types [tp]))]
    (or (try (first (fn args))
             (catch Exception e
               (throw
                 (ex-info "Conversion failed"
                          {:path (:path path), :arguments args, :to tp}
                          e))))
        (throw (ex-info
                 (format "Conversion from %s to %s returned nil!" args tp)
                 {:arguments args, :to tp, :constructor nm})))
    (throw (ex-info (format "Could not convert %s to %s!" args tp)
                    {:arguments args, :to tp, :constructor nm}))))

(defn- gen-implicit-arities [nm tp arities]
  (for [arity arities]
    (let [args (mapv #(gensym (str "arg_" (inc %) "_")) (range arity))]
      `([~@args]
        (call-conversion ~nm ~tp ~args)))))

(defn get-path [from to]
  (let [[p _] (g/conversion-fn @graph
                (g/types (to-seq from))
                (g/types (to-seq to)))]
    (select-keys p [:path :cost])))

(defmacro deffactory [nm docstring returnskw tp implicit-arities-kw implicit-arities & fn-bodies]
  (assert (= :returns returnskw))
  (assert (= :implicit-arities implicit-arities-kw))
  (let [^Class tpcls (resolve tp)
        _ (assert (class? tpcls) (str tp " is not resolvable"))
        tp (-> ^Class tpcls .getName symbol)
        fn-name (with-meta nm {:tag tp})
        explain-fn-name (symbol (str "path-to-" nm))
        predicate-name (symbol (str nm "?"))]
    `(do (defn ~fn-name ~docstring
           ~@(concat
               fn-bodies
               (gen-implicit-arities nm tp implicit-arities)))

         (defn ~predicate-name
           ~(str "True if an instance of " tp ".")
           [v#]
           (instance? ~tp v#)))))
