(ns clj-cmdfm.utils
  (:gen-class))

(defn prompt
  [st]
  (println st)
  (print "$: ")
  (flush))

(defn spaces [n]
  (apply str (repeat n " ")))

(defn add-padding
  [width string]
  (let  [pad-len (- width  (count string))]
    (str string (spaces pad-len))))

(defn select-values [m ks] (map m ks))

;; this has to be done differently :/
(defn truef [& o] true)
(defn falsef [& o] false)


