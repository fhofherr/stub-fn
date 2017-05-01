(defproject fhofherr/stub-fn "0.1.0-SNAPSHOT"
  :description "A macro to stub arbitrary Clojure functions."
  :url "https://github.com/fhofherr/stub-fn"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :plugins [[lein-codox "0.10.3"]
            [lein-cljfmt "0.5.6"]]
  :codox {:namespaces [#"^fhofherr\.stub-fn\."]
          :metadata {:doc/format :markdown}}
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"
                                   :exclusions [org.clojure/clojure]]]}})
