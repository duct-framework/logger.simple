(ns duct.logger.simple-test
  (:require [clojure.test :refer [deftest is testing]]
            [integrant.core :as ig]
            [duct.logger :as logger]
            [duct.logger.simple :as simple])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest stdout-logger-test
  (let [system (ig/init {:duct.logger/simple
                         {:appenders [{:type :stdout}]}})]
    (try
      (let [logger (:duct.logger/simple system)
            output (java.io.StringWriter.)]
        (with-redefs [*out* output]
          (logger/debug logger :example/foo)
          (logger/info logger :example/bar {:x 1})
          (Thread/sleep 100))
        (is (re-matches
             #"(?x)[0-9TZ.:-]{24}\s:debug\s:example/foo\n
                   [0-9TZ.:-]{24}\s:info\s:example/bar\s\{:x\s1\}\n"
             (str output))))
      (finally
        (ig/halt! system)))))

(deftest file-logger-test
  (let [attrs    (make-array FileAttribute 0)
        tempdir  (Files/createTempDirectory "duct" attrs)
        tempfile (Files/createTempFile tempdir "logger-test" ".log" attrs)
        temppath (.toString (.toAbsolutePath tempfile))]
    ;; delete temporary files so we can test creation of files and parent dirs
    (Files/delete tempfile)
    (Files/delete tempdir)
    (let [system (ig/init {:duct.logger/simple
                           {:appenders [{:type :file, :path temppath}]}})]
      (try
        (let [logger (:duct.logger/simple system)]
          (logger/debug logger :example/foo)
          (logger/info logger :example/bar {:x 1})
          (Thread/sleep 100))
        (finally
          (ig/halt! system)))
      (is (re-matches
           #"(?x)[0-9TZ.:-]{24}\s:debug\s:example/foo\n
               [0-9TZ.:-]{24}\s:info\s:example/bar\s\{:x\s1\}\n"
           (slurp (.toFile tempfile))))
      (Files/delete tempfile)
      (Files/delete tempdir))))

(deftest level-restrict-test
  (testing "STDOUT logger"
    (let [system (ig/init {:duct.logger/simple
                           {:appenders [{:type :stdout
                                         :levels #{:info}}]}})]
      (try
        (let [logger (:duct.logger/simple system)
              output (java.io.StringWriter.)]
          (with-redefs [*out* output]
            (logger/debug logger :example/foo)
            (logger/info logger :example/bar {:x 1})
            (Thread/sleep 100))
          (is (re-matches
               #"[0-9TZ.:-]{24}\s:info\s:example/bar\s\{:x\s1\}\n"
               (str output))))
        (finally
          (ig/halt! system)))))
  (testing "file logger"
    (let [tempfile (java.io.File/createTempFile "logger-test" ".log")
          temppath (.getAbsolutePath tempfile)
          system   (ig/init {:duct.logger/simple
                             {:appenders [{:type :file, :path temppath
                                           :levels #{:info}}]}})]
      (try
        (let [logger (:duct.logger/simple system)]
          (logger/debug logger :example/foo)
          (logger/info logger :example/bar {:x 1})
          (Thread/sleep 100))
        (finally
          (ig/halt! system)))
      (is (re-matches
           #"[0-9TZ.:-]{24}\s:info\s:example/bar\s\{:x\s1\}\n"
           (slurp tempfile)))
      (.delete tempfile))))

(deftest stdout-brief-test
  (let [system (ig/init {:duct.logger/simple
                         {:appenders [{:type :stdout :brief? true}]}})]
    (try
      (let [logger (:duct.logger/simple system)
            output (java.io.StringWriter.)]
        (with-redefs [*out* output]
          (logger/info logger :example/foo)
          (logger/info logger :example/bar {:x 1})
          (Thread/sleep 100))
        (is (re-matches
             #"(?x):example/foo\n
                   :example/bar\s\{:x\s1\}\n"
             (str output))))
      (finally
        (ig/halt! system)))))
