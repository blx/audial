(ns ui.core
  (:require [reagent.core :as reagent]))

(defn root-component []
  [:p "heya"])

(reagent/render
  [root-component]
  (.-body js/document))
