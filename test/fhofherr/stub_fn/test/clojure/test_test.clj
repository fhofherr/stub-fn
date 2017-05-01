(ns fhofherr.stub-fn.test.clojure.test-test
  (:require [clojure.test :refer :all]
            [fhofherr.stub-fn.clojure.test :as stub]))


(deftest verify-invocations-using-test-is
  (let [f (stub/stub-fn f [])]
    (f)))
