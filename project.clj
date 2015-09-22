(defproject om-inputs "0.4.0"
  :description "Generate Web Input Form for Om/React.js, validation included."
  :url "https://github.com/hiram-madelaine/om-inputs"
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.7.0"]]}}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.122"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.omcljs/om "0.9.0"]
                 [prismatic/schema "0.4.2"]
                 [jkkramer/verily "0.6.0"]
                 [sablono "0.3.4"]
                 ;[clj-vat "0.1.2" :scope "provided"]
                 ]

  :plugins [[lein-cljsbuild "1.1.0"]
            [lein-figwheel "0.4.0"]
            [codox "0.8.10"]]

  :min-lein-version "2.5.0"

  :uberjar-name "om-inputs.jar"

  :jvm-opts ["-Xmx1g" "-server"]

  :codox {:language :clojurescript
          :include  [om-inputs.date-utils om-inputs.core]}
  :resource-paths ["examples"]
  :test-paths ["test/cljs"]
  :source-paths ["src" "examples/contact/src"]
  :clean-targets ^{:protect false} ["examples/contact/out" "target"]
  :cljsbuild {
              :builds [{:id           "dev"
                        :source-paths ["src" "examples/contact/src"]
                        :figwheel     true
                        :compiler     {:output-to     "examples/contact/out/om_inputs.js"
                                       :output-dir    "examples/contact/out"
                                       :asset-path    "contact/out"
                                       :optimizations :none
                                       :source-map    true}}
                       {:id           "simple"
                        :source-paths ["src" "examples/contact/src"]
                        :compiler     {:output-to     "examples/contact/out/main.js"
                                       :optimizations :simple
                                       :pretty-print  true
                                       :preamble      ["react/react.min.js"]
                                       :externs       ["react/externs/react.js"]}}
                       {:id           "release"
                        :source-paths ["src" "examples/contact/src"]
                        :compiler     {:output-to     "examples/contact/out/main.js"
                                       :optimizations :advanced
                                       ;:closure-warnings {:check-useless-code :on}
                                       :pretty-print  false
                                       :pseudo-names  false
                                       :preamble      []
                                       :externs       []}}]}


  :figwheel {:http-server-root "contact"
             :server-port      3450
             :css-dirs         ["examples/contact/css"]})
