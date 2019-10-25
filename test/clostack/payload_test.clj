(ns clostack.payload-test
  (:require [clostack.payload :refer [sign]]
            [clojure.test :refer :all]))

(def API_KEY "key")
(def API_SECRET "secret")

(deftest sign-cases
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
