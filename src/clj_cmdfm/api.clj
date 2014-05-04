(ns clj-cmdfm.api
  (:require [clj-http.client :as client]
            [clojure.data.json :as json])
  (:gen-class))

(defn fetch-genre
  "hits cmd.fm search api to get a list of songs by genre"
  ([genre]
   (fetch-genre genre 10))
  ([genre limit]
   (let [url (str "https://cmd.fm/api/tracks/search/?genre=" genre "&limit=" limit)]
     (->
       url
       client/get
       :body
       (json/read-str :key-fn keyword)))))
