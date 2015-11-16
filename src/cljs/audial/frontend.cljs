(ns audial.frontend
  (:require [reagent.core :refer [render atom]]
            [clojure.string :as str]))

(def songs (atom nil))
(def search-query (atom nil))

(defn song [song]
  [:li
   [:a {:href (:url song)
        :tabIndex 0}
    (:name song)]
   (str " by " (:artist song))])

(defn search []
  [:div
   [:input#q {:type "text" :auto-focus true
              :on-change #(reset! search-query (-> % .-target .-value))}]])

(defn song-matches? [query song]
    (let [qs (str/split query #"\s+")
          match? (->> (map #(str "(?=.*" % ")") qs)
                      (apply str)
                      (#(str "(?i)" % ".*"))
                      re-pattern
                      (partial re-find))
          fields (juxt :name :artist :album-artist :album)
          song-match? #(->> (fields %)
                            (apply str)
                            match?)]
      (song-match? song)))

(defn page []
  (let [songs (if (empty? @search-query)
                @songs
                (filter (partial song-matches? @search-query)
                        @songs))]
  [:div
   [search]
   [:ul (for [x songs]
          ^{:key (:id x)}
          [song x])]]))

(declare _songs)
(defn ^:export run []
  (reset! songs (map-indexed
                  #(assoc %2 :id %1)
                  (js->clj _songs :keywordize-keys true)))
  (render [page]
          (.-body js/document)))
