(ns clostack.client
  "A mostly generated wrapper to the CloudStack API."
  (:require [clojure.string           :as str]
            [cheshire.core            :as json]
            [aleph.http               :as http]
            [byte-streams             :as bs]
            [clostack.config          :as config]
            [clostack.payload         :as payload]))

(defn http-client
  "Create an HTTP client"
  ([]
   http-client {})
  ([{:keys [config pool]}]
   {:config (or config (config/init))
    :opts (if (some? pool)
            {:pool pool}
            {})}))

(defn wrap-body
  "Ensure that response is JSON-formatted, if so parse it"
  [body resp handler]
  (handler (assoc resp :body body)))

(defn api-name
  "Given a hyphenated name, yield a camel case one"
  [s]
  (let [[prelude & rest] (str/split (name s) #"-")
        capitalizer      #(if (#{"lb" "ssh" "vpc" "vm"} %)
                            (str/upper-case %)
                            (str/capitalize %))]
    (apply str prelude (map capitalizer rest))))

(defn async-request
  "Asynchronous request, will execute handler when response comes back."
  ([client opcode handler]
   (async-request client opcode {} handler))
  ([{:keys [config opts]} opcode args handler]
   (let [op          (if (keyword? opcode) (api-name opcode) opcode)
         params      (payload/build-payload config (api-name opcode) args)
         uri         (:endpoint config)
         response    @(http/post uri (-> opts
                                         (assoc :form-params params)))]
     (handler (-> response
                  (select-keys [:status :headers :body])
                  (update :body #(-> %
                                     bs/to-reader
                                     (json/parse-stream true))))))))

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
     (let [resp     (request client op (assoc args :page page :pagesize 500))
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
