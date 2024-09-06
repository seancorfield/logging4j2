# org.corfield/logging4j2

A Clojure wrapper for log4j2, intended as a partial replacement for
[`clojure.tools.logging`](https://github.com/clojure/tools.logging), that
supports MDC (mapped diagnostic context), NDC (nested diagnostic context)
and markers directly, in a Clojure-friendly manner.

This library explicitly depends on log4j2 and all of the bridge libraries
that route other logging frameworks to log4j2 (jcl, jul, log4j 1.x, slf4j 1.x and 2.x).

> Note: requires Clojure 1.11 or later.

## Installation

Add the following dependency to your `deps.edn` file:

```clojure
org.corfield/logging4j2 {:mvn/version "0.1.0-SNAPSHOT"}
```

> Note: this library is a work in progress -- feedback is appreciated!

## Usage

Require the main `org.corfield.logging4j2` namespace (`:as` whatever alias
you prefer -- the examples below use `logger`):

The library provides the following macros that you can use to log information:
`trace`, `debug`, `info`, `warn`, `error`, and `fatal`. There is also a
generic `log` macro that accepts a level keyword as its first argument.

```clojure
(logger/info "Hello, world!")
(logger/log :info "Hello, world!") ; equivalent to the above
```

Support for MDC and NDC is provided via the `with-log-context`, `with-log-tag`,
and `with-log-uuid` macros (see **MDC and NDC** below for more detail):

* `with-log-context` accepts a hash map as its first argument, followed by a
body of code to execute. The keys and values in the hash map are added to the
context as strings, for the execution of the body.
* `with-log-tag` accepts a keyword or string as its first argument, followed
by a body of code to execute. The tag is pushed onto the context, for
the execution of the body.
* `with-log-uuid` a body of code to execute. A unique tag is pushed onto the
context, for the execution of the body.

```clojure
(with-log-context {:uid (:id user)}
  (logger/info "Hello, world!")) ; INFO {uid=1234} Hello, world!

(with-log-tag (str "user_" (:id user))
  (logger/info "Hello, world!")) ; INFO [user_1234] Hello, world!

(with-log-uuid
  (logger/info "Hello, world!")) ; is equivalent to
(with-log-tag (str (random-uuid))
  (logger/info "Hello, world!")) ; INFO [8b21769c-33c5-42cb-b6c4-146ce8bb875f] Hello, world!
```

Support for markers is provided by the `as-marker` function, which accepts
one or more keywords or strings and returns a marker object that can be passed
as the first argument to any of the logging macros (or the second argument to
the generic `log` macro).

`(as-marker :sql-update :sql)` returns a marker object that represents the
string `"SQL_UPDATE"` with the marker `"SQL"` as its parent.

### Possible Arguments

Most logging will look like this:

```clojure
(logger/info "some message")
;; or
(logger/info my-exception "some message")
;; or
(logger/info my-marker "some message")
;; or
(logger/info my-marker my-exception "some message")
```

If multiple message arguments are provided, they are generally converted to
strings and concatenated with spaces between them, except as follows:

* If the first message argument is a string and contains one or more `{}`
placeholders, then the remaining arguments are treated as values for
those placeholders and a
[`ParameterizedMessage`](https://logging.apache.org/log4j/2.x/javadoc/log4j-api/org/apache/logging/log4j/message/ParameterizedMessage)
is constructed.
* If a single message argument is provided and it is a hash map, then a
[`MapMessage`](https://logging.apache.org/log4j/2.x/log4j-api/apidocs/org/apache/logging/log4j/message/MapMessage.html)
is constructed, with the keys of the Clojure hash map converted to strings and
the values left as-is.

### MDC and NDC

Mapped Diagnostic Context (MDC) and Nested Diagnostic Context (NDC) are supported
(as noted above) by the three `with-log-*` macros. Nested calls to these macros
will accumulate the map context and the stack of tags automatically, within
the current thread.

The underlying context for log4j2 is thread-local by default: each thread
starts out with an empty context. This library also tracks MDC and NDC using
a dynamic var in Clojure, so you can use `with-log-inherited` inside a spawned
thread to inherit the MDC and NDC from the parent thread.

```clojure
(with-log-context {:uid (:id user)}
  (future
    (with-log-inherited
      (logger/info "Hello, world!")))) ; INFO {uid=1234} Hello, world!
```

If you're passing functions between threads, you may need to use `bound-fn`
or `bound-fn*` in order to convey the dynamic context into the new thread.

## License

Copyright © 2024 Sean Corfield.

Distributed under the Eclipse Public License version 1.0.
