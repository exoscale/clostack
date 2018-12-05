(ns clostack.signature
  "HMAC-SHA1 signing functions"
  (:require [byte-streams :as bs])
  (:import javax.crypto.spec.SecretKeySpec
           javax.crypto.Mac
           java.util.Base64))


(defn sha1-signature
  "Given a secret, compute the base64 encoded representation of a
   payload's Hmac-Sha1"
  [^String secret input]
  {:pre [(seq secret)]}

  (let [key (SecretKeySpec. (.getBytes secret) "HmacSHA1")
        mac (doto (Mac/getInstance "HmacSHA1") (.init key))]

    (some->> input
             bs/to-byte-array
             (.doFinal mac)
             (.encodeToString (Base64/getEncoder)))))
