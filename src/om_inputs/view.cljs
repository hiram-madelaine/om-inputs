(ns om-inputs.view
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-inputs.core :as in :refer [build-input make-input-comp debug]]
   [clojure.string :as str]))



(def input [{:field :label :value "" :coercer (fn [n o](str/upper-case n))}
            {:field :version :value "" :coercer (fn [n o] (if (re-matches #"[0-9]*" n) n o))}
            {:field :tier :value "" :opts {:type "select" :data [{:code "middle" :label "Middleware"} {:code "front" :label "Front"} {:code "data" :label "Database"}]}}
            {:field :cat :value "" :opts {:type "select" :data [{:code "lang" :label "Language"} {:code "frmk" :label "Framework"}]}}
            {:field :level :value 0 :coercer #(js/parseInt %) :opts {:type "range" :min 0 :max 5 :labeled true}}
            ])

(def generated-view (make-input-comp input (fn [app owner v] (js/alert v)) #_(fn [app v](om/transact! app #(conj % v)))) )


(defn search-view
  [app owner]
  (om/component
   (dom/div #js {:style #js {:width "350px"
                             :margin "5px"}}
            (om/build generated-view app))))


(om/root
 search-view
 [{:label "Clojure" :version "1.5.1" :level 4}]
 {:target (. js/document (getElementById "app-2"))
  :shared {:i18n {:inputs {:cat "Catégorie"
                           :label "Libellé"
                           :level "Niveau"
                           :action "Recherche"}}}})
