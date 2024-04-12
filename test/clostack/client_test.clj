(ns clostack.client-test
  (:require
   [cheshire.core :as json]
   [clojure.test :refer [deftest is testing]]
   [clostack.client :as sut]))

(deftest prepare-error-fn-test
  (testing "exception from server with JSON response"
    (let [json-payload "{\"message\":\"Sorry, we are broken\"}"
          f (sut/prepare-error-fn identity)
          ex (ex-info "This is an error from the server"
                      {:status 400
                       :headers []
                       :body json-payload})
          {:keys [status body headers exception]} (f ex)]
      (is (= 400 status))
      (is (= [] headers))
      (is (= (json/parse-string json-payload true) body))
      (is (= ex exception))))

  (testing "exception from server with not a JSON response"
    (let [not-json-payload "<html><body>we are deeply broken</body></html>"
          f (sut/prepare-error-fn identity)
          ex (ex-info "This is an error from the server"
                      {:status 400
                       :headers []
                       :body not-json-payload})
          {:keys [status body headers exception]} (f ex)]
      (is (= 400 status))
      (is (= [] headers))
      (is (= not-json-payload (:original-body body)))
      (is (not-empty (:parser-exception-message body)))
      (is (= ex exception)))))
