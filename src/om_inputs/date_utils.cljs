(ns om-inputs.date-utils
  (:require [clojure.string :as str])
  (:import [goog.i18n DateTimeFormat DateTimeParse]
           [goog.ui InputDatePicker]
           [goog.date Date Interval]))


;_________________________________________________
;                                                 |
;          Handle Date Format Parse               |
;_________________________________________________|

(def default-fmt "dd/MM/yyyy")


(def format-map
  (let [f DateTimeFormat.Format]
    {:FULL_DATE (.-FULL_DATE f)
     :FULL_DATETIME (.-FULL_DATETIME f)
     :FULL_TIME (.-FULL_TIME f)
     :LONG_DATE (.-LONG_DATE f)
     :LONG_DATETIME (.-LONG_DATETIME f)
     :LONG_TIME (.-LONG_TIME f)
     :MEDIUM_DATE (.-MEDIUM_DATE f)
     :MEDIUM_DATETIME (.-MEDIUM_DATETIME f)
     :MEDIUM_TIME (.-MEDIUM_TIME f)
     :SHORT_DATE (.-SHORT_DATE f)
     :SHORT_DATETIME (.-SHORT_DATETIME f)
     :SHORT_TIME (.-SHORT_TIME f)}))



(defn fmt
  "Format a date using either the built-in goog.i18n.DateTimeFormat.Format enum
   or a formatting string like \"dd MMMM yyyy\""
  ([date]
   (fmt default-fmt date))
  ([date-format date]
   (.format (DateTimeFormat. (or (format-map date-format) date-format))
            (js/Date. date))))


(defn parse
  "Parse a Date according to the format specified
   Default format is dd/MM/yyyy"
  ([f s]
   (let [p (DateTimeParse. f)
         d (js/Date.)]
     (.strictParse p s d)
     d))
  ([s]
   (parse default-fmt s)))



(defn display-date
  "Takes care of date rendering in the input."
  ([f v]
   (when-not (str/blank? v) (fmt f v)))
  ([v]
    (display-date default-fmt v)))


(defn goog-date->js-date
  [d]
  (when d
    (parse (fmt default-fmt d))))


(defprotocol SetDate
  (setInputValue [this v]))

#_(extend-type InputDatePicker
  SetDate
  (setInputValue [this v]
                 (let [el (.getElement this)]
                   (js/alert (.-id el)))))


(defn date-picker
  "Build a google.ui.InputDatePicker with a specific format"
  [f]
 (do #_(set! (.-setInputValue (.-prototype InputDatePicker))
           (fn [v]
             (this-as this
                      (let [el (.getElement this)]
                       (set! (.-value el) v)))))
  (InputDatePicker. (DateTimeFormat. f) (DateTimeParse. f) nil nil)))


;_________________________________________________
;                                                 |
;          Point in Time Utils                    |
;_________________________________________________|

(defn at
  "Create a js/Date at plus/minus days"
  [d]
  (goog-date->js-date
   (doto (Date.)
     (.add (Interval. Interval.DAYS d)))))

(defn tomorrow
  []
  (at 1))
