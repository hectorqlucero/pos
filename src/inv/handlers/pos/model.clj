(ns inv.handlers.pos.model
  (:require
   [inv.hooks.movimientos :as mov-hooks]
   [inv.models.crud :refer [db Query Insert]]))

(defn get-productos
  "Obtiene todos los productos con sus cantidades de inventario"
  []
  (Query db [(str "SELECT p.id, p.nombre, p.precio, p.categoria, p.imagen,"
                  " COALESCE(i.cantidad, 0) as stock"
                  " FROM productos p"
                  " LEFT JOIN inventario i ON i.producto_id = p.id"
                  " ORDER BY p.nombre")]))

(defn search-productos
  "Busca productos por nombre o categoría"
  [term]
  (let [like-term (str "%" term "%")]
    (Query db [(str "SELECT p.id, p.nombre, p.precio, p.categoria, p.imagen,"
                    " COALESCE(i.cantidad, 0) as stock"
                    " FROM productos p"
                    " LEFT JOIN inventario i ON i.producto_id = p.id"
                    " WHERE p.nombre LIKE ? OR p.categoria LIKE ?"
                    " ORDER BY p.nombre")
               like-term like-term])))

(defn insert-venta
  "Inserta un registro de encabezado de venta"
  [data]
  (first (Insert db :ventas data)))

(defn insert-venta-detalle
  "Inserta una línea de detalle de venta"
  [data]
  (Insert db :ventas_detalle data))

(defn insert-movimiento
  "Inserta un registro de movimiento y actualiza inventario via hook after-save"
  [data]
  (let [result (Insert db :movimientos data)]
    (mov-hooks/after-save data result)
    result))
