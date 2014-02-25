(ns om-inputs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! >! <! alts!]]))

(enable-console-print!)


(defn ->label
  "Translate labels or business data"
  ([ref-data m k]
   "Find label in the ref data"
   (->> k
        ref-data
        (some #(when ((comp  #{(k m)} :code) %) %))
        :label))
  ([ref-data ks]
   (get-in ref-data ks)))



(defn trace
  "Display the raw data."
  [app owner]
  (om/component
   (dom/p nil (str app))))

;;;;;; Skill input ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn e-value
  "Get value from event"
  [e]
  (-> e .-target .-value))

(defn make-select
  [c l k v data]
  (dom/span nil
           (dom/label nil l)
           (apply dom/select #js {:value v
                                  :onChange #(put! c [k (e-value %)])}
                       (dom/option #js {:value ""} "")
                       (map (fn [{:keys [code label]}]
                              (dom/option #js {:value code} label)) data))))

(defn- make-input
  "Low level function to make an input field with 2 ways linkage."
  ([c l k v ]
   (make-input c l k v {}))
  ([c l k v  opts]
   (let [put-chan #(put! c [k (e-value %)])]
     (dom/span nil
                    (dom/label nil l)
                    (dom/input (clj->js (merge {:value v
                                                :onChange put-chan} opts)))
               (when (:labeled opts)
                 (dom/label nil v))))))
(defn build-input
  "Handle the complete display of an input and 2 ways linkage.
   opts is a map of options accepted by the React input
   The channel is expected in state under key :chan
   The i18n fn is expected in shared under key :i18n"
  [owner k & opts]
  (let [{:keys [chan inputs] :as state} (om/get-state owner)
        i18n (om/get-shared owner :i18n)
        opts (apply hash-map opts)]
    (make-input chan (i18n [:input k]) k (k inputs) opts)))


(def input [{:code :label :value "" }
            {:code :version :value 0}
            {:code :tier :value ""}
            {:code :cat :value "" :opts {:type "select"}}
            {:code :level :value 0 :opts {:type "range" :min 0 :max 5}}])


(defn build-init [m]
  (apply merge
         (for  [{:keys [code value]} m]
           {code value})))


(build-init input)

(defn make-input-comp
  [app owner m]
  (fn [app owner]
    (reify
      om/IInitState
      (init-state [_]
                  (let [init {:label "" :version "" :level 0 :cat "" :tier ""}]
                    {:chan (chan)
                     :inputs init
                     :init init
                     :coercers {:level int}}))
      om/IWillMount
      (will-mount [this]
                  (let [{ :keys [coercers chan init] :as state} (om/get-state owner)]
                    (go
                     (loop []
                       (let [[k v] (<! chan)
                             coerce (get coercers k identity)]
                         (condp = k
                           :create (do
                                     (om/transact! app (fn [skills] (conj skills v)))
                                     (om/set-state! owner [:inputs] init))
                           (om/set-state! owner [:inputs k] (coerce v))))
                       (recur)))))
      om/IRenderState
      (render-state [_ {chan :chan  {:keys [label level cat tier version] :as new-skill} :inputs }]
                    (let [i18n (om/get-shared owner :i18n)]
                      (dom/fieldset #js {:className "form"}
                                    (build-input owner :label)
                                    (build-input owner :version)
                                    (build-input owner :level :type "range" :min 1 :max 5)
                                    (make-select chan "Tier" :tier tier (i18n [:tier] ))
                                    (make-select chan (i18n [:input :cat]) :cat cat (i18n [:cat]))
                                    (dom/input #js {:type  "button"
                                                    :value (i18n [:input :add-skill])
                                                    :onClick #(put! chan [:create new-skill])})))))))















(def app-state (atom {:text "Hello world!"
                      :backgroundColor "blue"
                      :width 500
                      :height 250}))

(def app-ref {:input {:width "Largeur"
                      :height "Hauteur"
                      :backgroundColor "Couleur"}})



(defn app-view
  [app owner]
  (reify
    om/IInitState
    (init-state [_]
                {:inputs (select-keys (om/get-props owner)[:width :height :backgroundColor])
                 :chan (chan)})
    om/IWillMount
     (will-mount [this]
                 (let [chan (om/get-state owner :chan)]
                   (go
                    (loop []
                      (let [[k v] (<! chan)]
                        (om/set-state! owner [:inputs k] v)
                        (om/transact! app (fn [box] (assoc box k v)))
                        (recur))))))
    om/IRenderState
    (render-state [_ {:keys [inputs] :as state}]
                  (dom/div nil
                           (build-input owner :backgroundColor :type "color" :labeled true)
                           (dom/div nil  (build-input owner :width :type "range" :min 0 :max 500 :labeled true))
                           (dom/div nil (build-input owner :height :type "range" :min 0 :max 500 :labeled true))
                           (dom/div #js {:style #js {:backgroundColor  (:backgroundColor app)
                                                     :width (:width app)
                                                     :height (:height app)}}
                                    (dom/h1 nil (:text app)))))))


(om/root
  app-view
  app-state
  {:target (. js/document (getElementById "app"))
   :shared {:i18n (partial ->label app-ref)}})
