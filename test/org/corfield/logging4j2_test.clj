;; copyright (c) 2024 Sean Corfield

(ns org.corfield.logging4j2-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.tools.logging :as ctl]
            [org.corfield.logging4j2 :as sut]
            [clojure.string :as str]))

(deftest sanity-test
  (sut/log :info "Hello, World!")
  (sut/with-log-tag :hello/world
    (sut/with-log-context {:uid 1234}
      (sut/info "Hello, Tag and UID!")))
  (sut/with-log-tag :outer/tag
    (sut/with-log-tag :inner/tag
      (sut/info "Hello, Outer/Inner Tag!")))
  (sut/with-log-uuid
    (sut/fatal "Hello, UUID!"))
  (is true))

(deftest supplier-test
  (sut/info (reify org.apache.logging.log4j.util.MessageSupplier
              (get [_] (sut/as-message "Hello," "Reified Supplier!"))))
  ;; we only support Clojure 1.11.0 or later;
  ;; implicit suppliers only work for Clojure 1.12.0 or later:
  (if (str/starts-with? (clojure-version) "1.11")
    (sut/warn "MessageSupplier coercion not supported for Clojure 1.11")
    (sut/info (fn [] (sut/as-message "Hello," "Implicit Supplier!"))))
  (is true))

(deftest ctl-test
  (ctl/log :info "Hello, c.t.l World!")
  (sut/with-log-tag :hello/world
    (sut/with-log-context {:uid 1234}
      (ctl/info "Hello, c.t.l Tag and UUID!")))
  (sut/with-log-tag :outer/tag
    (sut/with-log-tag :inner/tag
      (ctl/info "Hello, c.t.l Outer/Inner Tag!")))
  (sut/with-log-uuid
    (ctl/fatal "Hello, c.t.l UUID!"))
  (is true))

(deftest marker-test
  (sut/log :info (sut/as-marker :hello/world) "Hello, Marker!")
  (sut/log :warn (sut/as-marker :child :parent) "Hello, Marker with Parent!")
  (is true))

(deftest formatting-test
  (sut/log :info "Hello, {}!" "Parameter")
  (sut/log :info "Hello, {} {}!" "Two" "Parameters")
  (sut/log :info "Hello, {} {} {}!" "Three" "Parameters" "Provided")
  (is true))

(deftest map-test
  (sut/log :warn {:hello "MapMessage" 13 42})
  (sut/log :warn {:hello/world :how.are/you?})
  (is true))

(deftest future-test
  (let [p (promise)]
    (sut/with-log-context {:uid 1234}
      (sut/with-log-tag :main/tag
        (future
          (sut/info "Hello, Basic Future!")
          (deliver p true))))
    @p)
  (let [p (promise)]
    (sut/with-log-context {:uid 1234}
      (sut/with-log-tag :main/tag
        (future
          (sut/info "Hello, Empty Context Future!")
          (deliver p true))))
    @p)
  (let [p (promise)]
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
    @p)
  (let [p (promise)]
    (sut/with-log-context {:uid 1234}
      (sut/with-log-tag :main/tag
        (future
          (sut/with-log-inherited
            (sut/with-log-context {:additional "context"}
              (sut/with-log-tag :additional/tag
                (sut/info "Hello, Inherited Context Future!"))))
          (deliver p true))))
    @p)
  (is true))
