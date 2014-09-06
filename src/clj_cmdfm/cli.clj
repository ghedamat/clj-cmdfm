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
        "Usage: clj-cmdfm <genre>"
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

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

(def cmd-opts
  [;; First three strings describe a short-option, long-option with optional
   ;; example argument description, and a description. All three are optional
   ;; and positional.
   ["p" "play" "play <genre> [limit]" :validate  [#(> (count %&) 0)  "Must have a genre"]]
   ["g" "genres" "list genres"]
   ["s" "stop" "stop playing"]
   ["q" "quit" "quit"]
   ["h" "help" "print available commands"]
   ["u" "pause" "pause"]
   ["n" "next" "next song"]])

(def app
  {:playing (atom false)
   :options (map compile-opts cmd-opts)
   :channel (chan)})

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond
      (:help options) (exit 0 (usage summary))
      (> (count arguments) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))
    ;; Execute program with options
    (apply command-loop app arguments)))
