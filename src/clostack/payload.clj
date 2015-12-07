(ns clostack.payload
  (:require [clojure.string     :as s]
            [clostack.signature :as sig]
            [clostack.utils     :refer [url-encode quote-plus]]))

(defn serialize-pair
  [[k v]]
  (str (s/lower-case (name k)) "=" (url-encode v)))

(defn transform-map
  [top-k [i m]]
  (for [[k v] m]
    [(format "%s[%d].%s" (name top-k) i (s/lower-case (name k))) (str v)]))

(defn transform-maps
  [k maps]
  (vec (mapcat (partial transform-map k) (map-indexed vector maps))))

(defn unroll-arg
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

(defn unroll
  [args]
  (vec (mapcat unroll-arg (filter (complement nil?) args))))

(defn build-path
  "Build a path, ready to be signed."
  [api-key opcode args]
  (->> (assoc args :apikey api-key :command opcode :response :json)
       (unroll)
       (sort-by first)
       (map serialize-pair)
       (s/join "&")))

(defn signable-path
  "Transform a string to its expected signable form"
  [path]
  (quote-plus (s/lower-case path)))

(defn build-uri
  "Build a valid Cloustack URL for a given config, opcode and args triplet"
  [config opcode args]
  (let [{:keys [endpoint api-key api-secret]} config]
    (let [path      (build-path api-key opcode args)
          signature (sig/sha1-signature api-secret (signable-path path))]
      (format "%s?%s&signature=%s" endpoint path (url-encode signature)))))
