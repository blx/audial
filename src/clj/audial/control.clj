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
  ; This works fine so long as the "Up Next" queue is empty.
  ; Otherwise, iTunes shows a dialog asking if you want to clear the queue,
  ; and it's proving tricky to toggle to that dialog and hit Enter programatically.
  ; Especially since if we hit Enter when not needed, it ends up going into Chrome
  ; and repeating our click on the song link 8 or 10 times.
  {:pre [(not (.isDirectory (io/as-file path)))]}
  (let [cmd (->> ["tell app \"iTunes\""
                  "  ignoring application responses"
                  (str "    play (POSIX file \"" path \"")")
                  "  end ignoring"
                  ;"  delay 0.2"
                  ;"  activate"
                  "end tell"]
                  ;"tell app \"System Events\""
                  ;"  keystroke return"
                  ;"  set visible of process \"iTunes\" to false"
                  ;"end tell"]
                  ;"delay 0.2"
                  ;"tell app \"iTunes\" to keystroke return"
                  ;"tell app \"System Events\" to keystroke return"
                  ;"set visible of process \"iTunes\" to false"]
                 (map #(str % "\n"))
                 (interleave (repeat "-e")))]
    (println cmd)
    (apply sh "osascript" cmd)))

(defn play-song [song]
  (let [path (get-file song)]
    (println "Attempting to play song at location " path)
    (play-file path)))
