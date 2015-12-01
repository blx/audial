(ns audial.spotify
  (:require [environ.core :refer [env]]
            [clj-http.client :as client]
            [clj-spotify.core :as sp]))

;; This function is derived from clj-spotify/test/clj_spotify/core_test.clj

(defn get-oauth-token
  "Returns an access token valid for Spotify Web API endpoints that
  do not require user authorization."
  []
  (-> "https://accounts.spotify.com/api/token" 
      (client/post {:form-params {:grant_type "client_credentials"}
                    :basic-auth ((juxt :spotify-client-id :spotify-client-secret) env)
                    :as :json})
      :body
      :access_token))

(defn search-tracks [token q & [opts]]
  (-> (sp/search (merge {:q q
                         :type "track"}
                        opts)
                 token)
      echo
      :tracks))

(defn echo [s]
  (println s)
  s)

(defn search-seq [token q]
  (let [offset-search #(->> {:offset %}
                            (search-tracks token q)
                            echo)
        wrap (fn wrap [{:keys [items offset limit]}]
               (lazy-seq
                 (when-not (empty? items)
                   (concat items (wrap (offset-search (+ offset limit)))))))]
    (wrap (offset-search 0))))
