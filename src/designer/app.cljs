(ns designer.app
  (:require
   [om.next :as om :include-macros true]
   [sablono.core :as sab :include-macros true]
   [designer.components :as components]
   [designer.core :as core]
   [designer.state :as state]
   [designer.parsing :as parsing])
  (:require-macros
    [designer.macros :refer [inspect inspect-with]]
    [devcards.core :as dc :refer [defcard defcard-doc defcard-om-next noframe-doc deftest dom-node]]))

(enable-console-print!)

(defn main []
  ;; conditionally start the app based on wether the #main-app-area
  ;; node is on the page
  (if-let [node (.getElementById js/document "main-app-area")]
    (om/add-root!
      core/reconciler
      components/Field
      node
      )))

(main)

;; remember to run lein figwheel and then browse to
;; http://localhost:3449/cards.html

