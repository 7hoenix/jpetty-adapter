(ns ttt-adapter.core
  (:import (server Server WrappedServerSocket Handler Request Response ApplicationBuilder)
           (java.net ServerSocket))
  (:require [clojure.string :as string]))

(defn- get-bytes-from-string [body]
  (-> (java.lang.StringBuilder. body)
      (.toString)
      (.getBytes)))

(defn- capitalize-header-key [header-key]
  (->> (string/split (name header-key) #"\b")
       (map string/capitalize)
       string/join))

(defn- prepare-action [action]
  (-> action
      string/lower-case
      keyword))

(defn ringify [request]
  {:server-port 5000
   :server-name "jpetty"
   :remote-addr "localhost"
   :uri (.getPath request)
   :query-string (.getPath request)
   :scheme :http
   :request-method (prepare-action
                     (.getAction request))
   :headers (.getHeaders request)
   :body (java.io.ByteArrayInputStream.
           (.getBytes
             (.getBody request)))})

(defn- add-header [jpetty-response header-key header-value]
  (.setHeader jpetty-response
              (capitalize-header-key header-key)
              (str header-value)))

(defn- add-headers [jpetty-response ring-response]
  (reduce-kv add-header
             jpetty-response
             (:headers ring-response)))

(defn body-in-bytes [body]
  (cond
    (= (type body) java.io.File) (slurp body)
    (= (type body) String) body))

(defn de-ringify [ring-response]
  (-> (Response. (:status ring-response))
      (add-headers ring-response)
      (.setBody
        (-> (:body ring-response)
            body-in-bytes
            get-bytes-from-string))))

(defn wrap-ring [handler]
  (reify Handler
    (handle [this request]
      (-> request
          ringify
          handler
          de-ringify))))

(defn run-jpetty [application]
  (let [ring-application (.build
                           (ApplicationBuilder/setHandler
                             (wrap-ring application)))
        wrapped-server-socket (WrappedServerSocket. (ServerSocket. 5000)
                                                    ring-application)
        server (Server. wrapped-server-socket)]
    (.run server)))
