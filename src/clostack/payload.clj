(ns clostack.payload
  "Functions to work with appropriate cloudstack payloads."
  (:require [clj-time.format    :as f]
            [clj-time.core      :as t]
            [clojure.string     :as s]
            [clostack.date      :refer [expires-args]]
            [clostack.signature :as sig]
            [clostack.utils     :refer [url-encode quote-plus]]))

(def default-expiration 600)

(defn serialize-pair
  "Encode a key/value pair"
  [[k v]]
  (str (name k) "=" (url-encode v)))

(defn transform-map
  "Encode a map argument appropriately"
  [top-k [i m]]
  (for [[k v] m]
    [(format "%s[%d].%s" (name top-k) i (name k)) (str v)]))

(defn transform-maps
  "For a list of maps, produce the expected key/value pairs."
  [k maps]
  (vec (mapcat (partial transform-map k) (map-indexed vector maps))))

(defn transform-arg
  "transform argument into a list of key/value pairs."
  [[k v]]
  (let [k (name k)
        v (if (keyword? v) (name v) v)]
    (when (and v (if (sequential? v) (not-empty v) true))
      (cond
        (and (sequential? v) (-> v first map?))
        (transform-maps k v)

        (sequential? v)
        [[k (s/join "," (map name v))]]

        :else
        [[k (str v)]]))))

(defn duplicated-keys [m]
  (let [duplicated (->> (group-by (comp clojure.string/lower-case key) m)
                        (filter (comp (partial < 1)count second))
                        (map first))]
    (seq duplicated)))

(defn transform-args
  "Transform arguments into a vector of key/value pairs."
  [args]
  (when-let [dups (duplicated-keys args)]
    (throw (ex-info "Keys should not be duplicated" {:duplicated dups})))
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

(defn sign
  "Sign the given query"
  [query api-secret]
  (->> query
       build-args
       signable-args
       (sig/sha1-signature api-secret)))

(defn build-payload
  "Build a signed payload for a given config, opcode and args triplet"
  ([config opcode args]
    (build-payload config (assoc args :command opcode)))
  ([{:keys [api-key api-secret expiration]} args]
    (let [exp-s      (try (int expiration)
                          (catch Exception _ default-expiration))
          exp-args   (expires-args exp-s)
          args       (-> args
                         (assoc :apikey api-key :response "json")
                         (merge exp-args))
          signature  (sign args api-secret)]
      (assoc args :signature signature))))
