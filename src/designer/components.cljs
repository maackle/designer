(ns designer.components
  #_(:use
    [jayq.core :only ($ children)])
  (:require
    [om.next :as om  :refer-macros [defui]]
    [om.dom]
    [jayq.core :as jq]
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

(defn get-drag-handle-node
  "If base-node has class .drag-handle, return it.
  Else if a single child has the class, return that."
  [base-node]
  (letfn [(find-node [node]
                     (as-> (jq/$ node) $
                           (jq/children $ :.drag-handle)
                           (do (js/console.log $) $)
                           (if (not= 1 (inspect (.-length $)))
                             (throw "found multiple .drag-handle nodes")
                             (.get $ 0))))]
    (if (.hasClass (jq/$ base-node) "drag-handle")
                        base-node
                        (find-node base-node))) )

(defn setup-drag-handlers!
  ([svg component]
   (setup-drag-handlers! svg component []))

  ([svg component render-keys]
   (doseq [[down-event move-event up-event] [["mousedown" "mousemove" "mouseup"]
                                             #_["touchstart" "touchmove" "touchend"]]]
     (let [base-node (om.dom/node component)
           drag-node (get-drag-handle-node base-node)]
       (letfn [(move-handler [e]
                             (doto e .preventDefault .stopPropagation)
                             (let [target (.. e -target)
                                   {:keys [x y] :as xy} (mouse-xy svg e)
                                   baseVal (.. drag-node -transform -baseVal)
                                   xf (. baseVal getItem 0)]
                               (. xf setTranslate x y)
                               #_(om/transact! component
                                             (into [] (concat
                                                        [`(gui/move-element ~xy)]
                                                        render-keys)))))
               (up-handler [e]
                           (let [xy (mouse-xy svg e)]
                             (om/transact! component
                                         (into [] (concat
                                                    [`(gui/end-drag-element ~xy)]
                                                    render-keys))))
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
       (events/listen drag-node down-event down-handler))))))


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
      (setup-drag-handlers! svg-node this [:blocks :accounts])))

  ;; TODO: teardown events

  (render [this]
          (let [{shape :shape
                 flowports :account/flowports
                 :as props} (om/props this)
                {:keys [x y r]} shape
                {:keys [svg-node]} (om/get-computed this)
                ]
            (sab/html
              [:g.account.drag-handle {:transform (str "translate(" x "," y ")")}
               [:text {}
                "yo"]
               [:circle.account-shape
                {:cx 0
                 :cy 0
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
                {:keys [svg-node]} (om/get-computed this) ]
            (sab/html
              (when-not account
                [:g.flowport.drag-handle {:transform (str "translate(" x "," y ")")}
                 [:circle.flowport-shape {:cx 0
                                          :cy 0
                                          :r r }]
                 [:text.flowport-name {:dy -15
                                       :text-anchor "middle"
                                       :font-size "16px"}
                  name]
                 [:text.flowport-rate {:dy 10
                                       :text-anchor "middle"
                                       :font-size "24px"}
                  rate]
                 [:text.flowport-units {:dy 20
                                        :text-anchor "middle"
                                        :font-size "16px"}
                  "."]])
              ))))

(def make-flowport (om/factory FlowPort))

(defn spline-arrow
  [shape-from shape-to]
  (let [spline (geom/spline-string shape-from shape-to {})]
    [:path.flow-arrow {:stroke "black"
                       :fill "transparent"
                       :marker-end "url(#arrow)"
                       :d spline} ] ))

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
        [:g
         [:g.flowports
          (for [port ports]
            (-> port
                (om/computed computed-props)
                make-flowport))]
         [:g.block.drag-handle {:transform (str "translate(" x "," y ")")}

          [:rect.block-shape {:x (- (/ width 2))
                              :y (- (/ height 2))
                              :width width
                              :height height
                              }]]]))))

(def make-block (om/factory Block))

(defn block-port-arrow
  [block port]
  (let [{block-shape :shape} block
        {port-type :flowport/type
         account :flowport/account} port
        dest-shape (if account
                     (:shape account)
                     (:shape port))]
    (case port-type
      :output (spline-arrow block-shape dest-shape)
      :input (spline-arrow dest-shape block-shape))))

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
                {:keys [dom-node]} (om/get-state this) ]
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
                  [:g.arrows
                   (for [block blocks port (:block/flowports block)]
                     (block-port-arrow block port))
                   ]
                  [:g.blocks
                   (for [block blocks]
                     (-> block
                         (om/computed {:svg-node dom-node})
                         make-block) )]
                  #_[:g.flowports
                   (for [block blocks port (:block/flowports block)]
                     )]
                  [:g.accounts
                   (for [account accounts]
                     (-> account
                         (om/computed {:svg-node dom-node})
                         make-account))]])]))))

(def Root Field)