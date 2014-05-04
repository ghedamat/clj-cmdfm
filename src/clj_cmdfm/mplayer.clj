(ns clj-cmdfm.mplayer
  (:use [clojure.java.shell :only [sh]])
  (:gen-class))

(defn play
  [file]
  (let [file (str file "?client_id=2cd0c4a7a6e5992167a4b09460d85ece")]
    (sh "mkfifo" "/tmp/mplayer-control")
    (sh "mplayer" "-slave" "-quiet" "-input" "file=/tmp/mplayer-control" file)))

(defn pause
  []
  (spit "/tmp/mplayer-control" "pause\n")
  :done)

(defn stop
  []
  (spit "/tmp/mplayer-control" "quit\n"))

