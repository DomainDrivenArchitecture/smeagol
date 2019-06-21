(ns smeagol.test.handler
  (:require [clojure.test :refer :all]
            [integrant.core :as ig]
            [ring.mock.request :refer :all]
            [smeagol.handler :refer :all]))


#_(def test-system
  "{:smeagol/resolver {:type :test-mock
                      :config #ig/ref :smeagol/configuration}

   :smeagol/configuration {:content-dir \"resources\"}

   :smeagol/web {:wiki #ig/ref :smeagol/wiki
               :config #ig/ref :smeagol/configuration}

   :smeagol/wiki {:resolver #ig/ref :smeagol/resolver
                  :config #ig/ref :smeagol/configuration}}")

#_(def test-app (-> test-system ig/read-string ig/init :smeagol/web :app))

#_(deftest test-app-ok
  (testing "main route"
    (let [response (test-app (request :get "/edit?page=Introduction" {:accept-language "en-GB"}))]
      (is (= 302 (:status response)))
      (is (re-find #"/auth$" (-> response :headers (get "Location"))))))

  (testing "not-found route"
    (let [response (test-app (request :get "/invalid" {:accept-language "en-GB"}))]
      (is (= 404 (:status response))))))
