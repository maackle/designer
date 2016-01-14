(ns designer.components-drag
  (:require
   [om.next :as om  :refer-macros [defui]]
   [sablono.core :as sab :include-macros true])
  (:require-macros
   [devcards.core :as dc :refer [defcard deftest]]))


;; -----------------------------------------------------------------------------


;; -----------------------------------------------------------------------------

(defui Circle

  Object
  (initLocalState [_]
                  {:drag-x 10
                   :drag-y 10
                   :drag-start {:x 0 :y 0}
                   :dragging false})
  (render [this]
          (let [{x :x
                 y :y} (om/props this)
                {:keys [drag-x drag-y dragging?]} (om/get-state this)
                startDrag (fn [e]
                            (let [mx (.. e -clientX)
                                  my (.. e -clientY)
                                     rect (.getBoundingClientRect (.. e -target))
                                     target (.. e -target)
                                     cx (/ (+ (.. rect -left) (.. rect -right)) 2)
                                     cy (/ (+ (.. rect -top) (.. rect -bottom)) 2)
                                     ;; cx (js/parseInt (.getAttribute target "cx") 10)
                                     ;; cy (js/parseInt (.getAttribute target "cy") 10)
                                     ]
                              (js/console.log mx my)
                              (om/update-state! this assoc :drag-start {:x mx :y my})
                              (om/update-state! this assoc :dragging? true)))

                handleDrag (fn [e]
                             (when dragging?
                               (let [mx (.. e -clientX)
                                     my (.. e -clientY)
                                     {:keys [drag-start]} (om/get-state this)
                                     {x0 :x y0 :y} drag-start
                                     rect (.getBoundingClientRect (.. e -target))
                                     target (.. e -target)
                                     cx (/ (+ (.. rect -left) (.. rect -right)) 2)
                                     cy (/ (+ (.. rect -top) (.. rect -bottom)) 2)
                                     ;; cx (js/parseInt (.getAttribute target "cx") 10)
                                     ;; cy (js/parseInt (.getAttribute target "cy") 10)
                                     ]
                                 (js/console.log x y mx my x0 y0)
                                 (om/update-state! this assoc :drag-x (+ x mx (- x0)))
                                 (om/update-state! this assoc :drag-y (+ y my (- y0))))
                               ))]
            (sab/html
              [:circle {:cx drag-x
                        :cy drag-y
                        :r 10
                        :onMouseDown startDrag
                        :onMouseUp #(om/update-state! this assoc :dragging? false)
                        :onMouseMove handleDrag}]))))

(def make-circle (om/factory Circle))

(defui Field
  Object
  (render [this]
          (let [{circles :circles} (om/props this)]
          (sab/html
            [:div [:svg
             [:g.field
              (map make-circle circles)]]]))))