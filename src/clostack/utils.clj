(ns clostack.utils
  "Common utility functions"
  (:require [clojure.string :as str])
  (:import java.net.URLEncoder))

(defn url-encode
  "Encode URL"
  [s]
  (URLEncoder/encode (str s) "UTF-8"))

(defn quote-plus
  "Replace + in encoded URL by %20"
  [s]
  (str/replace (str s) "+" "%20"))
