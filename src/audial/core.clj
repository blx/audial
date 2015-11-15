(ns audial.core
  (:gen-class)
  (:require [clojure.xml :as xml]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:dynamic *itunes-file*
  "/Users/ben/Music/iTunes/iTunes Library.xml")

(def parse-itunes-library-file
  (comp :content first :content xml/parse io/input-stream))

(defn- plist-key->keyword [plist-key]
  (-> plist-key
      str/lower-case
      (str/replace " " "-")
      keyword))

(def solitary?
  (comp empty? rest))

(defn parse-plist-seq
  "plist xml consists of pairs of tags, eg. <key>k</key><integer>5</integer>"
  [xml]
  (let [tagpair->tag (fn [[{[k] :content}
                           {[& vs] :content}]]
                       (let [k (plist-key->keyword k)
                             v (if (solitary? vs)
                                 (first vs)
                                 (parse-plist-seq vs))]
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

(def audio-track? (comp #(= :audio %) kind))

(defn parse []
  (-> *itunes-file*
      parse-itunes-library-file
      parse-itl))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
