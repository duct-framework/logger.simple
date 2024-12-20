(ns duct.logger.simple
  (:require [amalloy.ring-buffer :as rb]
            [clojure.java.io :as io]
            [duct.logger :as logger]
            [integrant.core :as ig])
  (:import [java.time Instant]
           [java.time.temporal ChronoUnit]
           [java.util.concurrent Executors ScheduledThreadPoolExecutor
            TimeUnit ThreadFactory]))

(defrecord BufferedLogger [buffer executor appenders options]
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
  (io/make-parents path)
  (let [print-level? (level-checker levels)
        writer       (io/writer (io/file path) :append true)]
    (reify
      clojure.lang.IFn
      (invoke [_ log] (write-logline writer log print-level? false))
      java.io.Closeable
      (close [_] (.close writer)))))

(defn- safe-pop [buffer]
  (cond-> buffer (pos? (count buffer)) pop))

(defn- consume-logs! [buffer amount appenders]
  (loop [n amount]
    (when (pos? n)
      (when-some [log (peek (first (swap-vals! buffer safe-pop)))]
        (run! #(% log) appenders)
        (recur (dec n))))))

(defn- daemon-thread-factory ^ThreadFactory []
  (let [default-factory (Executors/defaultThreadFactory)]
    (reify ThreadFactory
      (newThread [_ runnable]
        (doto (.newThread default-factory runnable)
          (.setDaemon true))))))

(defn- start-polling [^Runnable f ^long delay]
  (doto (ScheduledThreadPoolExecutor. 1 (daemon-thread-factory))
    (.scheduleAtFixedRate f delay delay TimeUnit/MILLISECONDS)))

(defmethod ig/init-key :duct.logger/simple
  [_ {:keys [buffer-size polling-rate poll-chunk-size appenders]
      :or   {buffer-size 1024, polling-rate 5, poll-chunk-size 8}
      :as   options}]
  (let [buffer    (atom (rb/ring-buffer buffer-size))
        appenders (mapv make-appender appenders)
        executor  (start-polling
                   #(consume-logs! buffer poll-chunk-size appenders)
                   polling-rate)]
    (->BufferedLogger buffer executor appenders options)))

(defmethod ig/halt-key! :duct.logger/simple
  [_ {:keys [executor appenders]
      {:keys [shutdown-delay shutdown-timeout]
       :or   {shutdown-delay 100, shutdown-timeout 1000}} :options}]
  (Thread/sleep shutdown-delay)
  (doto ^ScheduledThreadPoolExecutor executor
    (.shutdown)
    (.awaitTermination shutdown-timeout TimeUnit/MILLISECONDS))
  (run! #(when (instance? java.io.Closeable %) (.close %))
        appenders))
