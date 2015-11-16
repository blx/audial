(ns audial.control
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [ring.util.codec :refer [percent-decode]]
            [clojure.string :as str]))

(def applescript
  (partial sh "osascript" "-e"))

(def tell-itunes
  (comp applescript (partial str "tell app \"iTunes\" to ")))

(defn playpause []
  (tell-itunes "playpause"))

(defn get-file [song]
  (-> (or (:location song) "")
      percent-decode
      (str/replace #"^file:/*" "/")))

(defn available? [song]
  (-> (get-file song)
      io/as-file
      .exists))

(defn play-file [path]
  (tell-itunes (format "play (POSIX file \"%s\")" path)))

(defn play-song [song]
  (let [path (get-file song)]
    (println "Attempting to play song at location " path)
    (play-file path)))
