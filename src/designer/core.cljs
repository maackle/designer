(ns designer.core
  (:require
   [om.next :as om :include-macros true]
   [sablono.core :as sab :include-macros true]
   [designer.state :as state])
  (:require-macros
    [designer.macros :refer [inspect inspect-with]]
    [devcards.core :as dc :refer [defcard defcard-doc defcard-om-next noframe-doc deftest dom-node]]))

(enable-console-print!)

(defonce app-state (atom state/initial-data))

(defmulti reader om/dispatch)
(defmulti mutator om/dispatch)

(def parser (om/parser {:read reader
                        :mutate mutator}))

(def reconciler (om/reconciler {:state app-state
                                :parser parser}))

