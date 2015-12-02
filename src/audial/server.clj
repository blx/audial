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
            [audial.control :refer [get-file]]
            [audial.util :refer [update*]]))

(def url-encode
  (fnil codec/url-encode ""))
(def url-decode
  (fnil codec/url-decode ""))

(def file-prefix
  (env :file-prefix))

(defn url-encode-song [song]
  (->> (get-file song)
       (#(str/replace % file-prefix ""))
       url-encode))

(defn url-decode-path [path]
  (->> path
       url-decode
       (str file-prefix)))

(defn url-for [song]
  (->> song
       url-encode-song
       (str "/play/")))

(defn simplify-song [song]
  (-> (select-keys song [:name :artist :album-artist :album :year :track-number])
      (assoc :url (url-for song))))

(defn highlight-matches [s q]
  (if (str/blank? q)
    s
    (let [pattern (->> q (format "(?i)(%s)") re-pattern)]
      (str/replace s pattern (str "<span class='selected'>$1</span>")))))

(defn highlight-song [q song]
  (update* song [:name :artist :album] highlight-matches q))

(defn $song [{:keys [name artist album] :as song}]
  [:li
   [:a {:href (url-for song)
        :onclick (format "ui.core.play('%s'); return false;" (url-encode-song song))
        :tabindex 0}
    name]
   [:span.field artist]
   [:span.field album]])

(defn $results [results q]
  [:ul
   (map #(->> % (highlight-song q) $song) results)])

(defn $search []
  [:form {:action "/search"
          :method "get"}
   [:input#q {:type "text"
              :name "q"
              :autofocus true}]])

(def css
  (str
    "body {background-color:#fafafa; font-family:Helvetica Neue,Helvetica; padding:8px 8px;}"
    "#q {width:400px; font-size:2em; padding:0.3em;}"
    "ul {list-style:none; padding-left:.7em;}"
    "li {line-height:1.5em;}"
    ".field {padding-left:.5em;}"
    "li span:first-of-type {font-weight:bold;}"
    ".field:before {content: \" / \"; padding-right:.5em; color:#ccc;}"
    ".selected {background-color:#ccffcc;}"))

(defn $page [content]
  (html
    [:html
     [:head
      [:title "audial"]
      [:style css]]
     [:body
      ($search)
      content
      [:script {:src "js/ui-core.js"}]]]))

(defn search [q & [in-mem?]]
  (let [result ((if in-mem? audial/play-q' audial/play-q) audial/*songs* q)]
    (-> (if (keyword? result)
          (str result)
          ($results result q)))))


(defn play [path]
  (let [result (audial.control/play-file (str file-prefix (url-decode path)))]
    (if (zero? (:exit result))
      [:p "Playing"]
      [:p (str "Error: " result)])))

(defn render [content]
  (-> content
      $page))


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
  (POST "/api/play" {{:keys [path]} :body} (response (audial.control/play-file (url-decode-path path))))
  (route/resources "/"))

(def site
  (-> app-routes
      wrap-json-response
      (wrap-json-body {:keywords? true})
      (wrap-defaults (dissoc site-defaults :security))))

(defn -main []
  (run-jetty site {:port 8080}))
