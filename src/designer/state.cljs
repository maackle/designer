(ns designer.state)

(def sam-state nil)


(def constants {:block {:width 200
                        :height 100}
                :flowport {:radius 30
                           :offset 130}})

(def initial-data
  {:gui/drag nil
   :blocks [[:block/by-id 1]
            [:block/by-id 2]]
   :accounts []
   :account/by-id {}
   :block/by-id {1 {:db/id 1
                    :block/name "Biodigester"
                    :shape {:x 200
                            :y 200
                            :width (get-in constants [:block :width])
                            :height (get-in constants [:block :height])}
                    :block/flowports [[:flowport/by-id 1]
                                  [:flowport/by-id 2]
                                  [:flowport/by-id 3]]}
                 2 {:db/id 2
                    :block/name "Gasifier"
                    :shape {:x 400
                            :y 400
                            :width (get-in constants [:block :width])
                            :height (get-in constants [:block :height])}
                    :block/flowports [[:flowport/by-id 4]
                                      [:flowport/by-id 5]
                                      [:flowport/by-id 6]]}}
   :flowport/by-id {1 {:db/id 1
                       :flowport/type :input
                       :flowport/name "biomass"
                       :flowport/rate 20
                       :flowport/account nil
                       :shape {:x 100
                               :y 50
                               :r 80}}
                    2 {:db/id 2
                       :flowport/type :output
                       :flowport/name "biogas"
                       :flowport/rate 10
                       :flowport/account nil
                       :shape {:x 200
                               :y 50
                               :r 80}}
                    3 {:db/id 3
                       :flowport/type :output
                       :flowport/name "biogas"
                       :flowport/rate 10
                       :shape {:x 150
                               :y 400
                               :r 80}}
                    4 {:db/id 4
                       :flowport/type :input
                       :flowport/name "biomass"
                       :flowport/rate 20
                       :flowport/account nil
                       :shape {:x 300
                               :y 50
                               :r 80}}
                    5 {:db/id 5
                       :flowport/type :output
                       :flowport/name "biogas"
                       :flowport/rate 10
                       :flowport/account nil
                       :shape {:x 500
                               :y 120
                               :r 80}}
                    6 {:db/id 6
                       :flowport/type :output
                       :flowport/name "biogas"
                       :flowport/rate 10
                       :shape {:x 250
                               :y 400
                               :r 80}}}
   })
