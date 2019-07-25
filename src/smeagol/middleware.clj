(ns ^{:doc "In truth, boilerplate provided by LuminusWeb."
      :author "Simon Brooke"}
  smeagol.middleware
  (:require [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [selmer.middleware :refer [wrap-error-page]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [noir-exception.core :refer [wrap-internal-error]]
            [smeagol.util :as util]))

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


(defn log-request [handler]
  (fn [req]
    (timbre/debug req)
    (handler req)))


(defn development-middleware [_config]
  [wrap-error-page
   wrap-exceptions])


(defn production-middleware [config]
  [#(wrap-internal-error % :log (fn [e] (timbre/error e)))
   #(wrap-resource % "public")
   #(wrap-file % (util/content-dir config)
               {:index-files? false :prefer-handler? true})
   #(wrap-content-type %)
   #(wrap-not-modified %)])


(defn load-middleware [config]
  (concat (when (env :dev) (development-middleware config))
          (production-middleware config)))
