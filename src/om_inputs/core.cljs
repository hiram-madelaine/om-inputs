(ns om-inputs.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [schema.macros :as s])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! >! <! alts!]]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [clojure.string :as str]
            [om-inputs.utils :refer [full-name]]
            [om-inputs.date-utils :as d]
            [om-inputs.schema-utils :as su :refer [sch-type]]
            [om-inputs.schemas :refer [sch-business-state sch-field-state SchOptions]]
            [om-inputs.validation :as va]
            [om-inputs.i18n :refer [comp-i18n label desc desc? data error ph]]
            [om-inputs.typing-controls :refer [build-typing-control]]
            [jkkramer.verily :as v]
            [goog.events]))

(enable-console-print!)

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
                (dom/option #js {:value code}
                            (get-in data [code :label] (if (keyword? code)
                                                         (full-name code)
                                                         code)))) (:vs t))))


(defmethod magic-input "radio-group"
  [{:keys [k t data attrs chan]}]
  (apply dom/div #js {:className "input-group"}
           (map (fn [code]
                  (dom/div #js {:className "radio"}
                           (dom/input  (clj->js (merge attrs {:type "radio"
                                                              :checked (= (:value attrs) code)
                                                              :className ""
                                                              :id (full-name k)
                                                              :name (full-name k)
                                                              :value code
                                                              :onClick #(put! chan [k code])} )))
                           (get-in data [code :label] (if (keyword? code) (full-name code) code)))) (:vs t))))


(defmethod magic-input s/Inst
  [{:keys [k attrs]}]
  (let [v (:value attrs)
        date-in (d/display-date v)]
    (dom/input (clj->js (merge attrs
                               {:value date-in})))))


(defmethod magic-input s/Bool
  [{:keys [k attrs chan]}]
  (let [value (:value attrs)]
   (dom/input (clj->js (merge attrs {:checked (js/Boolean value)
                                     :onChange #(put! chan [k (-> % .-target .-checked)])
                                     :type "checkbox"})))))


#_(defmethod magic-input integer?
 [{:keys [k attrs data]}]
  (dom/input (clj->js (merge {:type "number"} attrs))))


(defmethod magic-input "range"
 [{:keys [k attrs data]}]
  (dom/input (clj->js (merge {:type "range"} attrs))))

(defmethod magic-input :default
  [{:keys [k attrs]}]
  (dom/input (clj->js attrs)))


;_________________________________________________
;                                                 |
;          Component Utils                        |
;_________________________________________________|

(defn styles
  [& args]
  (str/join " " args))


;_________________________________________________
;                                                 |
;          Component Initialisation               |
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



(defn init-state-value
  "Choose the initial value.
   s/Num fields must be represented as string because of the typing control.
  If an initial value is provided for a s/Num it will be represented as a string in the
  local state."
  [v t]
  (condp = t
    s/Num (str v)
    v))


(s/defn ^:always-validate  build-init-state :- sch-business-state
  "Build the initial business local state backing the inputs in the form.
   It accepts init values from the options
  TODO validate that the value is correct"
  ([sch
    init]
   (into {} (for [[k t] sch
                  :let [fk (get k :k k)
                        t (sch-type t)
                        init-val (get init fk "")]]
              [fk {:value (init-state-value init-val t)
                   :required (required? k)
                   :type t}])))
  ([sch]
   (build-init-state sch {})))




(defn add-date-picker!
  "Decorate an HTML node with google.ui.inputdatepicker"
  [k node chan f]
  (let [dp (d/date-picker f)]
    (.decorate dp node )
    (goog.events/listen dp goog.ui.DatePicker.Events.CHANGE #(do
                                                              (put! chan [k  (d/goog-date->js-date (.-date %))])
                                                              (put! chan [:validate k])))))



(defn handle-date-fields!
  [owner f]
  (let [chan (om/get-state owner :chan)
        state (om/get-state owner :inputs)
        date-fieds (for [[k {:keys [type]}] state
                         :when (= s/Inst type)]
                     k)]
    (doseq [k date-fieds]
      (add-date-picker! k (om/get-node owner (full-name k)) chan f))))



;___________________________________________________________
;                                                           |
;                 Om/React Sub-Components                   |
;___________________________________________________________|


(defn tooltip
  "Display a tooltip next to the field"
  [app owner m]
  (reify
    om/IDidMount
    (did-mount
     [this]
     (let [tool (om/get-node owner (str (:k m) "-tooltip"))
           _ (count (.-textContent tool))
           elem (.getElementById js/document (full-name (:k m)))
           e (.getBoundingClientRect elem)]
       (set! (.-left (.-style tool)) (str (.-width e) "px"))))
    om/IRenderState
    (render-state
     [this {:keys [chan mess] :as state}]
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
    (render-state
     [this {:keys [chan mess ] :as state}]
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
    (render-state
     [_ state]
     (dom/div #js {:className "description"} (:desc m)))))


(defn button-view
  [app owner {:keys [k labels comp-name] :as opts}]
  (reify
    om/IRenderState
    (render-state
     [_ state]
     (let [chan-name (keyword (str (name k) "-chan"))
           chan (om/get-state owner chan-name)
           disabled (get-in state [:action-state k :disabled])
           btn-style (when disabled "disabled")]
       (dom/input #js {:type  "button"
                       :id (str (full-name comp-name) "-" (name k))
                       :disabled disabled
                       :className (styles "btn btn-primary" btn-style)
                       :value (label labels k)
                       :onClick #(put! chan  k)})))))

;___________________________________________________________
;                                                           |
;        Syntactic sugar to access business state           |
;___________________________________________________________|

(s/defn ^:always-validate get-in-bs
            [m :- sch-business-state
             f :- s/Keyword
             k :- s/Keyword]
            (get-in m [f k]))


(s/defn ^:always-validate
  fvalue :- s/Any
        [m k]
        (get-in-bs m k :value))

(s/defn ^:always-validate
  fvalid :- (s/maybe s/Bool)
        [m k]
        (get-in-bs m k :valid))

(s/defn ^:always-validate
  frequired :- s/Bool
        [m k]
            (get-in-bs m k :required))

(s/defn ^:always-validate ferrors :- va/sch-errors-list
        [m k]
        (get-in-bs m k :error))

(s/defn fdisabled :- (s/maybe s/Bool)
  [m k]
  (get-in-bs m k :disabled))

;___________________________________________________________
;                                                           |
;        Business State Manipulation                        |
;___________________________________________________________|


(s/defn assoc-in-all :- sch-business-state
  [bs :- sch-business-state
   k :- s/Keyword
   v :- s/Any]
  (into {}
        (for [[fk s] bs]
          {fk (assoc s k v)})))


(defn disable-all
  "Disable all inputs after successful validation."
  [bs]
  (assoc-in-all bs :disabled true))

(defn enable-all
  "Disable all inputs after successful validation."
  [bs]
  (assoc-in-all bs :disabled false))


;___________________________________________________________
;                                                           |
;                 Om/React Form Component Builders          |
;___________________________________________________________|



(defn build-input
  "Handle the display of an input from state and push change on a channel.
  The map of inputs is expected in state under the key :inputs
  The channel is expected in state under key :chan
  The i18n fn is expected in shared under key :i18n"
  ([owner k t i18n ]
   (build-input owner k t i18n {}))
  ([owner k t i18n opts]
   (let [{:keys [chan inputs lang]} (om/get-state owner)
         full-i18n (om/get-shared owner [:i18n lang])
         valid (fvalid inputs k)
         controled (not (nil? valid))
         invalid (and controled (not valid))
         input-style (cond valid "has-success" invalid "has-error has-feedback" :else "")
         required-style (if (frequired inputs k) "required" "optional")
         [err-k & errs] (when invalid (ferrors inputs k))
         attrs {:id (full-name k)
                :key (full-name k)
                :ref (full-name k)
                :className "form-control"
                :value (fvalue inputs k)
                :onBlur #(put! chan [:validate k])
                :onChange #(put! chan [k (e-value %)])
                :placeholder (ph i18n k)
                :disabled (fdisabled inputs k)}
         attrs (merge attrs (get-in opts [k :attrs]))]
     (dom/div #js {:className (styles "form-group" input-style)}
              (dom/label #js {:htmlFor (full-name k)
                              :className (styles "control-label" required-style)}
                         (label i18n k))
              (when (get-in opts [k :labeled]) (dom/label #js {:className "badge"} (fvalue inputs k)))
              (when (desc? i18n k) (dom/div #js {:className "description"} (desc i18n k)))

              (dom/div #js {:className "input-container"}
                       (magic-input {:k k :t t :attrs attrs :chan chan :opts opts :data (data i18n k)})
                       (let [mess (error full-i18n err-k)]
                         (when (and invalid mess)
                           (om/build tooltip (om/get-props owner) {:opts {:k k}
                                                                   :state {:mess mess}
                                                                   :init-state {:chan chan}}))))))))


(s/defn ^:always-validate
  make-input-comp
  "Build an input form Om component based on a prismatic/Schema"
  ([comp-name :- s/Keyword
    schema
    action]
   (make-input-comp comp-name schema action nil {}))
  ([comp-name :- s/Keyword
    schema
    action
    opts]
  (make-input-comp comp-name schema action nil opts))
  ([comp-name :- s/Keyword
    schema
    action
    clean
    {:keys [validations init]  :as opts} :- SchOptions]
   (let [order (:order opts)
         verily-rules validations
         schema-coercer (coerce/coercer schema va/validation-coercer)
         validation (va/build-verily-validator verily-rules)
         checker (partial va/validate schema-coercer va/transform-schema-errors)
         unit-coercers (va/build-unit-coercers schema)
         unit-validators (va/unit-schema-validators unit-coercers)
         remove-errs-fn (va/build-error-remover verily-rules va/inter-fields-rules)
         typing-controls (build-typing-control schema)
         initial-bs (build-init-state schema init)
         initial-action-state {:action {:disabled false} :clean {:disabled true}}]
     (fn [app owner]
       (reify
         om/IDisplayName
         (display-name
          [_]
          (full-name comp-name))
         om/IInitState
         (init-state
          [_]
          {:chan (chan)
           :action-chan (chan)
           :validation-chan (chan)
           :created-chan (chan)
           :clean-chan (chan)
           :action-state initial-action-state
           :inputs initial-bs
           :unit-coercers unit-coercers
           :unit-validators unit-validators
           :verily-validator validation
           :remove-errs-fn remove-errs-fn
           :validation-deps (va/fields-dependencies verily-rules)})
         om/IWillMount
         (will-mount
          [this]
          (let [{:keys [chan action-chan validation-chan created-chan clean-chan]} (om/get-state owner)]
            (go-loop []
               (om/set-state-nr! owner :action-state initial-action-state )
               (om/set-state! owner :inputs initial-bs)
               (loop []
                 (<! action-chan)
                   (let [{:keys [inputs] :as state} (om/get-state owner)
                         new-bs (va/full-validation inputs state)]
                     (om/set-state! owner [:inputs] new-bs)
                     (if (va/no-error? new-bs)
                       (put! validation-chan :validee)
                       (recur))))
               (<! validation-chan)
                 (let [v (om/get-state owner :inputs)
                       raw (va/pre-validation v)
                       coerced (schema-coercer raw)]
                   (action app owner coerced)
                   (put! created-chan [:created]))
               (<! created-chan)
                 (if (get-in opts [:action :one-shot])
                   (do
                     (om/update-state-nr! owner [:action-state :action :disabled] not)
                     (om/update-state-nr! owner [:action-state :clean :disabled] not)
                     (om/update-state! owner :inputs #(disable-all %)))
                   (recur))
               (<! clean-chan)
                 (clean app owner)
                 (om/set-state-nr! owner [:action-state :action :disabled] false)
                 (om/update-state-nr! owner :inputs #(enable-all %))
                 (recur))
            (go
             (loop []
               (let [[k v] (<! chan)]
                 (condp = k
                   :kill-mess (om/update-state! owner [:inputs v] #(dissoc % :error) )
                   :validate (va/field-validation! owner v)
                   (let [coerce (get typing-controls k (fn [n _] n))
                         old-val (om/get-state owner [:inputs k :value])]
                     (om/set-state! owner [:inputs k :value] (coerce v old-val)))))
               (recur)))))
         om/IDidMount
         (did-mount
          [_]
          (handle-date-fields! owner d/default-fmt))
         om/IWillUnmount
         (will-unmount
          [_]
          (prn (str "WARNING : "  (full-name comp-name) " will unmount !")))
         om/IRenderState
         (render-state
          [_ {:keys [chan inputs action-state] :as state}]
          (let [labels (comp-i18n owner comp-name schema)
                title (get-in labels [:title])]
            (dom/div #js{:className "panel panel-default"
                         :key (full-name comp-name)
                         :ref (full-name comp-name)
                         :id (full-name comp-name)}
                     (when title
                       (dom/div #js {:className "panel-heading"}
                                (dom/h3 #js {:className "panel-title"} title)))
                     (dom/form #js {:className "panel-body"
                                    :role "form"}
                               (into-array (if order
                                             (map (fn [k] (build-input owner k (su/get-sch schema k) labels opts)) order)
                                             (map (fn [[k t]] (build-input owner (get k :k k) t labels opts)) schema)))
                               (dom/div #js {:className "panel-button"}
                                        (om/build button-view app {:state state :opts {:k :action
                                                                                       :labels labels
                                                                                       :comp-name comp-name}})
                                        (om/build button-view app {:state state :opts {:k :clean
                                                                                       :labels labels
                                                                                       :comp-name comp-name}}))
                               (dom/div #js {:className "description"} (desc labels :action)))))))))))



