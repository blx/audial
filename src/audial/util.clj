(ns audial.util
  (:require [clojure.string :as str]
            [clojure.core.reducers :as r]))

(def solitary?
  (comp empty? rest))

(defn keywordize [s]
  (-> (str s)
      str/lower-case
      (str/replace #"\s+" "-")
      keyword))

(defn dekeywordize [kw]
  (-> (name kw)
      (str/replace "-" "_")))

(defn pfilterv
  "Uses clojure.core.reducers for better performance on big colls or intensive preds.
  Gently avoid using on small colls as clojure.core/filter tends to be faster
  until a certain combination of pred complexity and coll size. Depending on
  your hardware this crossover point may be well into the millions of elements.
  
  Actually the sensible thing (#TODO) would just be to dump everything into an actual
  database engine and carry on with life. Right tool, when all you have is a hammer, etc."
  [pred coll]
  (->> coll
       (r/filter pred)
       (into [])))

(defn rename-keys*
  "Returns the map with each key renamed to the result of (f key)."
  [m f]
  (zipmap (map f (keys m))
          (vals m)))

(defn update*
  "Like update, but updates all of the keys in ks using f."
  ([m ks f]
   (reduce #(update %1 %2 f)
           m ks))
  ([m ks f x]
   (reduce #(update %1 %2 f x)
           m ks))
  ([m ks f x & args]
   (reduce #(apply update %1 %2 f x args)
           m ks)))
