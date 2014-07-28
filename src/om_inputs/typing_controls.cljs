(ns om-inputs.typing-controls
  (:require [clojure.string :as str]
            [schema.core :as s]
            [om-inputs.schema-utils :refer [sch-type]]))


;_________________________________________________
;                                                 |
;       Typing controls related Utils             |
;_________________________________________________|

 (defn only-integer
   "Only authorize integer or empty string.
    n is the new value
    o is the old value
    When the new value is valid returns it, else returns the previous one."
  [n o]
  (if (str/blank? n)
   ""
   (let [r (js/parseInt n)]
                (if (js/isNaN r)
                  o
                  r))))

 (defn only-number
   "Validate a that an input of type s/Num contains a value that can be converted in a numeric.
    If not the previous value is used.
    An empty string is left as is.
    n is the new value
    o is the old value"
  [n o]
  (if (str/blank? n)
    ""
    (if (js/isNaN n)
      o
      n)))



(def typing-control-fns
  {integer? only-integer
  ; js/Date parse-date ;Let schema coercion deal with data coercion
   js/Number only-number})



(defn build-typing-control
  "Build the coercion map field->coercion-fn from all entries of the Schema"
  [sch]
  (reduce (fn[acc [k v]]
            (if-let [cfn (get typing-control-fns (sch-type v))]
                          (assoc acc (get k :k k) cfn)
                           acc)) {} sch))

