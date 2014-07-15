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
            [om-inputs.date-utils :as d]
            [om-inputs.schema-utils :as su]
            [jkkramer.verily :as v]))

(enable-console-print!)


;_________________________________________________
;                                                 |
;          Schemas                                |
;_________________________________________________|



(def sch-i18n {:i18n {:inputs s/Any}})

(def sch-business-state
  "Local business state's data structure "
  {s/Keyword {:value s/Any
              :required s/Bool
              (s/optional-key :valid) s/Bool
              (s/optional-key :error) [s/Keyword]}})


(def sch-inputs {:inputs sch-business-state})

(def sch-chan {:chan ManyToManyChannel})

(def sch-state (merge sch-inputs sch-chan {s/Any s/Any}))



;_________________________________________________
;                                                 |
;          Events Utils                           |
;_________________________________________________|


(defn e-value
  "Get value from an event"
  [e]
  (-> e .-target .-value))

(defn e-checked
  "Get the checked status of a checkbox."
  [e]
  (-> e .-target .-checked))

(defn styles
  [& args]
  (str/join " " args))


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
  (merge coerce/+string-coercions+ {s/Str empty-string-coercer
                                    (s/maybe s/Str) empty-string-coercer}))


 (defn only-integer
   "Only authorize integer or empty string.
    n is the new value
    o is the old value
    When the new value is valid returns it, else returns the previous one."
  [n o]
  (if (str/blank? n)
   ""
   (let [r (js/parseInt n)]
                (if (js/isNaN r)
                  o
                  r))))

 (defn only-number
   "Validate a that an input of type s/Num contains a value that can be converted in a numeric.
    If not the previous value is used.
    An empty string is left as is.
    n is the new value
    o is the old value"
  [n o]
  (if (str/blank? n)
    ""
    (if (js/isNaN n)
      o
      n)))


 (def FULL-DATE "yyyy-MM-dd")

 (defn parse-date [n o]
   (d/parse FULL-DATE n ))



(defn sch-type [t]
  "This function is the link between Schema and the type of input.
   s/Str, s/Int are inclosed in Predicate
   s/Inst is represented "
  (condp = (type t)
    schema.core.Predicate (:p? t)
    schema.core.Maybe (sch-type (:schema t))
    schema.core.NamedSchema (sch-type (:schema t))
    schema.core.EnumSchema "enum"
    js/Function t
    "other"))

(def coertion-fns
  {integer? only-integer
   js/Date parse-date
   js/Number only-number})


(s/defn build-coercer
  "Build the coercion map field->coercion-fn from all entries of the Schema"
  [sch]
  (reduce (fn[acc [k v]]
            (if-let [cfn (get coertion-fns (sch-type v))]
                          (assoc acc (get k :k k) cfn)
                           acc)) {} sch))


;___________________________________________________________
;                                                           |
;          Mulitmethod to handle different inputs form      |
;___________________________________________________________|


(defmulti magic-input
  (fn [{t :t k :k opts :opts}]
    (get-in opts [k :type] (sch-type t))))


