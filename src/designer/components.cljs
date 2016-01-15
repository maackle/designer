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

(defn mouse-xy* [e]
  (let []
    {:x (.. e -clientX)
     :y (.. e -clientY)}))

(defn mouse-xy [svg e]
  (let [top (.. svg -offsetParent -offsetTop)  ;; offsetParent is not well-supported
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

(defn is-rect? [shape] (boolean (get shape :width)))
(defn is-circle? [shape] (boolean (get shape :r)))
(defn shape-type [shape] (cond
                           (is-rect? shape) :rect
                           (is-circle? shape) :circle
                           :default nil))

(defn polar->rect
  [r rad]
  {:x (* r (js/Math.cos rad))
   :y (* r (js/Math.sin rad))})

(defn intersect-circle-ray
  ([circle ray-origin] (intersect-circle-ray circle ray-origin 0))
  ([{cx :x cy :y r :r :as circle} ray-origin padding]
   (let [{dx :x dy :y} (merge-with - circle ray-origin)
         angle (js/Math.atan2 dy dx)
         intersect (merge-with - circle (polar->rect (+ r padding) angle))]
     (select-keys intersect [:x :y]))))

(defn spline-string
  [{cx0 :x cy0 :y :as shape1}
   {cx1 :x cy1 :y :as shape2}]
  (let [{x0 :x y0 :y} (intersect-circle-ray shape2 shape1 40)
        {x1 :x y1 :y} (intersect-circle-ray shape1 shape2 15)]
    (str "M" x0 " " y0 " C " cx0 " " cy0 ", " cx1 " " cy1 ", " x1 " " y1 "")
    ))


;; -----------------------------------------------------------------------------

(defui FlowPort

  static om/Ident
  (ident [this {:keys [db/id]}]
         [:flowport/by-id id])

  static om/IQuery
  (query [this]
         [:db/id :port/rate
          {:shape [:x :y :r]}])

  Object
  (initLocalState [_]
                  {})
  (render [this]
          (let [{shape :shape rate :port/rate} (om/props this)
                {:keys [x y r]} shape
                {:keys [svg-node block-shape]} (om/get-computed this)
                {:keys [radius]} (:block params)
                ]
            (sab/html
              [:g
               #_[:line {:stroke "black"
                       :stroke-width 10
                       :x1 0 :y1 0
                       :x2 100 :y2 50
                       :marker-end "url(#arrow)"}]
               [:path {:stroke "black"
                      :fill "transparent"
                      :marker-end "url(#arrow)"
                      :d (spline-string shape block-shape)}]
               [:circle.flowport (merge
                                  {:cx x
                                   :cy y
                                   :r r}
                                  (drag-handlers svg-node this))]]))))

(def make-flowport (om/factory FlowPort))

(defui Block
  static om/Ident
  (ident [this {:keys [db/id]}]
         [:block/by-id id])

  static om/IQuery
  (query [this]
         [:db/id
          {:shape [:x :y :width :height]}
          {:block/ports (om/get-query FlowPort)}])

  Object
  (render
    [this]
    (let [{ports :block/ports
           shape :shape
           id :db/id} (om/props this)

          {x :x y :y} shape
          {:keys [width height]} (:block (inspect params))
          {:keys [svg-node] :as computed} (om/get-computed this)
          computed-props {:block-shape shape
                          :svg-node svg-node}
          {flowport-offset :offset} (:flowport params)
          num-ports (count ports)]
      (sab/html
        [:g.block (merge {} (drag-handlers svg-node this))
         (for [[i port] (map-indexed vector ports)]
           (let [{x :x y :y} (polar->rect flowport-offset (/ (* 2 js/Math.PI i) num-ports))
                 port (if (:shape/x port)
                        port
                        (assoc port
                          :shape/x (+ x )
                          :shape/y (+ y )))]  ;; TODO only do if no position given already
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
              [:svg.field {:width 600
                           :height 900}
               [:defs
                {:dangerouslySetInnerHTML
                 {:__html "
                          <marker id=\"arrow\"
                                  markerWidth=\"3\"
                                  markerHeight=\"3\"
                                  refX=\"5\"
                                  refY=\"0\"
                                  viewBox=\"0 -5 10 10\"
                                  orient=\"auto\"
                                  markerUnits=\"strokeWidth\">

                            <path d=\"M0,-5L10,0L0,5\"
                                  fill=\"black\" />

                          </marker>
                          "
                          }}]

               [:g.field
                (map #(-> %
                          (om/computed {:svg-node dom-node})
                          make-block)
                     blocks)]]))))