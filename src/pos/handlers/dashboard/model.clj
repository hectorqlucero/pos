(ns pos.handlers.dashboard.model
  (:require
   [pos.models.crud :refer [Query]]))

(defn- gt
  [table]
  (->> (Query (str "select count(*) as count from " table))
       first
       :count))

(defn get-stats
  []
  {:total-ventas (gt "ventas")
   :total-productos (gt "productos")
   :total-provedores (gt "provedores")
   :total-users (gt "users")
   :total-inventario (gt "inventario")})

(comment
  (get-stats))
