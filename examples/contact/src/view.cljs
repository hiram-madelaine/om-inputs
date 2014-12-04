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
   [goog.events :as events]
   [figwheel.client :as fw :include-macros true]
   [weasel.repl :as ws-repl]
   [jkkramer.verily :as v :refer [validation->fn]]
   [om-inputs.validation :as va]
   [om-inputs.verily-ext :as ve])
   (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]
           [goog.ui IdGenerator]))


#_((v/validate [78] [[:string]])

 ((v/validations->fn [[:vat [:person/age] :positive]]) {:person/age -67})

 ((v/combine [:positive [:person/age] :positive]) {:person/age -67})

 (map v/validation->fn [[:positive [:person/age] :positive]
                        [:int [:person/age] :int]])



 (validation->fn [:positive [:person/age] :positive])




 (defmethod validation->fn :vat
   [vspec]
   (fn [vat]
     "VAT COntrol"))

 (v/validate {:person/age -67} [[:vat [:person/age] :positive]])

 (defmethod va/valider :vat
   [vspec]
   (fn [vat]
     "VAT COntrol"))


 (map v/validation->fn [[:positive [:person/age] :positive]
                        [:vat [:person/age] :int]])


 ((v/validations->fn [[:vat [:person/age] :vat]]) {:person/age -67})

 (va/valider [:vat]))

(ws-repl/connect "ws://192.168.1.42:9001")

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
    (.send xhr url )))

