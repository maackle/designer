(ns designer.parsing
  (:require
   [om.next :as om :include-macros true]
   [cljs.pprint :refer [pprint]]
   [sablono.core :as sab :include-macros true]
   [clojure.set :refer [rename-keys]])
  (:require-macros
    [designer.macros :refer [inspect inspect-with]]
    ))

(defn get-by-ref
  [st root ref]
  (get-in st root))

(defmulti read om/dispatch)

(defmethod read :default
  [{:keys [query state]} k params]
  (let [st @state]
    {:value (om/db->tree query (get st k) st)}))

(defmulti mutate om/dispatch)

(defmethod mutate 'flowports/combine
  [{:keys [state ref]} k {:keys [x y] :as xy}]
  (let []
    {:action (fn [] )}))

(defmethod mutate 'account/generate
  [{:keys [state ref]} k {port-refs :ports}]
  (let [st @state
        ports (map (partial get-in st) port-refs)
        num-ports (count ports)
        divisor {:x num-ports :y num-ports}
        centroid (as-> ports $
                      (map :shape $)
                      (apply merge-with + $)
                      (merge-with / $ divisor))
        account-name (-> ports first :flowport/name)
        account-id account-name ;; TODO - UUID or something
        account-ref [:account/by-id account-id]
        account {:db/id account-id
                 :account/name account-name
                 :shape {:x (:x centroid)
                         :y (:y centroid)
                         :r 60}}]
    {:action (fn []
               (swap! state assoc-in account-ref account)
               (swap! state update :accounts conj account-ref)
               (doseq [ref port-refs]
                 (swap! state update-in ref #(assoc-in % [:flowport/account] account-ref))))}))

(defmethod mutate 'gui/start-drag-element
  [{:keys [state ref]} k {:keys [x y] :as xy}]
  {:action #(swap! state assoc :gui/drag {:ref ref
                                          :origin xy})})

(defmethod mutate 'gui/end-drag-element
  [{:keys [state ref component] :as env} k {:keys [x y] :as xy}]

  (let [st @state
        blocks (->> st :blocks (map (partial get-in st)))
        ports (->> blocks (mapcat :block/flowports))]
    (pprint component)
    {:action (fn []
               (om/transact! component [`(account/generate {:ports ~ports})
                                        :blocks :accounts])
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
