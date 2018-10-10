(ns clostack.client
  "A mostly generated wrapper to the CloudStack API."
  (:require [clojure.string           :as str]
            [cheshire.core            :as json]
            [aleph.http               :as http]
            [manifold.deferred        :as d]
            [byte-streams             :as bs]
            [clostack.config          :as config]
            [clostack.payload         :as payload]))

(defn http-client
  "Create an HTTP client. Takes a map of two
   optional keys, if no configuration is present,
   it is picked up from the environment:

     - :config a map of the following optional:
       - :endpoint HTTP endpoint for the API
       - :api-key
       - :api-secret
       - :request-method (:get or :post, default to :post)
       - :page-size number of entities to fetch per page (500 per default)
     - :opts: an opt map handed out to aleph's http client
     "
  ([]
   (http-client {}))
  ([{:keys [config opts] :or {opts {}}}]
   {:config (or config (config/init))
    :opts opts}))

(defn wrap-body
  "Ensure that response is JSON-formatted, if so parse it"
  [body resp handler]
  (handler (assoc resp :body body)))

(defn api-name
  "Given a hyphenated name, yield a camel case one"
  [op]
  (cond
    (keyword? op)
    (let [[prelude & rest] (str/split (name op) #"-")
          capitalizer      #(if (#{"lb" "ssh" "vpc" "vm"} %)
                              (str/upper-case %)
                              (str/capitalize %))]
      (apply str prelude (map capitalizer rest)))

    (string? op)
    op

    :else
    (throw (IllegalArgumentException. "cannot coerce to opcode"))))

(defn http-get
  [uri opts params]
  (http/get uri (assoc opts :query-params params)))

(defn http-post
  [uri opts params]
  (http/post uri (assoc opts :form-params params)))

(def request-fns
  {:get  http-get
   :post http-post})

(defn request-fn
  [config]
  (get request-fns
       (some-> config :request-method name str/lower-case keyword)
       http-post))

(defn parse-body
  [response]
  (let [parse-json-body #(-> % bs/to-reader (json/parse-stream true))]
    (update response :body parse-json-body)))

(defn async-request
  "Asynchronous request, will execute handler when response comes back."
  ([client opcode handler]
   (async-request client opcode {} handler))
  ([{:keys [config opts]} opcode args handler]
   (let [params       (payload/build-payload config (api-name opcode) args)
         sanitize     #(select-keys % [:status :headers :body])
         send-request (request-fn config)]
     (-> (send-request (:endpoint config) opts params)
         (d/chain sanitize parse-body handler)
         (d/catch handler)))))

(defn request
  "Perform a synchronous HTTP request against the API"
  ([client opcode]
   (request client opcode {}))
  ([client opcode args]
   (let [p       (promise)
         handler (fn [response] (deliver p response))]
     (async-request client opcode args handler)
     (deref p))))

(defmacro with-response
  "Perform an asynchronous response, using body as the function body
   to execute."
  [[sym client opcode args] & body]
  `(async-request
    ~client
    ~opcode
    ~(or args {})
    (fn [~sym] ~@body)))

(defn paging-request
  "Perform a paging request. Elements are fetched by chunks of 500."
  ([client op]
   (paging-request client op {} 1 nil))
  ([client op args]
   (paging-request client op args 1 nil))
  ([client op args page width]
   (when (or (nil? width) (pos? width))
     (let [pagesize (get-in client [:config :page-size] 500)
           resp     (request client op (assoc args :page page :pagesize (int pagesize)))
           desc     (->> resp :body (map val) (filter map?) first)
           width    (or width (:count desc) 0)
           elems    (->> desc (map val) (filter vector?) first)
           pending  (- width (count elems))]
       (when (seq elems)
         (lazy-cat elems (paging-request client op args (inc page) pending)))))))

(defn polling-request
  "Perform a polling request, in a blocking fashion. Fetches are done every second."
  [client jobid]
  (let [resp (request client :query-async-job-result {:jobid jobid})
        jobresult (get-in resp [:body :queryasyncjobresultresponse])
        jobstatus (:jobstatus jobresult)
        result    (:jobresult jobresult)]
    (case (int jobstatus)
      0 (do (Thread/sleep 1000)
            (polling-request client jobid))
      1 jobresult
      (throw (ex-info (str "job " jobid " failed")
                      {:jobresult jobresult})))))
