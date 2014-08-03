(ns om-inputs.validation
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [schema.macros :as s])
  (:require [om.core :as om :include-macros true]
            [clojure.set :as st]
            [clojure.string :as str]
            [jkkramer.verily :as v]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [om-inputs.schemas :as su :refer [sch-business-state sch-field-state]]
            [om-inputs.date-utils :as d]))


;___________________________________________________________
;                                                           |
;          Errors Schemas                                   |
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
;          Coercers                                         |
;___________________________________________________________|


(defn empty-string-coercer
  "Do not validate an empty string as a valid s/Str"
  [s]
  (if (str/blank? s) nil s))

(defn inst-coercer
  [s]
  (when-not (str/blank? s)
    (d/parse (d/fmt d/default-fmt s))))


(def validation-coercer
  (merge coerce/+string-coercions+ {s/Str empty-string-coercer
                                    (s/maybe s/Str) empty-string-coercer
                                    s/Inst inst-coercer}))


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


(defn build-verily-validator
  [rules]
  (partial validate (v/validations->fn rules) transform-verily-errors ))

(defn build-schema-validator
  [schema]
  (let []))


;___________________________________________________________
;                                                           |
;          Complete Validation                              |
;___________________________________________________________|


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


(s/defn keep-fields-to-validate :- sch-business-state
  [bs :- sch-business-state]
  (into {} (for [[k m] bs
                 :let [in (:value m)
                       req (:required m)]
                 :when (or req (not (str/blank? in)))]
             {k m} )))



(s/defn ^:always-validate
  pre-validation :- {s/Keyword s/Any}
  "Create the map that will be validated by the Schema :
  Only keeps :
  - required keys
  - optional keys with non blank values"
  [bs :- sch-business-state]
  (let [vbs  (keep-fields-to-validate bs)]
    (into {}(for [[k m] vbs]
              {k (:value m)}))))


(s/defn ^:always-validate
  sch-glo->unit :- {s/Keyword s/Any}
  "Transform a Schema into a map of key -> individual Schema"
  [sch ]
  (into {} (for [ [k t] sch]
    {(get k :k k) {k t}})))


(defn build-unit-coercers
  "Build the map of field -> coercer"
  [sch]
  (apply merge (for [[k s] (sch-glo->unit sch)]
                 {k (coerce/coercer s validation-coercer)})))

(s/defn ^:always-validate
  unit-schema-validators :- {s/Keyword s/Any}
  [unit-coercers :- {s/Keyword s/Any}]
  (apply merge (for [[k c] unit-coercers]
                 {k (partial validate c transform-schema-errors)})))

;___________________________________________________________
;                                                           |
;          Unit Validation                                  |
;___________________________________________________________|


(s/defn ^:always-validate
  validate? :- s/Bool
  [s :- sch-field-state]
  (let [{:keys [required value]} s]
   (or required
      (not (str/blank? value)))))


(s/defn ^:always-validate
  add-field-error :- sch-business-state
  "Handle errors for a single field"
  [state :- sch-business-state
   errs :- sch-errors]
  (reduce (fn [s e]
            (-> s
                (assoc-in [e :valid] false)
                (assoc-in [e :error] (e errs)))) state (keys errs)))

(s/defn ^:always-validate
  remove-field-error :- sch-business-state
  [state :- sch-business-state
   k :- s/Keyword]
  (-> state
      (assoc-in [k :valid] true)
      (update-in [k] dissoc :error)))

(defn unit-verily-validation
  "validate a single field against verily rules"
  [fk unit unit-coercers  verily-validator]
  (let [coerced ((fk unit-coercers) unit)
        errs (verily-validator coerced)]
    (when (contains? errs fk)
     (select-keys errs [fk]))))

(defn field-validation!
  "Validate a single field of the local business state"
  [owner f ]
  (let [business-state (om/get-state owner :inputs)
        {:keys [unit-validators unit-coercers verily-validator]} (om/get-state owner)
        field-state (f business-state)
        unit {f (:value field-state)}]
    (when (validate? field-state)
      (om/set-state! owner [:inputs]
                     (if-let [errs (or ((f unit-validators) unit)
                                       (unit-verily-validation f unit unit-coercers verily-validator) )]
                       (add-field-error business-state errs)
                       (remove-field-error business-state f))))))



#_(

     ;; Processus de validation
     ;; Faut il identifier les validation inter champs afin de les jouer au bon moment ?
     ;; Une piste : Les erreurs verily nous indique si la validation est inter champs
     ;; The source schema
    (def dummy-sch {:email s/Str
                    :confirm-email s/Str
                    (s/optional-key :size) s/Int
                    (s/optional-key :company) s/Str})

   ;; The units schemas

   (sch-glo->unit dummy-sch)


   ;; business-state input

   (def bs {:email {:value "email"
                    :required true
                    :type s/Str}
            :confirm-email {:value "titi"
                            :type s/Str
                            :required true}
            :size {:value "7"
                   :required false
                   :type s/Int}
            :company {:value ""
                      :required false
                      :type s/Str}})

   ;; pre-validation keep required fields or non required with value.

   (pre-validation bs)

   (def pvbs {:email "email"
              :confirm-email "titi"
              :size "7"})

   ;; Coercion
   (def sch-coercer (coerce/coercer dummy-sch validation-coercer))

   (def pvcbs (sch-coercer pvbs))

    (assert (= pvcbs {:email "email"
               :confirm-email "titi"
               :size 7}))

   ;;Validation ->
   (def rules [[:min-val 10 :size :size-min-length]
               [:email [:email :confirm-email] :bad-email]
               [:equal [:email :confirm-email] :email-match]])

   (def validator (build-verily-validator rules))

   (validator pvcbs)

   (validator {:email "kjk"})

   (filter #(vector? (:keys %)  ) ((v/validations->fn rules) pvcbs))


   (unit-verily-validation :email {:email "hkhjk"} (build-unit-coercers  dummy-sch) validator)


    ((:email (unit-schema-validators dummy-sch)) {:email nil})

   )
