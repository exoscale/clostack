(defproject spootnik/clostack "0.2.11-SNAPSHOT"
  :description "clojure cloudstack client"
  :url "https://github.com/pyr/clostack"
  :license {:name "MIT License"}
  :plugins [[codox "0.10.3"]
            [lein-ancient "0.6.15"]]
  :global-vars {*warn-on-reflection* true}
  :dependencies [[org.clojure/clojure    "1.9.0"]
                 [cheshire               "5.8.0"]
                 [aleph                  "0.4.6"]])
