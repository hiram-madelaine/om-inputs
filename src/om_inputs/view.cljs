(ns om-inputs.view
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-inputs.core :as in :refer [build-input make-input-comp]]
   [clojure.string :as str]
    [schema.core :as s]))


(def opts {:order []})



(def lang-sch {:lang (s/enum "en" "fr")})


(def lang-view (make-input-comp :language lang-sch  (fn [app owner v] (om/transact! app (fn [app] (merge app v)) ))))


(def app-state (atom {:lang "fr"}))



(def sch-person {:person/first-name s/Str
                 :person/name s/Str
                 (s/optional-key :person/size) s/Int
                 (s/optional-key :person/gender) (s/enum "M" "Ms")})



(def person-input-view (make-input-comp :create-person sch-person #(js/alert %3) ))


(defn app-view
  [app owner]
  (reify
    om/IRender
    (render [this]
            (dom/div #js {:style #js {:width "350px"
                                      :margin "5px"}}
                     (om/build lang-view app)
                     (om/build person-input-view app)))))


(om/root
 app-view
 app-state
 {:target (. js/document (getElementById "person"))
  :shared {:i18n {"en" {:language {:action "Change language"
                                   :lang {:label "Language"
                                          :data {"en" "English"
                                                 "fr" "French"}}}
                        :create-person {:action "Create person"
                                        :person/name {:label "Name"}
                                        :person/first-name {:label "Firstname"}
                                        :person/size {:label "Size"}
                                        :person/gender {:label "Gender"
                                                        :data {"M" "Mister"
                                                               "Ms" "Miss"}}}}
                  "fr" {:language {:action "Choix de la langue"
                                   :lang {:label "Langue"
                                          :data {"en" "Anglais"
                                                 "fr" "Français"}}}
                        :create-person {:action "Créer personne"
                                       :person/name {:label "Nom"}
                                       :person/first-name {:label "Prénom"}
                                       :person/size {:label "Taille"}
                                       :person/gender {:label "Genre"
                                                       :data {"M" "Monsieur"
                                                              "Ms" "Madame"}}}}}}})


