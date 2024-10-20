(ns duct.logger.simple
  (:require [amalloy.ring-buffer :as rb]
            [clojure.java.io :as io]
            [duct.logger :as logger]
            [integrant.core :as ig])
  (:import [java.time Instant]
           [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))

(defrecord BufferedLogger [buffer executor appenders]
  logger/Logger
  (-log [_ level ns-str file line id event data]
    (swap! buffer conj [(Instant/now) level ns-str file line id event data])))

(defn- level-checker [levels]
  (if (= :all levels) (constantly true) (set levels)))

(defmulti make-appender :type)

(defmethod make-appender :stdout [{:keys [levels] :or {levels :all}}]
  (let [print-level? (level-checker levels)]
    (fn [[time level _ns-str _file _line _id event data]]
      (when (print-level? level)
        (if data
          (println (str time) event (pr-str data))
          (println (str time) event))))))

(defmethod make-appender :file [{:keys [path] :as opts}]
  (let [appender (make-appender (assoc opts :type :stdout))
        writer   (io/writer path :append true)]
    (reify
      clojure.lang.IFn
      (invoke [_ log]
        (binding [*out* writer] (appender log)))
      java.io.Closeable
      (close [_]
        (.close writer)))))

(defn- consume-logs [buffer amount appenders]
  (let [log (peek buffer)]
    (if (and log (pos? amount))
      (do (run! #(% log) appenders)
          (recur (pop buffer) (dec amount) appenders))
      buffer)))

(defn- start-polling [^Runnable f ^long delay]
  (doto (ScheduledThreadPoolExecutor. 1)
    (.scheduleAtFixedRate f delay delay TimeUnit/MILLISECONDS)))

(defmethod ig/init-key :duct.logger/simple
  [_ {:keys [buffer-size polling-rate poll-chunk-size appenders]
      :or   {buffer-size 1024, polling-rate 5, poll-chunk-size 8}}]
  (let [buffer    (atom (rb/ring-buffer buffer-size))
        appenders (mapv make-appender appenders)
        executor  (start-polling
                   #(swap! buffer consume-logs poll-chunk-size appenders)
                   polling-rate)]
    (->BufferedLogger buffer executor appenders)))

(defmethod ig/halt-key! :duct.logger/simple [_ {:keys [executor appenders]}]
  (.shutdown ^ScheduledThreadPoolExecutor executor)
  (run! #(when (instance? java.io.Closeable %) (.close %)) appenders))
