(defproject org.duct-framework/logger.simple "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.11.4"]
                 [amalloy/ring-buffer "1.3.1"]
                 [duct/logger "0.3.0"]
                 [integrant "0.12.0"]]
  :repl-options {:init-ns logger.simple})
