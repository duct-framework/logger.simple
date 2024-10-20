(ns duct.logger.simple-test
  (:require [clojure.test :refer [deftest is]]
            [integrant.core :as ig]
            [duct.logger :as logger]
            [duct.logger.simple :as simple]))

(deftest stdout-logger-test
  (let [system (ig/init {:duct.logger/simple
                         {:appenders [{:type :stdout}]}})]
    (try
      (let [logger (:duct.logger/simple system)
            output (java.io.StringWriter.)]
        (with-redefs [*out* output]
          (logger/info logger :example/foo)
          (logger/info logger :example/bar {:x 1})
          (Thread/sleep 100))
        (is (re-matches
             #"(?x)[0-9TZ.:-]+\s:example/foo\n
                   [0-9TZ.:-]+\s:example/bar\s\{:x\s1\}\n"
             (str output))))
      (finally
        (ig/halt! system)))))

(deftest file-logger-test
  (let [tempfile (java.io.File/createTempFile "logger-test" ".log")
        temppath (.getAbsolutePath tempfile)
        system   (ig/init {:duct.logger/simple
                           {:appenders [{:type :file, :path temppath}]}})]
    (try
      (let [logger (:duct.logger/simple system)]
        (logger/info logger :example/foo)
        (logger/info logger :example/bar {:x 1})
        (Thread/sleep 100))
      (finally
        (ig/halt! system)))
    (is (re-matches
         #"(?x)[0-9TZ.:-]+\s:example/foo\n
                   [0-9TZ.:-]+\s:example/bar\s\{:x\s1\}\n"
         (slurp tempfile)))
    (.delete tempfile)))
