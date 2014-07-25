(ns om-inputs.date-utils
  (:import [goog.i18n DateTimeFormat DateTimeParse]
           [goog.ui InputDatePicker]))


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
  [date-format date]
  (.format (DateTimeFormat. (or (format-map date-format) date-format))
    (js/Date. date)))


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

