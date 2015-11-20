(ns audial.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [com.github.bdesham.clj-plist :as plist]
            [clojure.java.jdbc :as j]
            [audial.util :refer [solitary? keywordize dekeywordize pfilterv rename-keys*]]
            [audial.control :as ctrl]))

(def itunes-db
  {:subprotocol "sqlite"
   :classname "org.sqlite.JDBC"
   :subname "db.sqlite3"})

(def itunes-db-fields
  [:name :artist :album-artist :album :location :play-count])

(def search-fields
  [:name :artist :album-artist :album])

(defn itunes-db-create! []
  (j/db-do-commands
    itunes-db
    (format "create virtual table tracks using fts4(%s)"
            (->> itunes-db-fields
                 (map dekeywordize)
                 (str/join ", ")))))

(defn itunes-db-insert! [song]
  (j/insert!
    itunes-db :tracks
    (-> song
        (select-keys itunes-db-fields)
        (rename-keys* keywordize))))

(defn populate-itunes-db! [songs]
  (doseq [song songs]
    (itunes-db-insert! song)))

(defn echo [s] (println s) s)

(defn query-itunes-db [q]
  (if (str/blank? q)
    (j/query itunes-db "select * from tracks order by play_count desc")
    (let [q' (->> (str/split q #"\s+")
                  (map #(str % "*"))
                  (str/join " OR "))]
      (j/query itunes-db
               (echo
                 ["select * from tracks where tracks match ? order by play_count desc"
                  (->> search-fields
                       (map dekeywordize)
                       (map #(format "(%s:'%s')" % q'))
                       (str/join " OR "))])))))

(defn kind [track]
  (condp re-find (or (:kind track) "")
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
       (filterv (every-pred audio-track? :location))))

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
           ((apply juxt search-fields))
           (apply str)
           match?))))

(defn search [songs q]
  (if (str/blank? q)
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

(defn play-q' [songs q]
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
  (-> (env :itunes-file)
      (plist/parse-plist {:keyword-fn keywordize})
      (update :tracks vals)))

(def ^:dynamic *songs*
  (songs (parse)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
