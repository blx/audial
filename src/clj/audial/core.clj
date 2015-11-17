(ns audial.core
  (:gen-class)
  (:require [clojure.xml :as xml]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.core.reducers :as r]
            [tesser.core :as t]
            [audial.control :as ctrl]))

(def ^:dynamic *itunes-file*
  "/Users/ben/Music/iTunes/iTunes Library.xml")

(def parse-itunes-library-file
  (comp :content first :content xml/parse io/input-stream))

(defn- str->keyword [s]
  (-> s
      str/lower-case
      (str/replace " " "-")
      keyword))

(def solitary?
  (comp empty? rest))

(defn parse-plist-seq
  "plist xml consists of pairs of consecutive tags,
  eg. <key>k</key><integer>5</integer>"
  [xml]
                     ; (defalias XMLTag (HMap :required
                     ;                        {:tag Kw :attrs (Option Vec)
                     ;                         :content (Option (Vec (U String XMLTag)))}))
                     ; (defalias Tag (Map Kw (U nil String Tag)))
                     ; tagpair->tag is [XMLTag XMLTag] -> Tag
  (let [tagpair->tag (fn tagpair->tag [[{[k] :content tag :tag}
                           {[& vs] :content}]]
                       (let [k (str->keyword k)
                             v (if (solitary? vs)
                                 (first vs)
                                 (if (= tag :array)
                                   (map tagpair->tag [nil vs])  ; arrays don't have key-value consecutive tags
                                   (parse-plist-seq vs)))]
                         {k v}))]
    (->> xml
         (partition 2)
         (map tagpair->tag)
         (reduce merge {}))))

(defn parse-itl [xml]
  (-> xml
      parse-plist-seq
      (update :tracks vals)
      ; TODO playlists are wonky
      (update :playlists vals)))

(defn kind [track]
  (condp re-find (:kind track)
    #"audio" :audio
    #"video" :video
    #"app$" :app
    #"PDF" :pdf
    #"iTunes LP" :itunes-lp
    :unknown))

(defn audio-track? [track]
  (= :audio (kind track)))

(defn songs [catalog]
  (->> (:tracks catalog)
       (filter audio-track?)
       vec))

(defn get-artist [catalog artist]
  (filter #{artist} (:tracks catalog)))

(defn rfilter [pred coll]
  (->> coll
       (r/filter pred)
       (into [])))

(defn search [songs q]
  (if (empty? q)
    songs
    (let [qs (str/split q #"\s+")
          match? (->> (map (partial format "(?=.*%s)") qs)
                      (apply str)
                      (format "(?i)%s.*")
                      re-pattern
                      (partial re-find))
          fields (juxt :name :artist :album-artist :album)
          song-match? #(->> (fields %)
                            (apply str)
                            match?)]
      (->> (rfilter song-match? songs)
           (sort-by #(-> (or (:play-count %) "0")
                         Integer/parseInt)
                    >)))))
;      (->> (t/filter song-match?)
           ;(t/map (juxt (fnil :play-count 0) identity))
           ;(t/fold {:reducer (fn [acc in]
           ;                    (
           ;         :reducer (comp second (partial sort-by first >))
           ;         :combiner +})
;           (t/into [])
;           (t/tesser (partition 256 songs))))))



(defn pfilter [pred coll]
  (let [v (vec coll)
        len (count v)
        chunk-size 2048]
    (when-not (zero? len)
      (if (< len (* 4 chunk-size))
        (do (println "Dispatching to clojure.core/filter")
            (clojure.core/filter pred coll))
        (let [n-full-chunks (/ len chunk-size)
              remainder-size (mod len chunk-size)
              partition-bounds (->> (range 0 len chunk-size)
                                    (partition 2 1)
                                    (#(if-not (zero? remainder-size)
                                        (concat % [[(- len remainder-size) len]])
                                        %)))]
          (println "ok")
          (->> (mapv (fn [[start end]] (subvec v start end))
                    partition-bounds)
               (pmap (partial filter pred))
               (r/reduce 
               (flatten))))))))
;               ((fn [filtered-partitions]
;                  (let [n-partitions (count filtered-partitions)]
;                    (loop [i 0 v (transient [])]
;                      (if (< i n-partitions)
;                        (recur (inc i) (reduce conj! v (nth filtered-partitions i)))
;                        (persistent! v))))))))))))
;  )
               ;(apply conj)))))))

(defn filter' [pred coll]
  (lazy-seq
    (when-let [s (seq coll)]
      (if (chunked-seq? s)
        (let [c (chunk-first s)
              size (count c)
              b (chunk-buffer size)]
          (dotimes [i size]
            (let [v (.nth c i)]
              (when (pred v)
                (chunk-append b v))))
          (chunk-cons (chunk b) (filter pred (chunk-rest s))))
        (let [f (first s) r (rest s)]
          (if (pred f)
            (cons f (filter pred r))
            (filter pred r)))))))


(defn -search [songs q]
  (if (empty? q)
    songs
    (let [qs (str/split q #"\s+")
          match? (->> (map (partial format "(?=.*%s)") qs)
                      (apply str)
                      (format "(?i)%s.*")
                      re-pattern
                      (partial re-find))
          fields (juxt :name :artist :album-artist :album)
          song-match? #(->> (fields %)
                            (apply str)
                            match?)]
      (filter song-match? songs))))

(defn matches'? [q]
  (let [qs (str/split q #"\s+")
        match? (->> (map (partial format "(?=.*%s)") qs)
                    (apply str)
                    (format "(?i)%s.*")
                    re-pattern
                    (partial re-find))
        fields (juxt :name :artist :album-artist :album)]
    #(->> (fields %)
          (apply str)
          match?)))


(defn play-q [songs q]
  (let [results (search songs q)]
    (cond
      (empty? results)
      :no-results

      (solitary? results)
      (let [song (first results)]
        (if (ctrl/available? song)
          (do (ctrl/play-song song)
              :success)
          :unavailable))

      :else results)))

(defn parse []
  (-> *itunes-file*
      parse-itunes-library-file
      parse-itl))

(def ^:dynamic *songs*
  (songs (parse)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
