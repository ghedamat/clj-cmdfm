(ns clj-cmdfm.cli
  (:require [clojure.tools.cli :refer  [parse-opts]]
            [clojure.core.async :refer [>!! alt! chan >! <! go <!! alt!!]]
            [clj-cmdfm.api :as api]
            [clojure.string :as string])
  (:use [clj-cmdfm.core])
  (:gen-class))

(def cli-options
  [;; First three strings describe a short-option, long-option with optional
   ;; example argument description, and a description. All three are optional
   ;; and positional.
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["clj-cmdfm"
        ""
        "Usage: clj-cmdfm genre"
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn play-genre
  [genre]
  (let [files (atom (->>
                      (api/fetch-genre genre 10)
                      (map #(select-keys % [:title :stream_url :duration]))))
        mplayer-chan (chan)
        command-chan (chan)]
    (main-loop files mplayer-chan command-chan)
    (play-next files mplayer-chan)
    (println "choose an action: [quit next pause]")
    (loop [input (read-line)]
      (do
        (>!! command-chan input)
        (recur (read-line))))))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond
      (:help options) (exit 0 (usage summary))
      (< (count arguments) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))
    ;; Execute program with options
    (if (seq arguments)
      (play-genre (first arguments))
      (exit 1 (usage summary)))))


(comment
  (def mplayer-chan (chan))
  (def command-chan (chan))

  (def files (atom (->>
    (api/fetch-genre :techno 10)
    (map #(select-keys % [:title :stream_url :duration])))))
)
