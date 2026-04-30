(ns pos.handlers.pos.model
  (:require [clojure.java.jdbc :as jdbc]
            [pos.hooks.movimientos :as mov-hooks]
            [pos.models.crud :refer [db Query Insert Update]]))

(defn get-productos
  "Fetch all products with their current inventory stock level."
  []
  (Query db [(str "SELECT p.id, p.nombre, p.precio, p.categoria, p.imagen,"
                  " COALESCE(i.cantidad, 0) as stock"
                  " FROM productos p"
                  " LEFT JOIN inventario i ON i.producto_id = p.id"
                  " ORDER BY p.nombre")]))

(defn search-productos
  "Search products by name or category."
  [term]
  (let [like-term (str "%" term "%")]
    (Query db [(str "SELECT p.id, p.nombre, p.precio, p.categoria, p.imagen,"
                    " COALESCE(i.cantidad, 0) as stock"
                    " FROM productos p"
                    " LEFT JOIN inventario i ON i.producto_id = p.id"
                    " WHERE p.nombre LIKE ? OR p.categoria LIKE ?"
                    " ORDER BY p.nombre")
               like-term like-term])))

(defn get-producto-by-id
  "Fetch a single product by ID (for barcode scanning)."
  [id]
  (first (Query db [(str "SELECT p.id, p.nombre, p.precio, p.categoria, p.imagen,"
                         " COALESCE(i.cantidad, 0) as stock"
                         " FROM productos p"
                         " LEFT JOIN inventario i ON i.producto_id = p.id"
                         " WHERE p.id = ?")
                    id])))

(defn get-productos-by-ids
  "Fetch multiple products by their IDs (for label printing)."
  [ids]
  (if (empty? ids)
    []
    (let [placeholders (apply str (interpose "," (repeat (count ids) "?")))
          sql (str "SELECT p.id, p.nombre, p.precio, p.categoria, p.imagen,"
                   " COALESCE(i.cantidad, 0) as stock"
                   " FROM productos p"
                   " LEFT JOIN inventario i ON i.producto_id = p.id"
                   " WHERE p.id IN (" placeholders ")"
                   " ORDER BY p.nombre")
          str-ids (mapv str ids)]
      (Query db (vec (cons sql str-ids))))))

(defn register-sale-tx!
  "Registers a complete sale inside a single database transaction.
   Inserts the sale header, one detail row per item, and one movimiento
   per item (which triggers the inventory hook) - except for servicio items.
   Returns the new venta id."
  [venta-header items]
  (jdbc/with-db-transaction [tx db]
    (let [venta-result (first (Insert tx :ventas venta-header))
          venta-id     (or (:generated_key venta-result)
                           ((keyword "last_insert_rowid()") venta-result)
                           (:last_insert_rowid venta-result)
                           (:id venta-result)
                           (first (vals venta-result)))]
      (when (nil? venta-id)
        (throw (ex-info "Could not determine venta id after insert" {:result venta-result})))
      (doseq [item items]
        (let [producto-id (:producto_id item)
              nombre      (:nombre item)
              categoria   (:categoria item)]
          (Insert tx :ventas_detalle
                  {:venta_id        venta-id
                   :producto_id     producto-id
                   :nombre          nombre
                   :categoria       categoria
                   :cantidad        (:cantidad item)
                   :precio_unitario (:precio item)
                   :subtotal        (* (:cantidad item) (:precio item))})
          (when (and producto-id
                     (not= categoria "servicio")
                     (not= categoria "misc"))
            (let [mov {:producto_id     producto-id
                       :tipo_movimiento "venta"
                       :cantidad        (:cantidad item)}
                  result (Insert tx :movimientos mov)]
              (mov-hooks/after-save tx mov result)))))
      (when-let [cotizacion-id (:cotizacion_id venta-header)]
        (when (first (Query tx ["SELECT * FROM cotizaciones WHERE id = ?" cotizacion-id]))
          (Update tx :cotizaciones
                  {:estado "aceptada"
                   :venta_id venta-id}
                  ["id = ?" cotizacion-id])))
      venta-id)))

(defn refund-sale-tx!
  "Refunds a completed sale and restores inventory for stocked items."
  [venta-id]
  (jdbc/with-db-transaction [tx db]
    (let [sale (first (Query tx ["SELECT * FROM ventas WHERE id = ?" venta-id]))]
      (when-not sale
        (throw (ex-info "Venta not found" {:venta-id venta-id})))
      (if (= (:estado sale) "cancelada")
        venta-id
        (let [details (Query tx ["SELECT * FROM ventas_detalle WHERE venta_id = ?" venta-id])]
          (doseq [item details]
            (let [producto-id (:producto_id item)
                  raw-categoria (:categoria item)
                  categoria    (or raw-categoria
                                   (when producto-id
                                     (:categoria (first (Query tx ["SELECT categoria FROM productos WHERE id = ?" producto-id]))))
                                   "")
                  cantidad    (:cantidad item)]
              (when (and producto-id
                         (not= categoria "servicio")
                         (not= categoria "misc"))
                (let [mov {:producto_id     producto-id
                           :tipo_movimiento "compra"
                           :cantidad        cantidad}
                      result (Insert tx :movimientos mov)]
                  (mov-hooks/after-save tx mov result)))))
          (Update tx :ventas {:estado "cancelada"} ["id = ?" venta-id])
          venta-id)))))
