(ns designer.parsing
  (:require
   [om.next :as om :include-macros true]
   [cljs.pprint :refer [pprint]]
   [sablono.core :as sab :include-macros true]
   [clojure.set :refer [rename-keys]]
   [designer.components :as components]
   [designer.util :as util]
   [designer.geom :as geom])
  (:require-macros
    [designer.macros :refer [inspect inspect-with]]
    ))

(defn resolve-refs [st coll] (map (partial get-in st) coll))

(defn get-by-ref
  [st root ref]
  (get-in st root))

(defn filter-colliders
  "given a collision test function,
  filter objects by whether their shape collides with given shape"
  [collide? shape objects]
  (filter #(->> % :shape (collide? shape)) objects))

(defn do-port-port-intersection!
  [st component port-ref]
  (let [;; could use: (om/db->tree [{:block/flowports [:shape :flowport/name]}] (get st :blocks) st)
        port (get-in st port-ref)
        port-shape (:shape port)
        blocks (->> st :blocks (resolve-refs st))
        other-colliding-ports (->> blocks
                           (mapcat :block/flowports)
                           (filter (partial not= port-ref))
                           (resolve-refs st)
                           (filter-colliders geom/circles-collide? port-shape)
                           )]
    (when-not (empty? other-colliding-ports)
      (let [combined-ports (conj other-colliding-ports port)]
        (om/transact! component [`(flowports/combine {:ports ~combined-ports})
                                 :blocks :accounts])))))

(defn create-account-transaction
  [account]
  (let [ref [:account/by-id (:db/id account)]]
    (fn [st]
        (-> st
            (assoc-in ref account)
            (update :accounts conj ref)))))

;; -----------------------------------------------------------------------------

(defmulti read om/dispatch)

(defmethod read :default
  [{:keys [query state]} k params]
  (let [st @state]
    {:value (om/db->tree query (get st k) st)}))

(defmulti mutate om/dispatch)

(defmethod mutate 'account/create
  [{:keys [state ref]} k {:keys [account]}]
  (let []
    {:action (fn []
               (swap! state (create-account-transaction account)))}))

(defmethod mutate 'flowport/add-to-account
  [{:keys [state]} k {:keys [account port]}]
  (let [account-ref [:account/by-id (:db/id account)]]
    {:action (fn []
               (swap! state update-in
                      [:flowport/by-id (:db/id port)]
                      (fn [st] (-> st
                                   (assoc-in [:flowport/account] account-ref)
                                   (update :shape #(merge % {:x nil :y nil}))))))}))


(defmethod mutate 'flowports/combine
  [{:keys [state ref component]} k {ports :ports}]
  (let [st @state
        num-ports (count ports)
        divisor {:x num-ports :y num-ports}
        centroid (as-> ports $
                      (map :shape $)
                      (apply merge-with + $)
                      (merge-with / $ divisor))
        account-name (-> ports first :flowport/name)
        account-id (util/rand-uuid)
        account-ref [:account/by-id account-id]
        account {:db/id account-id
                 :account/name account-name
                 :shape {:x (:x centroid)
                         :y (:y centroid)
                         :r 60}}]
    {:action (fn []
               (om/transact! component [`(account/create ~{:account account})])
               (doseq [port ports]
                 (om/transact! component [`(flowport/add-to-account ~{:account account :port port})])))}))

(defmethod mutate 'gui/start-drag-element
  [{:keys [state ref]} k {:keys [x y] :as xy}]
  {:action #(swap! state assoc :gui/drag {:ref ref
                                          :origin xy})})

(defmethod mutate 'gui/end-drag-element
  [{:keys [state ref component] :as env} k {:keys [x y] :as xy}]
  (let [st @state
        is-flowport? (= :flowport/by-id (first ref))]
    {:action (fn []
               (when is-flowport?
                 (when-not
                   (do-port-port-intersection! st component ref)
                   #_(do-port-account-intersection! st component ref)))
               (swap! state assoc :gui/drag nil)
             )}))

(defmethod mutate 'gui/move-element
  [{:keys [state ref]} k {:keys [x y] :as xy}]
  (let [st @state
        origin  {:x 0 :y 0} ;; #_(get-in st [:gui/drag :origin])
        position (as-> origin $
                       (merge-with - xy $)
                       (rename-keys $ {:x :position/x
                                       :y :position/y}))]
    {:action (fn [] (swap! state update-in ref #(-> %
                                                    (assoc-in [:shape :x] x)
                                                    (assoc-in [:shape :y] y))))}))

(defmethod mutate :default
  [{:keys [state]} k _]
  (js/console.error "invalid mutate key: " k)
  {:action #(js/console.log k)})

(def parser (om/parser {:read read
                        :mutate mutate}))
