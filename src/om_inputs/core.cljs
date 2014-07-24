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
            [om-inputs.i18n :refer [comp-i18n label desc desc? data]]
            [jkkramer.verily :as v]
            ))

(enable-console-print!)

;_________________________________________________
;                                                 |
;          Schemas                                |
;_________________________________________________|

(def sch-i18n {:i18n {:inputs s/Any}})


(def sch-field-state {:value s/Any
                      :required s/Bool
                      (s/optional-key :valid) s/Bool
                      (s/optional-key :error) [s/Keyword]})


(def sch-business-state
  "Local business state's data structure "
  {s/Keyword sch-field-state})


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
;          Date Utils                             |
;_________________________________________________|



 (def FULL-DATE "yyyy-MM-dd")

 (defn parse-date
   ([n o]
    (when-not (str/blank? n)
     (d/parse FULL-DATE n)))
   ([n]
    (parse-date n nil)))

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
                                    (s/maybe s/Str) empty-string-coercer
                                    s/Inst parse-date}))

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

(defn sch-type [t]
  "This function tries to determine the leaf Schema.
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
  ; js/Date parse-date ;Let schema coercion deal with data coercion
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
                                         :id (name k)
                                         :name (name k)
                                         :value code
                                         :onClick #(put! chan [k code])})
                         (get-in data [code :label] (if (keyword? code) (name code) code)))) (:vs t))))


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
;          Errors Schemas                             |
;___________________________________________________________|


(def sch-errors
  "Describes the om-input's error data structure.
   A field can have multiples errors."
  {s/Keyword [s/Keyword]})


(def sch-verily-errs
  "Describe the Verily errors data structure."
  [{:keys [s/Keyword]
    :msg s/Keyword}])

(def sch-schema-errs
  "Describe the Scheam errors data structure"
  {s/Keyword s/Any})


;___________________________________________________________
;                                                           |
;          Validation handlers                              |
;___________________________________________________________|


(defn validate
  "Generic sequence of validation.
  The first args can be partially applied to generate a custom validator."
  [validation-fn post m]
  (post (validation-fn m)))


(s/defn transform-schema-errors :- sch-errors
  "Transforms the Schema's error data structure into the common error data structure.
  For the moment Schema error are treated as missing field."
  [errs :- sch-schema-errs]
  (when-let [errors (:error errs)]
   (apply merge-with concat
         (for [[k _] errors]
           {k [:mandatory]}))))

(defn verily
  "Inversion of parameters for the verily validation function for partial application of the rules."
  [validators m]
  (v/validate m validators))


(s/defn transform-verily-errors :- sch-errors
  "Transforms the Verily's error data structure into the common error data structure."
  [errs :- sch-verily-errs]
  (when (seq errs)
    (apply merge-with concat {}
         (for [{:keys [keys msg]} errs
               k keys]
           {k [msg]}))))


(s/defn ^:always-validate validate? :- s/Bool
  [s :- sch-field-state]
  (let [{:keys [required value]} s]
   (or required
      (not (str/blank? value)))))


(s/defn ^:always-validate add-field-error :- sch-business-state
  "Handle errors for a single field"
  [state :- sch-business-state
   errs :- sch-errors]
  (reduce (fn [s e]
            (-> s
                (assoc-in [e :valid] false)
                (assoc-in [e :error] (e errs)))) state (keys errs)))

(s/defn ^:always-validate remove-field-error :- sch-business-state
  [state :- sch-business-state
   k :- s/Keyword]
  (-> state
      (assoc-in [k :valid] true)
      (update-in [k] dissoc :error)))


(defn field-validation!
  "Validate a single field of the local business state"
  [owner f coercers]
  (let [business-state (om/get-state owner :inputs)
        field-state (f business-state)]
    (when (validate? field-state)
      (om/set-state! owner [:inputs]
                     (if-let [errs ((f coercers)  {f (:value field-state)})]
                       (add-field-error business-state errs)
                       (remove-field-error business-state f))))))



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



(s/defn ^:always-validate sch-glo->unit :- {s/Keyword s/Any}
  "Transform a Schema into a map of key -> individual Schema"
  [sch ]
  (into {} (for [ [k t] sch]
    {(get k :k k) {k t}})))

(s/defn ^:always-validate unit->coercer :- {s/Keyword s/Any}
  [sch]
  (apply merge (for [[k s] (sch-glo->unit sch)]
                 {k (partial validate (coerce/coercer s validation-coercer) transform-schema-errors)})))



;___________________________________________________________
;                                                           |
;          Component builders                               |
;___________________________________________________________|


