(ns clostack.payload-test
  (:require [clostack.payload :refer [sign build-payload]]
            [clojure.test :refer :all]
            [clj-time.core :as t]))

(def API_KEY "key")
(def API_SECRET "secret")

(deftest test-sign-cases
  (testing "sha1 signature case sensitivity"
    (is (not= (sign {"a" "a" "b" "B"} API_SECRET)
              (sign {"a" "a" "B" "B"} API_SECRET)))
    (is (not= (sign {:a "a" :b "B"} API_SECRET)
              (sign {:a "a" :B "B"} API_SECRET)))
    (is (not= (sign {:a "a" :b [{:c 1 :d true}]} API_SECRET)
              (sign {:a "a" :b [{:c 1 :D true}]} API_SECRET))))

  (testing "keys should be uniq"
    (is (thrown-with-msg? Exception #"Keys should not be duplicated"
                          (sign {:a "foo" :A "bar"} API_SECRET)))))


(deftest test-payload
  (with-redefs [t/now (constantly (t/date-time 2019))]
    (let [payload (build-payload
                   {:api-key API_KEY
                    :api-secret API_SECRET
                    :expiration 3600}
                   :someApiCall
                   {:arg1 "test"
                    :arg2 42})]

      (is (= {:arg1 "test"
              :arg2 42
              :command :someApiCall
              :apiKey API_KEY
              :response "json"
              :signatureVersion "3"
              :expires "2019-01-01T01:00:00+0000"
              :signature "8KNWCEMTMhcMm8LG3nhAy4RSlhE="}
             payload)))))
