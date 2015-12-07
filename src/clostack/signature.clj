(ns clostack.signature
  "HMAC-SHA1 signing functions"
  (:require [clojure.string            :as s]
            [clojure.data.codec.base64 :as b64]
            [clostack.utils            :refer [url-encode]])
  (:import javax.crypto.spec.SecretKeySpec
           javax.crypto.Mac))

(defn to-base64
  "URL-encode a byte-array's base64 string representation"
  [bytes]
  (s/trim (String. (b64/encode bytes))))

(defn sha1-signature
  "Given a secret, compute the base64 encoded representation of a
   payload's Hmac-Sha1"
  [secret input]
  (let [key  (SecretKeySpec. (.getBytes secret) "HmacSHA1")
        mac  (doto (Mac/getInstance "HmacSHA1") (.init key))]
    (to-base64 (.doFinal mac (.getBytes input)))))
