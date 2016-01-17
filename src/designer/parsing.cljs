(ns designer.parsing
  (:require
   [om.next :as om :include-macros true]
   [cljs.pprint :refer [pprint]]
   [sablono.core :as sab :include-macros true]
   [clojure.set :refer [rename-keys]]
   [designer.util :as util]
   [designer.geom :as geom])
  (:require-macros
    [designer.macros :refer [inspect inspect-with]]
    ))

(defn resolve-seq [st coll] (map (partial get-in st) coll))

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
  [{:keys [state ref]} k {ports :ports}]
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
               (swap! state assoc-in account-ref account)
               (swap! state update :accounts conj account-ref)
               (doseq [port ports]
                 (inspect port)
                 (swap! state update-in [:flowport/by-id (:db/id port)] #(assoc-in % [:flowport/account] account-ref))))}))

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
                 (let [;; could use: (om/db->tree [{:block/flowports [:shape :flowport/name]}] (get st :blocks) st)
                       port (get-in st ref)
                       port-shape (:shape port)
                       blocks (->> st :blocks (resolve-seq st))
                       all-port-refs (->> blocks (mapcat :block/flowports))
                       other-colliding-ports (->> all-port-refs
                                                  (filter (partial not= ref))  ;; all port refs that aren't the one just dropped
                                                  (resolve-seq st)
                                                  (filter #(-> %
                                                               :shape
                                                               (geom/circles-collide? port-shape))))]
                   (when-not (empty? other-colliding-ports)
                     (let [combined-ports (inspect (conj other-colliding-ports port))]
                       (om/transact! component [`(account/generate {:ports ~combined-ports})
                                                :blocks :accounts])))))
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
