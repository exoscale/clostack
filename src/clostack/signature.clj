(ns clostack.signature
  "HMAC-SHA1 signing functions"
  (:require [clojure.string            :as s]
            [clostack.utils            :refer [url-encode]])
  (:import javax.crypto.spec.SecretKeySpec
           javax.crypto.Mac
           java.util.Base64
           javax.xml.bind.DatatypeConverter))

(defn sha1-signature
  "Given a secret, compute the base64 encoded representation of a
   payload's Hmac-Sha1"
  [^String secret ^String input]
  {:pre [(seq secret)]}
  (let [key  (SecretKeySpec. (.getBytes secret) "HmacSHA1")
        mac  (doto (Mac/getInstance "HmacSHA1") (.init key))]
    (some->> input
             .getBytes
             (.doFinal mac)
             (.encodeToString (Base64/getEncoder)))))
