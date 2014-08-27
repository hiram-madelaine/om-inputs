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
  {integer? (fnil only-integer "" "")
  ; js/Date parse-date ;Let schema coercion deal with data coercion
   js/Number (fnil only-number "" "")})

(defprotocol ControlTyping
  (control [r n o]))

(extend-protocol ControlTyping
  js/RegExp
  (control [f n o]
           (if (str/blank? n)
             ""
             (if (re-matches f n)
               n
               o))))


(defn get-typing-ctrl-fn
  [s]
  (if-let [cfn (get typing-control-fns (sch-type s))]
    cfn
    (when (= (type s) js/RegExp)
      (fnil (partial control s) "" ""))))


(defn build-typing-control
  "Build the coercion map field->coercion-fn from all entries of the Schema"
  [sch]
  (reduce (fn[acc [k s]]
            (if-let [cfn (get-typing-ctrl-fn s)]
                          (assoc acc (get k :k k) cfn)
                           acc)) {} sch))
