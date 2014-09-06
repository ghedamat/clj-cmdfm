(ns clj-cmdfm.core
  (:require [clojure.core.async :refer [go-loop <!]]
            [clj-cmdfm.player :as player]
            [clojure.string :as string])
  (:use [clj-cmdfm.utils])
  (:gen-class))

(defn prn-help [options]
  (prompt (str "Available commands:\n\n"
                (->> options
                     (map #(select-values % [:desc :short-opt :long-opt]))
                     (map (fn[[d, s, l]] (str s " | " (add-padding 10 l) " : " d)))
                     (string/join "\n")))))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn dispatch
  [command params app]
  (case command
    :play (player/play-genre params app)
    :done (player/done params app)
    :next (player/play-next app)
    :pause (player/pause app)
    :stop (player/stop app)
    :genres (player/list-genres)
    :help (prn-help (:options app))
    :quit (exit 0 "Bai Bai")))

(defn- validate [spec values]
  (let [{:keys [validate-fn validate-msg]} spec]
    (or (loop [[vfn & vfns] validate-fn [msg & msgs] validate-msg]
          (when vfn
            (if (try (apply vfn values) (catch Throwable e))
              (recur vfns msgs)
              [::error msg])))
        [spec values])))

(defn dispatch-console
  [command params app]
  (let [options (:options app)
        option (-> (filter (fn [o]
                             (if (or (= (:short-opt o) command)
                                     (= (:long-opt o) command))
                               o))
                           options)
                   first)
        [status value] (validate option params)]
    (if (nil? status)
      (do
        (println "No such command")
        (prn-help options))
      (if (= status ::error)
        (prompt (str "ERROR " command ": "value))
        (dispatch (keyword (:long-opt option)) params app)))))

(defn main-loop
  "Go routine that loops waiting for commands on the app channel."
  [app]
  (let [channel (:channel app)]
    (go-loop []
             (let [[command params] (<! channel)]
               (dispatch command params app)
               (recur)))))

(defn command-loop
  "Reads from stdin for the next command and dispatches it."
  ([app]
   (println "Welcome to clj-cmdfm")
   (prn-help (:options app))
   (main-loop app)
   (loop []
     (let [[command & params] (-> (read-line) (string/split #"\s+"))]
       (dispatch-console command params app)
       (recur))))
  ([app genre]
   (dispatch :play [genre] app)
   (command-loop app)))
