;; copyright (c) 2024-2026 Sean Corfield

(ns org.corfield.logging4j2
  (:require [org.corfield.logging4j2.appender :as appender]
            [org.corfield.logging4j2.impl :as impl])
  (:import (org.apache.logging.log4j
            CloseableThreadContext
            Level
            LogManager
            Logger
            Marker)
           (org.apache.logging.log4j.core Appender)))

(set! *warn-on-reflection* true)

(defmacro log
  "Write a message to the log."
  [level & args]
  `(let [^Logger logger# (LogManager/getLogger (str ~*ns*))
         ^Level  level#  (get impl/levels ~level Level/ERROR)]
     (when (.isEnabled logger# level#)
       (impl/log* logger# level# ~@args))))

(defmacro trace "Write a trace message to the log."   [& args] `(log :trace ~@args))
(defmacro debug "Write a debug message to the log."   [& args] `(log :debug ~@args))
(defmacro info  "Write an info message to the log."   [& args] `(log :info  ~@args))
(defmacro warn  "Write a warning message to the log." [& args] `(log :warn  ~@args))
(defmacro error "Write an error message to the log."  [& args] `(log :error ~@args))
(defmacro fatal "Write a fatal message to the log."   [& args] `(log :fatal ~@args))

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

(defn layout-formatted
  "Given an `Appender` and a `LogEvent`, return the fully formatted message, 
   using the layout from the appender."
  [appender event]
  (appender/layout-formatted appender event))

(defn add-event-appender
  "Given an appender name, a function that will accept two arguments, and an 
   optional layout pattern, create a log4j2 `Appender`, start it up, and 
   register it with the logging system.

   The function will be called with the `Appender` and an immutable `LogEvent`.

   The layout pattern %m%n will be used if none is provided, producing a plain
   message with a trailing newline, suitable for writing to a file.

   Returns a no-arg function that can be called to stop/deactivate the appender."
  ([appender-name handler] (add-event-appender appender-name handler "%m%n"))
  ([appender-name handler pattern]
   (appender/register-appender (appender/make-event-appender appender-name handler pattern))))

(defn add-message-appender
  "Given an appender name, a function that will accept a `String`, and an
   optional layout pattern, create a log4j2 `Appender`, start it up, and 
   register it with the logging system.

   The string passed to the handler function will be the formatted message from 
   the `LogEvent`, using the layout pattern from the appender.

   If no layout pattern is provided, the default pattern %m will be used, so
   there will be no trailing newline.

   Returns a no-arg function that can be called to stop/deactivate the appender."
  ([appender-name handler] (add-message-appender appender-name handler "%m"))
  ([appender-name handler pattern]
   (appender/register-appender
    (appender/make-message-appender appender-name handler pattern))))

(defn get-appender
  "Retrieve an appender by name from the current configuration."
  ^Appender [appender-name]
  (appender/get-appender appender-name))
