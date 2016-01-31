(ns designer.core
  (:require
   [om.next :as om :include-macros true]
   [cljs.pprint :refer [pprint]]
   [sablono.core :as sab :include-macros true]
   [designer.components :as components]
   [designer.importer :as importer]
   [designer.state :as state]
   [designer.util :as util]
   [cljs.core.async :as async :refer [<! >! put! chan]]
   [cljsjs.js-yaml])
  (:require-macros
    [designer.macros :refer [inspect inspect-with]]
    [devcards.core :as dc :refer [defcard defcard-doc defcard-om-next noframe-doc deftest dom-node]]
    [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(defonce app-state (atom state/initial-data))

(defmulti reader om/dispatch)
(defmulti mutator om/dispatch)


(def state-chan (chan))

(defn state-loop
  [c]
  (go
    (loop [[url cb] (<! c)]
      (let [yaml (<! (util/http-get url))
            tree {:field (importer/yaml->state yaml)}
            db (om/tree->db components/Root tree true) ]
        (cb db)))))

(state-loop state-chan)

(defn sender
  [remotes cb]
  (let [url (-> (:remote/state remotes)
                first
                second
                :url)
        ]
    (put! state-chan [url cb])))

(def parser (om/parser {:read reader
                        :mutate mutator}))

(def reconciler (om/reconciler {:state app-state
                                :parser parser
                                :send sender
                                :remotes [:remote/state]}))

