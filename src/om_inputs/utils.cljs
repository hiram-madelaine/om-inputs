(ns om-inputs.utils
  (:require [clojure.string :as str]))


;_________________________________________________
;                                                 |
;          Clojure Utils                          |
;_________________________________________________|


(defn full-name
  "Get the namespace of a keyword"
  [k]
  (if (namespace k)
   (str/join "/" ((juxt namespace name) k))
   (name k)))
