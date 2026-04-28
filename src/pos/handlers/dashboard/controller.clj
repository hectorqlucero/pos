(ns pos.handlers.dashboard.controller
  (:require
   [pos.handlers.dashboard.model :as model]
   [pos.handlers.dashboard.view :as view]
   [pos.layout :refer [application]]
   [pos.models.util :refer [get-session-id]]))

(defn main
  [request]
  (let [title "DASHBOARD"
        ok (get-session-id request)
        js nil
        stats (model/get-stats)
        content (view/main title stats)]
    (application request title ok js content)))
