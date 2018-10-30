(ns clostack.signature-test
  (:require [clostack.signature :refer [sha1-signature]]
            [clojure.test :refer :all]))

(deftest sha1-signature-nils
  (testing "sha1 signature without a secret"
    (is (thrown? AssertionError
                 (sha1-signature nil "input"))))
  (testing "sha1 signature without an input"
    (is (nil? (sha1-signature "secret" nil)))))
