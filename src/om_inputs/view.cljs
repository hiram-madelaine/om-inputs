(ns om-inputs.view
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-inputs.core :as in :refer [build-input make-input-comp debug]]
   [clojure.string :as str]))



(def input [{:code :label :value "" :coercer (fn [n o](str/upper-case n))}
            {:code :version :value "" :coercer (fn [n o] (if (re-matches #"[0-9]*" n) n o))}
            {:code :tier :value ""}
            {:code :cat :value "" :opts {:type "select"}}
            {:code :level :value 4 :coercer #(js/parseInt %) :opts {:type "range" :min 0 :max 5 :labeled true}}
            ])


(om/root
 (make-input-comp input)
 [{:label "Clojure" :version "1.5.1" :level 4}]
 {:target (. js/document (getElementById "app-2"))
  :shared {:i18n {:input {:cat "Catégorie"}}}})
