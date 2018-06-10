(ns fhofherr.stub-fn.test.clojure.test-test
  (:require [clojure.test :refer :all]
            [fhofherr.stub-fn.clojure.test :refer [stub-fn stub-protocol]]))

(deftest verify-invocations-using-test-is
  (let [f (stub-fn f [])]
    (f)
    (is (invoked? f))
    (is (invoked? f :times 1)))

  (let [f (stub-fn f [x])]
    (f 1)
    (is (invoked? f :times 1 :args {'x 1}))))

(defprotocol StubMe
  (some-method [this]))

(deftest verify-protocol-invocations-using-test-is
  (let [sp (stub-protocol
             StubMe
             (some-method [this]))]
    (some-method sp)
    (is (invoked? sp :method 'some-method :args {'this sp}))))
