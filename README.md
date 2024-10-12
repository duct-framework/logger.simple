# Duct Simple Logger

A simple logging library for the [Duct][] framework that's fully
compatible with [Integrant][].

[duct]: https://github.com/duct-framework/duct
[integrant]: https://github.com/weavejester/integrant

## Installation

Add the following dependency to your deps.edn file:

    org.duct-framework/logger.simple {:mvn/version "0.1.0-SNAPSHOT"}

Or to your Leiningen project file:

    [org.duct-framework/logger.simple "0.1.0-SNAPSHOT"]

## Usage

You can use the `:duct.logger.simple/stdout` logger to output logging
events. This key derives from `:duct/logger`.

```edn
{:duct.logger.simple/stdout {}
 :example/hello-world {:logger #ig/ref :duct/logger}}
```

The logger uses the protocol from the [duct.logger][] library:

```clojure
(ns example
  (:require [duct.logger :as logger]
            [integrant.core :as ig]))

(defmethod ig/init-key ::hello-world [_ {:keys [logger]}]
  (logger/log logger ::hello-world))
```

[duct.logger]: https://github.com/duct-framework/logger

## License

Copyright © 2024 James Reeves

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
