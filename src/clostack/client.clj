(ns clostack.client
  "A mostly generated wrapper to the cloudstack API."
  (:require [clojure.string            :as str]
            [clojure.data.codec.base64 :as base64]
            [clojure.data.json         :as json]
            [http.async.client         :as http])
  (:import java.net.URLEncoder
           javax.crypto.spec.SecretKeySpec
           javax.crypto.Mac
           java.security.MessageDigest))

(defn url-encode
  "Encode URL"
  [s]
  (URLEncoder/encode s "UTF-8"))

(defn quote-plus
  "Replace + in encoded URL by %20"
  [s]
  (str/replace (str s) "+" "%20"))

(defprotocol CloudstackClient
  (get-url [this opcode args] "Yield the URL to query for an opcode and args")
  (async-request [this opcode args] "Send out HTTP query, return deferred")
  (request [this opcode args] "Send out request, return output body"))

(defrecord ArgPair [k v]
  clojure.lang.IFn
  (toString [this]
    (str (str/lower-case (name k)) "=" (url-encode v)))
  java.lang.Comparable
  (compareTo [this other]
    (compare k (:k other))))

(defn get-signature
  "Build a SHA1 Digest of all args to send off to the API"
  [secret args]
  (let [key         (SecretKeySpec. (.getBytes secret) "HmacSHA1")
        mac         (doto (Mac/getInstance "HmacSHA1") (.init key))
        input       (str/join "&" (map (comp quote-plus str/lower-case) args))
        into-string #(String. %)]
    (-> (.doFinal mac (.getBytes input))
        (base64/encode)
        (into-string)
        (str/trim)
        (url-encode))))

(defn add-signature
  "Add computed signature to the list of args"
  [secret args]
  (let [signature (get-signature secret args)
        args      (conj (vec args) (format "signature=%s" signature))]
    (map quote-plus args)))

(defn parse-response
  "Given a complete HTTP object, deserialize"
  [resp]
  (case (quot (:code (http/status resp)) 100)
    2 (json/read-json (http/string resp))
    3 (http/string resp)
    4 (http/string resp)
    5 (http/string resp)))

(defrecord CloudstackHTTPClient [api-key api-secret endpoint http-client]
  CloudstackClient
  (async-request [this opcode args]
    (->> (for [[k v] (assoc args
                       :command opcode
                       :apikey api-key
                       :response :json)
               :let [k (str/lower-case (name k))
                     v (if (keyword? v) (name v) v)]
               :when (and v (if (sequential? v) (not-empty v) true))]
           (cond (and (sequential? v) (-> v first map?))
                 (for [[i submap] (map-indexed vector v)]
                   (for [[subk subv] submap
                         :let [subk (str/lower-case (name subk))
                               subv (name subv)]]
                     (ArgPair. (format "%s[%d].%s" k i subk subv) subv)))
                 (sequential? v)
                 (ArgPair. (name k) (str/join "," (map name v)))
                 :else
                 (ArgPair. (name k) v)))
         (flatten)
         (sort)
         (map str)
         (add-signature api-secret)
         (str/join "&")
         (str endpoint "?")
         (http/GET http-client)))
  (request [this opcode args]
    (-> (async-request this opcode args)
        (http/await)
        (parse-response))))

(defn get-envopt
  "Fetch option either from system properties or environment variables"
  [n]
  (or (System/getProperty n)
      (System/getenv (-> n
                         (str/replace "." "_")
                         str/upper-case))))

(defn http-client
  "Create an HTTP client"
  [& {:keys [api-key api-secret endpoint http]
      :or   {api-key    (get-envopt "cloudstack.api.key")
             api-secret (get-envopt "cloudstack.api.secret")
             endpoint   (get-envopt "cloudstack.endpoint")
             http       (http/create-client)}}]
  (CloudstackHTTPClient. api-key api-secret endpoint http))

(defn api-name
  "Given a hyphenated name, yield a camel case one"
  [s]
  (let [[prelude & rest] (str/split (name s) #"-")
        capitalizer      #(if (#{"lb" "ssh" "vpc" "vm"} %)
                            (str/upper-case %)
                            (str/capitalize %))]
    (apply str prelude (map capitalizer rest))))

(def doc-url "http://cloudstack.apache.org/docs/api/apidocs-4.0.0/user/")

(defmacro defreq
  "Generate both a sync function and an async one"
  {:no-doc true}
  [sym]
  `(do
     (defn ~sym
       ~(format "See %s/%s.html" doc-url (api-name sym))
       [~'client & {:as ~'args}]
        (request ~'client ~(api-name sym) ~'args))
     (defn ~(symbol (str "async-" sym))
       ~(format "See %s/%s.html" doc-url (api-name sym))
       [~'client & {:as ~'args}]
       (async-request ~'client ~(api-name sym) ~'args))))

(defreq create-remote-access-vpn)
(defreq delete-remote-access-vpn)
(defreq list-remote-access-vpns)
(defreq create-vpn-customer-gateway)
(defreq create-vpn-gateway)
(defreq create-vpn-connection)
(defreq delete-vpn-customer-gateway)
(defreq delete-vpn-gateway)
(defreq delete-vpn-connection)
(defreq update-vpn-customer-gateway)
(defreq reset-vpn-connection)
(defreq list-vpn-customer-gateways)
(defreq list-vpn-gateways)
(defreq list-vpn-connections)
(defreq deploy-virtual-machine)
(defreq destroy-virtual-machine)
(defreq reboot-virtual-machine)
(defreq start-virtual-machine)
(defreq stop-virtual-machine)
(defreq reset-password-for-virtual-machine)
(defreq change-service-for-virtual-machine)
(defreq update-virtual-machine)
(defreq list-virtual-machines)
(defreq get-vm-password)
(defreq restore-virtual-machine)
(defreq create-vpc)
(defreq list-vp-cs)
(defreq delete-vpc)
(defreq update-vpc)
(defreq restart-vpc)
(defreq list-vpc-offerings)
(defreq list-private-gateways)
(defreq create-static-route)
(defreq delete-static-route)
(defreq list-static-routes)
(defreq create-load-balancer-rule)
(defreq delete-load-balancer-rule)
(defreq remove-from-load-balancer-rule)
(defreq assign-to-load-balancer-rule)
(defreq create-lb-stickiness-policy)
(defreq delete-lb-stickiness-policy)
(defreq list-load-balancer-rules)
(defreq list-lb-stickiness-policies)
(defreq list-load-balancer-rule-instances)
(defreq update-load-balancer-rule)
(defreq create-project)
(defreq delete-project)
(defreq update-project)
(defreq activate-project)
(defreq suspend-project)
(defreq list-projects)
(defreq list-project-invitations)
(defreq update-project-invitation)
(defreq delete-project-invitation)
(defreq list-network-offerings)
(defreq create-network)
(defreq delete-network)
(defreq list-networks)
(defreq restart-network)
(defreq update-network)
(defreq create-network-acl)
(defreq delete-network-acl)
(defreq list-network-ac-ls)
(defreq attach-iso)
(defreq detach-iso)
(defreq list-isos)
(defreq update-iso)
(defreq delete-iso)
(defreq copy-iso)
(defreq update-iso-permissions)
(defreq list-iso-permissions)
(defreq extract-iso)
(defreq attach-volume)
(defreq upload-volume)
(defreq detach-volume)
(defreq create-volume)
(defreq delete-volume)
(defreq list-volumes)
(defreq extract-volume)
(defreq migrate-volume)
(defreq create-template)
(defreq update-template)
(defreq copy-template)
(defreq delete-template)
(defreq list-templates)
(defreq update-template-permissions)
(defreq list-template-permissions)
(defreq extract-template)
(defreq create-security-group)
(defreq delete-security-group)
(defreq authorize-security-group-ingress)
(defreq revoke-security-group-ingress)
(defreq authorize-security-group-egress)
(defreq revoke-security-group-egress)
(defreq list-security-groups)
(defreq create-snapshot)
(defreq list-snapshots)
(defreq delete-snapshot)
(defreq create-snapshot-policy)
(defreq delete-snapshot-policies)
(defreq list-snapshot-policies)
(defreq list-port-forwarding-rules)
(defreq create-port-forwarding-rule)
(defreq delete-port-forwarding-rule)
(defreq create-firewall-rule)
(defreq delete-firewall-rule)
(defreq list-firewall-rules)
(defreq enable-static-nat)
(defreq create-ip-forwarding-rule)
(defreq delete-ip-forwarding-rule)
(defreq list-ip-forwarding-rules)
(defreq disable-static-nat)
(defreq create-instance-group)
(defreq delete-instance-group)
(defreq update-instance-group)
(defreq list-instance-groups)
(defreq list-accounts)
(defreq add-account-to-project)
(defreq delete-account-from-project)
(defreq list-project-accounts)
(defreq add-vpn-user)
(defreq remove-vpn-user)
(defreq list-vpn-users)
(defreq create-ssh-key-pair)
(defreq delete-ssh-key-pair)
(defreq list-ssh-key-pairs)
(defreq create-tags)
(defreq delete-tags)
(defreq list-tags)
(defreq register-template)
(defreq register-iso)
(defreq register-ssh-key-pair)
(defreq associate-ip-address)
(defreq disassociate-ip-address)
(defreq list-public-ip-addresses)
(defreq list-os-types)
(defreq list-os-categories)
(defreq list-events)
(defreq list-event-types)
(defreq query-async-job-result)
(defreq list-async-jobs)
(defreq list-zones)
(defreq list-service-offerings)
(defreq logout)
(defreq login)
(defreq list-resource-limits)
(defreq list-hypervisors)
(defreq list-disk-offerings)
(defreq list-capabilities)
(defreq get-cloud-identifier)