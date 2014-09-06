(ns clj-cmdfm.player
  (:require [clj-cmdfm.api :as api]
            [clojure.core.async :refer [>! <! go ]]
            [clojure.string :as string]
            [clj-cmdfm.mplayer :as mplayer])
  (:use [clj-cmdfm.utils])
  (:gen-class))

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
          (prompt "Playlist is over")))))

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
  (when @(:playing app)
    (play-track params app)))

(defn stop
  [app]
  (when @(:playing app)
    (swap! (:playing app) falsef)
    (mplayer/stop)))

(defn pause
  [app]
  (when @(:playing app)
    (mplayer/pause)))

(defn list-genres []
  (let [genres (api/fetch-genres)
        max-len (->> genres (map :name) (map count) (apply max))]
    (->> (api/fetch-genres)
         (map :name)
         (map #(add-padding max-len %))
         (partition 4)
         (map #(string/join  " " %))
         (string/join "\n")
         prompt)))

