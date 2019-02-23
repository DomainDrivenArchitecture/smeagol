(ns smeagol.testing.execution
  (:require [clojure.edn :as edn]
            [nrepl.core :as nrepl]))


(defn- call-with-require [fn-name in]
  (let [ns-sym (some-> fn-name namespace symbol)]
    `(do ~(when ns-sym `(require (quote ~ns-sym))) (apply ~fn-name ~in))))


(defn- do-remote-execute [{:keys [fn-name in] :as params}]
  (let [{::keys [host port timeout]} params]
    (with-open [conn (nrepl/connect :host host :port port)]
      (-> (nrepl/client conn timeout)
          (nrepl/message {:op :eval :code (pr-str (call-with-require fn-name in))})
          nrepl/combine-responses))))


(defmulti execute ::type)


(defmethod execute :remote [params]
  (let [{:keys [value err status]} (do-remote-execute params)]
    (if (status "eval-error")
      {:error err}
      {:result (-> value last (or "") edn/read-string)})))


(defmethod execute :default [{:keys [fn-name in]}]
  (try (let [result (eval (call-with-require fn-name in))]
         {:result result})
       (catch Exception e
         {:error (.getMessage e)})))

