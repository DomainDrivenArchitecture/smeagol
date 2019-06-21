(ns user
  (:refer-clojure :exclude [eval])
  (:require [smeagol.main :as main]
            [clojure.walk :as walk]
            [clojure.java.io :as io]
            [aprint.core :refer :all]
            [juxt.dirwatch :refer [watch-dir close-watcher]]
            [integrant.repl :refer [clear halt go init prep suspend resume reset set-prep!]]
            [clojure.pprint :refer [print-table]]
            [clojure.reflect :as r]
            [integrant.core :as ig])
  (:import [java.io File]))

(defn public-inspect [x]
  (print-table
   (filter #(contains? (:flags %) :public)
           (map
            #(apply dissoc % [:exception-types])
            (:members (r/reflect x))))))

(defn x [a]
  (println a)
  a)

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

(def demo-data
  #:smeagol-demo.ttl{:salaries
                     [#:smeagol-demo.ttl{:recurrance :monthly,
                                         :name "me",
                                         :amount 100.0}
                      #:smeagol-demo.ttl{:recurrance :yearly,
                                         :name "myself",
                                         :amount 2400.0}
                      #:smeagol-demo.ttl{:recurrance :monthly,
                                         :name "I",
                                         :amount 100.0}]})
