(ns designer.core
  (:require
   [om.next :as om :include-macros true]
   [sablono.core :as sab :include-macros true]
   [designer.components :refer [Field]]
   [designer.parsing :refer [parser]])
  (:require-macros
    [designer.macros :refer [inspect inspect-with]]
   [devcards.core :as dc :refer [defcard defcard-doc defcard-om-next noframe-doc deftest dom-node]]))

(enable-console-print!)


(def initial-data
  {:gui/drag nil
   :account/by-id {"water" {:db/id "water"
                            :account/name "Water"
                            :shape {:x 300
                                    :y 300
                                    :r 60}
                            }}
   :blocks [[:block/by-id 1]]
   :accounts [[:account/by-id "water"]]
   :block/by-id {1 {:db/id 1
                    :block/name "Biodigester"
                    :shape {:x 200
                            :y 200
                            :width 100
                            :height 50}
                    :block/ports [[:flowport/by-id 1]
                                  [:flowport/by-id 2]
                                  [:flowport/by-id 3]]}}
   :flowport/by-id {1 {:db/id 1
                       :flowport/type :input
                       :flowport/name "biomass"
                       :flowport/rate 20
                       :flowport/account [:account/by-id "water"]
                       :shape {:x 100
                               :y 50
                               :r 40}}
                    2 {:db/id 2
                       :flowport/type :output
                       :flowport/name "biogas"
                       :flowport/rate 10
                       :flowport/account [:account/by-id "water"]
                       :shape {:x 200
                               :y 50
                               :r 40}}
                    3 {:db/id 3
                       :flowport/type :output
                       :flowport/name "biogas"
                       :flowport/rate 10
                       :shape {:x 150
                               :y 400
                               :r 40}}}
   })

(def reconciler (om/reconciler {:state (atom initial-data)
                                :parser parser}))

(defcard-om-next app-test
  Field
  reconciler
  {:inspect-data true})

(defn main []
  ;; conditionally start the app based on wether the #main-app-area
  ;; node is on the page
  (if-let [node (.getElementById js/document "main-app-area")]
    (js/React.render (sab/html [:div "This is working"]) node)))

(main)

;; remember to run lein figwheel and then browse to
;; http://localhost:3449/cards.html

