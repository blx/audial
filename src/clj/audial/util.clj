(ns audial.util
  (:require [clojure.string :as str]
            [clojure.core.reducers :as r]))

(def solitary?
  (comp empty? rest))

(defn ->keyword [s]
  (-> (str s)
      str/lower-case
      (str/replace " " "-")
      keyword))

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
