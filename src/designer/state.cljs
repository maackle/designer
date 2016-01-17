(ns designer.state)



(def initial-data
  {:gui/drag nil
   :blocks [[:block/by-id 1]]
   :accounts []
   :account/by-id {}
   :block/by-id {1 {:db/id 1
                    :block/name "Biodigester"
                    :shape {:x 200
                            :y 200
                            :width 100
                            :height 50}
                    :block/flowports [[:flowport/by-id 1]
                                  [:flowport/by-id 2]
                                  [:flowport/by-id 3]]}}
   :flowport/by-id {1 {:db/id 1
                       :flowport/type :input
                       :flowport/name "biomass"
                       :flowport/rate 20
                       :flowport/account nil
                       :shape {:x 100
                               :y 50
                               :r 40}}
                    2 {:db/id 2
                       :flowport/type :output
                       :flowport/name "biogas"
                       :flowport/rate 10
                       :flowport/account nil
                       :shape {:x 200
                               :y 50
                               :r 40}}
                    3 {:db/id 3
                       :flowport/type :output
                       :flowport/name "biogas"
                       :flowport/rate 10
                       :shape {:x 150
                               :y 400
                               :r 40}}}
   })
