(ns smeagol.testing
  (:require [clojure.string :as string]
            [clojure.edn :as edn]
            [smeagol.configuration :refer [config]]
            [smeagol.testing.execution :refer [execute]]))

(defn do-test [{:keys [out] :as params}]
  (let [{:keys [error result]} (execute params)]
    (if error
      {:result :error :error error}
      (if (= result out)
        {:result :ok}
        {:result :failure :expected out :actual result}))))


(defn- parse-value [^String line]
  (try
    (let [value (edn/read-string line)]
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
      {:error (str "Namespace: " ns-name " is not listed: "
                   (-> config :test-namespaces keys))})))


(defn process [^String text ^Integer index]
  (let [{:keys [error] :as params} (-> text parse whitelist-namespace)]
    (if error
      (str "<pre class=\"test-result error\">" (pr-str params) "</pre><pre>" text "</pre>")
      (let [{:keys [result] :as test-result} (do-test params)]
        (str "<pre class=\"test-result " (name result) "\">" (pr-str test-result) "</pre><pre>" text "</pre>")))))
