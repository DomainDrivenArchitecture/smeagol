(ns smeagol.testing
  (:require [clojure.string :as string]
            [clojure.test :as test]
            [com.stuartsierra.component :as component]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [campfire.core :as campfire]
            [campfire.project :as proj]
            [aero.core :as aero]
            [integrant.core :as ig]
            [smeagol.configuration :refer [config]]
            [smeagol.testing.execution :as execution :refer [execute]])
  (:import [java.io StringReader]
           [clojure.lang LineNumberingPushbackReader]))

(defn string->input-stream [s]
  (-> s .getBytes io/input-stream))

(defn smeagol-include-resolver [x include]
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

(defn do-test [{:keys [out] :as params}]
  (let [{:keys [error result]} (execute params)]
    (if error
      {:result :error :error error}
      (if (= result out)
        {:result :ok}
        {:result :failure :expected out :actual result}))))


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


(defn whitelist-namespace [test-namespaces {:keys [fn-name] :as params}]
  (let [ns-name (namespace fn-name)]
    (if-let [ns-config (-> test-namespaces (get ns-name))]
      (merge ns-config params)
      {:error (str "Namespace/Var: " (pr-str ns-name) " is not listed: "
                   (-> test-namespaces keys))})))


(defn process [test-namespaces ^String text ^Integer index]
  (let [{:keys [error] :as params} (-> text parse (partial whitelist-namespace test-namespaces))]
    (if error
      (str "<pre class=\"test-result error\">" (pr-str params) "</pre><pre>" text "</pre>")
      (let [{:keys [result] :as test-result} (do-test params)]
        (str "<pre class=\"test-result " (name result) "\">" (pr-str test-result) "</pre><pre>" text "</pre>")))))

(defn update-map [m f]
  (reduce-kv (fn [m k v]
               (assoc m k (f v))) {} m))

(defn run-tests [proc remote-ns]
  (let [require-code `(require (quote ~remote-ns))
        test-code `(test/run-tests (quote ~remote-ns))]
    ;; cannot supply a single form, :(
    (campfire/eval proc require-code)
    (campfire/eval proc test-code)))
