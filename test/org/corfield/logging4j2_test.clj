(ns org.corfield.logging4j2-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.tools.logging :as ctl]
            [org.corfield.logging4j2 :as sut]))

(deftest sanity-test
  (sut/log :info "Hello, World!")
  (sut/with-log-tag :hello/world
    (sut/with-log-context {:uid 1234}
      (sut/info "Hello, World!")))
  (sut/with-log-tag :outer/tag
    (sut/with-log-tag :inner/tag
      (sut/info "Hello, World!")))
  (sut/with-log-uuid
    (sut/fatal "Hello, World!"))
  (is true))

(deftest ctl-test
  (ctl/log :info "Hello, World!")
  (sut/with-log-tag :hello/world
    (sut/with-log-context {:uid 1234}
      (ctl/info "Hello, World!")))
  (sut/with-log-tag :outer/tag
    (sut/with-log-tag :inner/tag
      (ctl/info "Hello, World!")))
  (sut/with-log-uuid
    (ctl/fatal "Hello, World!"))
  (is true))

(deftest marker-test
  (sut/log :info (sut/as-marker :hello/world) "Hello, Marker!")
  (sut/log :warn (sut/as-marker :child :parent) "Hello, Marker!")
  (is true))

(deftest formatting-test
  (sut/log :info "Hello, {}!" "World")
  (sut/log :info "Hello, {} {}!" "World" "Extra")
  (sut/log :info "Hello, {} {} {}!" "World" "Extra" "More")
  (is true))

(deftest map-test
  (sut/log :warn {:hello "World" 13 42})
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
          (sut/with-log-context {}
            (sut/info "Hello, Context Future!"))
          (deliver p true))))
    @p)
  (is true))
