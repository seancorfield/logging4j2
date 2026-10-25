;; copyright (c) 2024-2026 Sean Corfield

(ns org.corfield.logging4j2-test
  "These tests require visual inspection of the log output at present."
  (:require [clojure.test :refer [deftest is]]
            [clojure.tools.logging :as ctl]
            [org.corfield.logging4j2 :as sut]
            [clojure.string :as str]))

(set! *warn-on-reflection* true)

(def ^:private log-file
  "This must match the log4j2-test.properties configuration!"
  "/tmp/logging4j2-test.log")

(defmacro log-expect [matches & body]
  `(do
     ;; ensure log is clear before running the test:
     (spit log-file "")
     ;; run the test:
     (do ~@body)
     (let [output# (slurp log-file)]
       (doseq [match# ~matches]
         (if (string? match#)
           (is (str/includes? output# match#))
           (is (re-find match# output#)))))))

(deftest sanity-test
  (log-expect
   [" INFO " "[]" "{}" "//" "Hello, World!"]
   (sut/log :info "Hello, World!"))
  (sut/with-log-tag :hello/world
    (sut/with-log-context {:uid 1234}
      (log-expect
       ["[hello/world]" "{uid=1234}" "//" "Hello, Tag and UID!"]
       (sut/info "Hello, Tag and UID!"))))
  (sut/with-log-tag :outer/tag
    (sut/with-log-tag :inner/tag
      (log-expect
       ["[outer/tag, inner/tag]" "{}" "//" "Hello, Outer/Inner Tag!"]
       (sut/info "Hello, Outer/Inner Tag!"))))
  (sut/with-log-uuid
    (log-expect
     [" FATAL " #"\[[-0-9a-f]+\]" "{}" "//" "Hello, UUID!"]
     (sut/fatal "Hello, UUID!"))))

(deftest supplier-test
  (log-expect
   [" INFO " "[]" "{}" "//" "Hello, Reified Supplier!"]
   (sut/info (reify org.apache.logging.log4j.util.MessageSupplier
               (get [_] (sut/as-message "Hello," "Reified Supplier!")))))
  ;; we only support Clojure 1.11.0 or later;
  ;; implicit suppliers only work for Clojure 1.12.0 or later:
  (if (str/starts-with? (clojure-version) "1.11")
    (log-expect
     [" WARN " "MessageSupplier coercion not supported for Clojure 1.11"]
     (sut/warn "MessageSupplier coercion not supported for Clojure 1.11"))
    (log-expect
     [" INFO " "Hello, Implicit Supplier!" "Hello, Shorthand Supplier!"]
     (do
       (sut/info (fn [] (sut/as-message "Hello," "Implicit Supplier!")))
       (sut/info #(sut/as-message "Hello," "Shorthand Supplier!"))))))

(deftest ctl-test
  (log-expect
   [" INFO " "[]" "{}" "Hello, c.t.l World!"]
   (ctl/log :info "Hello, c.t.l World!"))
  (sut/with-log-tag :hello/world
    (sut/with-log-context {:uid 1234}
      (log-expect
       ["[hello/world]" "{uid=1234}" "//" "Hello, c.t.l Tag and UID!"]
       (ctl/info "Hello, c.t.l Tag and UID!"))))
  (sut/with-log-tag :outer/tag
    (sut/with-log-tag :inner/tag
      (log-expect
       ["[outer/tag, inner/tag]" "{}" "//" "Hello, c.t.l Outer/Inner Tag!"]
       (ctl/info "Hello, c.t.l Outer/Inner Tag!"))))
  (sut/with-log-uuid
    (log-expect
     ;; default clojure.tools.logging implementation is slf4j, which doesn't
     ;; really have a FATAL level, so it produces ERROR :(
     [" ERROR " #"\[[-0-9a-f]+\]" "{}" "//" "Hello, c.t.l UUID!"]
     (ctl/fatal "Hello, c.t.l UUID!"))))

(deftest marker-test
  (log-expect
   [" INFO " "[]" "{}" "/HELLO/WORLD/" "Hello, Marker!"]
   (sut/log :info (sut/as-marker :hello/world) "Hello, Marker!"))
  (log-expect
   [" WARN " "[]" "{}" "/CHILD[ PARENT ]/" "Hello, Marker with Parent!"]
   (sut/log :warn (sut/as-marker :child :parent) "Hello, Marker with Parent!")))

(deftest formatting-test
  (log-expect
   [" INFO " "[]" "{}" "//" "Hello, Parameter!"]
   (sut/log :info "Hello, {}!" "Parameter"))
  (log-expect
   ["Hello, Two Parameters!"]
   (sut/log :info "Hello, {} {}!" "Two" "Parameters"))
  (log-expect
   ["Hello, Three Parameters Provided!"]
   (sut/log :info "Hello, {} {} {}!" "Three" "Parameters" "Provided")))

(deftest map-test
  (log-expect
   [" WARN " "[]" "{}" "//" "13=\"42\"" "hello=\"MapMessage\""]
   (sut/log :warn {:hello "MapMessage" 13 42}))
  (log-expect
   ["hello/world=\":how.are/you?\""]
   (sut/log :warn {:hello/world :how.are/you?})))

(deftest future-test
  (let [p (promise)]
    (log-expect
     [" INFO " "[]" "{}" "//" "Hello, Basic Future!"]
     (do
       (sut/with-log-context {:uid 1234}
         (sut/with-log-tag :main/tag
           (future
             (sut/info "Hello, Basic Future!")
             (deliver p true))))
       @p)))
  (let [p (promise)]
    (log-expect
     ["[]" "{}" "//" "Hello, Empty Context Future!"]
     (do
       (sut/with-log-context {:uid 1234}
         (sut/with-log-tag :main/tag
           (future
             (sut/info "Hello, Empty Context Future!")
             (deliver p true))))
       @p)))
  (let [p (promise)]
    (log-expect
     [; the first log entry there:
      "[additional/tag] {additional=\"context\"} // - Hello, Fresh Context Future!"
      ; the second log entry there:
      (str "[main/tag, additional/tag, nested/tag] {additional=\"context\", "
           "nested=\"context\", uid=1234} // - Hello, Nested Inherited Context Future!")]
     (do
       (sut/with-log-context {:uid 1234}
         (sut/with-log-tag :main/tag
           (future
             (sut/with-log-context {:additional "context"}
               (sut/with-log-tag :additional/tag
                 (sut/info "Hello, Fresh Context Future!")
                 (future
                   (sut/with-log-inherited
                     (sut/with-log-context {:nested "context"}
                       (sut/with-log-tag :nested/tag
                         (sut/info "Hello, Nested Inherited Context Future!"))))
                   (deliver p true)))))))
       @p)))
  (let [p (promise)]
    (log-expect
     ["[main/tag, additional/tag]" "{additional=\"context\", uid=1234}" "//"
      "Hello, Inherited Context Future!"]
     (do
       (sut/with-log-context {:uid 1234}
         (sut/with-log-tag :main/tag
           (future
             (sut/with-log-inherited
               (sut/with-log-context {:additional "context"}
                 (sut/with-log-tag :additional/tag
                   (sut/info "Hello, Inherited Context Future!"))))
             (deliver p true))))
       @p))))
