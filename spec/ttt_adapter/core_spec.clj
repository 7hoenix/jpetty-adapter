(ns ttt-adapter.core-spec
  (:import (server Response Request))
  (:require [speclj.core :refer :all]
            [ttt-adapter.core :as adapter]))

(defn mock-handler
  ([status] (mock-handler status {"Content-Length" 5} "Cake"))
  ([status headers body]
   (fn [request]
     {:status status
      :headers headers
      :body body})))

(defn mock-request [path action]
  (let [request (Request. path (clojure.string/upper-case (name action)))]
    (println (.getAction request))
    request))

(defn mock-ring-response
  ([status] (mock-ring-response status {}))
  ([status headers] (mock-ring-response status headers ""))
  ([status headers body]
   {:status status
    :body body
    :headers headers}))

(describe "ringify"
          (it "converts a jpetty request object into a ring request map"
              (let [jpetty-request (-> (mock-request "/" :get)
                                       (.setBody "hi")
                                       (.setHeader "some-header" "boom")
                                       (.setHeader "some-other-header" "ok"))
                    ringified-request (adapter/ringify jpetty-request)]
                (should= "/" (:uri ringified-request))
                (should= :get (:request-method ringified-request))
                (should= "hi" (slurp (:body ringified-request)))
                (should= {"some-header" "boom"
                          "some-other-header" "ok"}
                         (:headers ringified-request)))))

(describe "de-ringify"
          (it "converts a ring response map into a jpetty response object"
              (let [mock-response (mock-ring-response 200 {:content-length 5} "cake")
                    jpetty-response (adapter/de-ringify mock-response)]
                (should= 200 (.getStatusCode jpetty-response))
                (should= "5" (.getHeader jpetty-response "Content-Length"))
                (should= "cake" (String. (.getBody jpetty-response))))))

(describe "wrap-ring"
          (it "ringifies a jpetty request invokes it with given handler then de-ringifies response"
              (let [jpetty-request (mock-request "/" :get)
                    ring-handler (mock-handler 200 {"Content-Length" "10"} "pie is good")
                    jpetty-response (.handle (adapter/wrap-ring ring-handler)
                                      jpetty-request)]
                (should= 200 (.getStatusCode jpetty-response))
                (should= "10" (.getHeader jpetty-response "Content-Length"))
                (should= "pie is good" (String. (.getBody jpetty-response))))))

(describe "body-in-bytes"
          (it "can read a file and convert its contents to bytes."
              (let [file1 (clojure.java.io/file "spec/ttt_adapter/fixtures/file1.txt")
                    body-in-bytes (adapter/body-in-bytes file1)]
                (should= "this file has content\n" (String. body-in-bytes))))
          (it "can read a string and convert it to bytes"
              (let [body-in-bytes (adapter/body-in-bytes "great content")]
                (should= "great content" (String. body-in-bytes)))))
