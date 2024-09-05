;; copyright (c) 2024 Sean Corfield

(ns ^:no-doc org.corfield.logging4j2.impl
  (:require [clojure.string :as str])
  (:import (java.util Map)
           (org.apache.logging.log4j Level
                                     Logger
                                     Marker
                                     MarkerManager)
           (org.apache.logging.log4j.message MapMessage
                                             Message
                                             ParameterizedMessage
                                             SimpleMessage)))

(set! *warn-on-reflection* true)

(def levels
  {:trace Level/TRACE
   :debug Level/DEBUG
   :info  Level/INFO
   :warn  Level/WARN
   :error Level/ERROR
   :fatal Level/FATAL})

(defn ->str
  "Produce a string from anything. For a keyword, produce the fully-qualified
   name of the keyword."
  [s]
  (cond-> (str s) (keyword? s) (subs 1)))

(defn log*
  "Internal function to log a message at a given level."
  [^Logger logger ^Level level & more]
  (let [[^Marker marker & more]
        (if (instance? Marker (first more))
          more
          (cons nil more))
        [^Throwable throwable & more]
        (if (instance? Throwable (first more))
          next
          (cons nil more))
        [pattern & args :as more] more
        ^Message
        msg (cond (and (string? pattern) (str/includes? pattern "{}"))
                  (ParameterizedMessage. ^String pattern (object-array args))
                  (and (map? pattern) (empty? args))
                  (MapMessage. ^Map (update-keys pattern ->str))
                  :else
                  (SimpleMessage. ^String (apply print-str more)))]
    (cond (and marker throwable)
          (.log logger level marker msg throwable)
          marker
          (.log logger level marker msg)
          throwable
          (.log logger level msg throwable)
          :else
          (.log logger level msg))))

(def ^:dynamic *ctx*
  "So we can more easily carry logging context into threads.
   (with-log-context {} ...) will merge in any dynamically known context from
   outer with-log-context calls."
  {})

(defn stringize-context
  "Given a logging context (that is hopefully a hash map), return a
   hash map with strings for keys and for values. If the context is
   not a hash map, return a hash map with a ctx string key and the
   context value as a string."
  [ctx]
  (if (map? ctx)
    (reduce-kv (fn [m k v]
                 (assoc m
                        (if (keyword? k) (name k) (str k))
                        (when (some? v) (pr-str v))))
               *ctx*
               ctx)
    (assoc *ctx* "ctx" (pr-str ctx))))

(defn as-marker
  "Given a string or keyword and a possibly empty sequence of parents,
   return a Marker object."
  ([s] (as-marker s []))
  ([s parents]
   (let [m (MarkerManager/getMarker
            (-> s (->str) (str/upper-case) (str/replace "-" "_")))]
     (when (seq parents)
       (.setParents m (into-array Marker (map as-marker parents))))
     m)))
