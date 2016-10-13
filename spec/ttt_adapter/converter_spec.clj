(ns ttt-adapter.converter-spec
  (:import (server Response Request))
  (:require [speclj.core :refer :all]
            [ttt-adapter.converter :as converter]))

(defn mock-request [path action]
  (let [request (Request. path (clojure.string/upper-case (name action)))]
    (println (.getAction request))
    request))


(describe "ringify"
          (context "converts a jpetty request object into a ring request map"
                   (it "which include the port on which the request is being handled"
                       (let [jpetty-request (mock-request "/" :get)
                             ringified-request (converter/ringify jpetty-request 4444)]
                         (should= 4444 (:server-port ringified-request))))
                   (it "which include the resolved server name or server IP address"
                       (let [jpetty-request (mock-request "/" :get)
                             ringified-request (converter/ringify jpetty-request 4444)]
                         (should= "jpetty" (:server-name ringified-request))))

                   ; (it "which include the IP address of the client or the last proxy that sent the request"

; :remote-addr T


          (it "converts a jpetty request object into a ring request map"
              (let [jpetty-request (-> (mock-request "/" :get)
                                       (.setBody "hi")
                                       (.setHeader "some-header" "boom")
                                       (.setHeader "some-other-header" "ok"))
                    ringified-request (converter/ringify jpetty-request)]
                (should= "/" (:uri ringified-request))
                (should= :get (:request-method ringified-request))
                (should= "hi" (slurp (:body ringified-request)))
                (should= {"some-header" "boom"
                          "some-other-header" "ok"}
                         (:headers ringified-request))))


          (it "returns a body tag which is a java InputStream"
              (let [jpetty-request (-> (mock-request "/" :get)
                                       (.setBody "hello"))
                    ringified-request (converter/ringify jpetty-request)]
                (should-be-a java.io.InputStream (:body ringified-request))))))





; :uri The request URI (the full path after the domain name).

; :query-string The query string, if present.

; :scheme The transport protocol, either :http or :https.

; :request-method The HTTP request method, which is one of :get, :head, :options, :put, :post, or :delete.

; :headers A Clojure map of lowercase header name strings to corresponding header value strings.

; :body An InputStream for the request body, if present.



; ; :server-name T
