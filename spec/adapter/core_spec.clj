(ns adapter.core-spec
  (:import (server Response Request))
  (:require [speclj.core :refer :all]
            [adapter.core :as adapter]))

(defn mock-handler
  ([status] (mock-handler status {"Content-Length" 5} "Cake"))
  ([status headers body]
   (fn [request]
     {:status status
      :headers headers
      :body body})))

(defn mock-request [path action]
  (Request. path (clojure.string/upper-case (name action))))


(describe "wrap-ring"
          (it "ringifies a jpetty request invokes it with given handler then de-ringifies response"
              (let [jpetty-request (mock-request "/" :get)
                    ring-handler (mock-handler 200 {"Content-Length" "10"} "pie is good")
                    jpetty-response (.handle (adapter/wrap-ring ring-handler)
                                      jpetty-request)]
                (should= 200 (.getStatusCode jpetty-response))
                (should= "10" (.getHeader jpetty-response "Content-Length"))
                (should= "pie is good" (String. (.getBody jpetty-response))))))
