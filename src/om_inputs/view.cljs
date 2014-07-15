(ns om-inputs.view
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-inputs.core :as in :refer [build-input make-input-comp]]
   [clojure.string :as str]
   [schema.core :as s]))



(def lang-sch {:lang (s/enum "en" "fr")})


(def app-state (atom {:lang "en"}))




(defn display-edn [_ _ edn]
  (js/alert edn))


(om/root
(make-input-comp
 :language
 lang-sch
 (fn [app owner v] (om/transact! app (fn [app] (merge app v))))
 {:lang {:type "radio-group"}})
 app-state
 {:target (. js/document (getElementById "lang"))
  :shared {:i18n {"en" {:language {:action "Change language"
                                   :lang {:label "Language"
                                          :data {"en" {:label "English"}
                                                 "fr" {:label "French"}}}}}
                  "fr" {:language {:action "Choix de la langue"
                                   :lang {:label "Langue"
                                          :data {"en" "Anglais"
                                                 "fr" "Français"}}}}}} })
(om/root
 (make-input-comp
  :create-person
  {:person/first-name (s/maybe s/Str)
   :person/name s/Str
   :person/email (s/maybe s/Str)
   (s/optional-key :person/birthdate) s/Inst
   (s/optional-key :person/size) (s/named s/Num "size")
   :person/gender (s/enum "M" "Ms")
   :person/married s/Bool}
   display-edn
    {:order [:person/first-name :person/name :person/email :person/gender :person/birthdate :person/size :person/married]
     :person/gender {:type "radio-group"}
     :validations [[:min-val 10 :person/size :person-size-min-length]
                           [:email :person/email :bad-email]]})
 app-state
 {:target (. js/document (getElementById "person"))
  :shared {:i18n {"en" {:errors {:bad-email "The format of the email is invalid"
                                 :mandatory "This information is required"}
                        :create-person {:action {:label "Create person"
                                                 :desc "We won't debit your card now."}
                                        :person/name {:label "Name"}
                                        :person/email {:desc "You will never receive spam."}
                                        :person/birthdate {:label "Birthday"}
                                        :person/first-name {:label "Firstname"}
                                        :person/size {:label "Size (cm)"}
                                        :person/gender {:label "Gender"
                                                        :data {"M" {:label "Mister"}
                                                               "Ms" {:label "Miss"}}}}}
                  "fr" {:errors {:mandatory "Cette donnée est obligatoire"
                                 :bad-email "Cette adresse email est invalide"}
                        :create-person {:action {:label "Créer personne"
                                                 :desc "Nous n'allons pas débiter votre carte à cette étape."}
                                       :person/name {:label "Nom"}
                                       :person/first-name {:label "Prénom"}
                                       :person/birthdate {:label "Date de naissance"}
                                       :person/size {:label "Taille (cm)"}
                                       :person/gender {:label "Genre"
                                                       :data {"M" "Monsieur"
                                                              "Ms" "Madame"}}}}}}})
