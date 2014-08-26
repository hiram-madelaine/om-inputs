(ns om-inputs.view
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-inputs.core :as in :refer [build-input make-input-comp]]
   [clojure.string :as str]
   [schema.core :as s]
   [om-inputs.date-utils :refer  [tomorrow]]))


(def lang-sch {:lang (s/enum "en" "fr")})


(def app-state (atom {}))


(defn display-edn [_ _ edn]
  (js/alert edn))


#_(om/root
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

(def input-view (make-input-comp
  :create-person
  {:person/first-name (s/maybe s/Str)
   :person/name s/Str
   :person/email s/Str
   :person/email-confirm s/Str
   :person/vat (s/Regex #"^[A-Z]{1,2}[0-9]{0,12}$")
   :person/birthdate s/Inst
   :person/size (s/named s/Num "size")
   :person/age  (s/named s/Int "age")
   :person/gender (s/enum "M" "Ms")
   :person/married (s/eq true)}
   display-edn
    {:init {:person/gender "Ms"
            :person/size 187.50
            :person/age 39.89
            :person/birthdate (tomorrow)
            ;:person/married true
            :person/name "MADELAINE"}
     :order [:person/first-name :person/name  :person/vat :person/email :person/email-confirm :person/gender :person/birthdate :person/age :person/size :person/married]
     :person/gender {:type "radio-group"}
     :validations [
                   [:email [:person/email-confirm :person/email] :bad-email]
                   [:equal [:person/email-confirm :person/email] :email-match]]}))


(defn app
  [app owner]
  (reify
    om/IInitState
    (init-state
     [this]
     {:lang "fr"})
    om/IRenderState
    (render-state
     [this state]
     (dom/div #js {:className "container"}
       (dom/div #js {}
              (dom/a #js {:href "#"} (dom/img #js {:src "fr.png" :className "flag" :onClick #(om/set-state! owner [:lang] "fr")}))
              (dom/a #js {:href "#"} (dom/img #js {:src "gb.png" :className "flag" :onClick #(om/set-state! owner [:lang] "en")})))
      (om/build input-view app {:state state})))))

(om/root
 app
 app-state
 {:target (. js/document (getElementById "person"))
  :shared {:i18n {"en" {:errors {:email-match "email and confirmation email doesn't match"
                                 :bad-email "The format of the email is invalid"
                                 :person-size-min-length "Too short !"
                                 :mandatory "This information is required"}
                        :create-person {:title "User account"
                                        :action {:label "Create person"
                                                 :desc "We won't debit your card now."}
                                        :person/vat {:label "VAT"
                                                     :desc "Only alphanumeric"}
                                        :person/name {:label "Name"}
                                        :person/email {:desc "You will never receive spam."}
                                        :person/email-confirm {:desc "Sorry copy and paste deactivated."}
                                        :person/birthdate {:label "Birthday"}
                                        :person/first-name {:label "Firstname"}
                                        :person/size {:label "Size (cm)"}
                                        :person/gender {:label "Gender"
                                                        :data {"M" {:label "Mister"}
                                                               "Ms" {:label "Miss"}}}}}
                  "fr" {:errors {:email-match "email et la confirmation de l'email ne correspondent pas"
                                 :mandatory "Cette information est obligatoire"
                                 :person-size-min-length "Trop court !"
                                 :bad-email "Cette adresse email est invalide"}
                        :create-person {:title "Creation du compte"
                                        :action {:label "Créer personne"
                                                 :desc "Nous n'allons pas débiter votre carte à cette étape."}

                                       :person/name {:label "Nom"}
                                        :person/vat {:label "TVA"
                                                     :desc "Charactères alphanumeriques"}
                                       :person/email {:desc "Nous n'envoyons jamais de spam, promis !"}
                                       :person/first-name {:label "Prénom"}
                                       :person/birthdate {:label "Date de naissance"}
                                       :person/size {:label "Taille (cm)"}
                                       :person/gender {:label "Genre"
                                                       :data {"M" {:label "Monsieur"}
                                                              "Ms" {:label "Madame"}}}}}}}})
