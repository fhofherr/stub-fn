(ns fhofherr.stub-fn.test.core-test
  (:require [clojure.test :refer :all]
            [fhofherr.stub-fn.core :as core]))

(deftest find-fn-arg-syms

  (testing "positional parameters"
    (is (= [] (#'core/find-fn-arg-syms [])))
    (is (= '[a] (#'core/find-fn-arg-syms '[a]))))

  (testing "varargs"
    (is (= '[a] (#'core/find-fn-arg-syms '[& a])))
    (is (= '[a b] (#'core/find-fn-arg-syms '[a & b])))
    (is (= '[a b c d] (#'core/find-fn-arg-syms '[a & [b c :as d]]))))

  (testing "destructuring maps"
    (is (= '[a b] (#'core/find-fn-arg-syms '[{a :a b :b}])))
    (is (= '[a b] (#'core/find-fn-arg-syms '[{:keys [a b]}])))
    (is (= '[a b] (#'core/find-fn-arg-syms '[{:keys [a b] :or {a c b d}}])))))

(deftest stubbing-functions

  (testing "returns nil if no body is given"
    (let [f (core/stub-fn f [])
          result (f)]
      (is (nil? result))))

  (testing "all args are available within the function"
    (let [f (core/stub-fn f [a b c] (+ a b c))]
      (is (= (+ 1 2 3) (f 1 2 3))))

    (let [f (core/stub-fn f [a b c]
                          (+ a b c)
                          (- a b c))]
      (is (= (- 1 2 3) (f 1 2 3))))))

(deftest identify-stubs
  (is (true? (core/stub-fn? (core/stub-fn f []))))
  (is (false? (core/stub-fn? (fn [])))))

(deftest checking-function-invocation

  (testing "all invocations are registered"
    (let [f (core/stub-fn f [])]
      (f)
      (is (core/invoked? f)))

    (let [never-invoked (core/stub-fn never-invoked [])]
      (is (not (core/invoked? never-invoked)))))

  (testing "the number of expected invocatios can be specified"
    (let [f (core/stub-fn f [])]
      (f)
      (is (core/invoked? f :times 1))
      (is (not (core/invoked? f :times 2)))
      (f)
      (is (core/invoked? f :times 2))))

  (testing "the expected arguments can be specified"
    (let [f (core/stub-fn f [x y])]
      (f 1 2)
      (is (core/invoked? f :args {'x 1 'y 2}))
      (is (not (core/invoked? f :args {'x 3 'y 4})))
      (is (not (core/invoked? f :args {'a 3 'b 4})))))

  (testing "invocation counts relate to the expected arguments"
    (let [f (core/stub-fn f [x y])]
      (f 1 2)
      (is (core/invoked? f :args {'x 1 'y 2} :times 1))
      (is (not (core/invoked? f :args {'x 1 'y 2} :times 2)))
      (f 1 2)
      (is (core/invoked? f :args {'x 1 'y 2} :times 2))
      (f 3 4)
      (is (core/invoked? f :args {'x 1 'y 2} :times 2))
      (is (core/invoked? f :args {'x 3 'y 4} :times 1))
      (is (not (core/invoked? f :args {'x 3 'y 4} :times 2))))))

(defprotocol StubMe
  (first-method [_ x])
  (second-method [_]))

(deftest stubbing-protocols

  (testing "stub all methods of a protocol"
    (let [stubbed-protocol (core/stub-protocol
                             StubMe
                             (first-method [this x] x)
                             (second-method [this] "second"))
          first-result (first-method stubbed-protocol "param")
          second-result (second-method stubbed-protocol)]
      (is (= first-result "param"))
      (is (= second-result "second"))
      (is (core/invoked? stubbed-protocol
                         :method 'first-method
                         :args {'this stubbed-protocol
                                'x "param"}
                         :times 1))
      (is (core/invoked? stubbed-protocol
                         :method 'second-method
                         :args {'this stubbed-protocol}))))

  (testing "stub a protocol partially"
    (let [stubbed-protocol (core/stub-protocol
                             StubMe
                             (first-method [this x] "result"))
          result (first-method stubbed-protocol "param")]
      (is (= result "result"))
      (is (core/invoked? stubbed-protocol
                         :method 'first-method
                         :args {'this stubbed-protocol
                                'x "param"}))))

  (testing "invocation check requires a method to be specified"
    (let [stubbed-protocol (core/stub-protocol
                             StubMe
                             (first-method [this x]))]
      (is (thrown? IllegalArgumentException
                   (core/invoked? stubbed-protocol)))))

  (testing "method must be stubbed"
    (let [stubbed-protocol (core/stub-protocol
                             StubMe
                             (first-method [this x]))]
      (is (thrown? IllegalArgumentException
                   (core/invoked? stubbed-protocol :method 'second-method))))))
