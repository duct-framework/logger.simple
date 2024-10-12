(ns duct.logger.simple-test
  (:require [clojure.test :refer [deftest is]]
            [integrant.core :as ig]
            [duct.logger :as logger]
            [duct.logger.simple :as simple]))

(deftest stdout-logger-test
  (let [system (ig/init {::simple/stdout {}})
        logger (::simple/stdout system)]
    (is (= ":duct.logger.simple-test/example\n"
           (with-out-str (logger/info logger ::example))))
    (is (= ":duct.logger.simple-test/example {:x 1}\n"
           (with-out-str (logger/info logger ::example {:x 1}))))))
