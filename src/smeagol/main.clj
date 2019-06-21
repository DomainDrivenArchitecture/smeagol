(ns smeagol.main
  (:require [integrant.core :as ig]
            [campfire.core :as campfire]
            [campfire.project :as proj]
            [smeagol.configuration :as configuration]
            ;; namespaces named not as integrant config keys
            [smeagol.routes.wiki]
            [smeagol.handler]
            [smeagol.include.resolve])
  (:gen-class))

(defn read-config [config-fname]
  (let [cfg (-> config-fname
                (or configuration/config-file-path)
                slurp
                ig/read-string)]
    (doseq [[k v] cfg :when (:campfire.core/path v)]
      (defmethod ig/init-key k [_ opts] (campfire/init opts))
      (defmethod ig/halt-key! k [_ c] (campfire/halt c))
      (defmethod ig/suspend-key! k [_ c] (proj/suspend c))
      (defmethod ig/resume-key k [_ opts old-opts c] (proj/resume c opts old-opts)))
    cfg))

(defn -main [& args]
  (-> args first read-config ig/init))
