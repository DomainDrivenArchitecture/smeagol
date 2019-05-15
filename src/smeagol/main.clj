(ns smeagol.main
  (:require [integrant.core :as ig]
            [smeagol.configuration :as configuration]
            ;; namespaces named not as integrant config keys
            [smeagol.routes.wiki]
            [smeagol.handler]
            [smeagol.include.resolve])
  (:gen-class))

(defn read-config [config-fname]
  (-> config-fname
      (or configuration/config-file-path)
      slurp
      ig/read-string))

(defn -main [& args]
  (-> args first read-config ig/init))
