(ns inv.routes.proutes
  (:require
   [compojure.core :refer [defroutes GET POST]]
   [inv.handlers.pos.controller :as pos]))

;; All CRUD routes now handled by parameter-driven engine
;; Add custom non-CRUD routes here if needed

(defroutes proutes
  ;; POS routes
  (GET "/pos" request (pos/pos request))
  (GET "/api/pos/search" request (pos/api-search request))
  (POST "/api/pos/register" request (pos/api-register-sale request)))
