(ns designer.components
  (:require
    [om.next :as om  :refer-macros [defui]]
    [om.dom]
    [sablono.core :as sab :include-macros true]
    [designer.util :as util]
    [designer.geom :as geom]
    [designer.state :refer [constants]]
    [goog.events :as events]
    [goog.dom :as dom])
  (:require-macros
    [designer.macros :refer [inspect inspect-with]]
    [devcards.core :as dc :refer [defcard deftest]]))


;; -----------------------------------------------------------------------------

(defn mouse-xy* [e]
  (let []
    {:x (.. e -clientX)
     :y (.. e -clientY)}))

(defn mouse-xy [svg e]
  (let [top (.. svg -offsetParent -offsetTop)  ;; offsetParent is not well-supported
        left (.. svg -offsetParent -offsetLeft)]
    (merge-with - (mouse-xy* e) {:x left :y top})))

(defn setup-drag-handlers!
  ([svg component]
   (setup-drag-handlers! svg component []))

  ([svg component render-keys]
   (doseq [[down-event move-event up-event] [["mousedown" "mousemove" "mouseup"]
                                             #_["touchstart" "touchmove" "touchend"]]]
     (let [node (om.dom/node component)]
       (letfn [(move-handler [e]
                             (doto e .preventDefault .stopPropagation)
                             (inspect move-event)
                             (let [target (.. e -target)
                                   xy (mouse-xy svg e)]
                               (om/transact! component
                                             (into [] (concat
                                                        [`(gui/move-element ~xy)]
                                                        render-keys)))))
               (up-handler [e]
                           (om/transact! component
                                         (into [] (concat
                                                    [`(gui/end-drag-element)]
                                                    render-keys)))
                           (events/unlisten svg move-event move-handler))
               (down-handler [e]
                             (doto e .preventDefault .stopPropagation)
                             (let [xy (mouse-xy svg e)]
                               (om/transact! component
                                             (into [] (concat
                                                        [`(gui/start-drag-element ~xy)]
                                                        render-keys)))
                               (events/listen svg move-event move-handler)
                               (events/listenOnce js/document.body up-event up-handler)))
             ]
       (events/listen node down-event down-handler))))))


;; -----------------------------------------------------------------------------

(defui Account

  static om/Ident
  (ident [this {:keys [db/id]}]
         [:account/by-id id])

  static om/IQuery
  (query [this]
         [:db/id :account/name :account/flowports
          {:shape [:x :y :r]}])

  Object
  (componentDidMount
    [this]
    (let [{:keys [svg-node]} (om/get-computed this)]
      (setup-drag-handlers! svg-node this [:blocks])))

  ;; TODO: teardown events

  (render [this]
          (let [{shape :shape
                 flowports :account/flowports
                 :as props} (om/props this)
                {:keys [x y r]} shape
                {:keys [svg-node]} (om/get-computed this)
                ]
            (sab/html
              [:g.account
               [:text {}
                "yo"]
               [:circle.account-shape
                {:cx x
                 :cy y
                 :r r}]])))
  )

(def make-account (om/factory Account))

(defn index-of
  [coll x]
  (->> coll
       (map-indexed vector)
       (filter #(= x (second %)))
       ffirst))

(defui FlowPort

  static om/Ident
  (ident [this {:keys [db/id]}]
         [:flowport/by-id id])

  static om/IQuery
  (query [this]
         [:db/id :flowport/rate :flowport/type :flowport/name
          {:flowport/account (om/get-query Account)}
          {:shape [:x :y :r]}])

  Object
  (initLocalState [_]
                  {})

  (componentDidMount
    [this]
    (let [{:keys [svg-node]} (om/get-computed this)]
      (setup-drag-handlers! svg-node this [:blocks])))

  (render [this]
          (let [{shape :shape
                 id :db/id
                 rate :flowport/rate
                 type :flowport/type
                 name :flowport/name
                 account :flowport/account
                 :as props} (om/props this)
                {:keys [x y r]} shape
                {:keys [svg-node block-shape block]} (om/get-computed this)
                {account-shape :shape} account
                sibling-ids (->> block
                                 :block/flowports
                                 (filter #(and account (= account (:flowport/account %))))
                                 (map :db/id))
                rank (index-of sibling-ids id)
                num-siblings (count sibling-ids)
                angular-offset (if rank
                                 (/ (- rank (/ num-siblings 2)) num-siblings)
                                 0)
                angular-offset (if (= :output type)
                                 (- angular-offset)
                                 angular-offset)
                opts {:angular-offset 0}
                spline (let [sh (if-not (empty? account)
                                  account-shape
                                  shape)]
                         (case type
                           :output (geom/spline-string block-shape sh opts)
                           :input (geom/spline-string sh block-shape opts)))
                ]
            (sab/html
              [:g
               [:path.flow-arrow (merge {:stroke "black"
                                         :fill "transparent"
                                         :marker-end "url(#arrow)"
                                         :d spline}
                                        )]

               (when-not account
                 [:g.flowport {:transform (str "translate(" x "," y ")")}
                  [:circle.flowport-shape
                   {:cx 0
                    :cy 0
                    :r r
                    }
                   ]

                  [:text.flowport-name
                   {:dy -15
                    :text-anchor "middle"
                    :font-size "16px"}
                   name]
                  [:text.flowport-rate
                   {:dy 10
                    :text-anchor "middle"
                    :font-size "24px"}
                   rate]
                  [:text.flowport-units
                   {:dy 20
                    :text-anchor "middle"
                    :font-size "16px"}
                   "."]])]))))

(def make-flowport (om/factory FlowPort))

(defui Block
  static om/Ident
  (ident [this {:keys [db/id]}]
         [:block/by-id id])

  static om/IQuery
  (query [this]
         [:db/id
          {:shape [:x :y :width :height]}
          {:block/flowports (om/get-query FlowPort)}])

  Object

  (componentDidMount
    [this]
    (let [{:keys [svg-node]} (om/get-computed this)]
      (setup-drag-handlers! svg-node this [:blocks])))

  (render
    [this]
    (let [{ports :block/flowports
           shape :shape
           id :db/id
           :as props} (om/props this)
          {x :x y :y} shape
          {:keys [width height]} shape
          {:keys [svg-node] :as computed} (om/get-computed this)
          computed-props {:block-shape shape
                          :block props
                          :svg-node svg-node}
          {flowport-offset :offset} (get-in constants [:flowport :offset])
          num-ports (count ports)]
      (sab/html
        [:g.block
         (for [[i port] (map-indexed vector ports)]
           (let [{x :x y :y} (geom/polar->rect flowport-offset (/ (* 2 js/Math.PI i) num-ports))
                 port (if (:shape/x port)
                        port
                        (assoc port
                          :shape/x (+ x )
                          :shape/y (+ y )))]  ;; TODO only do if no position given already
             [:g
              (make-flowport (om/computed port computed-props))
              ]))
         [:rect.block-shape {:x (+ x (- (/ width 2)))
                             :y (+ y (- (/ height 2)))
                             :width width
                             :height height
                             }]]))))

(def make-block (om/factory Block))

(defui Field

  static om/IQuery
  (query [_]
         [{:blocks (om/get-query Block)}
          {:accounts (om/get-query Account)}])

  Object
  (componentDidMount [this]
                     (om/update-state! this assoc :dom-node (om.dom/node this)))

  (render [this]
          (let [{:keys [blocks accounts]} (om/props this)
                {:keys [dom-node]} (om/get-state this)
                ]
            (sab/html
              [:svg.field {:width 900
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

               (when dom-node ;; don't render if not mounted yet
                 [:g.field
                (for [block blocks]
                  (-> block
                      (om/computed {:svg-node dom-node})
                      make-block) )
                (for [account accounts]
                  (-> account
                      (om/computed {:svg-node dom-node})
                      make-account))])]))))

(def Root Field)