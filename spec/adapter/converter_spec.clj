(ns adapter.converter-spec
  (:import (server Response Request))
  (:require [speclj.core :refer :all]
            [adapter.converter :as converter]))

(defn mock-ring-response
  ([status] (mock-ring-response status {}))
  ([status headers] (mock-ring-response status headers ""))
  ([status headers body]
   {:status status
    :body body
    :headers headers}))

(defn mock-request [path action]
  (Request. path (clojure.string/upper-case (name action))))

(describe "ringify"
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
                (should-be-a java.io.InputStream (:body ringified-request)))))

(describe "de-ringify"
          (it "converts a ring response map into a jpetty response object"
              (let [mock-response (mock-ring-response 200 {:content-length 5} "cake")
                    jpetty-response (converter/de-ringify mock-response)]
                (should= 200 (.getStatusCode jpetty-response))
                (should= "5" (.getHeader jpetty-response "Content-Length"))
                (should= "cake" (String. (.getBody jpetty-response))))))

(describe "body-in-bytes"
          (it "can read a file and convert its contents to bytes."
              (let [file1 (clojure.java.io/file "spec/adapter/fixtures/file1.txt")
                    body-in-bytes (converter/body-in-bytes file1)]
                (should= "this file has content\n" (String. body-in-bytes))))
          (it "can read a string and convert it to bytes"
              (let [body-in-bytes (converter/body-in-bytes "great content")]
                (should= "great content" (String. body-in-bytes)))))

(describe "prepare-query-string"
          (it "can read a map of parameters and return a query string"
              (let [params {"arg1" "val1" "arg2" "val2"}
                    result (converter/prepare-query-string params)]
                (should= "arg1=val1&arg2=val2" result))))
