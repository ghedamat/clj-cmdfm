(ns clj-cmdfm.core
  (:require [clojure.core.async :refer [>!! alt! chan >! <! go go-loop <!! alt!! put!]]
            [clj-cmdfm.mplayer :as mplayer]
            [clj-cmdfm.api :as api]
            [clojure.string :as string])
  (:gen-class))

(defn truef [& o] true)
(defn falsef [& o] false)

(defn prn-help [options]
  (println (str "Available commands:\n\n"
                (string/join "\n" (map #(:desc %) options)))))
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

(defn dispatch
  [command params app]
  (case command
    :play (play-genre params app)
    :done (done params app)
    :next (play-next app)
    :pause (pause)
    :stop (stop app)
    :quit (exit 0 "Bai Bai")))


(defn compile-opts
  [spec]
  (let [sopt-lopt-desc (take-while #(or  (string? %)  (nil? %)) spec)
        spec-map  (apply hash-map  (drop  (count sopt-lopt-desc) spec))
        [short-opt long-opt desc] sopt-lopt-desc
        validate (:validate spec-map)
        [validate-fn validate-msg] (when (seq validate)
                                     (->> (partition 2 2 (repeat nil) validate)
                                          (apply map vector)))]
    {:short-opt short-opt
     :long-opt long-opt
     :desc desc
     :validate-fn validate-fn
     :validate-msg validate-msg}))

(defn- validate [spec values]
  (let [{:keys [validate-fn validate-msg]} spec]
    (or (loop [[vfn & vfns] validate-fn [msg & msgs] validate-msg]
          (when vfn
            (if (try (apply vfn values) (catch Throwable e))
              (recur vfns msgs)
              [::error msg])))
        [spec values])))


(defn dispatch-console
  [command params app options]
  (let [option (-> (filter (fn [o]
                             (if (or (= (:short-opt o) command)
                                     (= (:long-opt o) command))
                               o))
                           options)
                   first)
        [status value] (validate option params)]
    (if (nil? status)
      (do
        (println "no such command")
        (prn-help options))
      (if (= status ::error)
        (println "error " command value)
        (dispatch (keyword (:long-opt option)) params app)))))

(comment
(dispatch-console "p" nil app opts)
  )

(defn main-loop
  "Go routine that loops waiting for commands on the app channel."
  []
  (let [channel (:channel app)]
    (go-loop []
             (let [[command params] (<! channel)]
               (dispatch command params app)
               (recur)))))

(def cmd-opts
  [;; First three strings describe a short-option, long-option with optional
   ;; example argument description, and a description. All three are optional
   ;; and positional.
   ["p" "play" "play <genre> [limit]" :validate  [#(> (count %&) 0)  "Must have a genre"]]
   ["s" "stop" "stop playing"]
   ["w" "quit" "quit"]
   ["a" "pause" "pause"]
   ["n" "next" "next song"]])

(def opts (map compile-opts cmd-opts))




(defn command-loop
  "Reads from stdin for the next command and dispatches it."
  ([]
   (println "Welcome to clj-cmdfm")
   (prn-help opts)
   (main-loop)
   (loop []
     (let [[command & params] (-> (read-line) (string/split #"\s+"))]
       (dispatch-console command params app opts)
       (recur))))
  ([genre]
   (dispatch :play genre app)
   (command-loop)))
