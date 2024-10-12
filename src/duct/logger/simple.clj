(ns duct.logger.simple
  (:require [amalloy.ring-buffer :as rb]
            [duct.logger :as logger]
            [integrant.core :as ig])
  (:import [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))

(defrecord BufferedLogger [buffer executor]
  logger/Logger
  (-log [_ level ns-str file line id event data]
    (swap! buffer conj [level ns-str file line id event data])))

(defn- stdout-logger [[_level _ns-str _file _line _id event data]]
  (if data
    (prn event data)
    (prn event)))

(defn- consume-logs [buffer amount logger]
  (let [log (peek buffer)]
    (if (and log (pos? amount))
      (do (logger log) (recur (pop buffer) (dec amount) logger))
      buffer)))

(defn- start-polling [^Runnable f ^long delay]
  (doto (ScheduledThreadPoolExecutor. 1)
    (.scheduleAtFixedRate f delay delay TimeUnit/MILLISECONDS)))

(defmethod ig/init-key ::stdout [_ _]
  (let [buffer   (atom (rb/ring-buffer 1024))
        executor (start-polling
                  #(swap! buffer consume-logs 8 stdout-logger)
                  5)]
    (->BufferedLogger buffer executor)))

(defmethod ig/halt-key! ::stdout [_ {:keys [executor]}]
  (.shutdown ^ScheduledThreadPoolExecutor executor))
