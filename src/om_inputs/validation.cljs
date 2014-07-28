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

;_________________________________________________
;                                                 |
;          Date Utils                             |
;_________________________________________________|



 #_(def FULL-DATE "yyyy-MM-dd")

 (def FULL-DATE "dd/MM/yyyy")

 (defn parse-date
   ([n o]
    (when-not (str/blank? n)
     (d/parse FULL-DATE n)))
   ([n]
    (parse-date n nil)))

;___________________________________________________________
;                                                           |
;          Validation handlers                              |
;___________________________________________________________|


(defn empty-string-coercer
  "Do not validate an empty string as a valid s/Str"
  [s]
  (if (str/blank? s) nil s))


(def validation-coercer
  (merge coerce/+string-coercions+ {s/Str empty-string-coercer
                                    (s/maybe s/Str) empty-string-coercer
                                    s/Inst #(d/parse (d/fmt FULL-DATE %))}))



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

