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
                 [hiccup "1.0.5"]
                 [compojure "1.4.0"]
                 
                 [org.clojure/clojurescript "1.7.170"]
                 [reagent "0.5.1"]]
  
  :plugins [[lein-ring "0.9.7"]
            [lein-cljsbuild "1.1.1"]]

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
             :dev {:cljsbuild
                   {:builds {:dev {:compiler {:output-to "resources/public/js/audial-dev.js"
                                              :output-dir "resources/public/js"
                                              :asset-path "js"
                                              :main "audial.frontend"
                                              :optimizations :none}}}}}})
