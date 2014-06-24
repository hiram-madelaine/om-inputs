(ns om-inputs.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [schema.macros :as s])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! >! <! alts!]]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [clojure.string :as str]
            [clojure.set :as st]
            [om-inputs.date-utils :as d]))

(enable-console-print!)


;_________________________________________________
;                                                 |
;          Schemas                                |
;_________________________________________________|

(def sch-inputs {:inputs s/Any})

(def sch-chan {:chan ManyToManyChannel})


(def sch-i18n {:i18n {:inputs s/Any}})

(def sch-state (merge sch-inputs sch-chan {s/Any s/Any}))

(def sch-select-data {:code s/Str
                      :label s/Str})


(def sch-conf-opts {(s/optional-key :labeled) s/Bool
                    (s/optional-key :min) s/Int
                    (s/optional-key :max) s/Int
                    (s/optional-key :step) s/Int
                    (s/optional-key :type) (s/enum "text" "range" "number" "color" "select")
                    (s/optional-key :data) [sch-select-data]})


(def sch-conf [{:field s/Keyword
                :value s/Any
                (s/optional-key :coercer) s/Any
                (s/optional-key :opts) sch-conf-opts}])


(def sch-local-state {s/Keyword {:value s/Any
                                 :required s/Bool
                                 (s/optional-key :valid) s/Bool}})


;_________________________________________________
;                                                 |
;          Events Utils                           |
;_________________________________________________|


(defn e-value
  "Get value from an event"
  [e]
  (-> e .-target .-value))

;_________________________________________________
;                                                 |
;       prismatic/Schema related Utils            |
;_________________________________________________|

(defprotocol Required
  (required? [this]))

(extend-protocol Required
  schema.core.OptionalKey
  (required? [this]
             false)
  schema.core.RequiredKey
  (required? [this]
             true)
  Keyword
  (required? [k]
             true))


(defn empty-string-coercer
  "Do not validate an empty string as a valid s/Str"
  [s]
  (if (str/blank? s) nil s))


(def validation-coercer
  (merge  {s/Str empty-string-coercer} coerce/+string-coercions+))


(defn sch-type [t]
  "indicate the type a key must conform to."
  (condp = (type t)
    schema.core.Predicate (:p? t)
    schema.core.EnumSchema "enum"
    js/Function t
    "other"))


 (defn only-integer
   "Only authorize integer or empty string."
  [n o]
  (if (str/blank? n)
   ""
   (let [r (js/parseInt n)]
                (if (js/isNaN r)
                  o
                  r))))

 (def FULL-DATE "yyyy-MM-dd")

 (defn parse-date [n o]
   (d/parse FULL-DATE n ))



(def coertion-fns
  {integer? only-integer
   js/Date parse-date })

(s/defn build-coercer
  "Build the corecion map field->coercion-fn"
  [sch]
  (reduce (fn[acc [k v]]
            (if-let [cfn (get coertion-fns (sch-type v))]
                          (assoc acc (get k :k k) cfn)
                           acc)) {} sch))


;___________________________________________________________
;                                                           |
;          Mulitmethod to handle different inputs form      |
;___________________________________________________________|


(defmulti magic-input (fn [k t attrs data]
                        (sch-type t)))



