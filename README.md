# org.corfield/logging4j2

A Clojure wrapper for log4j2, intended as a partial replacement for
[`clojure.tools.logging`](https://github.com/clojure/tools.logging), that
supports MDC (mapped diagnostic context) and markers directly, in a
Clojure-friendly manner.

This library explicitly depends on log4j2 and all of the bridge libraries
that route other logging frameworks to log4j2 (jcl, jul, log4j 1.x, slf4j 1.x and 2.x).

## Installation

Add the following dependency to your `deps.edn` file:

```clojure
org.corfield/logging4j2 {:mvn/version "TBD"}
```

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

Support for MDC is provided via the `with-log-context`, `with-log-tag`,
and `with-log-uuid` macros:

* `with-log-context` accepts a hash map as its first argument, followed by a
body of code to execute. The keys and values in the hash map are added to the
dynamic context as strings, for the execution of the body, and a dynamic var
is used behind the scenes to track nested contexts.
* `with-log-tag` accepts a keyword or string as its first argument, followed
by a body of code to execute. The tag is pushed onto the dynamic context, for
the execution of the body.
* `with-log-uuid` a body of code to execute. A unique tag is pushed onto the
dynamic context, for the execution of the body.

```clojure
(with-log-context {:uid (:id user)}
  (logger/info "Hello, world!")) ; INFO {uid=1234} Hello, world!

(with-log-tag (str "user_" (:id user))
  (logger/info "Hello, world!")) ; INFO {user_1234} Hello, world!

(with-log-uuid
  (logger/info "Hello, world!")) ; is equivalent to
(with-log-tag (str (random-uuid))
  (logger/info "Hello, world!")) ; INFO {8b21769c-33c5-42cb-b6c4-146ce8bb875f} Hello, world!
```

Support for markers is provided by the `as-marker` function, which accepts
one or more keywords or strings and returns a marker object that can be passed
as the first argument to any of the logging macros (or the second argument to
the generic `log` macro).

`(as-marker :sql-update :sql)` returns a marker object that represents the
string `"SQL_UPDATE"` with the marker `"SQL"` as its parent.

## License

Copyright © 2024 Sean Corfield.

Distributed under the Eclipse Public License version 1.0.
