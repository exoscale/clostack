(ns clostack.client
  "A mostly generated wrapper to the cloudstack API."
  (:require [clojure.string   :as s]
            [cheshire.core    :as json]
            [net.http.client  :as http]
            [clostack.config  :as config]
            [clostack.payload :as payload]))

(defn http-client
  "Create an HTTP client"
  [{:keys [config client]}]
  {:config (or config (config/init))
   :client (or client (http/build-client))})

(defn json-response?
  [{:keys [headers] :as resp}]
  (let [ctype (get headers :content-type)]
    (or (.contains ctype "javascript")
        (.contains ctype "json"))))

(defn parse-response
  [resp]
  (if (json-response? resp)
    (update-in resp [:body] json/parse-string true)
    resp))

(defn api-name
  "Given a hyphenated name, yield a camel case one"
  [s]
  (let [[prelude & rest] (s/split (name s) #"-")
        capitalizer      #(if (#{"lb" "ssh" "vpc" "vm"} %)
                            (s/upper-case %)
                            (s/capitalize %))]
    (apply str prelude (map capitalizer rest))))

(defn async-request
  [{:keys [config client]} opcode args handler]
  (let [payload  (payload/build-input config opcode args)
        callback (comp handler parse-response)
        headers  {"Content-Length" (count payload)
                  "Content-Type"   "application/x-www-form-urlencoded"}
        req-map  {:uri            (:endpoint config)
                  :request-method :post
                  :headers        headers
                  :body           payload}]
    (http/request client req-map callback)))

(defn request
  [client opcode args]
  (let [p       (promise)
        handler (fn [response] (deliver p response))
        op      (if (keyword? opcode) (api-name opcode) opcode)]
    (async-request client op args handler)
    (deref p)))
