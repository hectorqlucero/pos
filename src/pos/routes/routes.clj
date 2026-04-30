(ns pos.routes.routes
  (:require
   [compojure.core :refer [defroutes GET POST]]
   [pos.handlers.home.controller :as home-controller]
   [pos.handlers.dashboard.controller :as dashboard]
   [pos.handlers.cotizaciones.controller :as cotizaciones-ctrl]))

(defroutes open-routes
  (GET "/" params [] (dashboard/main params))
  (GET "/home/login" params [] (home-controller/login params))
  (POST "/home/login" params [] (home-controller/login-user params))
  (GET "/home/logoff" params [] (home-controller/logoff-user params)))

(defroutes password-routes
  (GET "/change/password" params [] (home-controller/change-password params))
  (POST "/change/password" params [] (home-controller/process-password params)))

(defroutes cotizaciones-routes
  (GET "/cotizaciones" params [] (cotizaciones-ctrl/index params))
  (GET "/cotizaciones/nuevo" params [] (cotizaciones-ctrl/nuevo params))
  (GET "/cotizaciones/editar/:id" params [] (cotizaciones-ctrl/editar params))
  (GET "/cotizaciones/ver/:id" params [] (cotizaciones-ctrl/ver params))
  (POST "/api/cotizaciones/guardar" params [] (cotizaciones-ctrl/guardar params))
  (POST "/api/cotizaciones/eliminar" params [] (cotizaciones-ctrl/eliminar params))
  (POST "/api/cotizaciones/cambiar-estado" params [] (cotizaciones-ctrl/cambiar-estado params))
  (POST "/api/cotizaciones/reembolsar" params [] (cotizaciones-ctrl/reembolsar params))
  (GET "/api/cotizaciones/productos" params [] (cotizaciones-ctrl/api-search-productos params))
  (GET "/api/cotizaciones/clientes" params [] (cotizaciones-ctrl/api-search-clientes params)))
