(ns duct.logger.simple
  (:require [amalloy.ring-buffer :as rb]
            [clojure.java.io :as io]
            [duct.logger :as logger]
            [integrant.core :as ig])
  (:import [java.time Instant]
           [java.time.temporal ChronoUnit]
           [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))

(defrecord BufferedLogger [buffer executor appenders]
  logger/Logger
  (-log [_ level ns-str file line id event data]
    (swap! buffer conj [(Instant/now) level ns-str file line id event data])))

(defn- level-checker [levels]
  (if (= :all levels) (constantly true) (set levels)))

(defn- format-instant [^Instant instant]
  (-> instant (.truncatedTo ChronoUnit/MILLIS) .toString))

(let [space   (int \space)
      newline (int \newline)]
  (defn- write-logline
    [writer logline print-level? brief?]
    (let [[time level _ns-str _file _line _id event data] logline]
      (when (print-level? level)
        (when-not brief?
          (.write writer (format-instant time))
          (.write writer space)
          (.write writer (str level))
          (.write writer space))
        (.write writer (str event))
        (when data
          (.write writer space)
          (.write writer (pr-str data)))
        (.write writer newline)
        (.flush writer)))))

(defmulti make-appender :type)

(defmethod make-appender :stdout
  [{:keys [levels brief?] :or {levels :all, brief? false}}]
  (let [print-level? (level-checker levels)]
    (fn [log] (write-logline *out* log print-level? brief?))))

(defmethod make-appender :file [{:keys [levels path] :or {levels :all}}]
  (let [print-level? (level-checker levels)
        writer       (io/writer (io/file path) :append true)]
    (reify
      clojure.lang.IFn
      (invoke [_ log] (write-logline writer log print-level? false))
      java.io.Closeable
      (close [_] (.close writer)))))

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
