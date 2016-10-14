(ns adapter.core
  (:import (server Server WrappedServerSocket Handler Request Response ApplicationBuilder)
           (java.net ServerSocket)
           (java.io ByteArrayInputStream))
  (:require [clojure.string :as string]
            [adapter.converter :as converter]
            [clojure.tools.trace :as t]
            [clojure.java.io :as io]))

(defn wrap-ring [handler]
  (reify Handler
    (handle [this request]
      (-> request
          converter/ringify
          handler
          converter/de-ringify))))

(defn run-jpetty
  ([application] (run-jpetty application {:port 5000}))
  ([application args]
   (let [ring-application (.build
                           (ApplicationBuilder/forHandler
                             (wrap-ring application)))
        wrapped-server-socket (WrappedServerSocket. (ServerSocket. (:port args))
                                                    ring-application)
        server (Server. wrapped-server-socket)]
    (.run server))))
