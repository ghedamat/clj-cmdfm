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

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond
      (:help options) (exit 0 (usage summary))
      (> (count arguments) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))
    ;; Execute program with options
    (command-loop arguments)))


