(ns om-inputs.view
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [cljs.core.async :refer [chan timeout put! >! <! alts! close!]]
   [om-inputs.core :as in :refer [build-input make-input-comp]]
   [clojure.string :as str]
   [schema.core :as s]
   [om-inputs.date-utils :refer  [at tomorrow]]
   [goog.net.XhrIo :as xhr]
    [cljs.reader :as reader]
            [goog.events :as events])
   (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]
           [goog.ui IdGenerator]))


(def lang-sch {:lang (s/enum "en" "fr")})


(def app-state (atom {}))


(defn display-edn [_ _ edn]
  (js/alert edn))

;___________________________________________________________
;                                                           |
;        XhrIo call                                         |
;___________________________________________________________|

(def ^:private meths
  {:get "GET"
   :put "PUT"
   :post "POST"
   :delete "DELETE"})

(defn edn-xhr [{:keys [method url data on-complete on-error]}]
  (let [xhr (XhrIo.)
        _ (println data)]
    (events/listen xhr goog.net.EventType.SUCCESS
      (fn [e]
        (on-complete (.getResponseText xhr))))
    (events/listen xhr goog.net.EventType.ERROR
      (fn [e]
        (on-error (.getResponseText xhr))))
    (.send xhr url)))

(defn async-action
  [app owner value out]
  (edn-xhr {:method :get
            :url "https://api.github.com/users/hiram-madelaine/repos"
            :on-complete (fn [e]
                           (prn e)
                           (go (<! (timeout 2000))
                               (>! out [:ok])))
            :on-error (fn [e]
                        (put! out [:ko "Fuck an error"]))}))


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
                                                 "fr" "Français"}}}}}}})

