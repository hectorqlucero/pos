(ns inv.hooks.productos
  (:require [inv.models.util :refer [image-link]]))

(defn after-load
  [rows params]
  (println "[INFO] Loaded" (count rows) "contactos record(s)")
  (map #(assoc % :imagen (image-link (:imagen %))) rows))

(defn before-save
  [params]
  (if-let [imagen-file (:imagen params)]
    (if (and (map? imagen-file) (:tempfile imagen-file))
      (-> params
          (assoc :file imagen-file)
          (dissoc :imagen))
      params)
    params))
