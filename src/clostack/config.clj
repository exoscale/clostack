(ns clostack.config
  (:require [clojure.string :as s]
            [clojure.edn    :as edn]))

(defn getenv
  "Fetch variable from environment or system properties"
  ([prop]
   (getenv prop nil))
  ([prop default]
   (let [p (name prop)
         e (-> p (s/replace "." "_") (s/upper-case))]
     (or (System/getProperty p)
         (System/getenv e)
         default))))

(defn read-config
  [path]
  (try
    (edn/read-string (slurp path))
    (catch Exception _)))

(defn config-path
  []
  (or (getenv :clostack.config.file)
      (format "%s/.clostack" (System/getenv "HOME"))))

(defn environment-config
  []
  (let [names [:clostack.api.key :clostack.api.secret :clostack.endpoint]
        keys  [:api-key :api-key :endpoint]
        vars  (mapv getenv names)]
    (when-not (some nil? vars)
      (reduce merge {} (partition 2 (interleave keys vars))))))

(defn keywordify
  [m]
  (reduce merge {} (for [[k v] m] [(keyword k) v])))

(defn file-config
  [profile config]
  (let [var-or-profile (get config profile)]
    (when (keyword? var-or-profile)
      (get config var-or-profile))))

(defn init
  []
  (let [path    (config-path)
        config  (keywordify (read-config path))
        profile (keyword (getenv :clostack.profile "default"))]
    (keywordify
     (or (file-config profile config)
         (environment-config)
         (throw (ex-info "Could not find configuration profile for clostack."
                         {:config-path path
                          :config config}))))))
