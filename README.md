# Duct Simple Logger [![Build Status](https://github.com/duct-framework/logger.simple/actions/workflows/test.yml/badge.svg)](https://github.com/duct-framework/logger.simple/actions/workflows/test.yml)

A simple logging library for the [Duct][] framework that's fully
compatible with [Integrant][].

[duct]: https://github.com/duct-framework/duct
[integrant]: https://github.com/weavejester/integrant

## Installation

Add the following dependency to your deps.edn file:

    org.duct-framework/logger.simple {:mvn/version "0.2.0"}

Or to your Leiningen project file:

    [org.duct-framework/logger.simple "0.2.0"]

## Usage

You can use the `:duct.logger/simple` logger to output logging events.
This key derives from `:duct/logger`.

```edn
{:duct.logger/simple {:appenders [{:type :stdout}]}
 :example/hello-world {:logger #ig/ref :duct/logger}}
```

The logger uses the protocol from the [duct.logger][] library:

```clojure
(ns example
  (:require [duct.logger :as logger]
            [integrant.core :as ig]))

(defmethod ig/init-key ::hello-world [_ {:keys [logger]}]
  (logger/info logger ::hello-world))
```

[duct.logger]: https://github.com/duct-framework/logger

The logger should have one or more appenders added to the `:appenders`
key in the option map. These appenders are responsible for sending the
logs to some output, and there's two types available:

- `:stdout` - logs get sent to STDOUT
- `:file`   - logs get appended to a file

Appenders are specified as maps, and the `:type` key determines the
type.

#### STDOUT logger

The STDOUT logger prints to STDOUT and supports the following options:

- `:levels` - a set of log levels (or `:all`) to limit the appender to
- `:timestamps?` - whether to print timestamps (defaults to true)

#### File logger

The `:file` appender appends logs to a file, and takes the following
options:

- `:levels` - a set of log levels (or `:all`) to limit the appender to
- `:path` - the path of the log file

## License

Copyright © 2024 James Reeves

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
