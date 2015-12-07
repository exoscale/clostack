(ns clostack.utils
  "Common utility functions"
  (:require [clojure.string :as s])
  (:import java.net.URLEncoder))

(defn url-encode
  "Encode URL"
  [s]
  (URLEncoder/encode s "UTF-8"))

(defn quote-plus
  "Replace + in encoded URL by %20"
  [s]
  (s/replace (str s) "+" "%20"))
