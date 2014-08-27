(ns om-inputs.i18n
  "Handle all the aspectd related to the i18n of the components."
  (:require-macros [schema.macros :as s])
  (:require  [schema.core :as s]
             [clojure.string :as str]
             [om.core :as om :include-macros true]
             [om-inputs.utils :refer [full-name]]))

;_________________________________________________
;                                                 |
;          i18n Schemas                           |
;_________________________________________________|


(def sch-i18n-field-labels {(s/optional-key :label) s/Str
                            (s/optional-key :desc) s/Str
                            (s/optional-key :ph) s/Str})

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


(defn browser-lang
  "Try to determine the language of the browser"
  []
  (when-let [b-lang (or (.-userLanguage js/navigator)
                        (.-browserLanguage js/navigator)
                        (.-language js/navigator))]
    (.substr b-lang 0 2)))


(def I18NSchema {s/Str
                 (merge sch-i18n-errors
                        {s/Keyword
                         (merge
                          {s/Keyword sch-i18n-field}
                          {(s/optional-key :title) s/Str
                           (s/optional-key :action) sch-i18n-field-labels}) })})



(defn i18n-comp-lang
  "If i18n is provided then determine the language in this order from
  the state
  the browser
  the first language available"
  [sch comp-name lang full-i18n]
  (let [langs (keys full-i18n)
        language (or (some #{lang} langs)
                 (some #{(browser-lang)} langs)
                 (first langs))]
    (when (not= lang language) (prn (str "Warning - Check your i18n language configuration; you set : " lang " but found no labels. Switching to : " language)))
    (s/validate (build-i18n-schema sch) (get-in full-i18n [language comp-name]))))



(def i18n-comp-lang-memo
  "Optimisation and as a side effect the warning is printed only once"
  (memoize i18n-comp-lang))

(defn comp-i18n
  "Get the specific i18n labels for the component and the language"
  [owner comp-name sch]
  (let [full-i18n (om/get-shared owner :i18n)
        lang (om/get-state owner :lang)]
    (when full-i18n
      (s/validate I18NSchema full-i18n)
      (i18n-comp-lang-memo sch comp-name lang full-i18n))))



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
  [i18n
   k]
  (get-in i18n [k :data]))

(defn enum-label [data code]
  (get-in data [code :label] (if (keyword? code) (full-name code) code)))

(s/defn error
  [full-i18n :- I18NSchema
   k :- s/Keyword]
  (get-in full-i18n [:errors k]))

(defn ph
  [i18n k]
  (get-in i18n [k :ph]))
