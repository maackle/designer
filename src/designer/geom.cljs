(ns designer.geom
  (:require-macros [designer.macros :refer [inspect]]))


(defn is-rect? [shape] (boolean (get shape :width)))
(defn is-circle? [shape] (boolean (get shape :r)))
(defn shape-type [shape] (cond
                           (is-rect? shape) :rect
                           (is-circle? shape) :circle
                           :default nil))

(defn polar->rect
  [r rad]
  {:x (* r (js/Math.cos rad))
   :y (* r (js/Math.sin rad))})

(defn distance-squared
  [a b]
  (as-> [a b] $
        (apply merge-with - $)
        (merge-with * $ $)
        (reduce + (vals $))))

(defn square [x] (* x x))

(defn circles-collide?
  [{r1 :r :as circle1} {r2 :r :as circle2}]
  (<= (distance-squared circle1 circle2) (-> (+ r1 r2) square)))

(defn intersect-circle-ray
  ([circle ray-origin] (intersect-circle-ray circle ray-origin 0))
  ([{cx :x cy :y r :r :as circle} ray-origin padding]
   (let [{dx :x dy :y} (merge-with - circle ray-origin)
         angle (js/Math.atan2 dy dx)
         intersect (merge-with - circle (polar->rect (+ r padding) angle))]
     (select-keys intersect [:x :y]))))

#_(defn intersect-rect-ray  ;; TODO
    ([rect ray-origin] (intersect-rect-ray rect ray-origin 0))
    ([{cx :x cy :y w :width h :height :as rect} ray-origin padding]
     (let [{dx :x dy :y} (merge-with - rect ray-origin)
           angle (js/Math.atan2 dy dx)
           intersect (merge-with - rect (polar->rect (+ r padding) angle))]
       (select-keys intersect [:x :y]))))

(defn spline-string
  [{cx0 :x cy0 :y :as shape1}
   {cx1 :x cy1 :y :as shape2}]

  (letfn [(intercept
            [from to arrow?]
            (case (shape-type to)
              :circle (intersect-circle-ray to from (if arrow? 15 0))
              :rect (intersect-circle-ray to from (if arrow? 50 35))))]
    (let [{x0 :x y0 :y} (intercept shape1 shape2 false)
          {x1 :x y1 :y} (intercept shape2 shape1 true)]
      (str "M" x0 " " y0 " C " cx0 " " cy0 ", " cx1 " " cy1 ", " x1 " " y1 "")
      )))
