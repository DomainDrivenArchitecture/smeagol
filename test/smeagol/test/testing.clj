(ns smeagol.test.testing
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [nrepl.server :refer [start-server]]
            [smeagol.testing :refer [parse whitelist-namespace do-test]]))

(def config (-> "public/content/example.edn" io/resource slurp edn/read-string))
(def text "fn-name\n{:in [#include \"example.edn\"]}")

#_(deftest test-read-with-aero
  (is (=
       {:fn-name 'fn-name
        :in [config]
        :out nil
        :text text}
       (parse text))))

(defn eval-2 [x] (println x) (str x))
(defmacro with-nrepl [port & body]
  `(with-open [s# (start-server :port ~port)]
     ~@body))


#_(deftest test-inalid-input
  (are [match input] (re-find match (-> input parse :error))
    #"Failed parsing line.*Config error on line" "smeagol.sample/pow\r\n{"))


#_(deftest test-whitelisting-input
  (are [match input] (re-find match (-> input parse whitelist-namespace :error))
    #"Namespace: nil is not listed" "wtf\r\n1\r\n2"
    #"Namespace: \"wtf\" is not listed" "wtf/xyz\r\n1\r\n2"))


(def port 7878)
(def failure " smeagol.sample/pow\r\n{:in [4] :out 15}\r\n")
(def success "smeagol.sample/pow\r\n{:in [4] :out 16}\r\n")
(def local-ns-config #:smeagol.testing.execution{:type :local})
(def remote-ns-config #:smeagol.testing.execution{:port port :type :remote})

#_(deftest test-local-executon
  (are [result input] (= result (-> input parse (merge local-ns-config) do-test))
    {:result :failure, :expected 15, :actual 16} failure
    {:result :ok} success))

#_(deftest test-remote-executon
  (with-nrepl port
    (are [result input] (= result (-> input parse (merge remote-ns-config) do-test))
      {:result :failure, :expected 15, :actual 16} failure
      {:result :ok} success)))

#_(deftest test-remote-connection-refused
  (are [result input] (= result (-> input parse (merge remote-ns-config) do-test))
    {:result :error, :error "Connection refused"} " smeagol.sample/pow\r\n{:in [4] :out 16}\r\n"))
