;; copyright (c) 2024 Sean Corfield

(ns org.corfield.logging4j2
  (:import (org.apache.logging.log4j CloseableThreadContext
                                     Level Logger LogManager
                                     Marker MarkerManager)))

(set! *warn-on-reflection* true)

(def ^:private levels
  {:trace Level/TRACE
   :debug Level/DEBUG
   :info  Level/INFO
   :warn  Level/WARN
   :error Level/ERROR
   :fatal Level/FATAL})

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
        msg (apply print-str more)]
    (cond (and marker throwable)
          (.log logger level marker msg throwable)
          marker
          (.log logger level marker msg)
          throwable
          (.log logger level msg throwable)
          :else
          (.log logger level msg))))

(defmacro log
  "Write a message to the log."
  [level & args]
  `(let [^Logger logger# (LogManager/getLogger (str *ns*))
         ^Level  level#  (get levels ~level Level/ERROR)]
     (when (.isEnabled logger# level#)
       (log* logger# level# ~@args))))

(defmacro trace "Write a trace message to the log." [& args] `(log :trace ~@args))
(defmacro debug "Write a debug message to the log." [& args] `(log :debug ~@args))
(defmacro info  "Write an info message to the log." [& args] `(log :info  ~@args))
(defmacro warn  "Write a warning message to the log." [& args] `(log :warn  ~@args))
(defmacro error "Write an error message to the log." [& args] `(log :error ~@args))
(defmacro fatal "Write a fatal message to the log." [& args] `(log :fatal ~@args))

(defn- ->str [s] (cond-> (str s) (keyword? s) (subs 1)))

(defn as-marker
  "Given a string or keyword, return a Marker object.
   If multiple strings or keywords are given, use the first one as the name
   of the marker and the rest as the names of parents."
  ^Marker [s & parents]
  (let [m (MarkerManager/getMarker (->str s))]
    (when (seq parents)
      (.setParents m (into-array Marker (map as-marker parents))))
    m))

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

(defmacro with-log-context
  "Given a hash map and a code body, add the hash map to the log4j2 mapped
   diagnostic context, and execute the body.

   The logging context is accumulated dynamically."
  [ctx & body]
  `(let [ctx# (stringize-context ~ctx)]
     (with-open [_# (CloseableThreadContext/putAll ctx#)]
       (binding [*ctx* ctx#]
         ~@body))))

(defmacro with-log-tag
  "Given a keyword or string and a code body, push the tag onto the log4j2
   stack context, and execute the body."
  [tag & body]
  `(let [tag# ~tag]
     (with-open [_# (CloseableThreadContext/push (->str tag#))]
       ~@body)))

(defmacro with-log-uuid
  "Given a code body, push a unique tag onto the log4j2 stack context, and
   execute the body."
  [& body]
  `(with-log-tag (str (random-uuid))
     ~@body))


(comment
  (log :info "Hello, World!")
  (with-log-tag :hello/world
    (with-log-context {:uid 1234}
      (info "Hello, World!")))
  (with-log-tag :outer/tag
    (with-log-tag :inner/tag
      (info "Hello, World!")))
  (log :info (as-marker :hello/world) "Hello, World!")
  (fatal "Hello, World!")
  )
