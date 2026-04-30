(ns pos.handlers.cotizaciones.view
  (:require [pos.i18n.core :as i18n]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [hiccup.util :refer [raw-string]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn badge [e]
  (case e
    "borrador" [:span.badge.bg-secondary "Borrador"]
    "enviada" [:span.badge.bg-info "Enviada"]
    "aceptada" [:span.badge.bg-success "Aceptada"]
    "rechazada" [:span.bg-danger "Rechazada"]
    "cancelada" [:span.badge.bg-warning "Cancelada"]
    [:span.badge.bg-light e]))

(defn date-str [f]
  (if f
    (.format (java.time.LocalDateTime/parse (str/replace f " " "T"))
             (java.time.format.DateTimeFormatter/ofPattern "dd/MM/yyyy HH:mm"))
    ""))

(defn btn-view [id] [:a.btn.btn-sm.btn-outline-primary {:href (str "/cotizaciones/ver/" id) :target "_blank"} "Ver"])
(defn btn-edit [id] [:a.btn.btn-sm.btn-outline-secondary {:href (str "/cotizaciones/editar/" id)} "Edit"])
(defn btn-del [id] [:button.btn.btn-sm.btn-outline-danger {:onclick (str "Cotizaciones.delete(" id ")")} "X"])

(defn tr-cotizacion [c]
  [:tr
   [:td (str (:id c))]
   [:td (date-str (:fecha c))]
   [:td (or (:cliente_nombre c) "-")]
   [:td (or (:cliente_telefono c) "-")]
   [:td (str "$" (format "%.2f" (double (:total c))))]
   [:td (badge (:estado c))]
   [:td (or (:usuario_nombre c) "-")]
   [:td (list (btn-view (:id c)) (btn-edit (:id c)) (btn-del (:id c)))]])

(defn index-view [req list]
  (let [t (i18n/tr req :cotizaciones/title)
        csrf (anti-forgery-field)]
    [:div
     [:h2 t]
     (raw-string csrf)
     [:a.btn.btn-primary {:href "/cotizaciones/nuevo"} "Nueva"]
     [:table.table.table-striped.mt-3
      [:thead [:tr [:th "ID"] [:th "Fecha"] [:th "Cliente"] [:th "Tel"] [:th "Total"] [:th "Estado"] [:th ""]]]
      [:tbody (if (empty? list) [:tr [:td {:colSpan 7} "No hay"]] (map tr-cotizacion list))]]
     [:script {:src "/js/cotizaciones.js"}]]))

(defn row-servicio [selected? s readonly?]
  [:div.col-3
   [:div.form-check.mb-3
    [:input.form-check-input
     {:type "checkbox"
      :id (str "servicio-" (:id s))
      :checked selected?
      :disabled readonly?
      :data-id (:id s)
      :data-nombre (:nombre s)
      :data-precio (:precio s)
      :onchange (when-not readonly? "Cotizaciones.toggleServicio(this)")}]
    [:label.form-check-label {:for (str "servicio-" (:id s))}
     (:nombre s)
     [:span.badge.bg-success.ms-2 (str "$" (:precio s))]]]])

(defn row-item [idx i readonly?]
  [:tr
   [:td (inc idx)]
   [:td (:nombre i)]
   [:td (:cantidad i)]
   [:td (str "$" (:precio i))]
   [:td (str "$" (* (:cantidad i) (:precio i)))]
   [:td (when-not readonly?
          [:button.btn.btn-sm.btn-danger
           {:onclick (str "Cotizaciones.removeItem(" idx ")")}
           "X"])]])

(defn table-items [items readonly?]
  (if (empty? items)
    [:tr [:td "Sin items"]]
    (map-indexed (fn [idx item] (row-item idx item readonly?)) items)))

(defn select-estado [sel]
  (for [o ["borrador" "enviada" "aceptada" "rechazada" "cancelada"]]
    [:option {:value o :selected (= o sel)} o]))

(defn edit-view [_req cot servicios dets]
  (let [csrf (anti-forgery-field)
        items (mapv (fn [d]
                      {:producto_id (:producto_id d)
                       :nombre      (:nombre d)
                       :precio      (double (:precio_unitario d))
                       :cantidad    (:cantidad d)
                       :tipo        (:tipo d)})
                    (or dets []))
        selected-service-ids (set (for [i items :when (= (:tipo i) "servicio")] (:producto_id i)))
        js-it (json/write-str items)
        tot (double (or (:total cot) 0))
        est (or (:estado cot) "borrador")
        accepted? (= est "aceptada")
        cancelled? (= est "cancelada")
        readonly? (or accepted? cancelled?)]
    [:div
     [:h2 (if cot "Editar Cotización" "Nueva Cotización")]
     (when cancelled?
       [:div.alert.alert-warning "Cotización cancelada. No se puede modificar; cree una nueva cotización si necesita cambios."])
     (when accepted?
       [:div.alert.alert-info "Cotización procesada en POS. No se puede modificar; puede devolverla si es necesario."])
     [:div.mb-3
      [:a.btn.btn-secondary.me-2 {:href "/cotizaciones"} "Volver"]
      [:button.btn.btn-success.me-2
       {:onclick "Cotizaciones.save()"
        :disabled readonly?
        :title (when readonly? "No se puede guardar porque esta cotización ya fue procesada o cancelada")}
       "Guardar"]
      (when (:id cot)
        (cond
          cancelled? nil
          accepted? [:button.btn.btn-info
                     {:type "button"
                      :onclick nil
                      :disabled true
                      :title "Ya fue procesada en POS"}
                     "Ya procesada"]
          :else [:button.btn.btn-info
                 {:type "button"
                  :onclick "Cotizaciones.processToPOS()"}
                 "Procesar en POS"]))
      (when accepted?
        [:button.btn.btn-danger {:type "button" :onclick "Cotizaciones.refund()"} "Reembolsar/Cancelar"])]
     [:input#cliente-id {:type "hidden" :value (or (:cliente_id cot) "")}]
     [:input#cotizacion-id {:type "hidden" :value (or (:id cot) "")}]
     [:input#venta-id {:type "hidden" :value (or (:venta_id cot) "")}]
     (raw-string csrf)
     [:div.row.mt-3
      [:div.col-8
       [:div.card.mb-3 [:div.card-header "Cliente"]
        [:div.card-body
         [:div.mb-3
          [:label.form-label "Buscar cliente existente"]
          [:input#cliente-search.form-control {:type "text"
                                               :placeholder "Nombre o teléfono"
                                               :autocomplete "off"
                                               :disabled readonly?
                                               :oninput (when-not readonly? "Cotizaciones.searchClientes(this.value)")}]
          [:div#cliente-results.list-group.mt-2]]
         [:input#cliente-nombre.form-control {:value (or (:cliente_nombre cot) "") :placeholder "Nombre del cliente" :disabled readonly?}]
         [:input#cliente-telefono.form-control.mt-2 {:value (or (:cliente_telefono cot) "") :placeholder "Teléfono" :disabled readonly?}]
         [:textarea#notas.form-control.mt-2 {:placeholder "Notas" :disabled readonly?} (or (:notas cot) "")]]]
       [:div.card.mb-3 [:div.card-header "Servicios"]
        [:div.card-body [:div.row (map (fn [s] (row-servicio (contains? selected-service-ids (:id s)) s readonly?)) servicios)]]]
       [:div.card.mb-3 [:div.card-header "Productos"]
        [:div.card-body
         [:div.d-flex.gap-2.mb-3
          [:button.btn.btn-primary {:type "button" :data-bs-toggle "modal" :data-bs-target "#searchProductModal" :disabled readonly?} "Buscar producto"]
          [:button.btn.btn-outline-secondary {:type "button" :onclick "Cotizaciones.addMiscCharge()" :disabled readonly?} "Agregar cargo extra"]]
         [:div.modal {:id "searchProductModal"}
          [:div.modal-dialog.modal-lg
           [:div.modal-content
            [:div.modal-header
             [:h5.modal-title "Buscar producto"]
             [:button.btn-close {:type "button" :data-bs-dismiss "modal" :aria-label "Cerrar"}]]
            [:div.modal-body
             [:input#product-search-input.form-control {:type "text"
                                                        :placeholder "Buscar producto por nombre, categoría o ID"
                                                        :autocomplete "off"
                                                        :disabled readonly?
                                                        :oninput (when-not readonly? "Cotizaciones.searchProducts(this.value)")}]
             [:div#search-results.row.g-3.mt-3]]
            [:div.modal-footer
             [:button.btn.btn-secondary {:type "button" :data-bs-dismiss "modal"} "Cerrar"]]]]]]]]
      [:div.col-4
       [:div.card [:div.card-header "Carrito"]
        [:div.card-body
         [:table.table.table-sm [:tbody {:id "cart-items"} (table-items items readonly?)]]
         [:hr]
         [:h4 "Total: " [:span#cart-total (str "$" (format "%.2f" (double tot)))]]
         [:select#estado.form-select {:disabled readonly?
                                      :title (when readonly? "No se puede cambiar el estado de una cotización procesada o cancelada")}
          (select-estado est)]]]]]
     [:script (str "var IT=" js-it ";")]
     [:script {:src "/js/cotizaciones.js"}]]))

(defn print-view [_req c ds]
  [:div
   [:h2 "COTIZACIÓN"]
   [:h5 "No. " (:id c)]
   [:p "Fecha: " (date-str (:fecha c))]
   [:h5 "Cliente: " (or (:cliente_nombre c) "-")]
   [:h5 "Tel: " (or (:cliente_telefono c) "-")]
   [:table.table
    [:thead [:tr [:th "#"] [:th "Desc"] [:th "Cant"] [:th "P.Unit"] [:th "Subtotal"]]]
    [:tbody (map-indexed (fn [i d] [:tr [:td (inc i)] [:td (:nombre d)] [:td (:cantidad d)] [:td (str "$" (:precio_unitario d))] [:td (str "$" (:subtotal d))]]) ds)]
    [:tfoot [:tr [:td {:colSpan 4} "TOTAL"] [:td (str "$" (:total c))]]]]
   [:button.btn.btn-primary {:onclick "window.print()"} "Imprimir"]])