(ns pos.handlers.pos.view
  (:require [pos.i18n.core :as i18n]
            [pos.handlers.qr.model :as qr-model]
            [clojure.data.json :as json]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn- product-image [product]
  (if (not-empty (:imagen product))
    [:img {:src (str "/uploads/" (:imagen product))
           :alt (:nombre product)
           :style "max-height: 80px; max-width: 100%; object-fit: contain;"}]
    [:i.bi.bi-box-seam {:style "font-size: 3rem; color: #6c757d;"}]))

(defn- product-qr-code [product]
  (let [qr-base64 (qr-model/generate-product-qr-base64 (:id product) {:size 80})]
    [:img.qr-code
     {:src qr-base64
      :alt (str "QR " (:nombre product))
      :style "width: 50px; height: 50px; border: 1px solid #ddd; border-radius: 4px;"
      :title (str "QR: PROD-" (:id product))}]))

(defn- product-card [product]
  [:div.col-6.col-md-4.col-xl-3.pos-product-card
   {:data-id (:id product) :data-nombre (:nombre product)
    :data-precio (str (:precio product)) :data-stock (str (:stock product))}
   [:div.card.h-100.border.pos-card-clickable
    {:role "button" :onclick (str "POS.addItem(" (:id product) ")")}
    [:div.card-body.text-center.p-2
     [:div.pos-product-img.mb-2 (product-image product)]
     [:p.card-text.fw-semibold.mb-1.text-truncate (:nombre product)]
     [:span.badge.bg-success.fs-6 (str "$" (:precio product))]
     [:div.mt-2 (product-qr-code product)]]]])

(defn- products-panel [request productos]
  (let [csrf-token (or (some->> request :session :csrf-token) "")]
    [:div.col-lg-8
     [:div.d-flex.gap-2.mb-3
      [:button.btn.btn-outline-secondary {:type "button" :onclick "POS.clearCart()"}
       [:i.bi.bi-pencil-square.me-2] (i18n/tr request :pos/clear)]
      [:button.btn.btn-outline-primary {:type "button" :data-bs-toggle "modal" :data-bs-target "#qrScannerModal"}
       [:i.bi.bi-qr-code-scan.me-2] (i18n/tr request :pos/scan-qr)]
      [:button.btn.btn-outline-warning {:type "button" :onclick "POS.addMiscCharge()"}
       [:i.bi.bi-plus-circle.me-2] "Cargo"]]
     [:div.card.shadow-sm.border-0
      [:div.card-body
       [:div.mb-3 [:button.btn.btn-primary.btn-lg {:type "button"}
                   [:i.bi.bi-person-fill.me-2] (i18n/tr request :pos/public-sale)]]
       [:div.mb-3
        [:div.input-group.input-group-lg
         [:span.input-group-text [:i.bi.bi-search]]
         [:input#pos-search.form-control {:type "text" :placeholder (i18n/tr request :pos/search-product) :autocomplete "off"}]]]
       [:h5.fw-bold.mb-3 (i18n/tr request :pos/best-sellers)]
       [:div#pos-product-grid.row.g-3 (for [p productos] (product-card p))]]]]))

(defn- sale-details-panel [request csrf-token]
  [:div.col-lg-4
   [:div.card.shadow-sm.border-0
    [:div.card-header.bg-light [:h5.fw-bold.mb-0 (i18n/tr request :pos/sale-details)]]
    [:div.card-body
     [:div#pos-cart-items [:p.text-muted.text-center (i18n/tr request :pos/empty-cart)]]
     [:hr]
     [:div.d-flex.justify-content-between.align-items-center.mb-3
      [:span.fw-bold.fs-5 (i18n/tr request :pos/total)] [:span#pos-total.fw-bold.fs-4 "$0.00"]]
     [:div.mb-3 [:div.d-flex.justify-content-between.align-items-center
                 [:label.fw-semibold (i18n/tr request :pos/payment)]
                 [:input#pos-payment.form-control.text-end
                  {:type "number" :step "0.01" :min "0" :style "max-width: 150px;" :placeholder "0.00" :oninput "POS.calcChange()"}]]]
     [:div.d-flex.justify-content-between.align-items-center.mb-4
      [:label.fw-semibold (i18n/tr request :pos/change)] [:span#pos-change.fs-5 "0.00"]]
     [:button#pos-register-btn.btn.btn-success.btn-lg.w-100.mb-3
      {:type "button" :onclick "POS.registerSale()" :disabled "disabled"}
      (i18n/tr request :pos/register-sale)]
     [:div.text-center
      [:a#pos-print-btn.text-decoration-none
       {:href "#" :onclick "POS.printReceipt(); return false;" :style "display:none;"}
       [:i.bi.bi-printer.me-2] (i18n/tr request :pos/print-receipt)]]]]
   [:div {:style "display:none;"} csrf-token]])

(def qr-modal-str "<div class='modal fade' id='qrScannerModal' tabindex='-1' aria-labelledby='qrScannerModalLabel' aria-hidden='true' data-bs-backdrop='static'>
<div class='modal-dialog modal-lg modal-dialog-centered'>
<div class='modal-content'>
<div class='modal-header'>
<h5 class='modal-title' id='qrScannerModalLabel'>Escanear QR</h5>
<button type='button' class='btn-close' data-bs-dismiss='modal' aria-label='Close'></button>
</div>
<div class='modal-body text-center'>
<div id='qr-reader' style='width: 100%; max-width: 350px; margin: 0 auto;'></div>
<div class='mt-3'><p class='text-muted'>Apunte la cámara al código QR o suba una imagen</p>
<p class='small text-muted'>El producto se agregará sin cerrar el modal</p>
</div>
</div>
<div class='modal-footer'>
<button type='button' class='btn btn-secondary' data-bs-dismiss='modal'>Cerrar</button>
</div>
</div>
</div>
</div>")

(defn pos-view [request productos]
  (let [csrf-token (anti-forgery-field)]
    [:div
     [:link {:rel "stylesheet" :href "/css/pos.css?v=1"}]
     [:script {:src "https://cdn.jsdelivr.net/npm/qrcode@1.5.3/build/qrcode.min.js"}]
     [:div#pos-app {:data-productos (json/write-str productos)}
      [:div.row.g-3
       (products-panel request productos)
       (sale-details-panel request csrf-token)]]
     [:script {:type "text/html" :id "qr-modal-template"} qr-modal-str]
     [:script "document.getElementById('pos-app').insertAdjacentHTML('beforeend', document.getElementById('qr-modal-template').textContent);"]
     [:script {:src "https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js"}]
     [:script {:src "/js/qr-scanner.js"}]
     [:script {:src "/js/pos.js?v=4"}]]))