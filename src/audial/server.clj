(ns audial.server
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response content-type]]
            [ring.util.codec :as codec]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [clojure.string :as str]
            [hiccup.core :refer [html]]
            [audial.core :as audial]
            [audial.control :refer [get-file]]))

(def url-encode
  (fnil codec/url-encode ""))
(def url-decode
  (fnil codec/url-decode ""))

(def file-prefix
  (env :file-prefix))

(defn url-for [song]
  (->> (get-file song)
       (#(str/replace % file-prefix ""))
       url-encode
       (str "/play/")))

(defn simplify-song [song]
  (-> (select-keys song [:name :artist :album-artist :album :year :track-number])
      (assoc :url (url-for song))))
          
(defn render-song [song]
  [:li
   [:a {:href (url-for song)
        :tabindex 0}
    (:name song)]
   [:span (:artist song)]
   [:span (:album song)]])

(defn render-results [results]
  [:ul
   (map render-song results)])

(defn render-search []
  [:form {:action "/search"
          :method "get"}
   [:input#q {:type "text"
              :name "q"
              :autofocus true}]])

(def css (apply str
                "#q {width:400px; font-size:2em; padding:0.3em;}"
                "li {line-height:1.2em;}"
                "span {padding-left:.5em;}"
                "li span:first-of-type {font-weight:bold;}"
                "span:before {content: \" / \"; padding-right:.5em; color:#ccc;}"))

(defn render-page [content]
  (html
    [:html
     [:head
      [:title "audial"]
      [:style css]]
     [:body
      (render-search)
      content]]))

(defn search [q & [in-mem?]]
  (let [result ((if in-mem? audial/play-q' audial/play-q) audial/*songs* q)]
    (-> (if (keyword? result)
          (str result)
          (render-results result)))))


(defn play [path]
  (let [result (audial.control/play-file (str file-prefix (url-decode path)))]
    (println result)
    (if (zero? (:exit result))
      [:p "Playing"]
      [:p (str "Error: " result)])))

(defn render [content]
  (-> content
      render-page))


(defn cljs-page []
  (html
    [:html
     [:head
      [:title "audial"]
      [:style css]]
     [:body
      [:script {:src "/js/audial-dev.js"}]
      ; abuse wrap-json-response to encode json for no good reason
      [:script (str "audial.frontend._songs = "
                    (:body
                      ((wrap-json-response
                         (fn [& _]
                           {:body (map simplify-song audial/*songs*)}))
                       nil))
                    ";")]
      [:script "audial.frontend.run()"]]]))


(defroutes app-routes
  (GET "/cljs" [] (cljs-page))
  (GET "/search" [q] (render (search q)))
  (GET "/search-mem" [q] (render (search q true)))
  (GET "/play/:path" [path] (render (play path)))
  (GET "/api/songs" [] (response (map simplify-song audial/*songs*)))
  (GET "/api/search" [q] (response (map simplify-song (audial/search audial/*songs* q))))
  (POST "/api/play" [:as {{path :path} :body}] (response (audial.control/play-file (url-decode path))))
  (route/resources "/"))

(def site
  (-> app-routes
      wrap-json-response
      (wrap-json-body {:keywords? true})
      (wrap-defaults (dissoc site-defaults :security))))

(defn -main []
  (run-jetty site {:port 8080}))