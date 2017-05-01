(ns fhofherr.stub-fn.test.clojure.test-test
  (:require [clojure.test :refer :all]
            [fhofherr.stub-fn.clojure.test :refer [stub-fn]]))


(deftest verify-invocations-using-test-is
  (let [f (stub-fn f [])]
    (f)
    (is (invoked? f))
    (is (invoked? f :times 1)))

  (let [f (stub-fn f [x])]
    (f 1)
    (is (invoked? f :times 1 :args {'x 1}))))
