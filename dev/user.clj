(ns user
  (:require [smeagol.main :as main]
            [clojure.java.io :as io]
            [juxt.dirwatch :refer [watch-dir close-watcher]]
            [integrant.repl :refer [clear halt go init prep suspend resume reset set-prep!]]
            [integrant.core :as ig])
  (:import [java.io File]))

(defn- clj-file? [event]
  (println "event:" (pr-str event))
  (let [^File file (:file event)
        path (.getAbsolutePath file)]
    (and (.isFile file)
         (not (re-find #"/.git(/|$)" path))
         (re-find #".clj(c|s)?$" path))))

(defn do-reset [s]
  (println "Doing reset" (pr-str s))
  (binding [*ns* (find-ns 'user)]
    (reset)))

(defmethod ig/init-key :watch [_ _]
  (let [pwd (io/file "src")]
    (println (str "Watching: " (.getAbsolutePath pwd)))
    #_(watch-dir println pwd)
    (watch-dir #(when (clj-file? %) (do-reset %)) pwd)))

(defmethod ig/halt-key! :watch [_ w] (close-watcher w))

(set-prep! #(assoc (main/read-config nil) :watch {}))

(println "System: #'integrant.repl.state/system")
(println "Run with (reset)")

