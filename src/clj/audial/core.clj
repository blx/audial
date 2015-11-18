(ns audial.core
  (:gen-class)
  (:require [clojure.xml :as xml]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [clojure.java.jdbc :as j]
            [audial.util :refer [solitary? ->keyword pfilterv rename-keys*]]
            [audial.control :as ctrl]))

(def parse-itunes-library-file
  (comp :content first :content xml/parse io/input-stream))

(def itunes-db
  {:subprotocol "sqlite"
   :classname "org.sqlite.JDBC"
   :subname "db.sqlite3"})

(def itunes-db-fields
  [:name :artist :album-artist :album :location :play-count])

(def search-fields
  [:name :artist :album-artist :album])

(defn kw->db-field [kw]
  (-> (name kw)
      str
      (str/replace "-" "_")))

(defn itunes-db-create! []
  (j/db-do-commands
    itunes-db
    (format "create virtual table tracks using fts4(%s)"
            (->> itunes-db-fields
                 (map kw->db-field)
                 (str/join ", ")))))

(defn itunes-db-insert! [song]
  (j/insert!
    itunes-db :tracks
    (-> song
        (select-keys itunes-db-fields)
        (rename-keys* kw->db-field))))

(defn populate-itunes-db! [songs]
  (doseq [song songs]
    (itunes-db-insert! song)))

(defn echo [s] (println s) s)

(defn query-itunes-db [q]
  (let [q' (->> (str/split q #"\s+")
                (map #(format "*%s*" %))
                (str/join " OR "))]
    (j/query itunes-db
             (echo
               ["select * from tracks where tracks match ? order by play_count desc"
                (->> search-fields
                     (map audial.util/dekeywordize)
                     (map #(format "(%s:'*%s*')" % q'))
                     (str/join " OR "))]))))


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
                       (let [k (->keyword k)
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
  (let [str->int (fnil #(Integer/parseInt %) "0")]
    (->> (:tracks catalog)
         (filterv (every-pred audio-track? :location))
         (mapv #(update % :play-count str->int)))))

(defn get-artist [catalog artist]
  (filter #{artist} (:tracks catalog)))

(defn song-matcher [q]
  (let [qs (str/split q #"\s+")
        match? (->> (map (partial format "(?=.*%s)") qs)
                    (apply str)
                    (format "(?i)%s.*")
                    re-pattern
                    (partial re-find))]
    (fn [song]
      (->> song
           (map search-fields)
           (apply str)
           match?))))

(defn search [songs q]
  (if (empty? q)
    songs
    (->> (pfilterv (song-matcher q) songs)
         (sort-by :play-count >))))

(defn play-q [songs q]
  (let [results ;(search songs q)]
        (query-itunes-db q)]
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
  (-> (env :itunes-file)
      parse-itunes-library-file
      parse-itl))

(def ^:dynamic *songs*
  (songs (parse)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
