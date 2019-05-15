(ns smeagol.testing
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [aero.core :as aero]
            [smeagol.configuration :refer [config]]
            [smeagol.testing.execution :refer [execute]])
  (:import [java.io StringReader]
           [clojure.lang LineNumberingPushbackReader]))

(defn do-test [{:keys [out] :as params}]
  (let [{:keys [error result]} (execute params)]
    (if error
      {:result :error :error error}
      (if (= result out)
        {:result :ok :expected out :actual result}
        {:result :failure :expected out :actual result}))))

(defn string->input-stream [s]
  (-> s .getBytes io/input-stream))

(defn smeagol-include-resolver [_ include]
  (let [included-file (io/file (:content-dir config) include)]
    (if (.exists included-file)
      included-file
      ;; also expects an input-stream
      (-> {::not-found include} pr-str string->input-stream))))

(defn read-string-with-aero
  "In addition to aero.core/read-config, which accepts only input-stream"
  [source-string]
  (aero/read-config (string->input-stream source-string)
                    {:resolver smeagol-include-resolver}))


(defn read-tagged-aero-config
  ([source given-opts]
   (let [opts (merge {:resolver #(io/resource %2)} given-opts {:source source})
         tag-fn (partial aero/reader opts)]
     (-> source
         (aero/resolve-refs tag-fn)
         (#'aero/resolve-tag-wrappers tag-fn)
         (#'aero/realize-deferreds))))
  ([source] (read-tagged-aero-config source {})))


(defn flush-md
  "Renders md file under (:content-dir smeagol.configuration/config)"
  [tests]
  (println (pr-str {:log "flush-md called" :tests tests})))


(defmacro with-aero
  "You must call (setup-smeagol!)
  before using any specific tag literals"
  [form]
  `(read-tagged-aero-config ~form))


(defmacro setup! []
  `(do
     (def ~'-smeagol-tests (atom []))
     (require 'aero.core)
     (set! *default-data-reader-fn* #'aero.core/tag-wrapper)))


(defmacro teardown! []
  `(do
     (flush-md ~'-smeagol-tests)
     (set! *default-data-reader-fn* nil)))

(def local-ns-config #:smeagol.testing.execution{:type :local})

(defmacro defdatatest [name & body]
  `(do
     ;; TODO validate body structure
     (swap! ~'-smeagol-tests conj ~@body)
     (deftest ~name
       (let [expanded-body# (with-aero ~@body)
             result# (do-test expanded-body#)]
         (println (pr-str {:defdatatest result#}))
         (is (= (:expected result#) (:actual result#)))))))


(defn- parse-value [^String line]
  (try
    (let [value (read-string-with-aero line)]
      {:line line :value value})
    (catch Exception e
      {:line line :error (.getMessage e)})))


(defn parse [^String text]
  (let [[sym rest] (string/split text #"\r?\n" 2)
        fn-name (-> sym string/trim symbol)]
    (let [{:keys [value error line] :as x} (parse-value (or rest ""))]
      (if error
        {:error (str "Failed parsing line due: " error)}
        (let [{:keys [in out] :or {in [] out nil}} value]
          {:fn-name fn-name :out out :in in :text text})))))


(defn whitelist-namespace [{:keys [fn-name] :as params}]
  (let [ns-name (namespace fn-name)]
    (if-let [ns-config (-> config :test-namespaces (get ns-name))]
      (merge ns-config params)
      {:error (str "Namespace: " (pr-str ns-name) " is not listed: "
                   (-> config :test-namespaces keys))})))


(defn process [^String text ^Integer index]
  (let [{:keys [error] :as params} (-> text parse whitelist-namespace)]
    (if error
      (str "<pre class=\"test-result error\">" (pr-str params) "</pre><pre>" text "</pre>")
      (let [{:keys [result] :as test-result} (do-test params)]
        (str "<pre class=\"test-result " (name result) "\">" (pr-str test-result) "</pre><pre>" text "</pre>")))))
