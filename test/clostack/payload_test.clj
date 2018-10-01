(ns clostack.payload-test
  (:require [clostack.payload :refer [add-expires sign]]
            [clojure.test :refer :all]))

(def API_KEY "key")
(def API_SECRET "secret")

(deftest sign-case-sensitivity
  (testing "sha1 signature case sensitivity")
  (is (not= (sign {"a" "a" "b" "B"} API_SECRET)
            (sign {"a" "a" "B" "B"} API_SECRET)))
  (is (not= (sign {:a "a" :b "B"} API_SECRET)
            (sign {:a "a" :B "B"} API_SECRET)))
  (is (not= (sign {:a "a" :b [{:c 1 :d true}]} API_SECRET)
            (sign {:a "a" :b [{:c 1 :D true}]} API_SECRET))))

(deftest signature-v3
  (testing "signature v3")
  (is (= (:signatureVersion (add-expires {} 0))
         "3"))
  (is (:expires (add-expires {} 0)))
  (is (not (:expires (add-expires {} -1))))
  )
