(defproject audial "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://github.com/blx/audial"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-codec "1.0.0"]
                 [ring/ring-json "0.4.0"]
                 [environ "1.0.1"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.xerial/sqlite-jdbc "3.8.11.2"]
                 [hiccup "1.0.5"]
                 [compojure "1.4.0"]
                 ;[tesser.core "1.0.1"]
                 [clj-spotify "0.1.1" :exclusions [commons-codec]]
                 [clj-http "2.0.0"]
                 [cheshire "5.5.0"]
                 ;[bdesham/clj-plist "0.9.1" :exclusions [joda-time org.clojure/clojure]]
                 [com.github.bdesham/clj-plist "0.10.0" :exclusions [joda-time]]
                 
                 [org.clojure/clojurescript "1.7.170"]
                 [reagent "0.5.1"]]
  
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.0.1"]
            [lein-cljsbuild "1.1.1" :exclusions [org.clojure/clojure]]]

  :hooks [leiningen.cljsbuild]

  :ring {:handler audial.server/site
         :port 8080}

  :cljsbuild {:builds {:dev {:source-paths ["src/cljs"]}}}

  :main ^:skip-aot audial.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :cljsbuild
                       {:builds {:prod {:compiler {:output-to "resources/public/js/audial.js"
                                                   :optimizations :advanced
                                                   :pretty-print false}}}}}
             :dev [:local-dev
                   {:cljsbuild
                    {:builds {:dev {:compiler {:output-to "resources/public/js/audial-dev.js"
                                               :output-dir "resources/public/js"
                                               :asset-path "js"
                                               :main "audial.frontend"
                                               :optimizations :none}}}}}]})
