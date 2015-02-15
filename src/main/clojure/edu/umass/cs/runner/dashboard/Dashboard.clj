(ns edu.umass.cs.runner.dashboard.Dashboard
  (:gen-class
    :name edu.umass.cs.runner.dashboard.Dashboard
    :methods [#^{:static true} [run [net.sourceforge.argparse4j.inf.Namespace] void]])
  (:import net.sourceforge.argparse4j.inf.Namespace)
  (:import edu.umass.cs.surveyman.utils.Slurpie)
  (:use ring.adapter.jetty)
  (:use ring.middleware.params)
  (:use ring.util.codec)
  (:use clojure.walk)
  (:require [clojure.data.json :as json])
  )

(def runner-args (atom nil))
(def PORT 9000)

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
                      "survey_data" (json/write-str {"survey" (.get @runner-args "survey")
                                                     "backend" (.get @runner-args "backend")
                                                     })
                      )
                    )
                  (Slurpie/slurp (clojure.string/join "" (rest uri))))
           ;; :post (handle-post uri (keywordize-keys (form-decode (slurp body))))
           )
   }
  )

(defn run
  [^Namespace ns]
  (reset! runner-args ns)
  (ring.adapter.jetty/run-jetty
    handler
    {:port PORT :join? false})
  )

(defn -run
  [^Namespace ns]
  (run ns)
  )