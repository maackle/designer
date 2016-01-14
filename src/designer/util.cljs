(ns designer.util
  (:require
    [cljs.pprint :refer [pprint]]
    [om.next :as om  :refer-macros [defui]]
    [cljs.core.async :as async :refer [<! >! put! chan close!]]
    [goog.net.XhrIo :as xhr])
  (:require-macros
    [cljs.core.async.macros :refer [go alt!]])
  (:import [goog Uri]
           [goog.net Jsonp]))

(def sym->str (comp #(subs % 1) str))

(defn input-assoc-in
  [component ks]
  (fn [e]
    (om/update-state! component assoc-in ks (.. e -target -value))))


(defn unwrap-vec
  "*UNTESTED*
  Should unwrap a vec until the first non-singleton vec is found"
  [x]
  (loop [x x]
    (if (and (vector? x) (= 1 (count x)))
      (recur (first x))
      x)))

(defn dopp
  [& xs]
  (last
    (doseq [x xs]
      (pprint x)
      x)))

(defn jsonp
  ([uri] (jsonp (chan) uri))
  ([c uri]
   (let [gjsonp (Jsonp. (Uri. uri))]
     (.send gjsonp nil #(put! c %))
     c)))


(defn http-get
  ([url]
   (http-get url nil))
  ([url xf]
   (let [ch (chan 1 xf)]
     (xhr/send url
               (fn [event]
                 (let [res (-> event .-target .getResponseText)]
                   (put! ch res)
                   (close! ch))))
     ch)))