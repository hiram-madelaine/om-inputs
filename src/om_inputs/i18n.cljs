(ns om-inputs.i18n
  "Handle all the aspectd related to the i18n of the components."
  (:require  [schema.core :as s]
             [clojure.string :as str]
             [om.core :as om :include-macros true]))



;_________________________________________________
;                                                 |
;          i18n Schemas                           |
;_________________________________________________|




(def sch-i18n-field-labels {(s/optional-key :label) s/Str
                            (s/optional-key :desc) s/Str})

(def sch-i18n-enum-labels {(s/optional-key :data) {s/Any sch-i18n-field-labels}})

(def sch-i18n-field (merge  sch-i18n-enum-labels sch-i18n-field-labels))


(comment there is something really strange concerning
  the definition of Var that contains schema and that the compiler sees as undeclared )

#_(def sch-i18n-comp {(s/optional-key :title) s/Str
                    (s/optional-key :action) sch-i18n-field-labels})

(def sch-i18n-errors {(s/optional-key :errors) {s/Keyword s/Str}})


(defn build-i18n-schema
  "Build a specific i18n Schema with all possible keys."
  [sch]
  (reduce
   (fn [acc [k v]]  (assoc acc (s/optional-key (get k :k k)) sch-i18n-field))
   {(s/optional-key :title) s/Str
    (s/optional-key :action) sch-i18n-field-labels} sch))


(defn comp-i18n
  "Get the specific i18n labels for the component and the language"
  [owner comp-name sch]
  (let [full-i18n (om/get-shared owner :i18n)
        lang (om/get-state owner :lang)]
    (s/validate (build-i18n-schema sch) (get-in full-i18n [lang comp-name]))))



;_________________________________________________
;                                                 |
;          i18n Labels Utils                      |
;_________________________________________________|

(defn label
  [i18n k]
  (get-in i18n [k :label] (str/capitalize (name k))))

(defn desc
  [i18n k]
  (get-in i18n [k :desc]))

(defn desc?
  [i18n k]
  (not (nil? (desc i18n k))))
(defn data
  [i18n k]
  (get-in i18n [k data]))



