(ns inv.handlers.pos.view
  (:require
   [inv.i18n.core :as i18n]
   [clojure.data.json :as json]
   [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn pos-view
  "Render the POS interface content"
  [request productos]
  (let [csrf-token (anti-forgery-field)]
    (list
     [:link {:rel "stylesheet" :href "/css/pos.css?v=1"}]
     [:div#pos-app {:data-productos (json/write-str productos)}
      [:div.row.g-3
       ;; Left panel - Products
       [:div.col-lg-8
        [:div.card.shadow-sm.border-0
         [:div.card-body
          ;; Customer type button
          [:div.mb-3
           [:button.btn.btn-primary.btn-lg
            {:type "button"}
            [:i.bi.bi-person-fill.me-2]
            (i18n/tr request :pos/public-sale)]]
          ;; Search bar
          [:div.mb-3
           [:div.input-group.input-group-lg
            [:span.input-group-text [:i.bi.bi-search]]
            [:input#pos-search.form-control
             {:type "text"
              :placeholder (i18n/tr request :pos/search-product)
              :autocomplete "off"}]]]
          ;; Best sellers / product grid
          [:h5.fw-bold.mb-3 (i18n/tr request :pos/best-sellers)]
          [:div#pos-product-grid.row.g-3
           (for [p productos]
             [:div.col-6.col-md-4.col-xl-3.pos-product-card
              {:data-id (:id p)
               :data-nombre (:nombre p)
               :data-precio (str (:precio p))
               :data-stock (str (:stock p))}
              [:div.card.h-100.border.pos-card-clickable
               {:role "button"
                :onclick (str "POS.addItem(" (:id p) ")")}
               [:div.card-body.text-center.p-2
                [:div.pos-product-img.mb-2
                 (if (not-empty (:imagen p))
                   [:img {:src (str "/uploads/" (:imagen p))
                          :alt (:nombre p)
                          :style "max-height: 80px; max-width: 100%; object-fit: contain;"}]
                   [:i.bi.bi-box-seam {:style "font-size: 3rem; color: #6c757d;"}])]
                [:p.card-text.fw-semibold.mb-1.text-truncate (:nombre p)]
                [:span.badge.bg-success.fs-6 (str "$" (:precio p))]]]])]
          ;; Clear button
          [:div.mt-3
           [:button.btn.btn-outline-secondary
            {:type "button"
             :onclick "POS.clearCart()"}
            [:i.bi.bi-pencil-square.me-2]
            (i18n/tr request :pos/clear)]]]]]
       ;; Right panel - Sale details
       [:div.col-lg-4
        [:div.card.shadow-sm.border-0
         [:div.card-header.bg-light
          [:h5.fw-bold.mb-0 (i18n/tr request :pos/sale-details)]]
         [:div.card-body
          ;; Cart items
          [:div#pos-cart-items
           [:p.text-muted.text-center (i18n/tr request :pos/empty-cart)]]
          [:hr]
          ;; Total
          [:div.d-flex.justify-content-between.align-items-center.mb-3
           [:span.fw-bold.fs-5 (i18n/tr request :pos/total)]
           [:span#pos-total.fw-bold.fs-4 "$0.00"]]
          ;; Payment
          [:div.mb-3
           [:div.d-flex.justify-content-between.align-items-center
            [:label.fw-semibold (i18n/tr request :pos/payment)]
            [:input#pos-payment.form-control.text-end
             {:type "number"
              :step "0.01"
              :min "0"
              :style "max-width: 150px;"
              :placeholder "0.00"
              :oninput "POS.calcChange()"}]]]
          ;; Change
          [:div.d-flex.justify-content-between.align-items-center.mb-4
           [:label.fw-semibold (i18n/tr request :pos/change)]
           [:span#pos-change.fs-5 "0.00"]]
          ;; Register sale button
          [:button#pos-register-btn.btn.btn-success.btn-lg.w-100.mb-3
           {:type "button"
            :onclick "POS.registerSale()"
            :disabled "disabled"}
           (i18n/tr request :pos/register-sale)]
          ;; Print receipt
          [:div.text-center
           [:a#pos-print-btn.text-decoration-none
            {:href "#"
             :onclick "POS.printReceipt(); return false;"
             :style "display:none;"}
            [:i.bi.bi-printer.me-2]
            (i18n/tr request :pos/print-receipt)]]]]
        ;; Hidden CSRF field
        [:div {:style "display:none;"} csrf-token]]]]
     [:script {:src "/js/pos.js?v=1"}])))
