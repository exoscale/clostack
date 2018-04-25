(defproject spootnik/clostack "0.2.8-SNAPSHOT"
  :description "clojure cloudstack client"
  :url "https://github.com/pyr/clostack"
  :license {:name "MIT License"}
  :plugins [[codox "0.10.3"]]
  :global-vars {*warn-on-reflection* true}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [cheshire            "5.8.0"]
                 [spootnik/net        "0.3.3-beta18"]])
