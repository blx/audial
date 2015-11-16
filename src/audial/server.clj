(ns audial.server
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer [response content-type]]
            [ring.util.codec :as codec]
            [compojure.core :refer [defroutes GET]]
            [hiccup.core :refer [html]]
            [audial.core :as audial]
            [audial.control :refer [get-file]]))

(def url-encode
  (fnil codec/url-encode ""))
(def url-decode
  (fnil codec/url-decode ""))

(defn url-for [song]
  (->> (get-file song)
       url-encode
       (str "/play/")))

(defn render-song [song]
  [:li
   [:a {:href (url-for song)
        :tabindex 0}
    (:name song)]
   (str " by " (:artist song))])

(defn render-results [results]
  [:ul
   (map render-song results)])

(defn render-search []
  [:form {:action "/search"
          :method "get"}
   [:input#q {:type "text"
              :name "q"
              :autofocus true}]])

(defn render-page [content]
  (let [css "#q {width:400px; font-size:2em; padding:0.3em;}"]
    (html
      [:style css]
      (render-search)
      content)))

(defn search [q]
  (let [result (audial/play-q audial/*songs'* q)]
    (-> (if (keyword? result)
          (str result)
          (render-results result)))))

(defn play [path]
  (let [result (audial.control/play-file (url-decode path))]
    (if (zero? (:exit result))
      [:p "Playing"]
      [:p (str "Error: " result)])))

(defn render [content]
  (-> content
      render-page
      response
      (content-type "text/html")))

(defroutes app-routes
  (GET "/search" [q] (render (search q)))
  (GET "/play/:path" [path] (render (play path))))

(def site
  (-> app-routes
      (wrap-defaults site-defaults)))

(defn -main []
  (run-jetty site {:port 8080}))
