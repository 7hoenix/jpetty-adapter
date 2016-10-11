(defproject jpetty-ring/adapter "0.1.0-SNAPSHOT"
  :description "Adapter to make jpetty server work with ring handler"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [jpetty/server "0.1.0-SNAPSHOT"]]
  :profiles {:dev {:dependencies [[speclj "3.3.2"]]}}
  :plugins [[speclj "3.3.2"]]
  :test-paths ["spec"])
