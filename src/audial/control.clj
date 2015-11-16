(ns audial.control
  (:require [clojure.java.shell :refer [sh]]
            [audial.core :refer [get-file]]))

(def applescript
  (partial sh "osascript" "-e"))

(def tell-itunes
  (comp applescript (partial str "tell app \"iTunes\" to ")))

(defn playpause []
  (tell-itunes "playpause"))

(defn play-path [path]
  (tell-itunes (format "play (POSIX file \"%s\")" path)))

(defn play-song [song]
  (let [path (get-file song)]
    (println "Attempting to play song at location " path)
    (play-path path)))
