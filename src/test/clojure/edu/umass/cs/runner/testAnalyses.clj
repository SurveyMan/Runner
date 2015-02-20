(ns testAnalyses
  (:import [edu.umass.cs.surveyman.analyses AbstractSurveyResponse IQuestionResponse OptTuple]
           [edu.umass.cs.surveyman.qc QCMetrics])
  (:use clojure.test)
  (:use edu.umass.cs.runner.testLog)
  )

(deftest test-random-responses
  (println 'test-random-responses)
  (doseq [responses (vals @response-lookup)]
    (doseq [^AbstractSurveyResponse response responses]
      (doseq [^IQuestionResponse qr (.getResponses response)]
        (doseq [^OptTuple optTupe (.getOpts qr)]
          (when-not (or (.isEmpty (.c optTupe))
                        (.freetext (.getQuestion qr))
                        (empty? (.options (.getQuestion qr))))
            (if-let [opts (filter #(not (.isEmpty %)) (vals (.options (.getQuestion qr))))]
                (is (contains? (set opts) (.c optTupe)))
                )
              )
            )
          )
        )
      )
    )

(deftest test-answer-map
  (println 'test-answer-map)
  (doseq [responses (vals @response-lookup)]
    (let [ansMap (response-util/make-ans-map responses)]
      (doseq [k (keys ansMap)]
        (when-not (.freetext k)
          (doseq [^qc.response-util/Response r (ansMap k)]
            (let [optSet (set (map #(.getCid %) (.getOptListByIndex k)))]
              (when-not (empty? optSet)
                  (is (contains? optSet (.getCid (first (:opts r))))))
              )
            )
          )
        )
      )
    )
  )

(deftest test-ordered
  (println 'test-ordered)
  (doseq [survey (keys @response-lookup)]
    (doseq [q (.questions survey)]
      (when-not (.freetext q)
        (doseq [opt (vals (.options q))]
          (is (= (qc.response-util/getOrdered q opt)
                 (-> (map #(.getCid %) (sort-by #(.getSourceRow %) (vals (.options q))))
                     (.indexOf (.getCid opt))
                     (inc))))
          )
        )
      )
    )
  )

(deftest test-max-entropy
  (println 'test-max-entropy)
  (doseq [[survey responses] (seq @response-lookup)]
    (println (.sourceName survey))
    (is (>= (QCMetrics/getMaxPossibleEntropy survey)
            (QCMetrics/surveyEntropy survey responses)))
      )
  )