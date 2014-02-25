(ns om-inputs.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [schema.macros :as s])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan put! >! <! alts!]]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
            [schema.core :as s]
            [clojure.string :as str]))

(enable-console-print!)


;;;;;;;;; i18n Utils ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->label
  "Translate labels or business data
  If a label is not found the more precise k is used"
  ([ref-data m k]
   "Find label in the ref data"
   (if-let [label (->> k
                    ref-data
                    (some #(when ((comp  #{(k m)} :code) %) %))
                    :label)]
     label
     (str/capitalize (name k))))
  ([ref-data ks]
   (get-in ref-data ks (str/capitalize (name (last ks))))))


;;;;;;;;;;;;;;; Debug Utils ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn trace
  "Display the raw data."
  [app owner]
  (om/component
   (dom/p nil (str (:label app)))))

;;;;;; Generic single input ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn e-value
  "Get value from an event"
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


;;;;;;;;;;;;;; Complete inputs form ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def sch-inputs {:inputs s/Any})

(def sch-chan {:chan ManyToManyChannel})


(def sch-i18n {:i18n {:input s/Any}})

(def sch-state (merge sch-inputs sch-chan {s/Any s/Any}))


(s/defn  ^:always-validate magic-input
        [k :- s/Keyword
         state :- sch-state
         shared :- sch-i18n
         opts]
        (let [{:keys [chan inputs]} state
              i18n (:i18n shared)]
          (make-input chan (->label i18n [:input k]) k (k inputs) opts)))

(defn build-input
  "Handle the display of an input from state and push change on a channel.
   The map of inputs is expected in state under the key :inputs
   The channel is expected in state under key :chan
   The i18n fn is expected in shared under key :i18n
   Handle the display of an input from state and push change on a queue
   The channel is expected in state under key :chan"
  ([owner k opts]
   (let [state (om/get-state owner)
         shared (om/get-shared owner)]
     (magic-input k state shared opts)))
  ([owner k]
   (build-input owner k {})))


(def input [{:code :label :value "" }
            {:code :version :value ""}
            {:code :tier :value ""}
            {:code :cat :value "" :opts {:type "select"}}
            {:code :level :value 0 :opts {:type "range" :min 0 :max 5 :labeled true}}])






(def sch-conf-opts {(s/optional-key :labeled) s/Bool
                    (s/optional-key :min) s/Int
                    (s/optional-key :max) s/Int
                    (s/optional-key :type) (s/enum "text" "range" "number" "color" "select") })


(def sch-conf [{:code s/Keyword
                :value s/Any
                (s/optional-key :opts) sch-conf-opts}])


(s/defn build-init
  "Build the init map backing the inputs in the form."
  [m :- sch-conf]
  (apply merge
         (for  [{:keys [code value]} m]
           {code value})))

(s/defn ^:always-validate make-input-comp
  "Build the input Om component based on the config"
  [conf :- sch-conf]
  (fn [app owner]
    (reify
      om/IInitState
      (init-state [_]
                  (let [init (build-init conf)]
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
      (render-state [_ {:keys [chan inputs] :as state}]
                    (let [i18n (om/get-shared owner :i18n)]
                      (dom/fieldset #js {:className "form"}
                                    (into-array (map (fn [{:keys [code opts]}]
                                                      (build-input owner code opts) )  conf))
                                    (dom/input #js {:type  "button"
                                                    :value (->label i18n [:input :create])
                                                    :onClick #(put! chan [:create inputs])})
                                    (apply dom/div nil (om/build-all trace app))))))))




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
                           (build-input owner :backgroundColor {:type "color" :labeled true})
                           (dom/div nil (build-input owner :width {:type "range" :min 0 :max 500 :labeled true}))
                           (dom/div nil (build-input owner :height {:type "range" :min 0 :max 500 :labeled true}))
                           (dom/div #js {:style #js {:backgroundColor  (:backgroundColor app)
                                                     :width (:width app)
                                                     :height (:height app)}}
                                    (dom/h1 nil (:text app)))))))


(om/root
  app-view
  app-state
  {:target (. js/document (getElementById "app"))
   :shared {:i18n app-ref}})


(om/root
 (make-input-comp input)
 [{:label "Clojure" :version "1.5.1" :level 4}]
 {:target (. js/document (getElementById "app-2"))
  :shared {:i18n {:input {:cat "Catégorie"}}}})
