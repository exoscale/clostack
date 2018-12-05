(ns clostack.signature-test
  (:require [clostack.signature :refer [sha1-signature]]
            [clojure.test :refer :all]))

(deftest sha1-signature-nils
  (testing "sha1 signature without a secret"
    (is (thrown? AssertionError
                 (sha1-signature nil "input"))))
  (testing "sha1 signature without an input"
    (is (nil? (sha1-signature "secret" nil)))))



(deftest sha1-signature-input
  (testing "string and byte inputs give the same sig"
    (let [secret "secret"
          input "input"
          sig0 "MEQPNt3CgJu9TIsfN6boDXWIwwM="
          sig1 (sha1-signature secret input)
          sig2 (sha1-signature secret (.getBytes input))]
      (is (= sig0 sig1 sig2))))

  (testing "other input type fails"
    (is (thrown? Exception
                 (sha1-signature "secret" 42)))))
