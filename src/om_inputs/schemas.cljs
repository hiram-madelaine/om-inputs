(ns om-inputs.schemas
  (:require [schema.core :as s]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]))


;_________________________________________________
;                                                 |
;          Schemas                                |
;_________________________________________________|

(def sch-i18n {:i18n {:inputs s/Any}})


(def sch-field-state {:value s/Any
                      :required s/Bool
                      :type s/Any
                      (s/optional-key :valid) s/Bool
                      (s/optional-key :error) [s/Keyword]})


(def sch-business-state
  "Local business state's data structure "
  {s/Keyword sch-field-state})


(def sch-inputs {:inputs sch-business-state})

(def sch-chan {:chan ManyToManyChannel})

(def sch-state (merge sch-inputs sch-chan {s/Any s/Any}))


(def SchOptions
  {(s/optional-key :order) [s/Keyword]
   (s/optional-key :init) {s/Keyword s/Any}
   (s/optional-key :validations) s/Any
   s/Keyword {:type s/Str}})
