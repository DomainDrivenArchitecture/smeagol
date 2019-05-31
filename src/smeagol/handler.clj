(ns ^{:doc "Set up, configure, and clean up after the wiki server."
      :author "Simon Brooke"}
  smeagol.handler
  (:require [clojure.java.io :as cjio]
            [clojure.string :refer [lower-case]]
            [compojure.core :refer [defroutes]]
            [compojure.route :as route]
            [cronj.core :as cronj]
            [environ.core :refer [env]]
            [integrant.core :as ig]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [noir.util.middleware :refer [app-handler]]
            [ring.middleware.defaults :refer [site-defaults]]
            [ring.adapter.jetty :refer [run-jetty]]
            [selmer.parser :as parser]
            [smeagol.configuration :refer [config]]
            ;; [smeagol.routes.wiki :refer [wiki-routes]]
            [smeagol.middleware :refer [load-middleware]]
            [smeagol.session-manager :as session-manager]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;
;;;; Smeagol: a very simple Wiki engine.
;;;;
;;;; This program is free software; you can redistribute it and/or
;;;; modify it under the terms of the GNU General Public License
;;;; as published by the Free Software Foundation; either version 2
;;;; of the License, or (at your option) any later version.
;;;;
;;;; This program is distributed in the hope that it will be useful,
;;;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;;;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;;;; GNU General Public License for more details.
;;;;
;;;; You should have received a copy of the GNU General Public License
;;;; along with this program; if not, write to the Free Software
;;;; Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
;;;; USA.
;;;;
;;;; Copyright (C) 2014 Simon Brooke
;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn user-access [request]
  (session/get :user))


(defroutes base-routes
  (route/resources "/")
  (route/not-found "Not Found"))


(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "smeagol is shutting down...")
  (cronj/shutdown! session-manager/cleanup-job)
  (timbre/info "shutdown complete!"))


(defn init
  "init will be called once when
  app is deployed as a servlet on
  an app server such as Tomcat
  put any initialization code here"
  []
  (try
    (timbre/merge-config!
      {:appenders
       {:rotor (rotor/rotor-appender
                 {:path "smeagol.log"
                  :max-size (* 512 1024)
                  :backlog 10})}
       :level (or
                (:log-level config)
                (if (env :dev) :debug)
                :info)})
    (cronj/start! session-manager/cleanup-job)
    (if (env :dev) (parser/cache-off!))
    ;;start the expired session cleanup job
    (timbre/info "\n-=[ smeagol started successfully"
                 (when (env :dev) "using the development profile") "]=-")
    (catch Exception any
      (timbre/error any "Failure during startup")
      (destroy))))

;; timeout sessions after 30 minutes
(def session-defaults
  {:timeout (* 60 30)
   :timeout-response (redirect "/")})


(defn- make-defaults
  "set to true to enable XSS protection"
  [xss-protection?]
  (-> site-defaults
      (update-in [:session] merge session-defaults)
      (dissoc :static)
      (assoc-in [:security :anti-forgery] xss-protection?)))


(defn app [{:keys [config wiki]}]
  (app-handler
    ;; add your application routes here
    [wiki base-routes]
    ;; add custom middleware here
    :middleware (load-middleware config)
    :ring-defaults (make-defaults true)
    ;; add access rules here
    :access-rules [{:redirect "/auth"
                    :rule user-access}]
    ;; serialize/deserialize the following data formats
    ;; available formats:
    ;; :json :json-kw :yaml :yaml-kw :edn :yaml-in-html

    :formats [:json-kw :edn :transit-json]))

;; simulate lein-ring to prefer env var
(defmethod ig/init-key :smeagol/web [_ {:keys [port] :as system}]
  (let [app (app system)]
    {:app app
     :http (run-jetty app
                      {:port (Integer/valueOf (or (System/getenv "port") port)) :join? false})}))

(defmethod ig/halt-key! :smeagol/web [_ {:keys [http]}]
  (.stop http))
