(ns designer.components
  (:require
   [om.next :as om  :refer-macros [defui]]
   [om.dom]
   [sablono.core :as sab :include-macros true]
   [designer.util :refer []]
   [goog.events :as events]
   [goog.dom :as dom])
  (:require-macros
    [designer.macros :refer [inspect inspect-with]]
    [devcards.core :as dc :refer [defcard deftest]]))


;; -----------------------------------------------------------------------------

(def params {:block {:width 80
                     :height 40}
             :flowport {:radius 30
                        :offset 130}})

(defn drag-handlers
  [component]
  (let []
    (letfn [(get-svg [e] (.. e -target -nearestViewportElement)) ;; TODO: this apparently is deprecated
            (move [e]
                  (let [target (.. e -target)
                        x (.. e -clientX)
                        y (.. e -clientY)]
                    (om/transact! component
                                  `[(position/move ~{:position/x x
                                                     :position/y y})])))
            (up [e]
                (events/unlisten (get-svg e) "mousemove" move))

            (down [e]
                  (. e stopPropagation)
                  (let [svg (get-svg e)]
                    (events/listen svg "mousemove" move)
                    (events/listen js/document.body "mouseup"
                                   #(events/unlisten svg "mousemove" move))))
            ]

      {:onMouseDown down
       })))

;; -----------------------------------------------------------------------------

(defui FlowPort

  static om/Ident
  (ident [this {:keys [db/id]}]
         [:flowport/by-id id])

  static om/IQuery
  (query [this]
         [:db/id :position/x :position/y :port/rate])

  Object
  (initLocalState [_]
                  {})
  (render [this]
          (let [{cx :position/x cy :position/y rate :rate} (om/props this)
                {} (om/get-state this)
                {:keys [radius]} (:block params)
                ]
            (sab/html
              [:circle.flowport (merge
                                  {:cx cx
                                   :cy cy
                                   :r radius}
                                  (drag-handlers this))]))))

(def make-flowport (om/factory FlowPort))

(defn polar->rect
  [r rad]
  [(* r (js/Math.cos rad))
   (* r (js/Math.sin rad))])

(defui Block
  static om/Ident
  (ident [this {:keys [db/id]}]
         [:block/by-id id])

  static om/IQuery
  (query [this]
         [:db/id :position/x :position/y {:block/ports (om/get-query FlowPort)}])

  Object
  (render
    [this]
    (let [{ports :block/ports
           x :position/x
           y :position/y
           id :db/id} (om/props this)
          {:keys [width height]} (:block params)
          {:keys [svg-node]} (om/get-computed this)
          {flowport-offset :offset} (:flowport params)
          num-ports (count ports)]
      (sab/html
        [:g.block (merge {:transform (str "translate(" x ", " y ")")}
                          (drag-handlers this))
         (for [[i port] (map-indexed vector ports)]
           (let [[x y] (polar->rect flowport-offset (/ (* 2 js/Math.PI i) num-ports))]  ;; TODO only do if no position given already
             [:g
              [:path {:stroke "black"
                      :fill "transparent"
                      :marker-end "url(#arrow)"
                      :d (str "M" 0 " " 0 " C " -20 " " 0 ", " (+ x 20) " " y ", " x " " y "")}]
              (make-flowport (assoc port
                              :position/x x
                              :position/y y))
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
          (let [{:keys [blocks]} (om/props this)
                ;; dom-node (om.dom/node this)
                ]
            (sab/html
              [:svg.field {:width 400
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
                (map #(-> %
                          ;; (om/computed {:svg-node dom-node})
                          make-block)
                     blocks)]]))))