(ns designer.macros)
;; from https://github.com/shaunlebron/How-To-Debug-CLJS/blob/master/src/example/macros.clj

(defn- inspect-1
  [formatter expr]
  `(let [result# ~expr
         fmt# ~formatter]
     (js/console.info (str (fmt# '~expr) " => \n" (fmt# result#)))
     result#))

(defmacro inspect [& exprs]
  `(do ~@(map (partial inspect-1 'pr-str) exprs)))

(defmacro inspect-with [formatter & exprs]
  `(do ~@(map (partial inspect-1 formatter) exprs)))