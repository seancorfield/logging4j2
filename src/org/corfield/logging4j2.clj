;; copyright (c) 2024 Sean Corfield

(ns org.corfield.logging4j2
  (:require [org.corfield.logging4j2.impl :as impl])
  (:import (org.apache.logging.log4j
            CloseableThreadContext
            Level
            LogManager
            Logger
            Marker)))

(set! *warn-on-reflection* true)

(defmacro log
  "Write a message to the log."
  [level & args]
  `(let [^Logger logger# (LogManager/getLogger (str ~*ns*))
         ^Level  level#  (get impl/levels ~level Level/ERROR)]
     (when (.isEnabled logger# level#)
       (impl/log* logger# level# ~@args))))

(defmacro trace "Write a trace message to the log." [& args] `(log :trace ~@args))
(defmacro debug "Write a debug message to the log." [& args] `(log :debug ~@args))
(defmacro info  "Write an info message to the log." [& args] `(log :info  ~@args))
(defmacro warn  "Write a warning message to the log." [& args] `(log :warn  ~@args))
(defmacro error "Write an error message to the log." [& args] `(log :error ~@args))
(defmacro fatal "Write a fatal message to the log." [& args] `(log :fatal ~@args))

(defn as-marker
  "Given a string or keyword, return a Marker object.
   If multiple strings or keywords are given, use the first one as the name
   of the marker and the rest as the names of parents."
  ^Marker [s & parents]
  (impl/as-marker s parents))

(defn as-message
  "Given one or more arguments, return a Message object.

   If message is a string and contains '{}', return a ParameterizedMessage
   with any remaining arguments as parameters.

   If message is a map, return a MapMessage.

   Otherwise, return a SimpleMessage with all arguments stringified."
  ^org.apache.logging.log4j.message.Message
  [message & args]
  (impl/as-message message args))

(defmacro with-log-context
  "Given a hash map and a code body, add the hash map to the log4j2 mapped
   diagnostic context, and execute the body.

   The logging context is accumulated dynamically."
  [ctx & body]
  `(let [ctx# (impl/stringize-context ~ctx)]
     (with-open [_# (CloseableThreadContext/putAll ctx#)]
       (binding [impl/*ctx* (merge impl/*ctx* ctx#)]
         ~@body))))

(defmacro with-log-tag
  "Given a keyword or string and a code body, push the tag onto the log4j2
   stack context, and execute the body."
  [tag & body]
  `(let [tag# ~tag]
     (with-open [_# (CloseableThreadContext/push (impl/->str tag#))]
       (binding [impl/*stk* (conj impl/*stk* tag#)]
         ~@body))))

(defmacro with-log-inherited
  "Given a code body, inherit the MDC dynamically (from the parent thread),
   and execute the body."
  [& body]
  `(with-open [ctx# (CloseableThreadContext/putAll impl/*ctx*)]
     ;; .pushAll hangs for some reason, so push each tag individually:
     (doseq [tag# impl/*stk*]
       (.push ctx# (impl/->str tag#)))
     ~@body))

(defmacro with-log-uuid
  "Given a code body, push a unique tag onto the log4j2 stack context, and
   execute the body."
  [& body]
  `(with-log-tag (str (random-uuid))
     ~@body))
