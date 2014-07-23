(defproject om-inputs "0.1.7"
  :description "Generate Web Input Form for Om/React.js, validation included."
  :url "https://github.com/hiram-madelaine/om-inputs"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2268"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.6.4"]
                 [prismatic/dommy "0.1.2"]
                 [prismatic/schema "0.2.4"]
                 [com.facebook/react "0.9.0"]
                 [jkkramer/verily "0.6.0"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [codox "0.8.10"]]

  :codox {:language :clojurescript
          :include [om-inputs.date-utils om-inputs.core]}
  :source-paths ["src"]

  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src"]
                        :compiler {
                                   :output-to "om_inputs.js"
                                   :output-dir "out"
                                   :optimizations :none
                                   :source-map true}}
                       {:id "simple"
                        :source-paths ["src"]
                        :compiler {
                                   :output-to "main.js"
                                   :optimizations :simple
                                   :pretty-print true
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"]}}
                       {:id "release"
                        :source-paths ["src"]
                        :compiler {
                                   :output-to "main.js"
                                   :optimizations :advanced
                                   :pretty-print true
                                   :pseudo-names true
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"]}}]})