(defmethod magic-input "enum"
  [k t attrs data]
  (apply dom/select (clj->js attrs)
                       (dom/option #js {:value ""} "")
                       (map (fn [code]
                              (dom/option #js {:value code} (get data code (if (keyword? code) (name code) code)))) (:vs t))))


(defmethod magic-input js/Date
 [k t attrs data]
  (let [date-in (d/fmt FULL-DATE (:value attrs))]
   (dom/input (clj->js (merge attrs {:type "date"
                               :value date-in})))))


#_(defmethod magic-input integer?
 [k t attrs data]
  (dom/input (clj->js (merge {:type "number"} attrs))))


(defmethod magic-input :default
  [k t attrs data]
  (dom/input (clj->js attrs)))



;___________________________________________________________
;                                                           |
;          Errors state handler                             |
;___________________________________________________________|



(s/defn ^:always-validate handle-errors :- sch-local-state
  "Set valid to false for each key in errors, true if absent"
  [state :- sch-local-state
   errs :- {s/Keyword s/Any}]
  (let [err-ks (set (keys errs))
        all-ks (set (keys state))
        valid-ks (st/difference all-ks err-ks)
        state (reduce (fn [s e] (assoc-in s [e :valid] false)) state err-ks)]
    (reduce (fn [s e] (assoc-in s [e :valid] true)) state valid-ks)))

;___________________________________________________________
;                                                           |
;          Component builders                               |
;___________________________________________________________|


(defn build-input
  "Handle the display of an input from state and push change on a channel.
   The map of inputs is expected in state under the key :inputs
   The channel is expected in state under key :chan
   The i18n fn is expected in shared under key :i18n"
  ([owner n k t opts]
   (let [
         {:keys [chan inputs]} (om/get-state owner)
         lang (:lang (om/get-props owner))
         i18n (om/get-shared owner [:i18n lang] )
         label (get-in i18n [n k :label] (name k))
         value (get-in inputs [k :value])
         error (when-not (get-in inputs [k :valid] true) "has-error has-feedback")
         valid (when (get-in inputs [k :valid]) "has-success")
         attrs {:id (name k)
                :className "form-control"
                :value value
                :onChange #(put! chan [k (e-value %)]) }]
     (dom/div #js {:className (str/join " " ["form-group" error valid])}
           (dom/label #js {:htmlFor (name k)
                           :className "control-label"} label)
              (when (:labeled opts) (dom/span #js {} value))
              (magic-input k t attrs (get-in i18n [n k :data])))))
  ([owner n k t]
   (build-input owner n k t {})))



(s/defn ^:always-validate build-init :- sch-local-state
  "Build the inial local state backing the inputs in the form."
  [sch]
  (into {} (for [[k t] sch
                 :let [fk (get k :k k)]]
             [fk {:value ""
                  :required (required? k)}])))



(s/defn make-input-comp
  "Build an input form Om component based on the config"
  ([comp-name
    conf
    action]
   (make-input-comp comp-name conf action {}))
  ([comp-name
    conf
    action
    opts]
   (let [order (:order opts)
         input-coercer (coerce/coercer conf validation-coercer)]
     (fn [app owner]
       (reify
         om/IDisplayName
         (display-name [_]
                       (name comp-name))
         om/IInitState
         (init-state [_]
                     {:chan (chan)
                      :inputs (build-init conf)
                      :coercers (build-coercer conf)})
         om/IWillMount
         (will-mount [this]
                     (let [{:keys [coercers chan inputs] :as state} (om/get-state owner)]
                       (go
                        (loop []
                          (let [[k v] (<! chan)]
                            (condp = k
                              :create (let [raw (into {} (for [[k m] v
                                                               :let [in (:value m)
                                                                     req (:required m)]
                                                               :when (or req (not (str/blank? in)))]
                                                           {k (:value m)} ))
                                            res (input-coercer raw)]
                                        (if-let  [errs (:error res)]
                                          (let [new-state (handle-errors v errs)]
                                            (om/set-state! owner [:inputs] new-state))
                                          (do
                                            (om/set-state! owner [:inputs] inputs)
                                            (action app owner res))))
                              (let [coerce (get coercers k (fn [n _] n))
                                    old-val (om/get-state owner [:inputs k :value])]
                               (om/set-state! owner [:inputs k :value] (coerce v old-val)))))
                          (recur)))))
         om/IWillUpdate
         (will-update [this props state]
                         )
         om/IRenderState
         (render-state [_ {:keys [chan inputs] :as state}]
                       (let [i18n (om/get-shared owner :i18n)
                             lang (:lang app)]
                         (dom/fieldset nil (dom/form #js {:className "form"
                                                          :role "form"}
                                                     (into-array (if order
                                                                  (map (fn [o]
                                                                         (build-input owner comp-name o (o conf))) order)
                                                                  (map (fn [[k t]]
                                                                        (build-input owner comp-name (get k :k k) t)) conf)))
                                                     (dom/input #js {:type  "button"
                                                                     :className "btn btn-primary"
                                                                     :value (get-in i18n [lang comp-name :action] (str (name comp-name) " action"))
                                                                     :onClick #(put! chan [:create inputs])}))))))))))



