(ns om-inputs.view
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-inputs.core :as in :refer [build-input make-input-comp]]
   [clojure.string :as str]
    [schema.core :as s]))


(def rap-sch {:traversee/numero s/Str
               :traversee/navire (s/enum "BER" "ROD")
               :traversee/code s/Str
               :traversee/itineraire (s/enum "CALDOV" "DOVCAL")
               :traversee/duree s/Int #_(apply s/enum (map str (range 90 120 10)))
               ;:traversee/departProgramme s/Inst
               })


(def opts {:order [:traversee/numero :traversee/code :traversee/itineraire :traversee/navire  :traversee/duree]})


(def generated-view (make-input-comp :creation-traversee rap-sch validation-handler opts))


(def lang-sch {:lang (s/enum "en" "fr")})


(def lang-view (make-input-comp :language lang-sch  (fn [app owner v] (om/transact! app (fn [app] (merge app v)) ))))

(defn search-view
  [app owner]
  (reify
    om/IRender
    (render [this]
            (dom/div #js {:style #js {:width "350px"
                                      :margin "5px"}}
                     (om/build generated-view app)))))


(def app-state (atom {:lang "fr"}))


(defn app-view
  [app owner]
  (reify
    om/IRender
    (render [this]
     (dom/div nil
             (om/build lang-view app)
             (om/build search-view app)))))


(om/root
 app-view
 app-state
 {:target (. js/document (getElementById "app"))
  :shared {:i18n {"fr" {:language {:action "Choix de langue"
                                   :lang {:label "Langages"
                                          :data {"en" "Anglais"
                                                 "fr" "Français"}}}
                        :creation-traversee
                        {:action "Création"
                         :traversee/code {:label "Code traversée"}
                         :traversee/itineraire {:label "Itinéraire"
                                                :data {"CALDOV" "Calais->Douvres"
                                                       "DOVCAL" "Douvres->Calais"}}
                         :traversee/numero {:label "Numéro"}
                         :traversee/duree {:label "Durée"}
                         :traversee/navire {:label "Navire"
                                            :data {"ROD" "Rodin"
                                                   "BER" "Berlioz"
                                                   "NPC" "Nord-Pas-de-Calais"}}
                         :traversee/departProgramme {:label "Départ"}}

                        :inputs {
                                 :cat "Catégorie"
                                 :label "Libellé"
                                 :level "Niveau"
                                 :action "Recherche"}}
                  "en" {:language {:action "Change language"
                                   :lang {:label "Languages"
                                          :data {"en" "English"
                                                 "fr" "French"}}}
                        :creation-traversee
                        {:action "Creation"
                         :traversee/code {:label "Crossing Code"}
                         :traversee/itineraire {:label "Outward route"
                                                :data {"CALDOV" "Calais->Dover"
                                                       "DOVCAL" "Dover->Calais"}}
                         :traversee/numero {:label "Number"}
                         :traversee/duree {:label "Duration"}
                         :traversee/navire {:label "Navire"
                                            :data {"ROD" "Rodin"
                                                   "BER" "Berlioz"
                                                   "NPC" "Nord-Pas-de-Calais"}}
                         :traversee/departProgramme {:label "Départ"}}

                        :inputs {
                                 :cat "Catégorie"
                                 :label "Libellé"
                                 :level "Niveau"
                                 :action "Recherche"}}}}})
