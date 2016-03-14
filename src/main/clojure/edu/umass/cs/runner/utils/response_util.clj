(comment

(ns
  ^{:author etosch
    :doc "Utilities for manipulating survey responses for data analysis."}
  edu.umass.cs.runner.utils.response-util
  (:import
    (edu.umass.cs.surveyman.analyses SurveyResponse IQuestionResponse OptTuple)
    (edu.umass.cs.surveyman.input AbstractParser)
    (edu.umass.cs.surveyman.qc.respondents RandomRespondent RandomRespondent$AdversaryType QCMetrics)
    (edu.umass.cs.surveyman.survey Survey Question SurveyDatum)
    (java.util List Map))
  )

(defn find-first
  [fn some-seq]
  (let [[hd & tl] some-seq]
    (if (and some-seq (fn hd))
      hd
      (recur fn tl)
      )
    )
  )

(defrecord Response [^String srid
                     ^List opts
                     ^Integer indexSeen])


(defn get-random-survey-responses
  "Returns n unique random RandomRespondents for survey. Default profile is RandomRespondent$AdversaryType/UNIFORM."
  ([^Survey survey n]
    (get-random-survey-responses survey n RandomRespondent$AdversaryType/UNIFORM))
  ([^Survey survey n ^RandomRespondent$AdversaryType adversary]
    (clojure.core/repeatedly n #(RandomRespondent. survey adversary)))
  )


(defn get-true-responses
  "Takes an SurveyResponse and removes \"responses\" that are actually ad hoc metadata tagged with the id \"q_-1_-1\". "
  [^SurveyResponse sr]
  (try
    (->> (.getResponses sr)
      (remove #(= "q_-1_-1" (.quid (.getQuestion %))))
      (remove nil?))
    (catch Exception e (do (.printStackTrace e)
                         (println sr)))
    )
  )


(defn make-ans-map
  "Takes each question and returns a map from questions to a list of question responses.
   The survey response id is attached as metadata."
  [surveyResponses]
  (let [answers (for [^SurveyResponse sr surveyResponses]
                  (apply merge
                    (for [^IQuestionResponse qr (.getResponses sr)]
                      {(.getQuestion qr) (list (Response. (.srid sr)
                                                 (map (fn [opt] (.c ^OptTuple opt)) (.getOpts qr))
                                                 (.getIndexSeen qr)))
                       }
                      )
                    )
                  )]
    (reduce #(merge-with concat %1 %2) {} answers))
  )

(defn convertToOrdered
  "Returns a map of cids (String) to integers for use in ordered data."
  [^Question q]
  (into {} (zipmap (map #(.getCid %) (sort-by #(.getSourceRow %) (vals (.options q))))
             (iterate inc 1))
    )
  )


(defn getOrdered
  "Returns an integer corresponding to the ranked order of the option."
  [^Question q ^SurveyDatum opt]
  (let [m (convertToOrdered q)]
    (assert (contains? m (.getCid opt))
      (clojure.string/join "\n" (list (.getCid opt) m
                                  (into [] (map #(.getCid %) (vals (.options q)))))))
    (get m (.getCid opt))
    )
  )


(defn align-by-srid
  [l1 l2]
  (doall
    (loop [pointer l1
           l1sorted '()
           l2sorted '()]
      (if (empty? pointer)
        [l1sorted l2sorted]
        (let [matched (find-first #(= (:srid %) (:srid (first pointer))) l2)]
          (if (nil? matched)
            (recur (rest pointer) l1sorted l2sorted)
            (recur (rest pointer) (cons (first pointer) l1sorted) (cons matched l2sorted))
            )
          )
        )
      )
    )
  )

(defn get-ids-that-answered-option
  "Returns the set of srids corresponding to respondents who answered a given question with a specific response."
  [ansMap ^Question q1 ^SurveyDatum opt1]
  (->> (ansMap q1)
    (filter #(= opt1 (first (:opts %))))
    (flatten)
    (map #(:srid %))
    (set))
  )

(defn opt-list-by-index
  "Returns the option list for a question ordered by how it was entered in the csv."
  [^Question q]
  (sort #(< (.getSourceRow ^SurveyDatum %1) (.getSourceRow ^SurveyDatum %2)) (vals (.options q)))
  )

(defmulti get-last-q #(type %))

(defmethod get-last-q List [qrlist]
  (->> qrlist
    (sort (fn [^IQuestionResponse qr1
               ^IQuestionResponse qr2]
            (> (.getIndexSeen qr1) (.getIndexSeen qr2))
            )
      )
    (first)
    (.getQuestion))
  )

(defmethod get-last-q SurveyResponse [sr]
  (get-last-q (.getResponses sr))
  )


(defn freetext?
  "Returns whether a question response is of type freetext."
  [^IQuestionResponse qr]
  (.freetext (.getQuestion qr))
  )


(defn make-frequencies
  "Returns a map from quid to a map of options to counts."
  [responses]
  (reduce #(merge-with (fn [m1 m2] (merge-with + m1 m2)) %1 %2)
    (for [^SurveyResponse sr responses]
      (apply merge (for [^IQuestionResponse qr (get-true-responses sr)]
                     {(.quid (.getQuestion qr))
                      (apply merge
                        (for [^SurveyDatum c (map #(.c ^OptTuple %) (.getOpts qr))]
                          {(.getCid c) 1}
                          )
                        )
                      })
        )
      )
    )
  )

(defmulti make-probabilities
  "Returns empirical probabilities for each question and its response."
  (fn [_ thing] (type thing))
  )



(defmethod make-probabilities List
  [^Survey s responses]
  (make-probabilities s (make-frequencies responses))
  )


(defn get-variant
  "Returns the Question answered in the SurveyResponse that is equivalent to the input Question."
  [^Question q ^SurveyResponse sr]
  (when-not (= (.quid q) AbstractParser/CUSTOM_ID)
    (let [variants (set (.getVariants q))]
      (->> (.getResponses sr)
        (map #(.getQuestion %))
        (filter #(contains? variants %))
        (first)
        )
      )
    )
  )


(defn survey-response-contains-answer
  "Returns boolean for whether a particular SurveyResponse contains a question that has this SurveyDatum as an answer."
  [^SurveyResponse sr ^SurveyDatum c]
  (contains? (set (flatten (map (fn [^IQuestionResponse qr] (map #(.c %) (.getOpts qr))) (get-true-responses sr))))
    c
    )
  )

  )
