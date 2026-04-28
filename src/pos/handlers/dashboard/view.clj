(ns pos.handlers.dashboard.view)

;; 🎯 CARD REUTILIZABLE
(defn card
  [{:keys [title count image size]}]
  [:div {:class (str size " mb-3")}
   [:div.card.shadow-sm.h-100 {:style "border-radius:12px;"}

    ;; Imagen (placeholder si no hay)
    (if image
      [:img.card-img-top
       {:src (str "/uploads/" image)
        :style "height:140px; object-fit:cover; border-top-left-radius:12px; border-top-right-radius:12px;"}]
      [:div {:style "height:140px; background:#f1f1f1; border-top-left-radius:12px; border-top-right-radius:12px;"}])

    ;; Contenido
    [:div.card-body.text-center
     [:h6.card-title title]
     [:p.card-text.fw-bold count]]]])

;; 🚀 MAIN DASHBOARD
(defn main
  [title stats]
  [:div.container-fluid.p-4 {:style "background: linear-gradient(135deg,#5f2c82,#49a09d);"}

   [:h2.text-white.mb-4 title]

   ;; 🔹 FILA 1 → 4 tarjetas pequeñas
   [:div.row
    (card {:title "Ventas Hoy" :count (:total-ventas stats) :image "ventas.jpg" :size "col-md-3"})
    (card {:title "Transacciones" :count "86" :image "engrane.jpeg" :size "col-md-3"})
    (card {:title "Bajo Stock" :count "12" :image "bstock2.jpg" :size "col-md-3"})
    (card {:title "Clientes" :count "8" :image "clientes.jpg" :size "col-md-3"})]

   ;; 🔹 FILA 2 → 2 tarjetas grandes
   [:div.row
    (card {:title "Ventas del Mes" :count "$18,750" :image "vmes.jpg" :size "col-md-6"})
    (card {:title "Productos Más Vendidos" :count "Top 5" :image "pmasv.jpg" :size "col-md-6"})]

   ;; 🔹 FILA 3 → 3 tarjetas medianas
   [:div.row
    (card {:title "Últimas Ventas" :count "700" :image "ultimaventa.jpg" :size "col-md-4"})
    (card {:title "Métodos de Pago" :image "pagos.jpg" :size "col-md-4"})
    (card {:title "Alertas" :count "3 pendientes" :image "bstock.jpg" :size "col-md-4"})]])