(defmethod magic-input "enum"
  [{:keys [k t data attrs chan]}]
  (apply dom/select (clj->js attrs)
         (dom/option #js {:value ""} "")
         (map (fn [code]
                (dom/option #js {:value code} (get-in data [code :label] (if (keyword? code) (name code) code)))) (:vs t))))


(defmethod magic-input "radio-group"
  [{:keys [k t data attrs chan]}]
  (apply dom/div #js {:className "input-group"}
         (map (fn [code]
                (dom/div #js {:className "radio"}
                         (dom/input #js {:type "radio"
                                         :name (name k)
                                         :value code
                                         :onClick #(put! chan [k code])}
                                    (get-in data [code :label] (if (keyword? code) (name code) code))))) (:vs t))))


(defmethod magic-input js/Date
 [{:keys [attrs]}]
  (let [date-in (d/fmt FULL-DATE (:value attrs))]
   (dom/input (clj->js (merge attrs {:type "date"
                               :value date-in})))))

(defmethod magic-input js/Boolean
  [{:keys [k attrs chan]}]
  (let [value (:value attrs)]
   (dom/input (clj->js (merge attrs {:checked (js/Boolean value)
                                     :onChange #(put! chan [k (-> % .-target .-checked)])
                                     :type "checkbox"})))))


#_(defmethod magic-input integer?
 [{:keys [k attrs data]}]
  (dom/input (clj->js (merge {:type "number"} attrs))))


(defmethod magic-input :default
  [{:keys [k attrs]}]
  (dom/input (clj->js attrs)))



;___________________________________________________________
;                                                           |
;          Errors state handler                             |
;___________________________________________________________|


(def sch-errors
  "Describes the om-input's error data structure.
   A field can have multiples errors."
  {s/Keyword [s/Keyword]})

(defn validate
  "Generic sequence of validation.
  The first args can be partially applied to generate a custom validator."
  [validation-fn post m]
  (post (validation-fn m)))


(defn verily
  "Inversion of parameters for the verily validation function for partial application of the rules."
  [validators m]
  (v/validate m validators))


(def sch-verily-errs
  "Describe the Verily errors data structure."
  [{:keys [s/Keyword]
    :msg s/Keyword}])

(def sch-schema-errs
  "Describe the Scheam errors data structure"
  {s/Keyword s/Any})

(s/defn transform-verily-errors :- sch-errors
  "Transforms the Verily's error data structure into the common error data structure."
  [errs :- sch-verily-errs]
  (when (seq errs)
    (apply merge-with concat {}
         (for [{:keys [keys msg]} errs
               k keys]
           {k [msg]}))))

(s/defn transform-schema-errors :- sch-errors
  "Transforms the Schema's error data structure into the common error data structure.
  For the moment Schema error are treated as missing field."
  [errs :- sch-schema-errs]
  (when-let [errors (:error errs)]
   (apply merge-with concat
         (for [[k _] errors]
           {k [:mandatory]}))))


(s/defn ^:always-validate  handle-errors :- sch-business-state
  "Set valid to false for each key in errors, true if absent"
  [state :- sch-business-state
   errs :- sch-errors]
  (let [err-ks (set (keys errs))
        all-ks (set (keys state))
        valid-ks (st/difference all-ks err-ks)
        state (reduce (fn [s e]
                        (-> s
                            (assoc-in [e :valid] false)
                            (assoc-in [e :error] (e errs)))) state err-ks)]
    (reduce (fn [s e]
              (-> s
               (assoc-in [e :valid] true)
               (update-in [e] dissoc :error))) state valid-ks)))

;___________________________________________________________
;                                                           |
;          Component builders                               |
;___________________________________________________________|


(defn message
  "Display a dismissable error message"
  [app owner m]
  (reify om/IRenderState
    (render-state [this {:keys [chan ] :as state}]
                  (dom/div #js {:className "alert alert-danger"
                                :role "alert"}
                           (dom/button #js {:type "button"
                                            :className "close"
                                            :data-dismiss "alert"
                                            :onClick #(put! chan [:kill-mess (:k m)])} "x")
                           (:mess m)))))

(defn description
  "Display a small description under the label"
  [app owner m]
  (reify om/IRenderState
    (render-state [_ state]
        (dom/div #js {:className "description"} (:desc m)))))



(defn build-input
  "Handle the display of an input from state and push change on a channel.
   The map of inputs is expected in state under the key :inputs
   The channel is expected in state under key :chan
   The i18n fn is expected in shared under key :i18n"
  ([owner n k t opts]
   (let [{:keys [chan inputs]} (om/get-state owner)
         lang (:lang (om/get-props owner))
         i18n (om/get-shared owner [:i18n lang])
         label (get-in i18n [n k :label] (str/capitalize (name k)))
         value (get-in inputs [k :value])
         error (when-not (get-in inputs [k :valid] true) "has-error has-feedback")
         [err-k & errs] (when error (get-in inputs [k :error]))
         valid (when (get-in inputs [k :valid]) "has-success")
         required (if (get-in inputs [k :required]) "required" "optional")
         desc (get-in i18n [n k :desc])
         attrs {:id (name k)
                :className "form-control"
                :value value
                :onChange #(put! chan [k (e-value %)]) }]
     (dom/div #js {:className (styles "form-group" error valid)}
           (let [mess (get-in i18n [:errors err-k])]
            (when (and error mess)
              (om/build message (om/get-props owner) {:opts {:mess mess :k k}
                                                      :init-state {:chan chan}})))
              (dom/label #js {:htmlFor (name k)
                              :className (styles "control-label" required)}
                         label)
              (when desc (om/build description (om/get-props owner) {:opts {:desc desc}}))
              (when (:labeled opts) (dom/span #js {} value))
              (magic-input {:k k :t t :attrs attrs :chan chan :opts opts :data (get-in i18n [n k :data])}))))
  ([owner n k t]
   (build-input owner n k t {})))



(s/defn ^:always-validate build-init-state :- sch-business-state
  "Build the initial business local state backing the inputs in the form."
  [sch]
  (into {} (for [[k t] sch
                 :let [fk (get k :k k)]]
             [fk {:value ""
                  :required (required? k)}])))

(s/defn ^:always-validate pre-validation :- {s/Keyword s/Any}
  "Create the map that will be validated by the Schema :
   Only keeps :
   - required keys
   - optional keys with non blank values"
  [v :- sch-business-state]
  (into {} (for [[k m] v
                 :let [in (:value m)
                       req (:required m)]
                 :when (or req (not (str/blank? in)))]
             {k (:value m)} )))

(s/defn make-input-comp
  "Build an input form Om component based on a prismatic/Schema"
  ([comp-name
    schema
    action]
   (make-input-comp comp-name schema action {}))
  ([comp-name
    schema
    action
    opts]
   (let [order (:order opts)
         schema-coercer (coerce/coercer schema validation-coercer)
         validators (:validations opts)
         validation (partial validate (partial verily validators) transform-verily-errors)
         checker (partial validate schema-coercer transform-schema-errors)]
     (fn [app owner]
       (reify
         om/IDisplayName
         (display-name [_]
                       (name comp-name))
         om/IInitState
         (init-state [_]
                     {:chan (chan)
                      :inputs (build-init-state schema)
                      :coercers (build-coercer schema)})
         om/IWillMount
         (will-mount [this]
                     (let [{:keys [coercers chan inputs] :as state} (om/get-state owner)]
                       (go
                        (loop []
                          (let [[k v] (<! chan)]
                            (condp = k
                              :kill-mess (om/update-state! owner [:inputs v] #(dissoc % :error) )
                              :create (let [raw (pre-validation v)
                                            coerced (schema-coercer raw)]
                                        (if-let [errs (or (checker raw) (validation coerced))]
                                              (om/set-state! owner [:inputs] (handle-errors v errs))
                                              (do
                                                (om/set-state! owner [:inputs] inputs)
                                                (action app owner coerced))))
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
                             lang (:lang app)
                             title (get-in i18n [lang comp-name :title])]
                         (dom/div #js{:className "panel panel-default"}
                                  (when title
                                    (dom/div #js {:className "panel-heading"}
                                             (dom/h3 #js {:className "panel-title"} title)))
                                  (dom/form #js {:className "panel-body"
                                                          :role "form"}
                                                     (into-array (if order
                                                                  (map (fn [k]
                                                                         (build-input owner comp-name k (su/get-sch schema k) opts)) order)
                                                                  (map (fn [[k t]]
                                                                         (build-input owner comp-name (get k :k k) t opts)) schema)))
                                                     (dom/input #js {:type  "button"
                                                                     :className "btn btn-primary"
                                                                     :value (get-in i18n [lang comp-name :action :label] (str (name comp-name) " action"))
                                                                     :onClick #(put! chan [:create inputs])})
                                                     (om/build description app {:opts {:init-state state
                                                                                         :desc (get-in i18n [lang comp-name :action :desc])}}))))))))))



