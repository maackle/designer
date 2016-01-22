(ns designer.css.style
  (:require
    [garden.core :refer [css]]
    [garden.def :refer [defstyles defrule defkeyframes]]
    [garden.selectors :refer [& defpseudoelement]]))

(defpseudoelement selection)

(defstyles app

  [:html :body {:margin 0}]

  [:#main-app-area
   [:svg {:width "100%"
          :height "100%"}]]

  [:text {:user-select "none"
          :pointer-events "none"}
   [(& selection) {:background "transparent"}]]


  [:.block {:cursor "pointer"}
   [:.block-shape {:fill "white"
                   :stroke "black"
                   :stroke-width "2px"}]]

  [:.flowport
   :.account {:cursor "pointer"}
   [:circle {:fill "white"
             :stroke "black"
             :stroke-width "2px"}]]

  [:.flow-arrow {:stroke "black"
                 :stroke-width "20px"}]
  )

(defstyles styles
  (list app))