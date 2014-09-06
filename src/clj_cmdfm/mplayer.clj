(ns clj-cmdfm.mplayer
  (:use [clojure.java.shell :only [sh]])
  (:gen-class))

(defn play
  [file]
  (let [file (str file "?client_id=26fb3c513c8e0e2c18a75e6174f4ca70")]
    (sh "mkfifo" "/tmp/mplayer-control")
    (sh "mkfifo" "/tmp/mplayer-data")
    (sh "./play.sh" file)))

(defn pause
  []
  (spit "/tmp/mplayer-control" "pause\n")
  :done)

(defn stop
  []
  (spit "/tmp/mplayer-control" "quit\n"))

