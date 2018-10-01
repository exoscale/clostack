(ns clostack.date
  "Date manipulation functions."
  (:require [clj-time.format :refer [parse unparse formatters]]
            [clj-time.core :refer [seconds plus now after?]])
  (:import java.util.Date))

(defn expires-args
  "Builds the expires argument map. Expiration is in seconds.

   A negative expiration meants no expires"
  ([expiration]
   (expires-args expiration (now)))
  ([expiration now]
    (if (>= expiration 0)
        {:signatureVersion "3"
         :expires          (unparse (:date-time formatters)
                                    (plus now (seconds expiration)))}
        {})))

(defn safe-parse
  "Parse a date time string with the given formatter.

  See clj-time.format/parse"
  [formatter s]
  (let [fmt (if (keyword? formatter)
              (formatter formatters)
              formatter)]
    (try
      (parse fmt s)
      (catch IllegalArgumentException _))))

(defn is-expired?
  "If now is after the expiration, then it's expired.

  expiration must be a valid ISO 8061 date and time in UTC format, if the
  timezone is omitted, then UTC is assumed."
  ([expires]
   (is-expired? expires (now)))
  ([expires ^Date now]
    ;; it accepts non timezones values (which cloudstack probably doesn't)
   (let [formats [:date-hour-minute-second
                  :date-hour-minute-second-ms
                  :date-time
                  :date-time-no-ms]
         dt      (first (for [fmt formats
                              :let [date (safe-parse fmt expires)]
                              :when date]
                          date))]
     (if dt
       (after? now dt)
       false))))
