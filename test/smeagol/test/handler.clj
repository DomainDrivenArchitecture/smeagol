(ns smeagol.test.handler
  (:require [clojure.test :refer :all]
            [integrant.core :as ig]
            [ring.mock.request :refer :all]
            [smeagol.handler :refer :all]))


(def test-system
  "{:smeagol/resolver {:type :test-mock
                      :config #ig/ref :smeagol/configuration}

   :smeagol/configuration {:content-dir \"resources\"}

   :smeagol/wiki {:resolver #ig/ref :smeagol/resolver
                  :config #ig/ref :smeagol/configuration}}")

(def test-app (-> test-system ig/read-string ig/init app))

(deftest test-app-ok
  (testing "main route"
    (let [response (test-app (request :get "/" {:accept-language "en-GB"}))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response (test-app (request :get "/invalid" {:accept-language "en-GB"}))]
      (is (= 404 (:status response))))))
