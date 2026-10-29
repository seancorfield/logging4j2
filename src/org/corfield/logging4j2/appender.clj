;; copyright (c) 2026 Sean Corfield

(ns ^:no-doc org.corfield.logging4j2.appender
  "Internal utilities for creating and managing Log4j2 appenders."
  (:import (org.apache.logging.log4j LogManager)
           (org.apache.logging.log4j.core
            Appender
            LifeCycle
            LifeCycle$State
            LifeCycle2
            LogEvent
            Logger
            LoggerContext)
           (org.apache.logging.log4j.core.config Configuration LoggerConfig)
           (org.apache.logging.log4j.core.filter Filterable)
           (org.apache.logging.log4j.core.impl LocationAware)
           (org.apache.logging.log4j.core.layout PatternLayout)))

(set! *warn-on-reflection* true)

(defrecord EventAppender [appender-name handler layout active]
  Appender ; so LogEvents don't mutate out from under us:
  (append [this event] (handler this (.toImmutable ^LogEvent event)))
  (getHandler [_] nil)
  (getLayout [_] layout)
  (getName [_] appender-name)
  (ignoreExceptions [_] false)
  (setHandler [_ _] nil)
  Filterable
  (getFilter [_] nil)
  (addFilter [_ _] nil)
  (removeFilter [_ _] nil)
  (hasFilter [_] nil)
  (isFiltered [_ _] false)
  LocationAware
  (requiresLocation [_] false)
  LifeCycle
  (getState [_] (if @active LifeCycle$State/STARTED LifeCycle$State/STOPPED))
  (initialize [_] nil) ; no-op
  (isStarted [_] @active)
  (isStopped [_] (not @active))
  (start [_] (vreset! active true))
  (stop [_] (vreset! active false))
  LifeCycle2
  (stop [_ _ _] true))

(defn make-event-appender
  "Create a general LogEvent appender."
  [appender-name handler pattern]
  (let [layout (.. (PatternLayout/newBuilder)
                   (withPattern pattern)
                   (build))]
    (map->EventAppender {:appender-name appender-name
                         :handler handler
                         :layout layout
                         :active (volatile! false)})))

(defn layout-formatted
  "Given an Appender and a LogEvent, return the fully formatted message, using
   the layout from the appender."
  [^Appender appender ^LogEvent event]
  (.toString (.toSerializable (.getLayout appender) event)))

(defn make-message-appender
  "Create a LogEvent appender that handles formatted messages."
  [appender-name handler pattern]
  (make-event-appender appender-name
                       (fn [^Appender appender ^LogEvent event]
                         (let [message (layout-formatted appender event)]
                           (handler message)))
                       pattern))

(defn- find-top-logger
  "Walk up from any logger to the top-most one. This might not be the root one!"
  ^Logger [^Logger logger]
  (if-let [p (.getParent logger)]
    (recur p)
    logger))

(defn- update-loggers
  "Brute force method to add an appender to every logger and config we can find!
   I am indebted to @hiredman for the original idea, at World Singles Networks."
  [^Appender appender ^LoggerContext ctx ^Configuration config]
  (.addAppender ^Logger (LogManager/getRootLogger) appender)
  (doseq [^Logger logger (.getLoggers ctx)]
    (.addAppender logger appender))
  (doseq [^LoggerConfig logger-config (.values (.getLoggers config))]
    (.addAppender logger-config appender nil nil))
  (.addAppender (.getRootLogger config) appender nil nil))

(defn register-appender
  "Given an appender, start it and add it to everything we can find!
   Returns a function that can be used to stop/deactivate the appender."
  [^Appender appender]
  (let [^LoggerContext ctx    (LoggerContext/getContext false)
        ^Configuration config (.getConfiguration ctx)]
    (.start appender)
    (.addAppender config appender)
    (update-loggers appender ctx config)
    (let [top-logger (find-top-logger (LogManager/getLogger (str *ns*)))]
      (.addAppender top-logger appender))
    (.updateLoggers ctx)
    ;; let callers stop the appender if needed:
    (fn [] (.stop appender))))

(defn get-appender
  "Retrieve an appender by name from the current configuration."
  [appender-name]
  (let [^LoggerContext ctx    (LoggerContext/getContext false)
        ^Configuration config (.getConfiguration ctx)]
    (.getAppender config appender-name)))
