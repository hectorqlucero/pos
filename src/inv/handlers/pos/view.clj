(ns inv.handlers.pos.view
  (:require
   [inv.i18n.core :as i18n]
   [clojure.data.json :as json]
   [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn- product-image
  "Render product image or placeholder icon"
  [product]
  (if (not-empty (:imagen product))
    [:img {:src (str "/uploads/" (:imagen product))
           :alt (:nombre product)
           :style "max-height: 80px; max-width: 100%; object-fit: contain;"}]
    [:i.bi.bi-box-seam {:style "font-size: 3rem; color: #6c757d;"}]))

(defn- product-card
  "Render a single clickable product card"
  [product]
  [:div.col-6.col-md-4.col-xl-3.pos-product-card
   {:data-id (:id product)
    :data-nombre (:nombre product)
    :data-precio (str (:precio product))
    :data-stock (str (:stock product))}
   [:div.card.h-100.border.pos-card-clickable
    {:role "button"
     :onclick (str "POS.addItem(" (:id product) ")")}
    [:div.card-body.text-center.p-2
     [:div.pos-product-img.mb-2
      (product-image product)]
     [:p.card-text.fw-semibold.mb-1.text-truncate (:nombre product)]
     [:span.badge.bg-success.fs-6 (str "$" (:precio product))]]]])

(defn- product-grid
  "Render the grid of product cards"
  [productos]
  [:div#pos-product-grid.row.g-3
   (for [p productos]
     (product-card p))])

(defn- search-bar
  "Render the product search input"
  [request]
  [:div.mb-3
   [:div.input-group.input-group-lg
    [:span.input-group-text [:i.bi.bi-search]]
    [:input#pos-search.form-control
     {:type "text"
      :placeholder (i18n/tr request :pos/search-product)
      :autocomplete "off"}]]])

(defn- products-panel
  "Render the left panel with product browsing"
  [request productos]
  [:div.col-lg-8
   [:div.card.shadow-sm.border-0
    [:div.card-body
     [:div.mb-3
      [:button.btn.btn-primary.btn-lg
       {:type "button"}
       [:i.bi.bi-person-fill.me-2]
       (i18n/tr request :pos/public-sale)]]
     (search-bar request)
     [:h5.fw-bold.mb-3 (i18n/tr request :pos/best-sellers)]
     (product-grid productos)
     [:div.mt-3
      [:button.btn.btn-outline-secondary
       {:type "button"
        :onclick "POS.clearCart()"}
       [:i.bi.bi-pencil-square.me-2]
       (i18n/tr request :pos/clear)]]]]])

(defn- sale-details-panel
  "Render the right panel with cart and payment"
  [request csrf-token]
  [:div.col-lg-4
   [:div.card.shadow-sm.border-0
    [:div.card-header.bg-light
     [:h5.fw-bold.mb-0 (i18n/tr request :pos/sale-details)]]
    [:div.card-body
     [:div#pos-cart-items
      [:p.text-muted.text-center (i18n/tr request :pos/empty-cart)]]
     [:hr]
     [:div.d-flex.justify-content-between.align-items-center.mb-3
      [:span.fw-bold.fs-5 (i18n/tr request :pos/total)]
      [:span#pos-total.fw-bold.fs-4 "$0.00"]]
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
     [:div.d-flex.justify-content-between.align-items-center.mb-4
      [:label.fw-semibold (i18n/tr request :pos/change)]
      [:span#pos-change.fs-5 "0.00"]]
     [:button#pos-register-btn.btn.btn-success.btn-lg.w-100.mb-3
      {:type "button"
       :onclick "POS.registerSale()"
       :disabled "disabled"}
      (i18n/tr request :pos/register-sale)]
     [:div.text-center
      [:a#pos-print-btn.text-decoration-none
       {:href "#"
        :onclick "POS.printReceipt(); return false;"
        :style "display:none;"}
       [:i.bi.bi-printer.me-2]
       (i18n/tr request :pos/print-receipt)]]]]
   [:div {:style "display:none;"} csrf-token]])

(defn pos-view
  "Render the POS interface content"
  [request productos]
  (let [csrf-token (anti-forgery-field)]
    (list
     [:link {:rel "stylesheet" :href "/css/pos.css?v=1"}]
     [:div#pos-app {:data-productos (json/write-str productos)}
      [:div.row.g-3
       (products-panel request productos)
       (sale-details-panel request csrf-token)]]
     [:script {:src "/js/pos.js?v=1"}])))
