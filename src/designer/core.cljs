(ns designer.core
  (:require
   [om.next :as om :include-macros true]
   [sablono.core :as sab :include-macros true]
   [designer.components :refer [Field]])
  (:require-macros
    [designer.macros :refer [inspect inspect-with]]
   [devcards.core :as dc :refer [defcard defcard-doc defcard-om-next noframe-doc deftest dom-node]]))

(enable-console-print!)


(defn make-block
  [name ports]
  {:name name})

(def initial-data
  {:blocks [{:block/id 1
             :block/name "Biodigester"
             :block/ports [{:port/id 1
                            :port/type :input
                            :port/name "biomass"
                            :port/rate 20}
                           {:port/id 2
                            :port/type :output
                            :port/name "biogas"
                            :port/rate 10}
                           {:port/id 3
                            :port/type :output
                            :port/name "biogas"
                            :port/rate 10}]}]
   })

(defmulti read om/dispatch)

(defmethod read :default
  [{:keys [query state]} k params]
  (inspect k)
  (let [st @state]
    (om/db->tree query (get st k) st)))

(def parser (om/parser {:read read}))

(def reconciler (om/reconciler {:state initial-data
                                :parser parser}))

(defcard first-card
  (dc/om-next-root Field)
  initial-data
  {:inspect-data true})

(defn main []
  ;; conditionally start the app based on wether the #main-app-area
  ;; node is on the page
  (if-let [node (.getElementById js/document "main-app-area")]
    (js/React.render (sab/html [:div "This is working"]) node)))

(main)

;; remember to run lein figwheel and then browse to
;; http://localhost:3449/cards.html

