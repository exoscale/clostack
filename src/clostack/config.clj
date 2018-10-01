(ns clostack.config
  "This namespace provides a few functions which try very hard
   to find configuration for the library. See the `init` function
   for a description of the logic."
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
  "Try to read configuration from a path. Expects EDN files"
  [path]
  (try
    (edn/read-string (slurp path))
    (catch Exception _)))

(defn config-path
  "Find out where the configuration file lives"
  []
  (or (getenv :clostack.config.file)
      (format "%s/.clostack" (System/getenv "HOME"))))

(defn environment-config
  "Try getting configuration from the environment."
  []
  (let [names [:clostack.api.key :clostack.api.secret :clostack.endpoint :clostack.expiration]
        keys  [:api-key :api-secret :endpoint :expiration]
        vars  (mapv getenv names)]
    (when-not (some nil? vars)
      (reduce merge {} (partition 2 (interleave keys vars))))))

(defn keywordify
  "Single-depth walk of a map, keywordizing keys."
  [m]
  (reduce merge {} (for [[k v] m] [(keyword k) v])))

(defn file-config
  "Get a profile from a config."
  [profile config]
  (let [var-or-profile (get config profile)]
    (when (keyword? var-or-profile)
      (get config var-or-profile))))

(defn init
  "Get configuration. First try the environment, if not found,
   read from a file."
  []
  (let [path    (config-path)
        config  (keywordify (read-config path))
        profile (keyword (getenv :clostack.profile "default"))]
    (keywordify
     (or (environment-config)
         (file-config profile config)
         (throw (ex-info "Could not find configuration profile for clostack."
                         {:config-path path
                          :config config}))))))
