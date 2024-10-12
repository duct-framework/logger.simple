(ns duct.logger.simple
  (:require [amalloy.ring-buffer :as rb]
            [duct.logger :as logger]
            [integrant.core :as ig])
  (:import [java.time Instant]
           [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))

(defrecord BufferedLogger [buffer executor]
  logger/Logger
  (-log [_ level ns-str file line id event data]
    (swap! buffer conj [(Instant/now) level ns-str file line id event data])))

(defn- stdout-logger [[time _level _ns-str _file _line _id event data]]
  (if data
    (println (str time) event (pr-str data))
    (println (str time) event)))

(defn- consume-logs [buffer amount logger]
  (let [log (peek buffer)]
    (if (and log (pos? amount))
      (do (logger log) (recur (pop buffer) (dec amount) logger))
      buffer)))

(defn- start-polling [^Runnable f ^long delay]
  (doto (ScheduledThreadPoolExecutor. 1)
    (.scheduleAtFixedRate f delay delay TimeUnit/MILLISECONDS)))

(defmethod ig/init-key ::stdout
  [_ {:keys [buffer-size polling-rate poll-chunk-size]
      :or   {buffer-size 1024, polling-rate 5, poll-chunk-size 8}}]
  (let [buffer   (atom (rb/ring-buffer buffer-size))
        executor (start-polling
                  #(swap! buffer consume-logs poll-chunk-size stdout-logger)
                  polling-rate)]
    (->BufferedLogger buffer executor)))

(defmethod ig/halt-key! ::stdout [_ {:keys [executor]}]
  (.shutdown ^ScheduledThreadPoolExecutor executor))
