(ns duct.logger.simple-test
  (:require [clojure.test :refer [deftest is]]
            [integrant.core :as ig]
            [duct.logger :as logger]
            [duct.logger.simple :as simple]))

(deftest stdout-logger-test
  (let [system (ig/init {::simple/stdout {}})]
    (try
      (let [logger (::simple/stdout system)
            output (java.io.StringWriter.)]
        (with-redefs [*out* output]
          (logger/info logger :example/foo)
          (logger/info logger :example/bar {:x 1})
          (Thread/sleep 50))
        (is (re-matches
             #"(?x)[0-9TZ.:-]+\s:example/foo\n
                   [0-9TZ.:-]+\s:example/bar\s\{:x\s1\}\n"
             (str output))))
      (finally
        (ig/halt! system)))))
