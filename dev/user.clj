(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as t]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]))


(defn- do-test
  []
  (t/run-all-tests #"fhofherr\.stub-fn\.test\..+-test"))

(defn run-tests
  []
  (refresh :after 'user/do-test))