(defn tooltip
  "Display a tooltip next to the field"
  [app owner m]
  (reify
    om/IDidMount
    (did-mount [this]
               (let [tool (om/get-node owner (str (:k m) "-tooltip"))
                     _ (count (.-textContent tool))
                     elem (.getElementById js/document (name (:k m)))
                     e (.getBoundingClientRect elem)]
                 (set! (.-left (.-style tool)) (str (.-width e) "px"))))
    om/IRenderState
    (render-state [this {:keys [chan mess] :as state}]
                  (dom/div #js {:className "popover right in"
                                :role "alert"
                                :ref (str (:k m) "-tooltip")}
                           (dom/div #js {:className "arrow"} "")
                           (dom/div #js {:className "popover-content"} mess
                                    (dom/div #js {:type "button"
                                                     :className "close"
                                                     :onClick #(put! chan [:kill-mess (:k m)])} "x"))))))



(defn message
  "Display a dismissable error message"
  [app owner m]
  (reify om/IRenderState
    (render-state [this {:keys [chan mess ] :as state}]
                  (dom/div #js {:className "alert alert-danger"
                                :role "alert"}
                           (dom/button #js {:type "button"
                                            :className "close"
                                            :data-dismiss "alert"
                                            :onClick #(put! chan [:kill-mess (:k m)])} "x")
                           mess))))

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
  ([owner k t i18n opts]
   (let [{:keys [chan inputs lang]} (om/get-state owner)
         full-i18n (om/get-shared owner [:i18n lang])
         value (get-in inputs [k :value])
         error (when-not (get-in inputs [k :valid] true) "has-error has-feedback")
         [err-k & errs] (when error (get-in inputs [k :error]))
         valid (when (get-in inputs [k :valid]) "has-success")
         required (if (get-in inputs [k :required]) "required" "optional")
         attrs {:id (name k)
                :ref (name k)
                :className "form-control"
                :value value
                :onBlur #(put! chan [:validate k])
                :onChange #(put! chan [k (e-value %)]) }]
     (dom/div #js {:className (styles "form-group" error valid)}

              (dom/label #js {:htmlFor (name k)
                              :className (styles "control-label" required)}
                         (label i18n k))
              (when (desc? i18n k) (dom/div #js {:className "description"} (desc i18n k)))
              (when (:labeled opts) (dom/span #js {} value))
              (dom/div #js {:className "input-container"}
                       (magic-input {:k k :t t :attrs attrs :chan chan :opts opts :data (data i18n k)})
                       (let [mess (get-in full-i18n [:errors err-k])]
                         (when (and error mess)
                           (om/build tooltip (om/get-props owner) {:opts {:k k}
                                                                   :state {:mess mess}
                                                                   :init-state {:chan chan}})))))))
  ([owner k i18n t]
   (build-input owner k t i18n {})))





(s/defn ^:always-validate  build-init-state :- sch-business-state
  "Build the initial business local state backing the inputs in the form."
  [sch]
  (into {} (for [[k t] sch
                 :let [fk (get k :k k)]]
             [fk {:value ""
                  :required (required? k)}])))



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
         checker (partial validate schema-coercer transform-schema-errors)
         unit-coercers (unit->coercer schema)]
     (fn [app owner]
       (reify
         om/IDisplayName
         (display-name
          [_]
          (name comp-name))
         om/IInitState
         (init-state
          [_]
          {:chan (chan)
           :inputs (build-init-state schema)
           :coercers (build-coercer schema)
           :unit-coercers unit-coercers})
         om/IWillMount
         (will-mount
          [this]
          (let [{:keys [coercers unit-coercers chan inputs] :as state} (om/get-state owner)]
            (go
             (loop []
               (let [[k v] (<! chan)]
                 (condp = k
                   :kill-mess (om/update-state! owner [:inputs v] #(dissoc % :error) )
                   :validate (field-validation! owner v unit-coercers)
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
         (will-update
          [this props state])
         om/IRenderState
         (render-state
          [_ {:keys [chan inputs lang] :as state}]
          (let [labels (comp-i18n owner comp-name schema)
                title (get-in labels [:title])]
            (dom/div #js{:className "panel panel-default"}
                     (when title
                       (dom/div #js {:className "panel-heading"}
                                (dom/h3 #js {:className "panel-title"} title)))
                     (dom/form #js {:className "panel-body"
                                    :role "form"}
                               (into-array (if order
                                             (map (fn [k]
                                                    (build-input owner k (su/get-sch schema k) labels opts)) order)
                                             (map (fn [[k t]]
                                                    (build-input owner (get k :k k) t labels opts)) schema)))
                               (dom/input #js {:type  "button"
                                               :className "btn btn-primary"
                                               :value (label labels :action )
                                               :onClick #(put! chan [:create inputs])})
                               (dom/div #js {:className "description"} (desc labels :action)))))))))))



