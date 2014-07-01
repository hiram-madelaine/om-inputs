(ns om-inputs.view
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-inputs.core :as in :refer [build-input make-input-comp]]
   [clojure.string :as str]
   [schema.core :as s]))


(def opts {:order [:person/first-name :person/name :person/gender :person/birthdate :person/size :person/married]})



(def lang-sch {:lang (s/enum "en" "fr")})


(def lang-view (make-input-comp :language lang-sch  (fn [app owner v] (om/transact! app (fn [app] (merge app v)) ))))


(def app-state (atom {:lang "fr"}))



(def sch-person {:person/first-name s/Str
                 :person/name s/Str
                 (s/optional-key :person/gender) (s/enum "M" "Ms")
                 (s/optional-key :person/birthdate) s/Inst
                 (s/optional-key :person/size) s/Num
                 :person/married s/Bool})


(defn display-edn [_ _ edn]
  (js/alert edn))


(om/root
 lang-view
 app-state
 {:target (. js/document (getElementById "lang"))
  :shared {:i18n {"en" {:language {:action "Change language"
                                   :lang {:label "Language"
                                          :data {"en" "English"
                                                 "fr" "French"}}}}
                  "fr" {:language {:action "Choix de la langue"
                                   :lang {:label "Langue"
                                          :data {"en" "Anglais"
                                                 "fr" "Français"}}}}}} })
(om/root
 (make-input-comp
  :create-person
  {:person/first-name (s/maybe s/Str)
   :person/name s/Str
   (s/optional-key :person/birthdate) s/Inst
   (s/optional-key :person/size) s/Num
   (s/optional-key :person/gender) (s/enum "M" "Ms")
   :person/married s/Bool}
   display-edn
  opts)
 app-state
 {:target (. js/document (getElementById "person"))
  :shared {:i18n {"en" {:create-person {:action "Create person"
                                        :person/name {:label "Name"}
                                        :person/birthdate {:label "Birthday"}
                                        :person/first-name {:label "Firstname"}
                                        :person/size {:label "Size (cm)"}
                                        :person/gender {:label "Gender"
                                                        :data {"M" "Mister"
                                                               "Ms" "Miss"}}}}
                  "fr" {:create-person {:action "Créer personne"
                                       :person/name {:label "Nom"}
                                       :person/first-name {:label "Prénom"}
                                       :person/birthdate {:label "Date de naissance"}
                                       :person/size {:label "Taille (cm)"}
                                       :person/gender {:label "Genre"
                                                       :data {"M" "Monsieur"
                                                              "Ms" "Madame"}}}}}}})
