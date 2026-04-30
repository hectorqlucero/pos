(ns pos.handlers.pos.controller
  (:require [pos.config.loader       :as cfg]
            [pos.handlers.pos.model   :as model]
            [pos.handlers.pos.view    :as view]
            [pos.layout               :refer [application]]
            [pos.i18n.core            :as i18n]
            [pos.models.util          :refer [get-session-id]]
            [pos.services.qrcode     :as qrcode]
            [clojure.data.json        :as json]
            [hiccup.core              :as hiccup]))

(defn pos
  "Serve the main POS page."
  [request]
  (let [title    (i18n/tr request :pos/title)
        ok       (get-session-id request)
        productos (model/get-productos)
        content  (view/pos-view request productos)]
    (application request title ok nil content)))

(defn api-search
  "JSON API: search products by name or category.
   If search term is numeric, first tries to find by ID."
  [request]
  (let [term (get-in request [:params :q] "")]
    (if (empty? term)
      {:status  200
       :headers {"Content-Type" "application/json"}
       :body    (json/write-str {:ok true :data (model/get-productos)})}
      (let [numeric-id (try (Long/parseLong term) (catch Exception _ nil))
            results (if numeric-id
                      (let [product (model/get-producto-by-id numeric-id)]
                        (if product
                          [product]
                          (model/search-productos term)))
                      (model/search-productos term))
            scan-result (when (and numeric-id (= (count results) 1))
                          (first results))]
        {:status  200
         :headers {"Content-Type" "application/json"}
         :body    (json/write-str {:ok true :data results :scanned scan-result})}))))

(defn api-register-sale
  "JSON API: receive a cart from the browser and save the complete sale."
  [request]
  (try
    (let [body    (json/read-str (slurp (:body request)) :key-fn keyword)
          items   (:items body)
          pago    (or (:pago body) 0)
          user-id (get-in request [:session :user_id])]
      (if (empty? items)
        {:status  400
         :headers {"Content-Type" "application/json"}
         :body    (json/write-str {:ok false :error "No items in cart"})}
        (let [raw-cotizacion-id (:cotizacion_id body)
              cotizacion-id     (when (and raw-cotizacion-id (not= raw-cotizacion-id ""))
                                  (try (Long/parseLong (str raw-cotizacion-id))
                                       (catch Exception _ nil)))
              total             (reduce + 0 (map #(* (:cantidad %) (:precio %)) items))
              cambio            (- (double pago) (double total))
              site-name         (get-in (cfg/get-all-configs) [:app-config :site-name])
              venta-id          (model/register-sale-tx!
                                 {:total         total
                                  :pago          pago
                                  :cambio        (max cambio 0)
                                  :usuario_id    user-id
                                  :cotizacion_id cotizacion-id}
                                 items)]
          {:status  200
           :headers {"Content-Type" "application/json"}
           :body    (json/write-str {:ok        true
                                     :venta_id  venta-id
                                     :total     total
                                     :cambio    (max cambio 0)
                                     :site_name site-name})})))
    (catch Exception e
      (println "[ERROR] POS register-sale failed:" (.getMessage e))
      {:status  500
       :headers {"Content-Type" "application/json"}
       :body    (json/write-str {:ok false :error (.getMessage e)})})))

(defn- generate-label-html
  "Generate HTML for a product label."
  [product]
  (let [qr-text (qrcode/make-product-qr-text (:id product))
        qr-base64 (qrcode/encode-to-base64 qr-text {:size 150})
        name (:nombre product)
        price (str "$" (format "%.2f" (:precio product)))
        id-str (str (:id product))]
    [:div.label-card
     {:style "width: 200px; height: 120px; padding: 10px; margin: 5px; border: 1px solid #ccc; display: inline-block; text-align: center; page-break-inside: avoid;"}
     [:img {:src qr-base64 :style "width: 80px; height: 80px;"}]
     [:div {:style "font-size: 10px; margin-top: 5px;"} name]
     [:div {:style "font-size: 12px; font-weight: bold;"} price]
     [:div {:style "font-size: 8px; color: #888;"} (str "ID: " id-str)]]))

(defn api-print-labels
  "JSON API: generate HTML with QR code labels for selected products."
  [request]
  (try
    (let [body     (json/read-str (slurp (:body request)) :key-fn keyword)
          ids      (map #(Long/parseLong (str %)) (:producto_ids body))
          products (if (empty? ids)
                     []
                     (model/get-productos-by-ids ids))]
      (if (empty? products)
        {:status  400
         :headers {"Content-Type" "application/json"}
         :body    (json/write-str {:ok false :error "No products selected"})}
        (let [html-content (hiccup/html
                            [:html
                             [:head
                              [:title "Etiquetas de Productos"]
                              [:style
                               "@media print { .label-card { break-inside: avoid; } }"]]
                             [:body
                              [:h2 "Etiquetas de Productos"]
                              [:div (map generate-label-html products)]]])]
          {:status  200
           :headers {"Content-Type" "text/html; charset=utf-8"}
           :body    html-content})))
    (catch Exception e
      (println "[ERROR] POS print-labels failed:" (.getMessage e))
      (.printStackTrace e)
      {:status  500
       :headers {"Content-Type" "application/json"}
       :body    (json/write-str {:ok false :error (.getMessage e)})})))
