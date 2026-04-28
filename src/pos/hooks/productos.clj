(ns pos.hooks.productos
  (:require [pos.models.util :refer [image-link]]))

(defn after-load
  "Convert stored image filename to an HTML img tag for display."
  [rows params]
  (map #(assoc % :imagen (image-link (:imagen %))) rows))

(defn before-save
  "Move the uploaded image file and store only the filename."
  [params]
  (if-let [imagen-file (:imagen params)]
    (if (and (map? imagen-file) (:tempfile imagen-file))
      (-> params
          (assoc :file imagen-file)
          (dissoc :imagen))
      params)
    params))