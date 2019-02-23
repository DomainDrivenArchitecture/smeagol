(ns smeagol.testing
  (:require [clojure.string :as string]
            [clojure.edn :as edn]
            [clojure.edn :as edn]
            [nrepl.core :as nrepl]))

(defn- call-with-require [fn-name in]
  (let [ns-sym (some-> fn-name namespace symbol)]
    `(do ~(when ns-sym `(require (quote ~ns-sym))) (apply ~fn-name ~in))))


(defn- do-remote-execute [{:keys [fn-name host port in timeout] :or {host "localhost" port 7888 timeout 1000}}]
  (with-open [conn (nrepl/connect :host host :port port)]
    (-> (nrepl/client conn timeout)
        (nrepl/message {:op :eval :code (pr-str (call-with-require fn-name in))})
        nrepl/combine-responses)))


(defn remote-execute [params]
  (let [{:keys [value err status]} (do-remote-execute params)]
    (if (status "eval-error")
      {:error err}
      {:result (-> value last (or "") edn/read-string)})))


(defn- local-execute [{:keys [fn-name in]}]
  (try (let [result (eval (call-with-require fn-name in))]
         {:result result})
       (catch Exception e
         {:error (.getMessage e)})))


(def execute remote-execute)


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
        {:error (str "Failed parsing line: " line " due: " error)}
        (let [{:keys [in out] :or {in [] out nil}} value]
          {:fn-name fn-name :out out :in in :text text})))))


(defn process [^String text ^Integer index]
  (let [{:keys [error] :as params} (parse text)]
    (if error
      (str "<pre class=\"test-result error\">" (pr-str params) "</pre><pre>" text "</pre>")
      (let [{:keys [result] :as test-result} (do-test params)]
        (str "<pre class=\"test-result " (name result) "\">" (pr-str test-result) "</pre><pre>" text "</pre>")))))
