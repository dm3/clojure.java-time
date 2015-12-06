(ns java-time.seqs
  (:refer-clojure :exclude [iterate]))

(defn- partialr [f & args]
  (fn [a & as]
    (apply f a (concat as args))))

(defn iterate
  "Returns a lazy sequence of `initial` , `(apply f initial v vs)`, etc.

  Useful when you want to produce a sequence of temporal entities, for
  example:

    (iterate plus (days 0) 1)
    => (#<Period P0D> #<Period P1D> #<Period P2D> ...)

    (iterate plus (local-date 2010 1 1) (years 1))
    => (#<LocalDate 2010-01-01> #<LocalDate 2011-01-01> ...)

    (iterate adjust (local-date 2010 1 1) :next-working-day)
    => (#<LocalDate 2010-01-01> #<LocalDate 2010-01-04> ...)"
  [f initial v & vs]
  (clojure.core/iterate
    (apply partialr f v vs) initial))
