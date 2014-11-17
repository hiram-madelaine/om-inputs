(ns om-inputs.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! >! <! alts!]]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
            [schema.core :as s :include-macros true]
            [schema.coerce :as coerce]
            [clojure.string :as str]
            [om-inputs.utils :refer [full-name]]
            [om-inputs.date-utils :as d]
            [om-inputs.schema-utils :as su :refer [sch-type]]
            [om-inputs.schemas :refer [sch-business-state sch-field-state SchOptions]]
            [om-inputs.validation :as va]
            [om-inputs.i18n :as i :refer [comp-i18n label desc desc? data error ph info]]
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

;_________________________________________________
;                                                 |
;          Component Utils                        |
;_________________________________________________|

(defn styles
  [& args]
  (str/join " " args))

;___________________________________________________________
;                                                           |
;          Multimethod to handle different inputs form      |
;___________________________________________________________|


(defmulti magic-input
  (fn [{opts :opts}]
    (get-in opts [:type] (sch-type (:k-sch opts)))))

(defn enum-label
  "Display the i18n label of a select or radio group entry, fall backs to the code."
  [i18n code]
  (get-in i18n [:data code :label] (if (keyword? code) (full-name code) code)))

(defmethod magic-input "enum"
  [{{:keys [attrs i18n k-sch]} :opts}]
  (apply dom/select (clj->js attrs)
         (dom/option #js {:value ""} "")
         (map (fn [code]
                (dom/option #js {:value code}
                            (enum-label i18n code)))
              (:vs k-sch))))


(defn radio-group
  [style {{:keys [attrs k i18n k-sch]} :opts chan :chan}]
  (apply dom/div #js {:className "input-group"}
         (map (fn [code]
                (dom/div #js {:className style}
                         (dom/label #js {}
                                    (dom/input (clj->js (merge attrs {:type      "radio"
                                                                      :checked   (= code (:value attrs))
                                                                      :className ""
                                                                      ;  :name (full-name k)
                                                                      :value     code
                                                                      :onClick   #(put! chan [k code])})))
                                    (enum-label i18n code))))
              (:vs k-sch))))

(defmethod magic-input "radio-group"
  [m]
  (radio-group "radio" m))

(defmethod magic-input "radio-group-inline"
  [m]
  (radio-group "radio-inline" m))


(defn make-segmented
  "HOF, generates a function,  that closes over the value, for segemented control"
  [type k value i18n chan]
  (fn [code]
    (dom/button #js {:type      type
                     :active    (= code value)
                     :className (styles "btn" (if (= code value) "btn-primary" "btn-default"))
                     :key       (str (full-name k) "/" code)
                     :id        (str (full-name k) "/" code)
                     :value     code
                     :onClick   #(put! chan [k code])}
                (enum-label i18n code))))

(defmethod magic-input "btn-group"
  [{{:keys [attrs k i18n k-sch]} :opts chan :chan}]
  (apply dom/div (clj->js (merge attrs {:className "btn-group"}))
         (map
           (make-segmented "button" k (:value attrs) i18n chan)
           (:vs k-sch))))

(defmethod magic-input "range-btn-group"
  [{{:keys [attrs k]} :opts chan :chan}]
  (let [{:keys [min max step value] :or {step 1}} attrs]
    (apply dom/div (clj->js (merge attrs {:className "btn-group"}))
           (map
             (make-segmented "button" k value i18n chan)
                (range (int min) (inc (int max)) step)))))

(defmethod magic-input "stepper"
  [{{:keys [attrs k]} :opts chan :chan}]
  (let [{:keys [min max step value]} attrs
        plus (if step (partial + (long step)) inc)
        minus (if step (partial + (- (long step))) dec)]
    (prn (minus value))
   (dom/div (clj->js (merge attrs {:className "btn-group"}))
            (dom/button #js {:type      "button"
                             :className "btn btn-default"
                             :onClick #(when (or (nil? min)
                                                 (and min (<= (int min) (minus value))))
                                        (put! chan [k (minus value)]))} "-")
            (dom/button #js {:type      "button"
                             :className "btn btn-default"
                             :onClick #(when (or (nil? max)
                                                 (and max (<= (plus value) (int max))))
                                        (put! chan [k (plus value)]))} "+"))))


(defmethod magic-input s/Inst
  [{{:keys [attrs]} :opts}]
  (let [v (:value attrs)
        date-in (d/display-date v)]
    (dom/input (clj->js (merge attrs {:value date-in})))))


