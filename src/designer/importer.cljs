(ns designer.importer
  (:require
    [designer.util :as util])
  (:require-macros
    [designer.macros :refer [inspect inspect-with]]))

(defn- add-shape
  [item]
  (assoc item :shape {:x 50 :y 50 :width 50 :height 50 :r 50}))

(defn- add-uuid
  [item]
  (assoc item :db/id (util/rand-uuid)))

(def yaml-flowport-schema
  {"name" :flowport/name
   "rate" :flowport/rate
   "units" :flowport/units})

(def yaml-block-schema
  {"name" :block/name
   "image" :block/image
   "excerpt" :block/excerpt
   :block/flowports [nil yaml-flowport-schema add-shape add-uuid]})

(def yaml-schema
  {"blocks" [:blocks yaml-block-schema add-shape add-uuid]})

(defn- wrap-singleton [x] (if-not (sequential? x) [x] x))

(defn- mapv-or-apply [f x] (if (sequential? x) (mapv f x) (f x)))

(defn- apply-schema
  [schema state]

  (letfn [(reduce-entry [entry row]
                        (loop [[s & ss] entry
                               [k v] row]
                          (let [row' (cond
                                       (map? s) (if (sequential? v)
                                                  [k (mapv #(apply-schema s %) v)]
                                                  [k (apply-schema s v)])
                                       (fn? s) [k (mapv-or-apply s v)]
                                       (keyword? s) [s v]
                                       :else [k v])]
                            (if ss
                              (recur ss row')
                              row'))))]
    (into
    {}
    (for [[k v] state]
      (let [entry (wrap-singleton (get schema k))]
        (reduce-entry entry [k v])
        )))))

(defn- parse-yaml
  [yaml]
  (-> yaml
      (js/window.jsyaml.load)
      (js->clj)))

(defn- consolidate-ports
  [blocks]
  (into [] (for [block blocks]
             (let [inputs (as-> block $
                                (get $ "inputs")
                                (map #(assoc % :flowport/type :input) $))
                   outputs (as-> block $
                                 (get $ "outputs")
                                 (map #(assoc % :flowport/type :output) $))
                   ]
               (-> block
                   (assoc :block/flowports (concat inputs outputs))
                   (dissoc "inputs" "outputs"))))))

(defn yaml->state
  [yaml]
  (let [schemify (partial apply-schema yaml-schema)]
    (-> yaml
       parse-yaml
       (update "blocks" consolidate-ports)
       (schemify))))