(ns clostack.payload
  "Functions to work with appropriate cloudstack payloads."
  (:require [clojure.string     :as s]
            [clostack.signature :as sig]
            [clostack.utils     :refer [url-encode quote-plus]]))

(defn serialize-pair
  "Encode a key/value pair"
  [[k v]]
  (str (s/lower-case (name k)) "=" (url-encode v)))

(defn transform-map
  "Encode a map argument appropriately"
  [top-k [i m]]
  (for [[k v] m]
    [(format "%s[%d].%s" (name top-k) i (s/lower-case (name k))) (str v)]))

(defn transform-maps
  "For a list of maps, produce the expected key/value pairs."
  [k maps]
  (vec (mapcat (partial transform-map k) (map-indexed vector maps))))

(defn transform-arg
  "transform argument into a list of key/value pairs."
  [[k v]]
  (let [k (s/lower-case (name k))
        v (if (keyword? v) (name v) v)]
    (when (and v (if (sequential? v) (not-empty v) true))
      (cond
        (and (sequential? v) (-> v first map?))
        (transform-maps k v)

        (sequential? v)
        [[k (s/join "," (map name v))]]

        :else
        [[k (str v)]]))))

(defn transform-args
  "Transform arguments into a vector of key/value pairs."
  [args]
  (vec (mapcat transform-arg (filter (complement nil?) args))))

(defn build-args
  "Build arguments, ready to be signed."
  [args]
  (->> args
       transform-args
       (sort-by first)
       (map serialize-pair)
       (s/join "&")))

(defn signable-args
  "Transform a string to its expected signable form"
  [path]
  (quote-plus (s/lower-case path)))

(defn build-payload
  "Build a signed payload for a given config, opcode and args triplet"
  [config opcode args]
  (let [{:keys [endpoint api-key api-secret]} config]
    (let [args      (assoc args :apikey api-key :command opcode :response :json)
          query     (build-args args)
          signature (->> query
                         signable-args
                         (sig/sha1-signature api-secret))]
      (concat (->> args
                   transform-args
                   (sort-by first))
              [[:signature signature]]))))
