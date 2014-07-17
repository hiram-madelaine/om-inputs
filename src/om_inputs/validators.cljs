(ns om-inputs.validators
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [schema.macros :as s])
  (:require [jkkramer.verily :as v]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [om-inputs.core :as c]))


(defn validate
  "Generic sequence of validation
  Suitable for creation a partial application of the rules and post function."
  [validation-fn rules post m]
  (->> m
      (validation-fn rules)
      post))


(defn verily
  "Inversion of parameters for the verily validation function for partial application of the rules."
  [validators m]
  (v/validate m validators))



(def data {:person/email "toto"})
(def sch {:person/email s/Str})
(s/check sch {} )

(def validators [[:email :person/email :bad-email]])


(def sch-check (partial validate s/check sch identity))





(def verily-check (partial validate verily nil c/transform-errors ))


(sch-check {})

(verily-check data)

(let [m data]
 (when-let [errs (or (sch-check m) (verily-check m))]
  (prn "Errors" errs)))
