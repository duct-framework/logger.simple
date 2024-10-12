(ns duct.logger.simple
  (:require [duct.logger :as logger]
            [integrant.core :as ig]))

(defrecord StdoutLogger []
  logger/Logger
  (-log [_ _level _ns-str _file _line _id event data]
    (if data
      (prn event data)
      (prn event))))

(defmethod ig/init-key ::stdout [_ _]
  (->StdoutLogger))
