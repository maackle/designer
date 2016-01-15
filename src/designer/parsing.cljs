(ns designer.parsing
  (:require
   [om.next :as om :include-macros true]
   [sablono.core :as sab :include-macros true])
  (:require-macros
    [designer.macros :refer [inspect inspect-with]]
    ))


(defmulti read om/dispatch)

(defmethod read :default
  [{:keys [query state]} k params]
  (let [st @state]
    {:value (om/db->tree query (get st k) st)}))

(defmulti mutate om/dispatch)

(defmethod mutate 'position/move
  [{:keys [state ref]} k params]
  (let [st @state]
    {:action #(swap! state update-in ref merge (inspect params))}))

(defmethod mutate :default
  [{:keys [state]} k _]
  (js/console.error "invalid mutate key: " k)
  {:action #(js/console.log k)})

(def parser (om/parser {:read read
                        :mutate mutate}))
