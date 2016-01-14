(ns designer.components
  (:require
   [om.next :as om  :refer-macros [defui]]
   [sablono.core :as sab :include-macros true]
   [designer.util :refer []])
  (:require-macros
    [designer.macros :refer [inspect inspect-with]]
    [devcards.core :as dc :refer [defcard deftest]]))


;; -----------------------------------------------------------------------------

(def params {:block {:width 80
                     :height 40}
             :flowport {:radius 30
                        :offset 130}})

;; -----------------------------------------------------------------------------

(defui FlowPort

  static om/Ident
  (ident [this {:keys [port/id]}]
         [:flowport/by-id id])

  static om/IQuery
  (query [this]
         [:port/x :port/y :port/rate])

  Object
  (initLocalState [_]
                  {})
  (render [this]
          (let [{cx :port/x cy :port/y rate :rate} (om/props this)
                {} (om/get-state this)
                {:keys [radius]} (:block params)
                ]
            (sab/html
              [:circle.flowport {:cx cx
                                 :cy cy
                                 :r radius}]))))

(def make-flowport (om/factory FlowPort))

(defn polar->rect
  [r rad]
  [(* r (js/Math.cos rad))
   (* r (js/Math.sin rad))])

(defui Block
  static om/Ident
  (ident [this {:keys [block/id]}]
         [:block/by-id id])

  static om/IQuery
  (query [this]
         [:block/x :block/y {:block/ports (om/get-query FlowPort)}])

  Object
  (render
    [this]
    (let [{ports :ports x :block/x y :block/y} (om/props this)
          {:keys [width height]} (:block params)
          {flowport-offset :offset} (:flowport params)
          num-ports (count ports)]
      (sab/html
        [:g.block {:transform (str "translate(" x ", " y ")")}
         (for [[i port] (map-indexed vector ports)]
           (let [[x y] (polar->rect flowport-offset (/ (* 2 js/Math.PI i) num-ports))]
             [:g
              [:path {:stroke "black"
                      :fill "transparent"
                      :marker-end "url(#arrow)"
                      :d (str "M" 0 " " 0 " C " -20 " " 0 ", " (+ x 20) " " y ", " x " " y "")}]
              (make-flowport (assoc port
                              :port/x x
                              :port/y y))
              ]))
         [:rect.block {:x (- (/ width 2))
                       :y (- (/ height 2))
                       :width width
                       :height height
                       }]]))))

(def make-block (om/factory Block))

(defui Field

  static om/IQuery
  (query [_]
         [{:blocks (om/get-query Block)}])

  Object
  (render [this]
          (let [{:keys [blocks]} (om/props this)]
            (inspect blocks)
            (sab/html
              [:svg {:width 400
                     :height 400}
               [:defs
                [:marker {:id "arrow"
                          :markerWidth 4
                          :markerHeight 4
                          :viewBox "0 -5 10 10"
                          :refX 5
                          :refY 0
                          :orient "auto"
                          :markerUnits "strokeWidth"}
                 [:path {:d "M0,-5L10,0L0,5"
                         :fill "black"}]]]

               [:g.field
                (map #(-> % (assoc :block/x 200 :block/y 200) make-block) blocks)]]))))