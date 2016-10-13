(ns ttt-adapter.core
  (:import (server Server WrappedServerSocket Handler Request Response ApplicationBuilder)
           (java.net ServerSocket)
           (java.io ByteArrayInputStream))
  (:require [ttt-adapter.converter :as converter]
            [clojure.string :as string]
            [clojure.tools.trace :as t]
            [clojure.java.io :as io]))

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

(defn prepare-query-string [params]
  (reduce (fn [query-string [param-key param-value]]
               (if (= query-string "")
                 (str query-string param-key "=" param-value)
                 (str query-string "&" param-key "=" param-value)))
               ""
               params))

(defn- prepare-body [body]
  (ByteArrayInputStream.
    (.getBytes body)))

(defn ringify [request]
  {:server-port 5000
   :server-name "jpetty"
   :remote-addr "localhost"
   :uri (.getPath request)
   :query-string (prepare-query-string
                   (.getParams request))
   :scheme :http
   :request-method (prepare-action
                     (.getAction request))
   :headers (.getHeaders request)
   :body (prepare-body
           (.getBody request))})

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
    (.exists (io/as-file body)) (slurp body)
    :else body))

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
                           (ApplicationBuilder/forHandler
                             (wrap-ring application)))
        wrapped-server-socket (WrappedServerSocket. (ServerSocket. 5000)
                                                    ring-application)
        server (Server. wrapped-server-socket)]
    (.run server)))
