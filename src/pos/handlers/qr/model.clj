(ns pos.handlers.qr.model
  (:require [pos.handlers.pos.model :as pos-model]
            [pos.services.qrcode :as qrcode]))

(defn get-product-by-qr
  "Parse QR code text and return matching product."
  [qr-text]
  (let [product-id (qrcode/parse-product-qr qr-text)]
    (when product-id
      (pos-model/get-producto-by-id product-id))))

(defn generate-product-qr-base64
  "Generate QR code base64 for a product."
  [product-id & [opts]]
  (let [qr-text (qrcode/make-product-qr-text product-id)]
    (qrcode/encode-to-base64 qr-text opts)))