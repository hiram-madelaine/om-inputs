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
   (s/optional-key :error) [s/Keyword]
   (s/optional-key :disabled) s/Bool})

(def sch-business-state
  "Local business state's data structure "
  {sch-field sch-field-state})


(def sch-inputs
  "path to the business state in the component local state."
  {:inputs sch-business-state})

(def sch-chan {:chan ManyToManyChannel})

(def ActionStateSchema
  {:action-state {(s/optional-key :disabled) s/Bool}})


(def sch-state (merge sch-inputs sch-chan ActionStateSchema {s/Any s/Any}))


(def SchOptions
  {(s/optional-key :IWillReceiveProps) s/Any
   (s/optional-key :order) [sch-field]
   (s/optional-key :init) {sch-field s/Any}
   (s/optional-key :validations) s/Any
   (s/optional-key :action) {(s/optional-key :one-shot) s/Bool
                             (s/optional-key :no-reset) s/Bool}
   s/Keyword {(s/optional-key :type) s/Str
              (s/optional-key :labeled) s/Bool
              (s/optional-key :attrs) s/Any
              (s/optional-key :className) s/Str}})
