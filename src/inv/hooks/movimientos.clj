(ns inv.hooks.movimientos
  "Business logic hooks for movimientos entity.
   
   SENIOR DEVELOPER: Implement custom business logic here.
   
   See: HOOKS_GUIDE.md for detailed documentation and examples.
   Example: src/inv/hooks/alquileres.clj
   
   Uncomment the hooks you need and implement the logic."
  (:require
   [inv.models.crud :refer [Query Update Insert]]))

;; =============================================================================
;; Validators
;; =============================================================================

;; Example validator function:
;; (defn validate-dates
;;   "Validates that end date is after start date"
;;   [params]
;;   (let [start (:start_date params)
;;         end (:end_date params)]
;;     (when (and start end)
;;       ;; Add your validation logic here
;;       nil)))  ; Return nil if valid, or {:field "error message"}

;; =============================================================================
;; Computed Fields
;; =============================================================================

;; Example computed field:
;; (defn compute-total
;;   "Computes total from quantity and price"
;;   [row]
;;   (* (or (:quantity row) 0)
;;      (or (:price row) 0)))

;; =============================================================================
;; Lifecycle Hooks
;; =============================================================================

(defn before-load
  "Hook executed before loading records.
   
   Use cases:
   - Filter by user permissions
   - Add default filters
   - Log access
   
   Args: [params] - Query parameters
   Returns: Modified params map"
  [params]
  ;; TODO: Add your logic here
  (println "[INFO] Loading movimientos with params:" params)
  params)

(defn after-load
  "Hook executed after loading records.
   
   Use cases:
   - Add computed fields
   - Format data
   - Enrich with lookups
   
   Args: [rows params] - Loaded rows and query params
   Returns: Modified rows vector"
  [rows _params]
  (println "[INFO] Loaded" (count rows) "movimientos record(s)")
  ;; TODO: Add your transformations here, then return the result
  ;; Example: (map #(assoc % :full-name (str (:first-name %) " " (:last-name %))) rows)
  rows)

(defn before-save
  "Hook executed before saving a record.
   
   Use cases:
   - Validate data
   - Set defaults
   - Transform values
   - Check permissions
   
   Args: [params] - Form data to be saved
   Returns: Modified params map OR {:errors {...}} if validation fails"
  [params]
  (println "[INFO] Saving movimientos...")
  ;; TODO: Add validation and transformation logic
  params)

(defn after-save
  [data _result]
  ;; data es el mapa completo de datos guardados, _result es boolean
  (try
    (let [entity-id (let [id (:id data)]
                      (when (and id (not= id ""))
                        id))
          original (when entity-id
                     (first (Query ["SELECT * FROM movimientos WHERE id = ?" entity-id])))
          ;; Para registro nuevo (sin original), siempre actualizar inventario
          new-record? (nil? original)
          ;; Para registro existente, checkar si campos de inventario cambiaron
          inventory-fields [:producto_id :tipo_movimiento :cantidad]
          has-changes? (when original
                         (some (fn [field]
                                 (not= (str (get original field))
                                       (str (get data field))))
                               inventory-fields))
          should-update? (or new-record? has-changes?)]
      (when should-update?
        ;; Actualizar el inventario segun tipo de movimiento ej. compra/venta
        (let [producto-id (:producto_id data)
              tipo-movimiento (:tipo_movimiento data)
              cantidad (if (string? (:cantidad data))
                         (parse-long (:cantidad data))
                         (:cantidad data))
              adjustment (if (= tipo-movimiento "compra")
                           cantidad
                           (- cantidad))]  ; venta baja el inventario
          (when producto-id
            (println "[INFO] Updating inventory for producto ID:" producto-id "adjustment:" adjustment)
            ;; Actualizar o crear registro de inventario
            (let [existing-inv (first (Query ["SELECT * FROM inventario WHERE producto_id = ?" producto-id]))]
              (if existing-inv
                ;; Actualizar inventario existente
                (Update :inventario
                        {:cantidad (-> existing-inv :cantidad (+ adjustment))
                         :ultima_actualizacion (java.sql.Date/valueOf (java.time.LocalDate/now))}
                        ["id = ?" (:id existing-inv)])
                ;; Crear un nuevo registro de inventario
                (Insert :inventario
                        {:producto_id producto-id
                         :cantidad adjustment
                         :ultima_actualizacion (java.sql.Date/valueOf (java.time.LocalDate/now))})))))))
    (catch Exception e
      (println "[ERROR] after-save hook failed:" (.getMessage e))))
  {:success true})

;; Atom para guardar datos del movimiento antes de borrarlo
(def ^:private pending-delete-data (atom {}))

(defn before-delete
  "Guarda los datos del movimiento antes de borrarlo para poder revertir inventario."
  [{:keys [id]}]
  (try
    (when id
      (let [record (first (Query ["SELECT * FROM movimientos WHERE id = ?" id]))]
        (when record
          (swap! pending-delete-data assoc (str id) record)
          (println "[INFO] Captured movimiento data before delete. ID:" id))))
    (catch Exception e
      (println "[ERROR] before-delete hook failed:" (.getMessage e))))
  {:success true})

(defn after-delete
  "Revierte el ajuste de inventario cuando se borra un movimiento."
  [{:keys [id]} _result]
  (try
    (let [record (get @pending-delete-data (str id))]
      (when record
        (swap! pending-delete-data dissoc (str id))
        (let [producto-id (:producto_id record)
              tipo-movimiento (:tipo_movimiento record)
              cantidad (if (string? (:cantidad record))
                         (parse-long (:cantidad record))
                         (:cantidad record))
              ;; Revertir: si fue compra, restar; si fue venta, sumar
              adjustment (if (= tipo-movimiento "compra")
                           (- cantidad)
                           cantidad)]
          (when producto-id
            (println "[INFO] Reversing inventory for producto ID:" producto-id "adjustment:" adjustment)
            (let [existing-inv (first (Query ["SELECT * FROM inventario WHERE producto_id = ?" producto-id]))]
              (when existing-inv
                (Update :inventario
                        {:cantidad (-> existing-inv :cantidad (+ adjustment))
                         :ultima_actualizacion (java.sql.Date/valueOf (java.time.LocalDate/now))}
                        ["id = ?" (:id existing-inv)])))))))
    (catch Exception e
      (println "[ERROR] after-delete hook failed:" (.getMessage e))))
  {:success true})
