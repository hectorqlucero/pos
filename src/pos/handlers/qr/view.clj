(ns pos.handlers.qr.view
  (:require [pos.handlers.qr.model :as qr-model]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [clojure.walk :as walk]))

(defn print-labels-js []
  [:script {:src "/js/print-labels.js"}])

(defn generate-labels-html [products]
  (apply str (map (fn [p]
                    (let [product (walk/keywordize-keys p)
                          product-id (:id product)
                          qr-base64 (qr-model/generate-product-qr-base64 product-id {:size 150})
                          nombre (:nombre product)
                          precio (double (:precio product))]
                      (str "<div class=\"col-6 col-md-4 col-lg-3 mb-3\">"
                           "<div class=\"card h-100\">"
                           "<div class=\"card-body text-center\">"
                           "<img src=\"" qr-base64 "\" class=\"mb-2\" style=\"width:80px;height:80px;\">"
                           "<h6 class=\"card-title\">" nombre "</h6>"
                           "<p class=\"card-text fw-bold\">$" (format "%.2f" precio) "</p>"
                           "<small class=\"text-muted\">ID: " product-id "</small>"
                           "</div></div></div>")))
                  products)))

(defn cards
  [p]
  [:div.col-6.col-md-4.col-lg-3.mb-3
   [:div.card.h-100
    [:div.card-body.text-center
     [:div.form-check.mb-2
      [:input.form-check-input.label-checkbox {:type "checkbox"
                                               :id (str "label-" (:id p))
                                               :value (:id p)}]
      [:label.form-check-label {:for (str "label-" (:id p))} "Seleccionar"]]
     [:h6.card-title (:nombre p)]
     [:p.card-text (:nombre p) (str " $" (:precio p))]
     [:small.text-muted (str "ID: " (:id p))]]]])

(defn print-labels-content [productos]
  (let [csrf-token (anti-forgery-field)
        card-list (map cards productos)]

    [:div.container
     [:div.mb-3
      [:button#print-selected-btn.btn.btn-primary.me-2 {:disabled true :onclick "printSelectedLabels()"}
       [:i.bi.bi-qr-code.me-2] "Imprimir Etiquetas Seleccionadas"]
      [:button.btn.btn-outline-secondary.me-2 {:onclick "selectAllLabels()"} "Seleccionar Todos"]
      [:button.btn.btn-outline-secondary {:onclick "deselectAllLabels()"} "Deseleccionar Todos"]]
     [:div.row.g-3 card-list]
     csrf-token]))
