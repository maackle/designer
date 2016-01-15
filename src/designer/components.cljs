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

(defn get-svg [e] (.. e -target -nearestViewportElement)) ;; TODO: this apparently is deprecated

(defn mouse-xy* [e]
  (let []
    {:x (.. e -clientX)
     :y (.. e -clientY)}))

(defn mouse-xy [svg e]
  (let [top (.. svg -offsetParent -offsetTop)
        left (.. svg -offsetParent -offsetLeft)]
    (merge-with - (mouse-xy* e) {:x left :y top})))

(defn drag-handlers
  [svg component]
  (let []
    (letfn [
            (move [e]
                  (. e stopPropagation)
                  (let [target (.. e -target)
                        xy (mouse-xy svg e)]
                    (om/transact! component
                                  `[(gui/move-element ~xy)])))
            (down [e]
                  (. e stopPropagation)
                  (let [
                        xy (mouse-xy svg e)
                        up #(events/unlisten svg "mousemove" move)]
                    (om/transact! component
                                  `[(gui/start-drag-element ~xy)])
                    (events/listen svg "mousemove" move)
                    (events/listen js/document.body "mouseup" up)))
            ]

      {:onMouseDown down
       })))

(defn polar->rect
  [r rad]
  [(* r (js/Math.cos rad))
   (* r (js/Math.sin rad))])

(defn spline-string
  [[x0 y0] [x1 y1]]
  (str "M" x0 " " y0 " C " (- x0 100) " " y0 ", " (+ x1 100) " " y1 ", " x1 " " y1 ""))


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
                {:keys [svg-node block-position]} (om/get-computed this)
                {:keys [radius]} (:block params)
                ]
            (sab/html
              [:g
               [:path {:stroke "black"
                      :fill "transparent"
                      :marker-end "url(#arrow)"
                      :d (spline-string [cx cy] [(:x block-position) (:y block-position)])}]
               [:circle.flowport (merge
                                  {:cx cx
                                   :cy cy
                                   :r radius}
                                  (drag-handlers svg-node this))]]))))

(def make-flowport (om/factory FlowPort))

(defui Block
  static om/Ident
  (ident [this {:keys [db/id]}]
         [:block/by-id id])

  static om/IQuery
  (query [this]
         [:db/id :position/x :position/y
          {:block/ports (om/get-query FlowPort)}])

  Object
  (render
    [this]
    (let [{ports :block/ports
           x :position/x
           y :position/y
           id :db/id} (om/props this)
          {:keys [width height]} (:block (inspect params))
          {:keys [svg-node] :as computed} (om/get-computed this)
          computed-props {:block-position {:x x :y y}
                          :svg-node svg-node}
          {flowport-offset :offset} (:flowport params)
          num-ports (count ports)]
      (sab/html
        [:g.block (merge {} (drag-handlers svg-node this))
         (for [[i port] (map-indexed vector ports)]
           (let [[x y] (polar->rect flowport-offset (/ (* 2 js/Math.PI i) num-ports))
                 port (if (:position/x port)
                        port
                        (assoc port
                          :position/x (+ x )
                          :position/y (+ y )))]  ;; TODO only do if no position given already
             [:g
              (make-flowport (om/computed port computed-props))
              ]))
         [:rect.block {:x (+ x (- (/ width 2)))
                       :y (+ y (- (/ height 2)))
                       :width width
                       :height height
                       }]]))))

(def make-block (om/factory Block))

(defui Field

  static om/IQuery
  (query [_]
         [{:blocks (om/get-query Block)}])

  Object
  (componentDidMount [this]
                     (om/update-state! this assoc :dom-node (om.dom/node this)))

  (render [this]
          (let [{:keys [blocks]} (om/props this)
                {:keys [dom-node]} (om/get-state this)
                ]
            (sab/html
              [:svg.field {:width 900
                           :height 900}
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
                          (om/computed {:svg-node dom-node})
                          make-block)
                     blocks)]]))))