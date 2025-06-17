# com.github.seancorfield/logging4j2

[![Clojars](https://img.shields.io/badge/clojars-com.github.seancorfield/logging4j2_1.0.3-green.svg?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAMAAABEpIrGAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAABjFBMVEUAAAAdCh0qDikdChwAAAAnDSY0EjM2FjUnDiYnDSYnDSYpDigyEDEEAQRGNUb///////8mDSYAAAAAAAAAAAAFAgUqEyoAAAAAAAAAAAAFAgUAAABXU1c2FjVMx+dQx+f///////9Nx+b////4/f6y4vRPt+RQtOT///9Qt+P///8oDSey4vRQr9/////3/P5hzelNx+dNx+dNx+f///8AAAAuDy0zETIAAAAoDScAAAAAAAARBREAAAAvDy40ETMwEC9gSF+Ne42ilKKuoK6Rg5B5ZXlaP1o4Gzf///9nTWZ4YncyEDF/bn/8/Pz9/P339/c1FTUlDCRRM1AbCRtlS2QyEDEuDy1gRWAxEDAzETIwEC/g4OAvDy40EjOaiZorDiq9sbzNyM3UzdQyEDE0ETMzETKflZ/UzdQ5Fzmu4fNYyuhNx+dPt+RLu9xQyOhBbo81GTuW2vCo4PJNx+c4MFE5N1lHiLFEhKQyEDGDboMzETI5Fjh5bXje2d57aHrIw8jc2NyWhJUrDioxe9o4AAAAPnRSTlMAkf+IAQj9+e7n6e31RtqAD/QAAAED+A0ZEQ8DwvkLBsmcR4aG8+cdAD6C8/MC94eP+qoTrgH+/wj1HA8eEvpXOCUAAAABYktHRA8YugDZAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH3wcHFjou4Z/shwAAAUpJREFUOMul0/VTwzAUB/AAwyW4y3B3h8EDNuTh7u6UDHcd8I+TbHSjWdrjju/1h77kc+3Lu5aQvyakF/r6B5wu1+DQMEBomLRtG0EpozYDCEccA4iIjIqOiY0bB5iYxHgZ4FQCpYneKmmal0aQPMOXZnUAvJhLkbpInf8NFtKCTrGImK6DJcTlDGl/BXGV6oCsrSNIYAM3aQDwl2xJYBtBB5lZAuyYgWzY3YMcNcjN2wc4EGMEFTg8+hlyfgEenygAj71Q9FBExH0wKC4p1bRTJlJWXqEAVNM05ovbXfkPAHBmAUQPAGaAsXMBLiwA8z3h0gRcsWsObuAWLJu8Awb3ZoB5T8EvS/CgBo9Y5Z8TPwXBJwlUI9Ia/yRrEZ8lID71Olrf0MiamkkL4kurDEjba+C/e2sninR0wrsH8eMTvrqIWbodjh7jyjdtCY3Aniz4jwAAACV0RVh0ZGF0ZTpjcmVhdGUAMjAxNS0wNy0wN1QyMjo1ODo0NiswMjowMCgWtSoAAAAldEVYdGRhdGU6bW9kaWZ5ADIwMTUtMDctMDdUMjI6NTg6NDYrMDI6MDBZSw2WAAAAAElFTkSuQmCC)](https://clojars.org/com.github.seancorfield/logging4j2)
[![cljdoc](https://cljdoc.org/badge/com.github.seancorfield/logging4j2?1.0.3)](https://cljdoc.org/d/com.github.seancorfield/logging4j2/CURRENT)

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
com.github.seancorfield/logging4j2 {:mvn/version "1.0.3"}
```

This version of the library depends on log4j2 2.25.0, which is the
latest stable release as of June 2025.

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
* `with-log-uuid` accepts a body of code to execute. A unique tag is pushed onto the
context, for the execution of the body.

```clojure
(logger/with-log-context {:uid (:id user)}
  (logger/info "Hello, world!")) ; INFO {uid=1234} Hello, world!

(logger/with-log-tag (str "user_" (:id user))
  (logger/info "Hello, world!")) ; INFO [user_1234] Hello, world!

(logger/with-log-uuid
  (logger/info "Hello, world!")) ; is equivalent to
(logger/with-log-tag (str (random-uuid))
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

You can build a `Message` directly with `logger/as-message`:

```clojure
(logger/as-message "Hello, {}!" "Parameter")
;; => ParameterizedMessage
(logger/as-message {:hello "world"})
;; => MapMessage
(logger/as-message "Hello," "World!")
;; => SimpleMessage
```

If you are using Clojure 1.12 (or later), you can provide a `(fn [] ...)` which
will be used as a `MessageSupplier` object:

```clojure
(logger/info #(logger/as-message "Hello, Supplier!"))
```

On Clojure 1.11, you have to construct the `MessageSupplier` object directly,
for example by using `reify` to implement the `MessageSupplier` interface
and provide the `get` method:

```clojure
(logger/info (reify org.apache.logging.log4j.message.MessageSupplier
               (get [_] (logger/as-message "Hello, Supplier!"))))
```

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
(logger/with-log-context {:uid (:id user)}
  (future
    (logger/with-log-inherited
      (logger/info "Hello, world!")))) ; INFO {uid=1234} Hello, world!
```

If you're passing functions between threads, you may need to use `bound-fn`
or `bound-fn*` in order to convey the dynamic context into the new thread.

> Note: `with-log-inherited` will inherit the entire MDC/NDC from the dynamic parent context, even if there are intervening calls to `with-log-context`, `with-log-tag`, or `with-log-uuid` in the child thread, that did not inherit the context.

## Configuration

Log4j2 is very flexible and can be confgured via XML, JSON, YAML, or properties
files. See the [log4j2 configuration documentation](https://logging.apache.org/log4j/2.x/manual/configuration.html).

The configuration file can either be on the classpath (e.g., in your `resources`
folder), or you can specify the name/location using the `log4j2.configurationFile`
JVM property or the `LOG4J_CONFIGURATION_FILE` environment variable.

I prefer configuration via properties files, so I typically have
something like this in `resources/log4j2.properties`:

```properties
rootLogger.level = info
rootLogger.appenderRef.stdout.ref = STDOUT
rootLogger.appenderRef.file.ref = logfile

appender.console.type = Console
appender.console.name = STDOUT
appender.console.filter.threshold.type = ThresholdFilter
appender.console.filter.threshold.level = info
appender.console.layout.type = PatternLayout
appender.console.layout.pattern = [%c] %x %X %highlight{%m}%n
```

For development, I usually have a second properties file that is in a dev-only
directory (added to the classpath via an alias), called `log4j2-test.properties`:

```properties
# so that log4j2 checks every 30 seconds for changes in configuration:
monitorInterval = 30

rootLogger.level = debug
rootLogger.appenderRef.stdout.ref = STDOUT
rootLogger.appenderRef.file.ref = logfile

appender.console.type = Console
appender.console.name = STDOUT
appender.console.filter.threshold.type = ThresholdFilter
appender.console.filter.threshold.level = debug
appender.console.layout.type = PatternLayout
appender.console.layout.pattern = [%c] %x %X %highlight{%m}%n
```

You may also want to configure the log level threshold higher for some libraries
that are very chatty at `info` or `debug`:

```properties
# Chatty libraries we need to suppress:
logger.sshj.name = net.schmizz.sshj
logger.sshj.level = warn

logger.hikari.name = com.zaxxer.hikari
logger.hikari.level = warn

logger.authnet.name = net.authorize
logger.authnet.level = warn

logger.eclipse1.name = com.eclipse
logger.eclipse1.level = warn

logger.eclipse2.name = org.eclipse
logger.eclipse2.level = warn
```

## `java.util.logging` Bridge

In order to correctly bridge `java.util.logging` (JUL) to log4j2, you need to
provide some sort of configuration. Several options are described in the
[JUL-to-Log4j bridge docs](https://logging.apache.org/log4j/2.x/log4j-jul.html),
which also explains the limitations imposed by JUL that make this necessary.

The simplest approach is to set the `java.util.logging.manager` JVM property.
If you can guarantee that no logging is done before your application gets
control (in your `-main` function), then you can set this property in code:

```clojure
(System/setProperty "java.util.logging.manager"
                    "org.apache.logging.log4j.jul.LogManager")
```

You can put this as a top-level form in your main namespace, e.g., immediately
after the `ns` form.

If your code is running in a context that may initialize JUL before your
application gets control (e.g., a servlet container), then you need to
set this property as a JVM property when starting the JVM, e.g.,

```bash
java -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager \
  -jar your.jar
```

If you are using the Clojure CLI, you can either set it directly or via an
alias. Setting it directly using the CLI's `-J` option:

```bash
clojure -J-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager \
  -M -m your.namespace
```

Setting it via an alias in `deps.edn`:

```clojure
{:aliases
 {:jul
  {:jvm-opts ["-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"]}}}
```

Then you can run your code with the alias:

```bash
clojure -M:jul -m your.namespace
```

If you configure logging levels for log4j2 and they do not seem to work as
expected, it's possible that you may be running into this
[log4j-jul issue](https://github.com/apache/logging-log4j2/issues/2353),
where some code is setting the JUL logging level programmatically and
overriding your configuration. With log4j2 2.24.0 and later, this should
not be a problem, but if you want that programmatic setting to be effective,
i.e., to override your configuration, you can set the `log4j2.julLoggerAdapter`
JVM property to `org.apache.logging.log4j.jul.CoreLoggerAdapter` (which
was the default behavior in log4j2 2.23.0).

As above, you can set this via the CLI's `-J` option or via an alias in
`deps.edn`, or directly for the `java` command:

```bash
java -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager \
  -Dlog4j2.julLoggerAdapter=org.apache.logging.log4j.jul.CoreLoggerAdapter \
  -jar your.jar
```

## logj42 and `tools.build`

If you are using `tools.build` (`build.clj`) to build uberjars,
and you are using either this library or using log4j2 directly,
you probably need to look at this
[log4j2 conflict handler for `tools.build`](https://github.com/seancorfield/build-uber-log4j2-handler).
If your project has dependencies that provide log4j2 plugins
(such as templates), using the conflict handler will ensure
that those plugins have their configuration merged correctly.

## `clojure.tools.logging` Migration/FAQ

c.t.l has `logf` but this library does not. For most uses (with `%s`), you
can use a `ParameterizedMessage` -- replace `%s` with `{}` and you
should get the same behavior. For more complex cases, you can use
`clojure.core/format` with the arguments to build a string for logging.

You will no longer need `-Dclojure.tools.logging.factory=clojure.tools.logging.impl/log4j2-factory`
as this library uses log4j2 directly (c.t.l defaults to `slf4j`).

If you want to get a `Logger` object directly, you can use
`org.apache.logging.log4j.LogManager/getLogger`, passing a namespace
name as a string. c.t.l provided a generic `get-logger` in its `impl` namespace.

## License

Copyright © 2024-2025 Sean Corfield.

Distributed under the Eclipse Public License version 1.0.