(defn async-action
  [app owner value out]
  (edn-xhr {:method :get
            :url "http://ec.europa.eu/taxation_customs/vies/vatResponse.html/memberStateCode=FR&number=89753948272"
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

#_(clojure.core/defrecord EnumSchema [vs]
                        Schema
                        (walker [this]
                                (clojure.core/fn [x]
                                                 (if (some #{x} vs)
                                                   x
                                                   (macros/validation-error this x (list vs (utils/value-name x))))))
                        (explain [this] (cons 'enum vs)))

#_(clojure.core/defn enum
                   "A value that must be = to some element of vs."
                   [& vs]
                   (EnumSchema. vs))





(def input-view (make-input-comp
  :create-person
  {:person/first-name (s/maybe s/Str)
   :person/date-aller s/Inst
   (s/optional-key :person/date-retour) s/Inst
   ; :person/immat (s/Regex #"^[A-Z0-9]{1,12}")
   :person/name s/Str
   :person/email s/Str
   :person/email-confirm s/Str
   :person/vat s/Str
   :person/birthdate s/Inst
   :person/size (s/named s/Num "size")
   :person/age  (s/named s/Int "age")
   :person/gender  (s/enum "ALB" "DZA" "GER" "AND" "ARG" "ARM" "ZZZ" "AUT" "AZE" "BEL" "BIE" "BIH" "BRA" "BGR" "CDN" "HRV" "DNK" "EGY" "ESP" "EST" "USA" "FIN" "FRA" "GEO" "GRC" "HUN" "IRL" "ISL" "ISR" "ITA" "KAZ" "LVA" "LIE" "LTU" "LUX" "MKD" "MLT" "MAR" "MCO" "MNE" "NOR" "UZB" "NLD" "POL" "PRT" "CZE" "ROM" "GBR" "RUS" "SEN" "SRB" "SVK" "SVN" "SWE" "CHE" "TUN" "TUR" "UKR")  #_(s/enum "B" "AA"  "Ms" "M"  "3" "A" "_")
   :person/married (s/eq true)}
  async-action
  ;async-action
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
              :async true
              :attrs {:tabIndex 0}}
     :init {
            ;:person/gender "Ms"
;;             :person/date-aller (at 0)
            :person/vat "FR7589272"
            :person/size 187.50
            ;:person/age 3
            :person/birthdate (tomorrow)
            :person/email "h@h"
            :person/email-confirm "h@h"
            :person/married true
            :person/name "MADELAINE"}
     :order [:person/date-aller  :person/date-retour :person/first-name :person/name :person/vat :person/email :person/email-confirm :person/gender :person/birthdate :person/age :person/size :person/married]
     :person/first-name {:layout "horizontal"
                         :attrs {:tabIndex 0}}
     :person/gender {;:type "btn-group"
                     :label-order true}
     :person/email {:type "email"
                    :attrs {:tabIndex 0}}
     :person/email-confirm {:type "email"}
     :person/date-aller {:type "now"
                         :labeled true
                         :attrs {:tabIndex 0}}
     :person/vat {:attrs {:tabIndex 0
                          :autoCapitalize "characters"}}
     :person/date-retour {:labeled true}
     :person/size {:attrs {:type "number"}}
     :person/age {:type "range-btn-group"
                  :attrs {:min "0" :max "10" :step 2}
                  :labeled true}
      :person/married {:layout "in-line"}
     :validations [[:vat [:person/vat] :vat]
                   [:positive [:person/age] :positive]
                   [:after (at 0) :person/date-aller :date-aller]
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



(defn sort-pays-by-labels [liste]
  (into (sorted-map-by  (fn [e1 e2] (compare (:label (get liste e1)) (:label (get liste e2))))) liste))

(def liste-pays {"ALB" {:label "ALBANIE"},
                 "GER" {:label "ALLEMAGNE"},
                 "ZZZ" {:label "AUTRES"},
                 "AUT" {:label "AUTRICHE"},
                 "BEL" {:label "BELGIQUE"},
                 "BIH" {:label "BOSNIE HERZEGOVINE"},
                 "BGR" {:label "BULGARIE"},
                 "HRV" {:label "CROATIE"},
                 "DNK" {:label "DANEMARK"},
                 "ESP" {:label "ESPAGNE"},
                 "EST" {:label "ESTONIE"},
                 "FIN" {:label "FINLANDE"},
                 "FRA" {:label "FRANCE"},
                 "GRC" {:label "GRECE"},
                 "HUN" {:label "HONGRIE"},
                 "IRL" {:label "IRLANDE"},
                 "ITA" {:label "ITALIE"},
                 "LVA" {:label "LETTONIE"},
                 "LTU" {:label "LITUANIE"},
                 "LUX" {:label "LUXEMBOURG"},
                 "MKD" {:label "MACEDOINE"},
                 "MAR" {:label "MAROC"},
                 "MNE" {:label "MONTENEGRO"},
                 "NOR" {:label "NORVEGE"},
                 "NLD" {:label "PAYS-BAS"},
                 "POL" {:label "POLOGNE"},
                 "PRT" {:label "PORTUGAL"},
                 "CZE" {:label "REPUBLIQUE TCHEQUE"},
                 "ROM" {:label "ROUMANIE"},
                 "GBR" {:label "ROYAUME UNI"},
                 "RUS" {:label "RUSSIE"},
                 "SRB" {:label "SERBIE"},
                 "SVK" {:label "SLOVAQUIE"},
                 "SVN" {:label "SLOVENIE"},
                 "SWE" {:label "SUEDE"},
                 "CHE" {:label "SUISSE"},
                 "TUR" {:label "TURQUIE"},
                 "UKR" {:label "UKRAINE"}})

(def liste-pays-sorted-by-labels (sort-pays-by-labels liste-pays))


(def liste-pays-en {"ALB" {:label "ALBANIA"},
                    "AUT" {:label "AUSTRIA"},
                    "BEL" {:label "BELGIUM"},
                    "BIH" {:label "BOSNIA HERZEGOVINA"},
                    "BGR" {:label "BULGARIA"},
                    "HRV" {:label "CROATIA"},
                    "CZE" {:label "CZECH REPUBLIC"},
                    "DNK" {:label "DENMARK"},
                    "EST" {:label "ESTONIA"},
                    "FIN" {:label "FINLAND"},
                    "FRA" {:label "FRANCE"},
                    "GER" {:label "GERMANY"},
                    "GBR" {:label "GREAT BRITAIN"},
                    "GRC" {:label "GREECE"},
                    "HUN" {:label "HUNGARY"},
                    "IRL" {:label "IRELAND"},
                    "ITA" {:label "ITALY"},
                    "LVA" {:label "LATVIA"},
                    "LTU" {:label "LITHUANIA"},
                    "LUX" {:label "LUXEMBOURG"},
                    "MKD" {:label "MACEDONIA"},
                    "MNE" {:label "MONTENEGRO"},
                    "MAR" {:label "MOROCCO"},
                    "NLD" {:label "NETHERLANDS"},
                    "NOR" {:label "NORWAY"},
                    "ZZZ" {:label "OTHERS"},
                    "POL" {:label "POLAND"},
                    "PRT" {:label "PORTUGAL"},
                    "ROM" {:label "ROMANIA"},
                    "RUS" {:label "RUSSIA"},
                    "SRB" {:label "SERBIA"},
                    "SVK" {:label "SLOVAKIA"},
                    "SVN" {:label "SLOVENIA"},
                    "ESP" {:label "SPAIN"},
                    "SWE" {:label "SWEDEN"},
                    "CHE" {:label "SWITZERLAND"},
                    "TUR" {:label "TURKEY"},
                    "UKR" {:label "UKRAINE"}})

(def liste-pays-en-sorted-by-labels (sort-pays-by-labels liste-pays-en))


(defn main []
 (om/root
 app
 app-state
 {:target (. js/document (getElementById "person"))
  ;:descriptor (om/no-local-descriptor om/no-local-state-methods)
  :shared {:i18n {"en" {:errors {:positive "Value must be positive"
                                 :date-retour "Date aller avant date aller"
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
                                        :person/email {:desc "You will never receive spam."
                                                       :html [:div {:class "description"} "I agree the " [:a {:href   "http://www.myferrylink.fr/fret-ferry/conditions-vente?packedargs=site%3DSF_Freight"
                                                                                                                :target "_blank"} "General Sales and Transport Conditions"]
                                                              " and the " [:a {:href   "http://www.myferrylink.fr/fret-ferry/infos-pratiques/code-practice-fr?packedargs=site%3DSF_Freight"
                                                                             :target "_blank"} "the Code of Practice"]]}
                                        :person/email-confirm {:desc "Sorry copy and paste deactivated."}
                                        :person/birthdate {:label "Birthday"}
                                        :person/first-name {:label "Firstname"}
                                        :person/size {:label "Size (cm)"}
                                        :person/gender {:label "Gender"
                                                        :data liste-pays-en-sorted-by-labels #_{"M" {:label "Mister"}
                                                               "Ms" {:label "Miss"}}}}}
                  "fr" {:errors {:vat "Votre identification de TVA semble erroné"
                                 :positive "La valeur doit être positive"
                                 :date-aller "Il n'est pas possible de réserver dans le passé"
                                 :date-retour "Date aller avant date aller"
                                 :email-match "email et la confirmation de l'email ne correspondent pas"
                                 :mandatory "Cette information est obligatoire"
                                 :person-size-min-length "Trop court !"
                                 :bad-email "Cette adresse email est invalide"}
                        :create-person {:title "Creation du compte"
                                        :clean {:label "Nouveau client"
                                                :desc "Procéder à la création d'un nouveau compte"}
                                        :action {:label "Créer personne"
                                                 :desc "Nous n'allons pas débiter votre carte à cette étape."}
                                                 :person/date-aller {:label "Date aller"}
                                        :person/age {:label "Nombre de passagers"
                                                     :desc "Votre age"}
                                        :person/name {:label "Nom"
                                                      :info-title "Information importante !"
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
                                        :person/email {:desc "hdkjfhsjfhskdj"
                                                       :html [:div {:class "description"} "J'accepte les " [:a {:href   "http://www.myferrylink.fr/fret-ferry/conditions-vente?packedargs=site%3DSF_Freight"
                                                                                                                :target "_blank"} "Conditions Générales de Vente et de Transport"]
                                                              " et le " [:a {:href   "http://www.myferrylink.fr/fret-ferry/infos-pratiques/code-practice-fr?packedargs=site%3DSF_Freight"
                                                                             :target "_blank"} "Le Code of Practice"]]}
                                        :person/date-retour {:label "Date de retour"}
                                        :person/email-confirm {:label "Confirmation de l'email"}
                                       :person/first-name {:label "Prénom"}
                                       :person/birthdate {:label "Date de naissance"}
                                       :person/size {:label "Taille (cm)"}
                                       :person/married {:label "Marié(e)"
                                                        :desc "Pas de mensonge"
                                                        }
                                        :person/gender {:label "Genre"
                                                       :data liste-pays-sorted-by-labels
                                                       #_{"M" {:label "Monsieur"}
                                                              "Ms" {:label "Madame"}}
                                                        }}}}}}))


(fw/watch-and-reload
 :websocket-url "ws://localhost:3449/figwheel-ws"
 :jsload-callback
 (fn []
   (println "reloaded")
   (main)))

(defonce initial-call-to-main (main))