(defmethod magic-input s/Bool
  [{{:keys [k attrs]} :opts chan :chan}]
  (let [value (:value attrs)]
    (dom/input (clj->js (merge attrs {:checked  (js/Boolean value)
                                      :onChange #(put! chan [k (-> % .-target .-checked)])
                                      :type     "checkbox"})))))


#_(defmethod magic-input integer?
 [{:keys [k attrs data]}]
  (dom/input (clj->js (merge {:type "number"} attrs))))


(defmethod magic-input "range"
 [{{:keys [attrs]} :opts}]
  (dom/input (clj->js (merge {:type "range"} attrs))))

(defmethod magic-input "now"
  [{{:keys [attrs k]} :opts chan :chan}]
  (dom/input (clj->js (merge attrs {:type           "button"
                                    :className      "btn"
                                    :preventDefault true
                                    :onClick        #(put! chan [k (js/Date.)])}))))


(defmethod magic-input :default
  [{{:keys [attrs]} :opts}]
  (dom/input (clj->js attrs)))




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


;___________________________________________________________
;                                                           |
;                 Decorate s/inst fields                    |
;___________________________________________________________|



(defn add-date-picker!
  "Decorate an HTML node with google.ui.inputdatepicker"
  [k node chan f]
  (let [dp (d/date-picker f)]
    (.decorate dp node )
    (goog.events/listen dp goog.ui.DatePicker.Events.CHANGE #(do
                                                              (put! chan [k  (d/goog-date->js-date (.-date %))])
                                                              (put! chan [:validate k])))))



(defn handle-date-fields!
  [owner f opts]
  (let [chan (om/get-state owner :chan)
        state (om/get-state owner :inputs)
        date-fieds (for [[k {:keys [type]}] state
                         :when (and (= s/Inst type)
                                    (not= "now" (get-in opts [k :type])))]
                     k)]
    (doseq [k date-fieds]
      (add-date-picker! k (om/get-node owner (full-name k)) chan f))))



;___________________________________________________________
;                                                           |
;                 Om/React Sub-Components                   |
;___________________________________________________________|


(defn tooltip
  "Display a tooltip next to the field to inform the user.
  options :
  :k the target field
  :type serves to build the css class tooltip-type
  :action attach a function when closing the tooltip"
  [app owner opts]
  (reify
    om/IDidMount
    (did-mount
     [_]
     (let [_ (prn (str "tooltip : " (full-name (:k opts))))
           tool (om/get-node owner (str (:k opts) "-tooltip"))
           elem (.getElementById js/document (full-name (:k opts)))
           rect-tool (.getBoundingClientRect tool)
           rect (.getBoundingClientRect elem)
           delta (* 0.5 (- (.-height rect) (.-height rect-tool)))]
       (set! (.-left (.-style tool)) (str (.-width rect) "px"))
       (set! (.-top (.-style tool)) (str delta "px"))))
    om/IRender
    (render
     [_]
     (dom/div #js {:className (styles "popover right" (str "popover-" (:type opts)))
                   :role "alert"
                   :ref (str (:k opts) "-tooltip")}
              (dom/div #js {:className "arrow"} "")
              (when (:title app)  (dom/div #js {:className "popover-title"} (:title app)))
              (dom/div #js {:className "popover-content"} (:mess app)
                       (dom/div #js {:type "button"
                                     :className "close"
                                     :onClick (:action opts)} "x"))))))



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
  (om/component
   (dom/div #js {:className "description"} (:desc m))))


(defn button-view
  [app owner {:keys [k labels comp-name]}]
  (reify
    om/IRenderState
    (render-state
     [_ state]
     (let [chan-name (keyword (str (name k) "-chan"))
           chan (om/get-state owner chan-name)
           button-state (get-in state [:action-state k])
           btn-style (when button-state (name button-state))]
       (dom/div nil
                (dom/button #js {:type  "button"
                                 :id (str (full-name comp-name) "-" (name k))
                                 :disabled (= "disabled" btn-style)
                                 :className (styles "btn btn-primary has-spinner has-error" btn-style)
                                 :onClick #(put! chan  k)}
                           (label labels k)
                            (dom/span #js {:className "error"}
                                     (dom/i #js {:className "fa fa-ban text-danger"}))
                            (dom/span #js {:className "spinner"}
                                     (dom/i #js {:className "fa fa-spin fa-cog"})))
                (dom/div #js {:className "description"} (:desc labels)))))))

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

(defn ffocus
  [m k]
  (get-in-bs m k :focus))

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

(defn error-mess
  "Finds the i18n message for the first error on a field."
  [owner kbs lang]
  (let [full-i18n (om/get-shared owner [:i18n lang])]
   (when-let [[err-k & errs] (:error kbs)]
     (i/error full-i18n err-k))))

(defn validation-style
  [{:keys [valid invalid]}]
  (cond valid "has-success" invalid "has-error has-feedback" :else ""))

(defn required-style
  [kbs]
  (if (:required kbs) "required" "optional"))



(defmulti layout-input
          (fn [_ {layout :layout} _ _]
            layout))

(defmethod layout-input :default
  [owner opts kbs {:keys [invalid] :as val-states}]
  (let [{:keys [chan lang]} (om/get-state owner)]
    (dom/div #js {:className (styles "form-group" (validation-style val-states))}
            (dom/label #js {:htmlFor   (full-name (:k opts))
                            :className (styles "control-label" (required-style kbs))}
                       (i/label opts))
            (when (:labeled opts) (dom/label #js {:className "badge"} (:value kbs)))
            (when (i/desc opts) (dom/div #js {:className "description"} (i/desc opts)))
            (dom/div #js {:className "input-container"}
                     (let [opts (assoc-in opts [:attrs :className] "form-control")]
                       (magic-input {:chan chan :opts opts}))
                     (when (and (i/info opts)
                                (:focus kbs))
                       (om/build tooltip {:mess  (i/info opts)
                                          :title (i/info-title opts)} {:opts {:k (:k opts) :type "info"}}))
                     (let [mess (error-mess owner kbs lang)]
                       (when (and invalid mess)
                         (om/build tooltip {:mess mess} {:opts  {:k      (:k opts)
                                                                 :type   "error"
                                                                 :action #(put! chan [:kill-mess (:k opts)])}
                                                         :state {:mess mess}})))))))

(defmethod layout-input "in-line"
  [owner opts kbs {:keys [invalid] :as val-states}]
  (let [{:keys [chan lang]} (om/get-state owner)]
   (dom/div #js {:className (validation-style val-states)}
            (dom/div #js {:className (styles "checkbox" (required-style kbs))}
                     (dom/label #js {:htmlFor   (full-name (:k opts))}
                                (when (:labeled opts) (dom/label #js {:className "badge"} (:value kbs)))
                                (dom/div #js {:className "input-container"}
                                         (magic-input {:chan chan :opts opts})
                                         (when (and (i/info opts)
                                                    (:focus opts))
                                           (om/build tooltip {:mess  (i/info opts)
                                                              :title (i/info-title opts)} {:opts {:k (:k opts) :type "info"}})))
                                (dom/div #js {} (i/label opts)))
                     (let [mess (error-mess owner kbs lang)]
                       (when (and invalid mess)
                         (om/build tooltip {:mess mess} {:opts  {:k      (:k opts)
                                                                 :type   "error"
                                                                 :action #(put! chan [:kill-mess (:k opts)])}
                                                         :state {:mess mess}}))))
            (when (i/desc opts) (dom/p #js {:className "description"} (i/desc opts))))))


(defn build-input
  [owner {:keys [k] :as opts}]
  (let [{:keys [chan inputs]} (om/get-state owner)
        kbs (k inputs)
        valid (:valid kbs)
        controled (not (nil? valid))
        invalid (and controled (not valid))
        val-states {:valid     valid
                    :invalid   invalid
                    :controled controled}
        k-attrs {:id          (full-name k)
                 :key         (full-name k)
                 :ref         (full-name k)
                 :value       (:value kbs)
                 :onBlur      #(do
                                (put! chan [:focus k])
                                (put! chan [:validate k]))
                 :onFocus     #(put! chan [:focus k])
                 :onChange    #(put! chan [k (e-value %)])
                 :placeholder (get-in opts [:i18n :ph])
                 :disabled    (:disabled  kbs)}
        opts (update-in opts [:attrs] #(merge k-attrs %))]
    (layout-input owner opts kbs val-states)))



(def action-states
  {:init :active
   :in-error :active
   :active :disabled
   :disabled :init})

(def error-flow
  {:init :in-error
   :in-error :in-error})

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
    {:keys [validations init] :as opts} :- SchOptions]
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
         initial-action-state {:action :init :clean :disabled}
         willReceivePropsFn (:IWillReceiveProps opts)]
     (fn [app owner]
       (reify
         om/IDisplayName
         (display-name
          [_]
          (full-name comp-name))
         om/IInitState
         (init-state
          [_]
          {:opts opts
           :chan (chan)
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
               (when-not (get-in opts [:action :no-reset])(om/set-state! owner :inputs initial-bs))
                     (om/set-state! owner :action-state initial-action-state )
               (loop []
                 (<! action-chan)
                   (let [{:keys [inputs] :as state} (om/get-state owner)
                         new-bs (va/full-validation inputs state)]
                     (om/set-state! owner [:inputs] new-bs)
                     (if (va/no-error? new-bs)
                       (put! validation-chan :validee)
                       (do
                         (om/update-state! owner [:action-state :action] error-flow)
                         (recur)))))
               (<! validation-chan)
                 (let [v (om/get-state owner :inputs)
                       raw (va/pre-validation v)
                       coerced (schema-coercer raw)]
                   (om/update-state! owner [:action-state :action] action-states)
                   (if (get-in opts [:action :async])
                     (action app owner coerced created-chan)
                     (try
                         (action app owner coerced)
                         (put! created-chan [:ok])
                         (catch js/Object e
                           (put! created-chan [:ko e])))))
                     (let [[v m]  (<! created-chan)]
                       (if (= :ko v)
                         (do
                           (prn (str "An error has occured during action : " m))
                           (recur))
                         (if (get-in opts [:action :one-shot])
                           (do
                             (om/update-state-nr! owner [:action-state :action] action-states)
                             (om/update-state-nr! owner [:action-state :clean] action-states)
                             (om/update-state! owner :inputs #(disable-all %)))
                           (recur))))
               (<! clean-chan)
                 (clean app owner)
                 (om/update-state-nr! owner [:action-state :action] action-states)
                 (om/update-state-nr! owner :inputs #(enable-all %))
                 (recur))
            (go
             (loop []
               (let [[k v] (<! chan)]
                 (condp = k
                   :focus (om/update-state! owner [:inputs v :focus] not)
                   :kill-mess (om/update-state! owner [:inputs v] #(dissoc % :error) )
                   :validate (va/field-validation! owner v)
                   (let [coerce (get typing-controls k (fn [n _] n))
                         old-val (om/get-state owner [:inputs k :value])]
                     (om/set-state! owner [:inputs k :value] (coerce v old-val)))))
               (recur)))))
         om/IDidMount
         (did-mount
          [_]
          (handle-date-fields! owner d/default-fmt opts))
         om/IWillUnmount
         (will-unmount
          [_]
          (prn (str "WARNING : "  (full-name comp-name) " will unmount !")))
         om/IWillReceiveProps
         (will-receive-props
          [this next-props]
          (when willReceivePropsFn
           (willReceivePropsFn owner next-props)))
         om/IWillUpdate
         (will-update
          [this next-props next-state])
         om/IRenderState
         (render-state
          [_ {:keys [chan inputs action-state dyn-opts] :as state}]
          (let [labels (comp-i18n owner comp-name schema)
                title (get-in labels [:title])
                opts (merge-with merge opts dyn-opts)
                comp-class (get-in opts [comp-name :className])]
            (dom/div #js{:className (styles "panel panel-default" comp-class)
                         :key (full-name comp-name)
                         :ref (full-name comp-name)
                         :id (full-name comp-name)}
                     (when title
                       (dom/div #js {:className "panel-heading"}
                                (dom/h3 #js {:className "panel-title"} title)))
                     (dom/form #js {:className "panel-body"
                                    :role "form"}
                               (into-array (if order
                                             (map (fn [k] (build-input owner (assoc (k opts) :k k :k-sch (su/get-sch schema k) :i18n (k labels)))) order)
                                             (map (fn [[k t]]
                                                    (let [k (if (keyword? k) k (:k k))]
                                                     (build-input owner (assoc (k opts) :k k :k-sch t :i18n (k labels))))) schema)))
                               (dom/div #js {:className "panel-button"}
                                        (om/build button-view app {:state state :opts {:k :action
                                                                                       :labels (:action labels)
                                                                                       :comp-name comp-name}})
                                        (om/build button-view app {:state state :opts {:k :clean
                                                                                       :labels (:clean labels)
                                                                                       :comp-name comp-name}})))))))))))



