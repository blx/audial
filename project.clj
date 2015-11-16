(defproject audial "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-codec "1.0.0"]
                 [hiccup "1.0.5"]
                 [compojure "1.4.0"]]
  :main ^:skip-aot audial.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
