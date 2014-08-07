(ns om-inputs.schemas
  (:require [schema.core :as s]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]))


;_________________________________________________
;                                                 |
;          Schemas                                |
;_________________________________________________|

(def sch-field
  "A field representation"
  (s/named s/Keyword "field"))


(def sch-field-state
  "Meta data about a single field"
  {:value                  s/Any
   :required               s/Bool
   :type                   s/Any
   (s/optional-key :valid) s/Bool
   (s/optional-key :error) [s/Keyword]})

(def sch-business-state
  "Local business state's data structure "
  {sch-field sch-field-state})


(def sch-inputs
  "path to the business state in the component local state."
  {:inputs sch-business-state})

(def sch-chan {:chan ManyToManyChannel})

(def sch-state (merge sch-inputs sch-chan {s/Any s/Any}))


(def SchOptions
  {(s/optional-key :order) [sch-field]
   (s/optional-key :init) {sch-field s/Any}
   (s/optional-key :validations) s/Any
   s/Keyword {:type s/Str}})
