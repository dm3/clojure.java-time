; The MIT License  (MIT)
;
; Copyright (c) 2013 Zachary Tellman
;
; Permission is hereby granted, free of charge, to any person obtaining a copy
; of this software and associated documentation files (the "Software"), to deal
; in the Software without restriction, including without limitation the rights
; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
; copies of the Software, and to permit persons to whom the Software is
; furnished to do so, subject to the following conditions:
;
; The above copyright notice and this permission notice shall be included in
; all copies or substantial portions of the Software.)
;
; Partly Copied from https://github.com/ztellman/potemkin/blob/master/src/potemkin/util.clj
; to avoid having a dependency
(ns java-time.potemkin.util
  #?@(:bb []
      :default [(:import [java.util.concurrent ConcurrentHashMap])]))
;;; fast-memoize

#?(:bb nil :default (do
(definline re-nil [x]
  `(let [x# ~x]
     (if (identical? ::nil x#) nil x#)))

(definline de-nil [x]
  `(let [x# ~x]
     (if (nil? x#) ::nil x#)))

(defmacro memoize-form [m f & args]
  `(let [k# (vector ~@args)]
     (let [v# (.get ~m k#)]
       (if-not (nil? v#)
         (re-nil v#)
         (let [v# (de-nil (~f ~@args))]
           (re-nil (or (.putIfAbsent ~m k# v#) v#))))))))
)

(defn fast-memoize
  "A version of `memoize` which has equivalent behavior, but is faster."
  [f]
  #?(:bb (memoize f)
     :default (let [m (ConcurrentHashMap.)]
                (fn
                  ([]
                   (memoize-form m f))
                  ([x]
                   (memoize-form m f x))
                  ([x y]
                   (memoize-form m f x y))
                  ([x y z]
                   (memoize-form m f x y z))
                  ([x y z w]
                   (memoize-form m f x y z w))
                  ([x y z w u]
                   (memoize-form m f x y z w u))
                  ([x y z w u v]
                   (memoize-form m f x y z w u v))
                  ([x y z w u v & rest]
                   (let [k (list* x y z w u v rest)]
                     (let [v (.get ^ConcurrentHashMap m k)]
                       (if-not (nil? v)
                         (re-nil v)
                         (let [v (de-nil (apply f k))]
                           (or (.putIfAbsent m k v) v))))))))))
