(ns org.corfield.logging4j2-test
  (:require [clojure.test :refer [deftest is]]
            [org.corfield.logging4j2 :as sut]))

(deftest sanity-test
  (sut/log :info "Hello, World!")
  (sut/with-log-tag :hello/world
    (sut/with-log-context {:uid 1234}
      (sut/info "Hello, World!")))
  (sut/with-log-tag :outer/tag
    (sut/with-log-tag :inner/tag
      (sut/info "Hello, World!")))
  (sut/log :info (sut/as-marker :hello/world) "Hello, World!")
  (sut/log :warn (sut/as-marker :child :parent) "Hello, World!")
  (sut/fatal "Hello, World!")
  (is true))
