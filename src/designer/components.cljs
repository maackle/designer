(ns designer.components
  #_(:use
    [jayq.core :only ($ children)])
  (:require
    [om.next :as om  :refer-macros [defui]]
    [om.dom]
    [sablono.core :as sab :include-macros true]
    [designer.events :refer [setup-drag-handlers!]]
    [designer.util :as util]
    [designer.geom :as geom]
    [designer.state :refer [constants]]
    [goog.dom :as dom])
  (:require-macros
    [designer.macros :refer [inspect inspect-with]]
    [devcards.core :as dc :refer [defcard deftest]]))


;; -----------------------------------------------------------------------------

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
    (let [{:keys [field-component]} (om/get-computed this)]
      (setup-drag-handlers! field-component this nil [:blocks :accounts])))

  ;; TODO: teardown events

  (render [this]
          (let [{shape :shape
                 flowports :account/flowports
                 :as props} (om/props this)
                {:keys [x y r]} shape
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
    (let [{:keys [field-component]} (om/get-computed this)]
      (setup-drag-handlers! field-component this nil [:blocks])))

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
                [:g.flowport.drag-handle {:transform (str "translate(" x "," y ")")
                                          :key id}
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
    (let [{:keys [field-component]} (om/get-computed this)]
      (setup-drag-handlers! field-component this ".block.drag-handle" [:blocks])))

  (render
    [this]
    (let [{ports :block/flowports
           shape :shape
           id :db/id
           :as props} (om/props this)
          {x :x y :y} shape
          {:keys [width height]} shape
          {:keys [svg-node field-component] :as computed} (om/get-computed this)
          computed-props {:block-shape shape
                          :block props
                          :svg-node svg-node
                          :field-component field-component}
          {flowport-offset :offset} (get-in constants [:flowport :offset])
          num-ports (count ports)]
      (sab/html
        [:g.block-and-ports {:key id}
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

(defn spline-arrow
  [from to & [extra-props]]
  (let [spline (geom/spline-string (:shape from) (:shape to) {})]
    [:path.flow-arrow (merge
                        {:stroke "black"
                         :fill "transparent"
                         :marker-end "url(#arrow)"
                         :d spline }
                        extra-props) ] ))

(defn block-port-arrow
  [block port]
  (let [{block-shape :shape} block
        {port-type :flowport/type
         account :flowport/account} port
        dest (if account
                     account
                     port)
        [from to] (case port-type
                    :output [block dest]
                    :input [dest block])
        arrow-key (util/arrow-ref block port)]
    (spline-arrow from to {:key arrow-key
                           :ref arrow-key})))

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
                {:keys [dom-node]} (om/get-state this)]
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
                         (om/computed {:svg-node dom-node
                                       :field-component this})
                         make-block) )]
                  [:g.accounts
                   (for [account accounts]
                     (-> account
                         (om/computed {:svg-node dom-node
                                       :field-component this})
                         make-account))]])]))))

(def make-field (om/factory Field))

(defui Root

  static om/IQueryParams
  (params [_]
          {:url "blox.yml"})

  static om/IQuery
  (query [_]
         `[({:field ~(om/get-query Field)} {:url ?url})])

  Object
  (render [this]
          (let [{:keys [field]} (om/props this)]
            (make-field field))))

