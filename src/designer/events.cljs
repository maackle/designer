(ns designer.events
  (:require
    [om.dom]
    [om.next :as om :refer-macros [defui]]
    [jayq.core :as jq]
    [designer.geom :as geom]
    [designer.util :as util]
    [goog.events :as events])
  (:require-macros
    [designer.macros :refer [inspect]]))

(defn is-block?
  "Check for blockness with duck-typing"
  [c]
  (boolean (get c :block/flowports)))

(defn- get-arrow-refs
  [component]
  (let [props (om/props component)
        [block ports] (if (-> props is-block?)
                        [props (-> props :block/flowports)]
                        [(-> component
                             (om/get-computed)
                             :block) [props]])]
    (for [port ports]
      (util/arrow-ref block port))))


(defn mouse-xy* [e]
  (let []
    {:x (.. e -clientX)
     :y (.. e -clientY)}))

(defn mouse-xy [svg e]
  (let [top (.. svg -offsetParent -offsetTop)  ;; offsetParent is not well-supported
        left (.. svg -offsetParent -offsetLeft)]
    (merge-with - (mouse-xy* e) {:x left :y top})))

(defn setup-drag-handlers!
  ([field component]
   (setup-drag-handlers! field component nil []))

  ([field component handle-selector render-keys]

     (if-let [base-node (om.dom/node component)]
       (let [
             svg (om.dom/node field)
           drag-node (if handle-selector
                       (-> base-node jq/$ (jq/find handle-selector) (.get 0))
                       base-node)
           handle-mousemove (fn [e]
                          (doto e .preventDefault .stopPropagation)
                          (let [target (.. e -target)
                                arrow-refs (-> component get-arrow-refs)
                                {:keys [x y] :as xy} (mouse-xy svg e)
                                baseVal (.. drag-node -transform -baseVal)
                                xf (. baseVal getItem 0)]
                            #_(doseq [arrow-ref arrow-refs]
                              (let [arrow (om/react-ref field arrow-ref)
                                    spline (geom/spline-string nil nil {})]  ;; TODO: get/set spline
                                (. arrow setAttribute "d" spline)))
                            (. xf setTranslate x y) ))
           handle-touchmove handle-mousemove
           handle-mouseup (fn [e]
                        (doto e .preventDefault .stopPropagation)
                        (let [xy (mouse-xy svg e)]
                          (om/transact! component
                                        (into [] (concat
                                                   [`(gui/end-drag-element ~xy)]
                                                   render-keys))))
                        (events/unlisten svg "mousemove" handle-mousemove))
           handle-touchend (fn [e]
                        (doto e .preventDefault .stopPropagation)
                        (let [xy (mouse-xy svg e)]
                          (om/transact! component
                                        (into [] (concat
                                                   [`(gui/end-drag-element ~xy)]
                                                   render-keys))))
                        (events/unlisten svg "touchmove" handle-touchmove))
           handle-mousedown (fn [e]
                          (doto e .preventDefault .stopPropagation)
                          (let [xy (mouse-xy svg e)]
                            (om/transact! component
                                          (into [] (concat
                                                     [`(gui/start-drag-element ~xy)]
                                                     render-keys)))
                            (events/listen svg "mousemove" handle-mousemove)
                            (events/listenOnce js/document.body "mouseup" handle-mouseup)))
           handle-touchstart (fn [e]
                            (doto e .preventDefault .stopPropagation)
                            (let [xy (mouse-xy svg e)]
                              (om/transact! component
                                            (into [] (concat
                                                       [`(gui/start-drag-element ~xy)]
                                                       render-keys)))
                              (events/listen svg "touchmove" handle-touchmove)
                              (events/listenOnce js/document.body "touchend" handle-touchend)))]

         (events/listen drag-node "mousedown" handle-mousedown)
         (events/listen drag-node "touchstart" handle-touchstart))
       (js/console.warn "missing node"))))

