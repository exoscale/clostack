(ns clostack.signature
  "HMAC-SHA1 signing functions"
  (:require [clojure.string :as s]
            [net.codec.b64  :as b64]
            [clostack.utils :refer [url-encode]])
  (:import javax.crypto.spec.SecretKeySpec
           javax.crypto.Mac
           javax.xml.bind.DatatypeConverter))

(defn sha1-signature
  "Given a secret, compute the base64 encoded representation of a
   payload's Hmac-Sha1"
  [^String secret ^String input]
  (let [key  (SecretKeySpec. (.getBytes secret) "HmacSHA1")
        mac  (doto (Mac/getInstance "HmacSHA1") (.init key))]
    (->> input .getBytes (.doFinal mac) (b64/b->b64))))
