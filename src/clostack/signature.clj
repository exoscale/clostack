(ns clostack.signature
  "HMAC-SHA1 signing functions"
  (:import javax.crypto.spec.SecretKeySpec
           javax.crypto.Mac
           java.util.Base64))


(def byte-array?
  (partial instance? (Class/forName "[B")))


(defn to-bytes
  [input]
  (cond

    (string? input)
    (.getBytes ^String input)

    (byte-array? input)
    input

    (some? input)
    (throw (Exception. (format "Wrong input: %s" input)))))


(defn sha1-signature
  "Given a secret, compute the base64 encoded representation of a
   payload's Hmac-Sha1"
  [^String secret input]
  {:pre [(seq secret)]}

  (let [key   (SecretKeySpec. (.getBytes secret) "HmacSHA1")
        mac   (doto (Mac/getInstance "HmacSHA1") (.init key))
        bytes (to-bytes input)]

    (some->> bytes
             (.doFinal mac)
             (.encodeToString (Base64/getEncoder)))))
