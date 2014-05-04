(defproject clj-cmdfm "0.0.0-SNAPSHOT"
  :description "experimenting with clojure and cmdfm"
  :url "http://github.com/ghedamat/clj-cmdfm"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.4"]
                 [org.clojure/core.async  "0.1.301.0-deb34a-alpha"]
                 [org.clojure/tools.cli "0.3.1"]
                 [clj-http "0.9.1"]]
  :main clj-cmdfm.cli)
