(ns pos.hooks.movimientos
  (:require [pos.models.crud :refer [Query Update Insert]]))

;; This atom temporarily stores movement data before deletion
;; so that after-delete can reverse the inventory adjustment.
;; An atom is a thread-safe mutable container in Clojure.
(def ^:private pending-delete-data (atom {}))

(defn after-save
  "After a movement is saved, update the inventory for the product.
   - compra (purchase) increases stock
   - venta  (sale)     decreases stock
   If no inventory record exists for the product, create one.
   Optionally accepts a db connection/transaction as the first argument
   so inventory updates can share an open transaction."
  ([data result] (after-save nil data result))
  ([conn data _result]
   (try
     (let [producto-id     (:producto_id data)
           tipo-movimiento (:tipo_movimiento data)
           cantidad        (if (string? (:cantidad data))
                             (parse-long (:cantidad data))
                             (:cantidad data))
           ;; Purchases add to stock, sales subtract
           adjustment      (if (= tipo-movimiento "compra")
                             cantidad
                             (- cantidad))
           today           (java.sql.Date/valueOf (java.time.LocalDate/now))]
       (when producto-id
         (if conn
           ;; --- inside an explicit transaction ---
           (let [existing-inv (first (Query conn ["SELECT * FROM inventario WHERE producto_id = ?" producto-id]))]
             (if existing-inv
               (Update conn :inventario
                       {:cantidad             (+ (:cantidad existing-inv) adjustment)
                        :ultima_actualizacion today}
                       ["id = ?" (:id existing-inv)])
               (Insert conn :inventario
                       {:producto_id          producto-id
                        :cantidad             adjustment
                        :ultima_actualizacion today})))
           ;; --- no explicit connection: use global db ---
           (let [existing-inv (first (Query ["SELECT * FROM inventario WHERE producto_id = ?" producto-id]))]
             (if existing-inv
               (Update :inventario
                       {:cantidad             (+ (:cantidad existing-inv) adjustment)
                        :ultima_actualizacion today}
                       ["id = ?" (:id existing-inv)])
               (Insert :inventario
                       {:producto_id          producto-id
                        :cantidad             adjustment
                        :ultima_actualizacion today}))))))
     (catch Exception e
       (println "[ERROR] after-save failed:" (.getMessage e))))
   {:success true}))

(defn before-delete
  "Save the movement record before it is deleted so after-delete can reverse it."
  [{:keys [id]}]
  (try
    (when id
      (let [record (first (Query ["SELECT * FROM movimientos WHERE id = ?" id]))]
        (when record
          ;; Store the record in the atom, keyed by the ID as a string
          (swap! pending-delete-data assoc (str id) record))))
    (catch Exception e
      (println "[ERROR] before-delete failed:" (.getMessage e))))
  {:success true})

(defn after-delete
  "After a movement is deleted, reverse the inventory adjustment it made."
  [{:keys [id]} _result]
  (try
    (let [record (get @pending-delete-data (str id))]
      (when record
        ;; Remove the stored record from the atom
        (swap! pending-delete-data dissoc (str id))
        (let [producto-id     (:producto_id record)
              tipo-movimiento (:tipo_movimiento record)
              cantidad        (if (string? (:cantidad record))
                                (parse-long (:cantidad record))
                                (:cantidad record))
              ;; Reverse the original adjustment:
              ;; if it was a compra, subtract; if it was a venta, add back
              adjustment      (if (= tipo-movimiento "compra")
                                (- cantidad)
                                cantidad)]
          (when producto-id
            (let [existing-inv (first (Query ["SELECT * FROM inventario WHERE producto_id = ?" producto-id]))]
              (when existing-inv
                (Update :inventario
                        {:cantidad            (+ (:cantidad existing-inv) adjustment)
                         :ultima_actualizacion (java.sql.Date/valueOf (java.time.LocalDate/now))}
                        ["id = ?" (:id existing-inv)])))))))
    (catch Exception e
      (println "[ERROR] after-delete failed:" (.getMessage e))))
  {:success true})
