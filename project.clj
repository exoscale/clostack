(defproject exoscale/clostack "0.2.17-SNAPSHOT"
  :description "clojure cloudstack client"
  :url "https://github.com/exoscale/clostack"
  :license {:name "MIT License"}
  :plugins [[lein-codox "0.10.5"]
            [lein-ancient "0.6.15"]]
  :global-vars {*warn-on-reflection* true}
  :codox {:source-uri  "https://github.com/exoscale/clostack/blob/{version}/{filepath}#L{line}"
          :output-path "docs"
          :metadata    {:doc/format :markdown}}
  :dependencies [[org.clojure/clojure    "1.9.0"]
                 [cheshire               "5.8.1"]
                 [clj-time               "0.15.1"]
                 [aleph                  "0.4.6"]])
