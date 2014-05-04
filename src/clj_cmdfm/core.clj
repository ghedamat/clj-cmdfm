(ns clj-cmdfm.core
  (:require [clojure.core.async :refer [>!! alt! chan >! <! go <!! alt!!]]
            [clj-cmdfm.mplayer :as mplayer]
            [clj-cmdfm.api :as api])
  (:gen-class))



(defn delayed-play
  [file c]
  (go
    (println "playing" (:title file))
    (mplayer/play (:stream_url file))
    (println "finished playing" (:title file))
    (>! c :done)))

(defn play-next
  [files c]
  (if (seq @files)
    (do (delayed-play (first @files) c)
        (swap! files rest))))

(defn exec-command
  [cmd files mplayer-chan]
  (case cmd
    "pause" (mplayer/pause)
    "next" (do (mplayer/stop) (<!! mplayer-chan) (play-next files mplayer-chan))
    "quit" (do (mplayer/stop) (<!! mplayer-chan) nil)
    (do (println "valid operations are [pause next quit]") :no-op)))

(defn main-loop
  [files mplayer-chan command-chan]
  (go
    (loop [n :start]
      (if (nil? n)
        (do
          (println "playlist is over")
          (System/exit 0))
        (recur
          (alt!
            mplayer-chan (play-next files mplayer-chan)
            command-chan ([cmd] (exec-command cmd files mplayer-chan))))))))

