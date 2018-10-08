(ns clostack.date-test
  (:require [clostack.date :refer [is-expired? expires-args]]
            [clj-time.core :refer [date-time]]
            [clojure.test :refer :all]))

(deftest is-expired
  (testing "no timezone"
    (is (is-expired? "2018-01-01T12:12:12" (date-time 2018 1 1 12 12 13)))
    (is (not (is-expired? "2018-01-02T12:12:12" (date-time 2018 1 2 12 12 11)))))
  (testing "no timezone with milliseconds"
    (is (is-expired? "2018-01-03T12:12:12.000" (date-time 2018 1 3 12 12 13)))
    (is (not (is-expired? "2018-01-04T12:12:12.000" (date-time 2018 1 4 12 12 11)))))
  (testing "utc timezone"
    (is (is-expired? "2018-01-05T12:12:12Z" (date-time 2018 1 5 12 12 13)))
    (is (not (is-expired? "2018-01-06T12:12:12Z" (date-time 2018 1 6 12 12 11)))))
  (testing "utc timezone and milliseconds"
    (is (is-expired? "2018-01-07T12:12:12.000Z" (date-time 2018 1 7 12 12 13)))
    (is (not (is-expired? "2018-01-08T12:12:12.000Z" (date-time 2018 1 8 12 12 11)))))
  (testing "other timezone"
    (is (is-expired? "2018-01-09T12:12:12+02:00" (date-time 2018 1 9 12 12 12)))
    (is (not (is-expired? "2018-01-10T12:12:12-02:00" (date-time 2018 1 10 12 12 12)))))
  (testing "other timezone and milliseconds"
    (is (is-expired? "2018-01-11T12:12:12.000+02:00" (date-time 2018 1 11 12 12 12)))
    (is (not (is-expired? "2018-01-12T12:12:12.000-02:00" (date-time 2018 1 12 12 12 12))))))

(deftest signature-v3
  (testing "expires and signatureVersion args"
           (is (:expires (expires-args 0)))
           (is (= (:signatureVersion (expires-args 0))
                  "3")))
  (testing "negative expiration time produces nothing"
           (is (not (:expires (expires-args -1))))))
