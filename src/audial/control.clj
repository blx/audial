(ns audial.control
  (:require [clojure.java.shell :refer [sh]]
            [ring.util.codec :refer [percent-decode]]))

(def applescript
  (partial sh "osascript" "-e"))

(def tell-itunes
  (comp applescript (partial str "tell app \"iTunes\" to ")))

(defn playpause []
  (tell-itunes "playpause"))

(defn play-path [path]
  (tell-itunes (format "play (POSIX file \"%s\")" path)))

(defn play-song
  (-> song
      :location
      percent-decode
      play-path))
