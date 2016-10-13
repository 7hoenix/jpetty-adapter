(ns ttt-adapter.converter
  (:import (server Server WrappedServerSocket Handler Request Response ApplicationBuilder)
           (java.net ServerSocket)
           (java.io ByteArrayInputStream))
  (:require [clojure.string :as string]
            [clojure.tools.trace :as t]
            [clojure.java.io :as io]))

(defn- prepare-action [action]
  (-> action
      string/lower-case
      keyword))

(defn- prepare-query-string [params]
  (reduce (fn [query-string [param-key param-value]]
               (if (= query-string "")
                 (str query-string param-key "=" param-value)
                 (str query-string "&" param-key "=" param-value)))
               ""
               params))

(defn- prepare-body [body]
  (ByteArrayInputStream.
    (.getBytes body)))

(defn ringify
  ([request] (ringify request 5000))
  ([request server-port]
   {:server-port server-port
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
            (.getBody request))}))
