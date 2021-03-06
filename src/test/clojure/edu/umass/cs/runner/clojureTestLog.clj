(ns edu.umass.cs.runner.clojureTestLog
  (:gen-class)
  (:import
    (edu.umass.cs.runner Record)
    (edu.umass.cs.runner.system.backend KnownBackendType AbstractLibrary)
    (edu.umass.cs.runner.system.backend.known.localhost LocalLibrary Server)
    (edu.umass.cs.surveyman.input.csv CSVParser CSVLexer)
    (edu.umass.cs.surveyman.qc RandomRespondent QCMetrics)
    (edu.umass.cs.surveyman.survey Survey Question Component)
    (edu.umass.cs.surveyman.utils Slurpie)
    (org.apache.log4j Logger FileAppender PatternLayout)
           (java.util.regex Pattern))
  (:require [clojure.string :as s]
            [edu.umass.cs.runner.utils.response-util :as response-util])
  )

(def numResponses 250)
(def alpha 0.05)
(def response-lookup (atom {}))
(def numQ (atom 1))
(def DUMMY_ID "dummy")
(def SUBMIT_FINAL "submit_final")
(def SUBMIT_PREFIX "submit_")
(def NEXT_PREFIX "next_")
(def MTURK_FORM "mturk_form")
(def ^KnownBackendType bt KnownBackendType/LOCALHOST)
(def ^AbstractLibrary lib (LocalLibrary. ""))
(def prefix "src/test/resources/data/samples/")



(defn generateNRandomResponses
  [survey]
  (try
    (map (fn [^RandomRespondent rr] (.response rr))
         (response-util/get-random-survey-responses survey numResponses))
    (catch Exception e (do (println (format "Error in %s" (.source survey)))
                            (.printStackTrace e)))
    )
  )

(defn makeSurvey
    [filename sep]
    (->> (CSVLexer. filename sep)
         (CSVParser.)
         (.parse))
    )

(defn get-survey-and-responses-by-filename
  [^String filename]
  (loop [data @response-lookup]
    (cond (nil? data) (throw (Exception. (format "Survey %s not found." filename)))
      (= filename (.source ^Survey (ffirst data))) (first data)
      :else (recur (rest data))
      )
    )
  )

(defn sm-get-url
    [^Record record]
    (-> record
        (.getHtmlFileName)
        (.split (AbstractLibrary/fileSep))
        (->> (last) (format "http://localhost:%d/logs/%s" Server/frontPort))))

(def tests
    (map #(s/split % #"\s+" )
          (s/split (Slurpie/slurp "test_data")
                   (re-pattern (System/getProperty "line.separator")))))

(def LOGGER (Logger/getLogger (str (ns-name *ns*))))

(let [^FileAppender txtHandler (FileAppender. (PatternLayout. "%d{dd MMM yyyy HH:mm:ss,SSS}\t%-5p [%t]: %m%n")
                                              (format "logs/%s.log" (str (ns-name *ns*))))]
    (.setEncoding txtHandler "UTF-8")
    (.setAppend txtHandler false)
    (.addAppender LOGGER txtHandler))


(pmap (fn [[filename sep outcome]]
        (try
          (let [^Survey survey (makeSurvey (str "src/test/resources/" filename) sep)
                responses (generateNRandomResponses survey)]
            (when-not (read-string outcome)
              (println "Unexpected success for file " filename)
              )
            (swap! response-lookup assoc survey responses)
            )
          (catch Exception e
            (when (read-string outcome)
              (println "Unexpected failure for file " filename)
              (.printStackTrace e)
              (System/exit 1)))
          )
        )
      tests)