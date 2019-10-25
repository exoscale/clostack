(let [cfg   (clojure.edn/read-string (slurp "deps.edn"))
      deps  (for [[k {:keys [mvn/version exclusions]}] (:deps cfg)]
              [k version :exclusions exclusions])
      paths (:paths cfg)]

  (defproject exoscale/clostack "0.2.18-SNAPSHOT"
    :description "clojure cloudstack client"
    :url "https://github.com/exoscale/clostack"
    :license {:name "MIT License"}
    :plugins [[lein-codox "0.10.7"]
              [lein-ancient "0.6.15"]]
    :global-vars {*warn-on-reflection* true}
    :codox {:source-uri  "https://github.com/exoscale/clostack/blob/{version}/{filepath}#L{line}"
            :output-path "docs"
            :metadata    {:doc/format :markdown}}
    :deploy-repositories [["snapshots" :clojars] ["releases" :clojars]]
    :dependencies ~deps
    :source-paths ~paths))
