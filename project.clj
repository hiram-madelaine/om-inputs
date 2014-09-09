(defproject om-inputs "0.3.1-SNAPSHOT"
  :description "Generate Web Input Form for Om/React.js, validation included."
  :url "https://github.com/hiram-madelaine/om-inputs"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [om "0.7.1"]
                 [prismatic/dommy "0.1.2"]
                 [prismatic/schema "0.2.6"]
                 [com.facebook/react "0.11.1"]
                 [jkkramer/verily "0.6.0"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
            [codox "0.8.10"]]

  :codox {:language :clojurescript
          :include [om-inputs.date-utils om-inputs.core]}
  :source-paths ["src"]


  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src" "examples/contact/src"]
                        :compiler {:output-to "examples/contact/om_inputs.js"
                                   :output-dir "examples/contact/out"
                                   :optimizations :none
                                   :source-map true}}
                       {:id "simple"
                        :source-paths ["src"]
                        :compiler {:output-to "main.js"
                                   :optimizations :simple
                                   :pretty-print true
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"]}}
                       {:id "release"
                        :source-paths ["src"]
                        :compiler {:output-to "main.js"
                                   :optimizations :advanced
                                   ;:closure-warnings {:check-useless-code :on}
                                   :pretty-print false
                                   :pseudo-names false
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"]}}]})
