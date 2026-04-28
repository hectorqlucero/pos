(ns pos.routes.proutes
  (:require [compojure.core :refer [defroutes GET POST]]
            [pos.handlers.pos.controller :as pos]
            [pos.handlers.qr.controller :as qr]
            [pos.handlers.dashboard.controller :as dashboard]))

(defroutes proutes
  (GET "/dashboard" request (dashboard/main request))
  (GET  "/pos"              request (pos/pos request))
  (GET  "/print-labels"     request (qr/print-labels request))
  (GET  "/api/pos/search"   request (pos/api-search request))
  (POST "/api/pos/register" request (pos/api-register-sale request))
  (POST "/api/pos/print-labels" request (qr/api-print-labels request))
  (GET  "/api/pos/parse-qr" request (qr/api-parse-qr request)))
