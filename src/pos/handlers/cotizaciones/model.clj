(ns pos.handlers.cotizaciones.model
  (:require [pos.models.crud :refer [db Query Insert Delete Update]]))

(defn get-all
  "Fetch all cotizaciones ordered by date descending."
  []
  (Query db ["SELECT c.*, u.username as usuario_nombre
              FROM cotizaciones c
              LEFT JOIN users u ON u.id = c.usuario_id
              ORDER BY c.fecha DESC"]))

(defn get-by-id
  "Fetch a single cotizacion by ID."
  [id]
  (first (Query db ["SELECT c.*, u.username as usuario_nombre
                    FROM cotizaciones c
                    LEFT JOIN users u ON u.id = c.usuario_id
                    WHERE c.id = ?" id])))

(defn get-detail-by-cotizacion-id
  "Fetch all detail items for a cotizacion."
  [cotizacion-id]
  (Query db ["SELECT * FROM cotizaciones_detalle WHERE cotizacion_id = ?" cotizacion-id]))

(defn- normalize-insert-result [raw-result]
  (cond
    (map? raw-result) raw-result
    (sequential? raw-result) (first raw-result)
    (number? raw-result) {:id raw-result}
    :else raw-result))

(defn create!
  "Create a new cotizacion with details."
  [cotizacion data-items]
  (let [raw-result (Insert :cotizaciones cotizacion)
        result     (normalize-insert-result raw-result)
        id         (or (:generated_key result)
                       ((keyword "last_insert_rowid()") result)
                       (:id result))]
    (doseq [item data-items]
      (Insert :cotizaciones_detalle
              {:cotizacion_id   id
               :producto_id     (:producto_id item)
               :nombre          (:nombre item)
               :cantidad        (:cantidad item)
               :precio_unitario (:precio item)
               :subtotal        (* (:cantidad item) (:precio item))
               :tipo            (or (:tipo item) "producto")}))
    id))

(defn update!
  "Update cotizacion and replace all detail items."
  [id cotizacion data-items]
  (Update :cotizaciones cotizacion ["id = ?" id])
  (Delete :cotizaciones_detalle ["cotizacion_id = ?" id])
  (doseq [item data-items]
    (Insert :cotizaciones_detalle
            {:cotizacion_id   id
             :producto_id     (:producto_id item)
             :nombre          (:nombre item)
             :cantidad        (:cantidad item)
             :precio_unitario (:precio item)
             :subtotal        (* (:cantidad item) (:precio item))
             :tipo            (or (:tipo item) "producto")}))
  id)

(defn delete!
  "Delete a cotizacion and its details (cascade)."
  [id]
  (Delete :cotizaciones_detalle ["cotizacion_id = ?" id])
  (Delete :cotizaciones ["id = ?" id]))

(defn update-estado!
  "Update just the estado of a cotizacion."
  [id estado]
  (Update :cotizaciones {:estado estado} ["id = ?" id]))

(defn find-linked-venta-id
  "Find the venta id linked to this cotizacion, by cotizacion.venta_id or ventas.cotizacion_id."
  [cotizacion-id]
  (or (:venta_id (get-by-id cotizacion-id))
      (:id (first (Query db ["SELECT id FROM ventas WHERE cotizacion_id = ? AND estado != 'cancelada' ORDER BY fecha DESC LIMIT 1" cotizacion-id])))))

(defn search-productos
  "Search products by name, category or id."
  [term]
  (let [like-term (str "%" term "%")]
    (Query db [(str "SELECT p.id, p.nombre, p.precio, p.categoria, p.imagen,"
                    " COALESCE(i.cantidad, 0) as stock"
                    " FROM productos p"
                    " LEFT JOIN inventario i ON i.producto_id = p.id"
                    " WHERE p.nombre LIKE ? OR p.categoria LIKE ? OR CAST(p.id AS TEXT) LIKE ?"
                    " ORDER BY p.nombre")
               like-term like-term like-term])))

(defn get-producto-by-id
  "Fetch a single product by ID."
  [id]
  (first (Query db [(str "SELECT p.id, p.nombre, p.precio, p.categoria, p.imagen,"
                         " COALESCE(i.cantidad, 0) as stock"
                         " FROM productos p"
                         " LEFT JOIN inventario i ON i.producto_id = p.id"
                         " WHERE p.id = ?")
                    id])))

(defn get-servicios
  "Fetch all products with categoria='servicio'."
  []
  (Query db [(str "SELECT p.id, p.nombre, p.precio, p.categoria"
                  " FROM productos p"
                  " WHERE p.categoria = 'servicio'"
                  " ORDER BY p.nombre")]))

(defn find-cliente-by-phone
  "Fetch a cliente by phone if it exists."
  [telefono]
  (when (seq telefono)
    (first (Query db ["SELECT id, nombre, telefono FROM clientes WHERE telefono = ? LIMIT 1" telefono]))))

(defn find-cliente-by-name
  "Fetch a cliente by exact name if it exists."
  [nombre]
  (when (seq nombre)
    (first (Query db ["SELECT id, nombre, telefono FROM clientes WHERE nombre = ? LIMIT 1" nombre]))))

(defn create-cliente!
  "Insert a new cliente record and return the new id."
  [cliente]
  (let [raw-result (Insert :clientes cliente)
        result     (normalize-insert-result raw-result)]
    (or (:generated_key result)
        ((keyword "last_insert_rowid()") result)
        (:id result))))

(defn set-venta-id!
  "Link a cotizacion to a processed venta id."
  [cotizacion-id venta-id]
  (Update :cotizaciones {:venta_id venta-id} ["id = ?" cotizacion-id]))

(defn search-clientes
  "Search clientes by nombre or telefono."
  [term]
  (let [like-term (str "%" term "%")]
    (Query db ["SELECT id, nombre, telefono
               FROM clientes
               WHERE nombre LIKE ? OR telefono LIKE ?
               ORDER BY nombre
               LIMIT 20"
               like-term like-term])))