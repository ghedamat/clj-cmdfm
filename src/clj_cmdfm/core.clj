(ns clj-cmdfm.core
  (:require [clojure.core.async :refer [>!! alt! chan >! <! go go-loop <!! alt!! put!]]
            [clj-cmdfm.mplayer :as mplayer]
            [clj-cmdfm.api :as api]
            [clojure.string :as string])
  (:gen-class))

(defn truef [& o] true)
(defn falsef [& o] false)

(def app
  {:playing (atom false)
   :channel (chan)})

(defn play-track
  [files app]
  (let [c (:channel app)
        file (first files)]
    (if (seq files)
      (go
        (println "playing" (:title file))
        (mplayer/play (:stream_url file))
        (println "finished playing" (:title file))
        (>! c [:done (rest files)]))
      (do (swap! (:playing app) falsef)
          (println "playlist is over")))))

(defn play-genre
  "fetches a playlist for a given genre and starts playing"
  ([[genre limit] app]
   (let [playing (:playing app)
         limit (if limit limit 10)]
     (if @playing
       (println "already playing")
       (do (swap! playing truef)
           (play-track (api/fetch-genre genre limit) app))))))

(defn play-next
  [app]
  (if @(:playing app)
    (mplayer/stop)
    (println "nothing to play")))

(defn done
  [params app]
  (if @(:playing app)
    (play-track params app)))

(defn stop
  [app]
  (swap! (:playing app) falsef)
  (mplayer/stop))

(defn pause
  []
  (mplayer/pause))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn prn-help []
  (println "Available commands:
           play <genre>
           next
           pause
           stop
           quit"))

(defn dispatch
  [command params app]
  (case command
    :play (play-genre params app)
    :done (done params app)
    :next (play-next app)
    :pause (pause)
    :stop (stop app)
    :quit (exit 0 "Bai Bai")
    (prn-help)))

(defn main-loop
  "Go routine that loops waiting for commands on the app channel."
  []
  (let [channel (:channel app)]
    (go-loop []
             (let [[command params] (<! channel)]
               (dispatch command params app)
               (recur)))))

(defn command-loop
  "Reads from stdin for the next command and dispatches it."
  ([]
   (println "Welcome to clj-cmdfm")
   (prn-help)
   (main-loop)
   (loop []
     (let [[command & params] (-> (read-line) (string/split #"\s+"))]
       (dispatch (keyword command) params app)
       (recur))))
  ([genre]
   (dispatch :play genre app)
   (command-loop)))
