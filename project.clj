(defproject designer "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [devcards "0.2.1-4"]
                 [sablono "0.4.0"]
                 [jayq "2.5.4"]
                 [org.omcljs/om "1.0.0-alpha30"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [cljsjs/js-yaml "3.3.1-0"]
                 [prismatic/schema "1.0.4"]
                 ]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-1"]
            [lein-garden "0.2.6"]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "target"]

  :source-paths ["src"]

  :cljsbuild {:builds [{:id "devcards"
                        :source-paths ["src"]
                        :figwheel { :devcards true } ;; <- note this
                        :compiler { :main       "designer.cards"
                                    :asset-path "js/compiled/devcards_out"
                                    :output-to  "resources/public/js/compiled/designer_devcards.js"
                                    :output-dir "resources/public/js/compiled/devcards_out"
                                    :source-map-timestamp true }}
                       {:id "dev"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler {:main       "designer.app"
                                   :asset-path "js/compiled/out"
                                   :output-to  "resources/public/js/compiled/designer.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :source-map-timestamp true }}
                       {:id "prod"
                        :source-paths ["src"]
                        :compiler {:main       "designer.app"
                                   :asset-path "js/compiled/out"
                                   :output-to  "resources/public/js/compiled/designer.js"
                                   :optimizations :simple
                                   }}]}

  :garden {:builds [{:id "design"
                     :source-paths ["src"]
                     :stylesheet designer.css.style/styles
                     :compiler {:output-to "resources/public/css/styles.css"
                                :pretty-print true}}
                    #_{:id "prod"
                     :source-paths ["src"]
                     :stylesheet designer.css.style/styles
                     :compiler {:output-to "dist/styles.min.css"
                                :pretty-print? false}}]}

  :figwheel {:css-dirs ["resources/public/css"]
             :server-port 7777})
