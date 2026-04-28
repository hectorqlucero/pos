(ns pos.services.qrcode
  (:import [com.google.zxing BarcodeFormat]
           [com.google.zxing.client.j2se MatrixToImageWriter]
           [com.google.zxing.common BitMatrix]
           [com.google.zxing.qrcode QRCodeWriter]))

(defn generate-qr-bytes
  "Generate QR code as PNG bytes.
   Options: :size (default 200)"
  [text & [opts]]
  (let [size (or (:size opts) 200)
        qr-writer (QRCodeWriter.)
        hints (doto (java.util.HashMap.)
               (.put com.google.zxing.EncodeHintType/CHARACTER_SET "UTF-8")
               (.put com.google.zxing.EncodeHintType/MARGIN 1))
        bit-matrix (.encode qr-writer text BarcodeFormat/QR_CODE size size hints)
        baos (java.io.ByteArrayOutputStream.)]
    (MatrixToImageWriter/writeToStream bit-matrix "PNG" baos)
    (.toByteArray baos)))

(defn encode-to-base64
  "Generate QR code and return as base64 string (with data URI prefix)."
  [text & [opts]]
  (let [bytes (generate-qr-bytes text opts)
        b64 (.encodeToString (java.util.Base64/getEncoder) bytes)]
    (str "data:image/png;base64," b64)))

(defn generate-qr-file
  "Generate QR code and save to file."
  [text file-path & [opts]]
  (let [size (or (:size opts) 200)
        qr-writer (QRCodeWriter.)
        hints (doto (java.util.HashMap.)
               (.put com.google.zxing.EncodeHintType/CHARACTER_SET "UTF-8")
               (.put com.google.zxing.EncodeHintType/MARGIN 1))
        bit-matrix (.encode qr-writer text BarcodeFormat/QR_CODE size size hints)]
    (MatrixToImageWriter/writeToPath bit-matrix "PNG" (java.nio.file.Paths/get file-path (into-array [])))))

(defn make-product-qr-text
  "Create QR code text for a product (uses ID as barcode).
   Format: PROD-{id}"
  [product-id]
  (str "PROD-" product-id))

(defn parse-product-qr
  "Parse QR code text back to product ID.
   Returns nil if not a valid product QR code."
  [qr-text]
  (when (and qr-text (string? qr-text))
    (let [[_ id-str] (re-find #"^PROD-(\d+)$" qr-text)]
      (when id-str
        (Long/parseLong id-str)))))