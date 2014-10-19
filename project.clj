(defproject om-inputs "0.3.4-SNAPSHOT"
  :description "Generate Web Input Form for Om/React.js, validation included."
  :url "https://github.com/hiram-madelaine/om-inputs"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371" :scope "provided"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [om "0.7.3"]
                 [prismatic/dommy "0.1.2"]
                 [prismatic/schema "0.3.0"]
                 [com.facebook/react "0.11.1"]
                 [jkkramer/verily "0.6.0-SNAPSHOT"]
                 ;           [figwheel "0.1.4-SNAPSHOT"]
                 ;[environ "1.0.0"]
                 ;[com.cemerick/piggieback "0.1.3"]
                 ;[weasel "0.4.0-SNAPSHOT"]
                 ;[leiningen "2.5.0"]
                 ]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
            [codox "0.8.10"]
            ;[lein-environ "1.0.0"]
            ]

  :min-lein-version "2.5.0"

  :uberjar-name "om-inputs.jar"

  :codox {:language :clojurescript
          :include [om-inputs.date-utils om-inputs.core]}


  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src" "examples/contact/src"]
                        :compiler {:output-to "examples/contact/om_inputs.js"
                                   :output-dir "examples/contact/out"
                                   :optimizations :none
                                   :source-map true}}
                       {:id "simple"
                        :source-paths ["src" "examples/contact/src"]
                        :compiler {:output-to "main.js"
                                   :optimizations :simple
                                   :pretty-print true
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"]}}
                       {:id "release"
                        :source-paths ["src" "examples/contact/src"]
                        :compiler {:output-to "main.js"
                                   :optimizations :advanced
                                   ;:closure-warnings {:check-useless-code :on}
                                   :pretty-print false
                                   :pseudo-names false
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"]}}]}


  #_(:profiles {:dev     {:repl-options {:init-ns          try-chestnut.server
                                       :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                        :plugins      [[lein-figwheel "0.1.4-SNAPSHOT"]]
                        :figwheel     {:http-server-root "public"
                                       :port             3449
                                       :css-dirs         ["resources/public/css"]}
                        :env          {:is-dev true}
                        :cljsbuild    {:builds {:app {:source-paths ["env/dev/cljs"]}}}}

              :uberjar {:hooks       [leiningen.cljsbuild]
                        :env         {:production true}
                        :omit-source true
                        :aot         :all
                        :cljsbuild   {:builds {:app
                                                {:source-paths ["env/prod/cljs"]
                                                 :compiler
                                                               {:optimizations :advanced
                                                                :pretty-print  false}}}}}}))
