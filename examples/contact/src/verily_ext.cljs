(ns om-inputs.verily-ext
  (:require
    [jkkramer.verily :as v :refer [make-validator validation->fn]]
    ;[clj-vat.algo :refer [check-ident]]
    ))


#_(defn vat
  [keys & [msg]]
  (make-validator
    keys #(not (check-ident %))
    (or msg "must be a valid VAT")))


#_(defmethod validation->fn :vat
  [vspec]
  (apply vat (rest vspec)))



