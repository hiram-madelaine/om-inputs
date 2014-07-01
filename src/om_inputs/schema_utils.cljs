(ns om-inputs.schema-utils
  (:require-macros [schema.macros :as s])
  (:require  [schema.core :as s]
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
