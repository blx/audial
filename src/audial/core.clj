(ns audial.core
  (:gen-class)
  (:require [clojure.xml :as xml]
            [clojure.java.io :as io]
            [clojure.string :as str]
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
  (filter audio-track? (:tracks catalog)))

(defn get-artist [catalog artist]
  (filter #{artist} (:tracks catalog)))

(defn search [songs q]
  (let [qs (str/split q #"\s+")
        match? (->> (str/join " " (map #(str "(?i)" %) qs))
                    re-pattern
                    (partial re-find)
                    (#(fnil % "")))
        fields (juxt :name :artist :album-artist :album)
        song-match? #(some match? (fields %))]
    (filter song-match? songs)))

(defn play-q [songs q]
  (let [results (search songs q)]
    (cond
      (empty? results)
      :no-results

      (empty? (rest results))
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

(def ^:dynamic *songs'*
  (songs (parse)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
