(ns audial.spotify
  (:require [environ.core :refer [env]]
            [clj-http.client :as client]
            [clj-spotify.core :as sp]))

(defn get-oauth-token
  "Returns an access token valid for Spotify Web API endpoints that
  do not require user authorization."
  ; This function is derived from clj-spotify/test/clj_spotify/core_test.clj
  []
  (-> "https://accounts.spotify.com/api/token" 
      (client/post {:form-params {:grant_type "client_credentials"}
                    :basic-auth ((juxt :spotify-client-id :spotify-client-secret) env)
                    :as :json})
      :body
      :access_token))

(defn echo [s]
  (println s)
  s)

(defn search-tracks [token q & [opts]]
  (println (format "searching offset=%d" (or (:offset opts) 0)))
  (-> (sp/search (merge {:q q
                         :type "track"}
                        opts)
                 token)
      :tracks))

(defn search-seq
  "Returns a lazy-seq of search results for query q. Be aware that this
  will transparently call Spotify's API as many times as necessary to populate
  the seq, so, eg. (take 500 (search-seq ...)) will take some time and make
  a dozen-ish GET requests."
  [token q]
  (let [chunk-size 40  ; Spotify supports [1..50] inclusive
        offset-search #(search-tracks token q {:offset %
                                               :limit chunk-size})
        wrap (fn wrap [{:keys [items offset limit]}]
               (when (seq items)
                 (concat items (-> (+ offset limit)
                                   offset-search
                                   wrap
                                   lazy-seq))))]
    (lazy-seq
      (wrap (offset-search 0)))))
