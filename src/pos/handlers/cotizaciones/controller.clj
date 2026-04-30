(ns pos.handlers.cotizaciones.controller
  (:require [pos.handlers.cotizaciones.model :as model]
            [pos.handlers.cotizaciones.view :as view]
            [pos.handlers.pos.model :as pos-model]
            [pos.layout :refer [application]]
            [pos.i18n.core :as i18n]
            [pos.models.util :refer [get-session-id]]
            [clojure.data.json :as json]))

(defn index
  "List all cotizaciones."
  [request]
  (let [title    (i18n/tr request :cotizaciones/title)
        ok       (get-session-id request)
        list     (model/get-all)
        content  (view/index-view request list)]
    (application request title ok nil content)))

(defn nuevo
  "Create new cotizacion form."
  [request]
  (let [title    (i18n/tr request :cotizaciones/new-title)
        ok       (get-session-id request)
        servicios (model/get-servicios)
        content  (view/edit-view request nil servicios nil)]
    (application request title ok nil content)))

(defn editar
  "Edit existing cotizacion form."
  [request]
  (let [id (get-in request [:params :id])
        cotizacion (model/get-by-id id)
        detalles (model/get-detail-by-cotizacion-id id)
        servicios (model/get-servicios)
        title    (i18n/tr request :cotizaciones/edit-title)
        ok       (get-session-id request)
        content  (view/edit-view request cotizacion servicios detalles)]
    (application request title ok nil content)))

(defn- parse-number [x]
  (cond
    (number? x) x
    (string? x) (try (Double/parseDouble x) (catch Exception _ 0))
    :else 0))

(defn- normalize-item [item]
  {:producto_id (:producto_id item)
   :nombre      (:nombre item)
   :cantidad    (parse-number (:cantidad item))
   :precio      (parse-number (:precio item))
   :tipo        (:tipo item)})

(defn- normalize-items [items]
  (mapv normalize-item (remove nil? items)))

(defn- ensure-cliente!
  "Ensure a cliente record exists for the quote and return its id."
  [cliente-id nombre telefono]
  (or cliente-id
      (:id (model/find-cliente-by-phone telefono))
      (:id (model/find-cliente-by-name nombre))
      (when (and (seq nombre) (seq telefono))
        (model/create-cliente! {:nombre nombre :telefono telefono}))))

(defn guardar
  "Save (create or update) a cotizacion."
  [request]
  (try
    (let [body       (json/read-str (slurp (:body request)) :key-fn keyword)
          id         (:id body)
          cliente-id (:cliente_id body)
          nombre     (:cliente_nombre body)
          telefono   (:cliente_telefono body)
          notas      (:notas body)
          estado     (:estado body)
          items      (normalize-items (or (:items body) []))
          user-id    (get-in request [:session :user_id])
          _          (ensure-cliente! cliente-id nombre telefono)
          total      (reduce + 0 (map #(* (:cantidad %) (:precio %)) items))
          cotizacion {:cliente_nombre nombre
                      :cliente_telefono telefono
                      :notas           notas
                      :estado          (or estado "borrador")
                      :total           total
                      :usuario_id      user-id}]
      (if id
        (let [existing (model/get-by-id id)
              existing-state (:estado existing)]
          (if (#{"aceptada" "cancelada"} existing-state)
            {:status 400
             :headers {"Content-Type" "application/json"}
             :body    (json/write-str {:ok false :error "No se puede modificar una cotización procesada o cancelada."})}
            (do
              (model/update! id cotizacion items)
              {:status 200
               :headers {"Content-Type" "application/json"}
               :body    (json/write-str {:ok true :id id})})))
        (let [new-id (model/create! cotizacion items)]
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body    (json/write-str {:ok true :id new-id})})))
    (catch Exception e
      (println "[ERROR] cotizaciones guardar failed:" (.getMessage e))
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body    (json/write-str {:ok false :error (.getMessage e)})})))

(defn eliminar
  "Delete a cotizacion."
  [request]
  (try
    (let [id (get-in request [:params :id])]
      (model/delete! id)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body    (json/write-str {:ok true})})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body    (json/write-str {:ok false :error (.getMessage e)})})))

(defn cambiar-estado
  "Change cotizacion estado."
  [request]
  (try
    (let [body    (json/read-str (slurp (:body request)) :key-fn keyword)
          id      (:id body)
          estado  (:estado body)]
      (model/update-estado! id estado)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body    (json/write-str {:ok true})})
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body    (json/write-str {:ok false :error (.getMessage e)})})))

(defn reembolsar
  "Refund a processed cotizacion and cancel the linked venta if present."
  [request]
  (try
    (let [body       (json/read-str (slurp (:body request)) :key-fn keyword)
          cot-id     (:id body)
          cotizacion (model/get-by-id cot-id)
          venta-id   (or (:venta_id cotizacion)
                         (model/find-linked-venta-id cot-id))]
      (when venta-id
        (try
          (pos-model/refund-sale-tx! venta-id)
          (catch Exception e
            (when-not (= "Venta ya cancelada" (.getMessage e))
              (throw e)))))
      (model/update-estado! cot-id "cancelada")
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body    (json/write-str {:ok true})})
    (catch Exception e
      (println "[ERROR] cotizaciones reembolsar failed:" (.getMessage e))
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body    (json/write-str {:ok false :error (.getMessage e)})})))

(defn api-search-productos
  "JSON API: search products for adding to cotizacion."
  [request]
  (let [term (get-in request [:params :q] "")]
    (if (empty? term)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body    (json/write-str {:ok true :data []})}
      (let [results (model/search-productos term)]
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body    (json/write-str {:ok true :data results})}))))

(defn api-search-clientes
  "JSON API: search clientes by name or phone."
  [request]
  (let [term (get-in request [:params :q] "")]
    (if (empty? term)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body    (json/write-str {:ok true :data []})}
      (let [results (model/search-clientes term)]
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body    (json/write-str {:ok true :data results})}))))

(defn ver
  "View a cotizacion (for printing/PDF)."
  [request]
  (let [id          (get-in request [:params :id])
        cotizacion  (model/get-by-id id)
        detalles    (model/get-detail-by-cotizacion-id id)
        ok          (get-session-id request)
        content     (view/print-view request cotizacion detalles)]
    (application request (str "Cotización #" id) ok nil content)))