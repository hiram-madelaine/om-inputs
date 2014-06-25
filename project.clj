(defproject om-inputs "0.1.3-SNAPSHOT"
  :description "Try to handle form of inputs in an Om app"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.6.4"]
                 [prismatic/dommy "0.1.2"]
                 [prismatic/schema "0.2.3"]
                 [com.facebook/react "0.9.0"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "om-inputs"
              :source-paths ["src"]
              :compiler {
                :output-to "om_inputs.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
