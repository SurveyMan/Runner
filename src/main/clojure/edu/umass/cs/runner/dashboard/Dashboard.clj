(ns edu.umass.cs.runner.dashboard.Dashboard
  (:gen-class
    :name edu.umass.cs.runner.dashboard.Dashboard
    :prefix "-"
    :main false
    :methods [#^{:static true} [run [edu.umass.cs.runner.Record] void]
                               [getPort [] Integer]])
  (:import [edu.umass.cs.surveyman.qc QCMetrics]
           [edu.umass.cs.runner.system BoxedBool])
  (:import edu.umass.cs.surveyman.utils.Slurpie)
  (:import edu.umass.cs.runner.Record)
  (:import edu.umass.cs.runner.system.Parameters)
  (:use ring.adapter.jetty)
  (:use ring.middleware.params)
  (:use ring.util.codec)
  (:use clojure.walk)
  (:require [clojure.data.json :as json])
  )

(def record-data (atom nil))
(def PORT (atom 9000))

(defn get-content-type-for-request
  [uri]
  (condp = (last (clojure.string/split uri #"\\."))
    "js" "application/javascript"
    "css" "application/css"
    ""
    )
  )

(defn handler [{request-method :request-method
                query-string :query-string
                uri :uri
                params :params
                body :body
                :as request}]
  (println (format "request:%s\tquery:%s\turi:%s\tparams:%s\n" request-method query-string uri params))
  (when query-string
    (println (keywordize-keys (form-decode query-string))))
  {:status 200
   :headers {"Content-Type" (if (= :get request-method)
                              (get-content-type-for-request uri)
                              "text/html")
             }
   :body (condp = request-method
           :get (if query-string
                  (let [item (last (clojure.string/split query-string  #"/"))]
                    (println item)
                    (condp = item
                      "survey_data" (json/write-str {"survey" (.jsonize (.survey @record-data))
                                                     "backend" (.name (.backendType @record-data))
                                                     "targetresponses" (-> @record-data
                                                                         (.library)
                                                                         (.props)
                                                                         (.get Parameters/NUM_PARTICIPANTS)
                                                                         )
                                                     "expectedcost" (.expectedCost @record-data)
                                                     "classificationmethod" (.name (.classifier @record-data))
                                                     "record-pointer" (System/identityHashCode @record-data)
                                                     })
                      "response_data" (.jsonizeResponses @record-data)
                      )
                    )
                  (Slurpie/slurp (clojure.string/join "" (rest uri))))
           ;; :post (handle-post uri (keywordize-keys (form-decode (slurp body))))
           )
   }
  )

(defn run
  [^Record record]
  (reset! record-data record)
  (try
    (let [dashboardServer (ring.adapter.jetty/run-jetty
                            handler
                            {:port @PORT :join? false})]
      (println (str "Dashboard server running at http://localhost:" @PORT "/dashboard/Dashboard.html"))
      dashboardServer)
    (catch java.net.BindException e
      (swap! PORT (inc @PORT))
      (run record)))
  )

(defn -getPort [] @PORT)

(defn -run
  [^Record record]
  (run record)
  )

(defn -main
  [& args]
  ;; This is called when we have the Runner running a survey in another process, and we want to monitor it in a
  ;; separate process.
  )