(def input-view (make-input-comp
  :create-person
  {:person/first-name (s/maybe s/Str)
   :person/date-aller s/Inst
   (s/optional-key :person/date-retour) s/Inst
   :person/immat (s/Regex )
   :person/name s/Str
   :person/email s/Str
   :person/email-confirm s/Str
   :person/vat (s/Regex #"^[A-Z]{1,2}[0-9]{0,12}$")
   :person/birthdate s/Inst
   :person/size (s/named s/Num "size")
   :person/age  (s/named s/Int "age")
   :person/gender (s/enum "M" "Ms")
   :person/married (s/eq true)}
   async-action
                 #_(fn [a b c]
     (throw (js/Error. "Oops")))
                 #_(fn [app owner value chan]
     (go
      #_(<! (timeout 3000))
      (if-let [err (js/confirm "Error ?")]
        (>! chan :ko)
        (>! chan :ok))
      #_(display-edn app owner value)
      ))
   display-edn
    {:IWillReceiveProps (fn [owner next-props]
                          (prn "on receive props")
                          (when-let [first-name (get-in next-props [:first-name])]
                            (put! (om/get-state owner :chan) [:person/first-name first-name])
                                               #_(om/set-state! owner [:inputs :person/first-name :value] first-name)))
     :create-person {:className "visible"}
     :action {:one-shot true
              :no-reset false
              :async true}
     :init {:person/gender "M"
;;             :person/date-aller (at 0)
            :person/vat "FR7589272"
            :person/size 187.50
            :person/age 1
            :person/birthdate (tomorrow)
            :person/email "h@h"
            :person/email-confirm "h@h"
            :person/married true
            :person/name "MADELAINE"}
     :order [:person/date-aller  :person/date-retour :person/first-name :person/name :person/vat :person/email :person/email-confirm :person/gender :person/birthdate :person/age :person/size :person/married]
     :person/first-name {}
     :person/gender {:type "radio-group-inline"}
     :person/date-aller {:type "now"
                         :labeled true}
     :person/date-retour {:labeled true}
     :person/age {:type "range"
                  :attrs {:min "1" :max "5"}
                  :labeled true}
     :validations [[:after (at 0) :person/date-aller :date-aller]
;;                    [:greater [:person/date-retour :person/date-aller] :date-retour]
                   [:email [:person/email-confirm :person/email] :bad-email]
                   [:equal [:person/email-confirm :person/email] :email-match]]}))

(defn app
  [app owner]
  (reify
    om/IInitState
    (init-state
     [this]
     {:lang "fr"
      :create-person {}})
    om/IRenderState
    (render-state
     [this state]
     (dom/div #js {:className "container"}
       (dom/input #js {:type "button"
                       :onClick #(do
                                   #_(om/set-state-nr! owner [:dyn-opts :person/first-name :value] "Hiram")
                                   #_(om/set-state! owner [:dyn-opts :person/date-retour :type] "now")
                                   (om/update! app [:client :first-name] "Hiram"))})
              (dom/div #js {}
              (dom/a #js {:href "#"} (dom/img #js {:src "img/fr.png" :className "flag" :onClick #(om/set-state! owner [:lang] "fr")}))
              (dom/a #js {:href "#"} (dom/img #js {:src "img/gb.png" :className "flag" :onClick #(om/set-state! owner [:lang] "en")})))
      (om/build input-view (:client app) {:state state})))))

(om/root
 app
 app-state
 {:target (. js/document (getElementById "person"))
  :shared {:i18n {"en" {:errors {:date-retour "Date aller avant date aller"
                                 :email-match "email and confirmation email doesn't match"
                                 :bad-email "The format of the email is invalid"
                                 :person-size-min-length "Too short !"
                                 :mandatory "This information is required"}
                        :create-person {:title "User account"
                                        :action {:label "Create person"
                                                 :desc "We won't debit your card now."}
                                        :person/vat {:label "VAT"
                                                     :desc "Only alphanumeric"
                                                     :ph "AB0123456789"}
                                        :person/name {:label "Name"}
                                        :person/email {:desc "You will never receive spam."}
                                        :person/email-confirm {:desc "Sorry copy and paste deactivated."}
                                        :person/birthdate {:label "Birthday"}
                                        :person/first-name {:label "Firstname"}
                                        :person/size {:label "Size (cm)"}
                                        :person/gender {:label "Gender"
                                                        :data {"M" {:label "Mister"}
                                                               "Ms" {:label "Miss"}}}}}
                  "fr" {:errors {:date-aller "Il n'est pas possible de réserver dans le passé"
                                 :date-retour "Date aller avant date aller"
                                 :email-match "email et la confirmation de l'email ne correspondent pas"
                                 :mandatory "Cette information est obligatoire"
                                 :person-size-min-length "Trop court !"
                                 :bad-email "Cette adresse email est invalide"}
                        :create-person {:title "Creation du compte"
                                        :clean {:label "Nouveau client"}
                                        :action {:label "Créer personne"
                                                 :desc "Nous n'allons pas débiter votre carte à cette étape."}

                                        :person/age {:label "Nombre de passagers"
                                                     :desc "Votre age véritable"}
                                        :person/name {:label "Nom"
                                                      :info-title "Information importante"
                                                      :info "hjkdfhd fhdsjfh hfdjsf dskffshf
                                                      dshkfhsd  sdhfjhsdfk hjkhkj  hjhk hjj h hjhk h hkj h
                                                      tty g hgh gh  gj https://api.github.com/users/hiram-madelaine/repos
                                                      sqdksh sqd hash-imap
                                                      djskq
                                                      qsjdk
                                                      qsjkldj jqskd  jkqjd kjd zaljdaz "}
                                        :person/vat {:label "TVA"
                                                     :desc "Charactères alphanumeriques"
                                                     :ph "AB0123456789"}
                                        :person/email {:desc "Nous n'envoyons jamais de spam, promis !"}
                                       :person/first-name {:label "Prénom"}
                                       :person/birthdate {:label "Date de naissance"}
                                       :person/size {:label "Taille (cm)"}
                                       :person/gender {:label "Genre"
                                                       :data {"M" {:label "Monsieur"}
                                                              "Ms" {:label "Madame"}}}}}}}})
