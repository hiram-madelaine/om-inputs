(ns om-inputs.schema-utils
  (:require  [schema.core :as s :include-macros true]
             [schema.coerce :as coerce]))



; The description of a component is a Map schema
; The keys are of type : Keyword, schema.core.Optional
; Nevertheless we need to get the value of a key regardless if it as Optional
;

(s/defn norm-sch :- {s/Keyword s/Any}
  "Transform a schema with Optional Keys with only Keywords"
  [sch]
 (reduce (fn [acc [k v]]
           (let [k (if (keyword? k) k (:k k))]
             (assoc acc k v)))
        {} sch))

(defn get-sch [sch k]
  (get sch k (k (norm-sch sch))))



(defn sch-type [t]
  "This function tries to determine the leaf Schema.
   s/Str, s/Int are inclosed in Predicate
   s/Inst is represented "
  (condp = (type t)
    schema.core.Predicate (:p? t)
    schema.core.Maybe (sch-type (:schema t))
    schema.core.NamedSchema (sch-type (:schema t))
    schema.core.EnumSchema "enum"
    schema.core.EqSchema (type (:v t))
    js/Function t
    "other"))

