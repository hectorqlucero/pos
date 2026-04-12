(ns inv.handlers.pos.controller
  (:require
   [inv.handlers.pos.model :as model]
   [inv.handlers.pos.view :as view]
   [inv.layout :refer [application]]
   [inv.i18n.core :as i18n]
   [inv.models.util :refer [get-session-id]]
   [clojure.data.json :as json]))

(defn pos
  "Main POS page"
  [request]
  (let [title (i18n/tr request :pos/title)
        ok (get-session-id request)
        productos (model/get-productos)
        content (view/pos-view request productos)]
    (application request title ok nil content)))

(defn api-search
  "API: search products"
  [request]
  (let [term (get-in request [:params :q] "")
        results (if (empty? term)
                  (model/get-productos)
                  (model/search-productos term))]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str {:ok true :data results})}))

(defn api-register-sale
  "API: register a sale with items"
  [request]
  (try
    (let [body (json/read-str (slurp (:body request)) :key-fn keyword)
          items (:items body)
          pago (or (:pago body) 0)
          user-id (get-in request [:session :user_id])]
      (if (empty? items)
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:ok false :error "No hay productos en la venta"})}
        (let [total (reduce + 0 (map #(* (:cantidad %) (:precio %)) items))
              cambio (- (double pago) (double total))
              venta-result (model/insert-venta
                            {:total total
                             :pago pago
                             :cambio (max cambio 0)
                             :usuario_id user-id})
              venta-id (or (:generated_key venta-result)
                           (:id venta-result)
                           (:GENERATED_KEY venta-result))]
          (doseq [item items]
            (model/insert-venta-detalle
             {:venta_id venta-id
              :producto_id (:producto_id item)
              :cantidad (:cantidad item)
              :precio_unitario (:precio item)
              :subtotal (* (:cantidad item) (:precio item))})
            (model/insert-movimiento
             {:producto_id (:producto_id item)
              :tipo_movimiento "venta"
              :cantidad (:cantidad item)}))
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (json/write-str {:ok true
                                  :venta_id venta-id
                                  :total total
                                  :cambio (max cambio 0)})})))
    (catch Exception e
      (println "[ERROR] POS register-sale failed:" (.getMessage e))
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:ok false :error (.getMessage e)})})))
