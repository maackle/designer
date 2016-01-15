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
   :blocks [{:db/id 1
             :block/name "Biodigester"
             :shape {:x 200
                     :y 200
                     :width 100
                     :height 50}
             :block/ports [{:db/id 1
                            :port/type :input
                            :port/name "biomass"
                            :port/rate 20
                            :shape {:x 0
                                    :y 0
                                    :r 40}}
                           {:db/id 2
                            :port/type :output
                            :port/name "biogas"
                            :port/rate 10
                            :shape {:x 0
                                    :y 0
                                    :r 40}}
                           {:db/id 3
                            :port/type :output
                            :port/name "biogas"
                            :port/rate 10
                            :shape {:x 0
                                    :y 0
                                    :r 40}}]}]
   })

(def reconciler (om/reconciler {:state initial-data
                                :parser parser}))

(defcard-om-next first-card
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

