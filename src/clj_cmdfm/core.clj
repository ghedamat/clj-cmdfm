(ns clj-cmdfm.core
  (:require [clojure.core.async :refer [>!! alt! chan >! <! go go-loop <!! alt!! put!]]
            [clj-cmdfm.mplayer :as mplayer]
            [clj-cmdfm.api :as api]
            [clojure.string :as string])
  (:gen-class))

(defn truef [& o] true)
(defn falsef [& o] false)

(defn play-track
  [files app]
  (let [c (:channel app)
        file (first files)]
    (if (seq files)
      (go
        (println "playing" (:title file))
        (mplayer/play (:stream_url file))
        (println "finished playing" (:title file))
        (>! c [:play-done (rest files)]))
      (do (swap! (:playing app) falsef)
          (println "playlist is over")))))

(defn play-genre
  [genre app]
  (let [c (:channel app)
        playing (:playing app)
        files (->> (api/fetch-genre genre 10)
                   (map #(select-keys % [:title :stream_url :duration])))]
    (if @playing
      (println "already playing")
      (do (swap! (:playing app) truef)
          (play-track files app)))))

(defn play-next
  [app]
  (if @(:playing app)
    (mplayer/stop)
    (println "nothing to play")))

(defn play-done
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

(defn help []
  (println "Available commands:
           play <genere>
           next
           pause
           stop
           quit"))

(defn dispatch
  [command params app]
  (case command
    :play (play-genre params app)
    :play-done (play-done params app)
    :next (play-next app)
    :pause (pause)
    :stop (stop app)
    :quit (exit 0 "Bai Bai")
    :test (println "test")
    :save (println "save")
    (help)))

(def app
  {:playing (atom false)
   :channel (chan)})

(defn main-loop
  []
  (let [channel (:channel app)]
    (go-loop []
             (let [[command params] (<! channel)]
               (dispatch command params app)
               (recur)))))

(defn command-loop
  ([]
  (println "Welcome to clj-cmdfm")
  (help)
  (main-loop)
  (loop []
    (let [[command & params] (-> (read-line) (string/split #"\s+"))]
      (dispatch (keyword command) params app)
      (recur))))
  ([genre]
   (dispatch :play genre app)
   (command-loop)))

