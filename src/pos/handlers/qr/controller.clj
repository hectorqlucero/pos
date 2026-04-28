(ns pos.handlers.qr.controller
  (:require [pos.handlers.qr.model :as model]
            [pos.handlers.qr.view :as view]
            [pos.handlers.pos.model :as pos-model]
            [pos.layout :as layout]
            [pos.models.util :refer [get-session-id]]
            [clojure.data.json :as json]))

(defn print-labels
  "Serve the print labels page."
  [request]
  (let [title "Imprimir Etiquetas"
        ok (get-session-id request)
        productos (pos-model/get-productos)
        js (view/print-labels-js)
        page-content (view/print-labels-content productos)]
    (println "productos count:" (count productos))
    (println "first producto:" (first productos))
    (layout/application request title ok js page-content)))

(defn api-parse-qr
  "JSON API: parse a scanned QR code and return matching product."
  [request]
  (let [qr-text (get-in request [:params :qr] "")]
    (if (empty? qr-text)
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:ok false :error "No QR code provided"})}
      (let [product (model/get-product-by-qr qr-text)]
        (if product
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (json/write-str {:ok true :product product})}
          {:status 404
           :headers {"Content-Type" "application/json"}
           :body (json/write-str {:ok false :error "Product not found"})})))))

(defn api-print-labels
  "JSON API: generate HTML with QR code labels for selected products."
  [request]
  (try
    (let [body-str (slurp (:body request))
          body (json/read-str body-str :key-fn keyword)
          ids (get body :producto_ids [])
          num-ids (mapv #(Long/parseLong (str %)) ids)
          products (pos-model/get-productos-by-ids num-ids)]
      (if (empty? products)
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:ok false :error "No products selected"})}
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:ok true :labels (view/generate-labels-html products)})}))
    (catch Exception e
      (println "ERROR:" (.getMessage e))
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:ok false :error (.getMessage e)})})))