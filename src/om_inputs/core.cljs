(ns om-inputs.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [schema.macros :as s])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! >! <! alts!]]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [clojure.string :as str]))

(enable-console-print!)

;_________________________________________________
;                                                 |
;          Low level Clojure Utils                |
;_________________________________________________|


;_________________________________________________
;                                                 |
;          i18n Utils                             |
;_________________________________________________|





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

(def exemple-input [{:field :label :value "" :coercer (fn [n o](str/upper-case n))}
                    {:field :version :value "" :coercer (fn [n o] (if (re-matches #"[0-9]*" n) n o))}
                    {:field :tier :value ""}
                    {:field :cat :value "" :opts {:type "select"}}
                    {:field :level :value 4 :coercer #(js/parseInt %) :opts {:type "range" :min 0 :max 5 :labeled true}}
                    {:field :comment :value ""}])
;_________________________________________________
;                                                 |
;          Events Utils                           |
;_________________________________________________|


(defn e-value
  "Get value from an event"
  [e]
  (-> e .-target .-value))

;___________________________________________________________
;                                                           |
;          Mulitmethod to handle differents inputs form     |
;___________________________________________________________|


(defmulti magic-input (fn [k t attrs data] (type t)))



(defmethod magic-input schema.core.EnumSchema
  [k t  attrs data]
  (apply dom/select (clj->js attrs)
                       (dom/option #js {:value ""} "")
                       (map (fn [code]
                              (dom/option #js {:value code} (get data code code))) (:vs t))))

(defmethod magic-input :default
  [k t attrs data]
  (dom/input (clj->js attrs)))


(defn build-input
  "Handle the display of an input from state and push change on a channel.
   The map of inputs is expected in state under the key :inputs
   The channel is expected in state under key :chan
   The i18n fn is expected in shared under key :i18n"
  ([owner n k t opts]
   (let [
         state (om/get-state owner)
         {:keys [chan inputs]} state
         i18n (om/get-shared owner [:i18n] )
         label (get-in i18n [n k :label] (name k))
         data (get-in i18n [n k :data])
         value (k inputs)
         attrs {:id (name k)
                :className "form-control"
                :value value
                :onChange #(put! chan [k (e-value %)])}]
     (dom/div #js {:className "form-group"}
           (dom/label #js {:htmlFor (name k)} label)
              (when (:labeled opts) (dom/span #js {} value))
              (magic-input k t attrs data))))
  ([owner n k t]
   (build-input owner n k t {})))



(s/defn build-init
  "Build the inial local state backing the inputs in the form."
        [sch]
        (into {} (for [[k t] sch]
          [k ""])))



(defn key-value-view
  [entry owner]
  (om/component
   (dom/label #js {:className "item-view"} (val entry))))


(defn item-view
  [item owner]
  (om/component
   (apply dom/span nil (om/build-all key-value-view item))))


(s/defn make-input-comp
  "Build an input form Om component based on the config"
  [comp-name
   conf
   action]
  (let [input-coercer (coerce/coercer conf coerce/json-coercion-matcher)]
   (fn [app owner]
    (reify
      om/IInitState
      (init-state [_]
                  {:chan (chan)
                   :inputs (build-init conf)
                   :coercers {}})
      om/IWillMount
      (will-mount [this]
                  (let [{:keys [coercers chan inputs] :as state} (om/get-state owner)]
                    (go
                     (loop []
                       (let [[k v] (<! chan)
                             coerce (get coercers k (fn [n _] n))
                             o (om/get-state owner [:inputs k])]
                         (condp = k
                           :create (let [res (input-coercer v)]
                                     (if (:error res)
                                      (prn (:error res))
                                      (action app owner res))
                                     (om/set-state! owner [:inputs] inputs))
                           (om/set-state! owner [:inputs k] (coerce v o))))
                       (recur)))))
      om/IRenderState
      (render-state [_ {:keys [chan inputs] :as state}]
                    (let [i18n (om/get-shared owner :i18n)]
                      (dom/fieldset nil (dom/form #js {:className "form"
                                                       :role "form"}
                                    (into-array (map (fn [[k t]]
                                                      (build-input owner comp-name k t)) conf))
                                    (dom/input #js {:type  "button"
                                                    :className "btn btn-primary"
                                                    :value (get-in i18n [comp-name :action])
                                                    :onClick #(put! chan [:create inputs])})))))))))



