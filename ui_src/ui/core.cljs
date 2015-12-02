(ns ui.core
  (:require [reagent.core :as reagent]
            [ajax.core :refer [POST]]))

(comment
(defn root-component []
  [:p "heya"])

(reagent/render
  [root-component]
  (.-body js/document))
)



(defn ^:export play [url]
  (POST "/api/play"
        {:format :json
         :params {:path url}}))
