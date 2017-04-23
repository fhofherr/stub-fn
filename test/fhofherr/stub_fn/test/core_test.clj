(ns fhofherr.stub-fn.test.core-test
  (:require [clojure.test :refer :all]
            [fhofherr.stub-fn.core :as core]))


(deftest find-fn-param-syms

  (testing "positional parameters"
    (is (= [] (#'core/find-fn-param-syms [])))
    (is (= '[a] (#'core/find-fn-param-syms '[a]))))

  (testing "varargs"
    (is (= '[a] (#'core/find-fn-param-syms '[& a])))
    (is (= '[a b] (#'core/find-fn-param-syms '[a & b])))
    (is (= '[a b c d] (#'core/find-fn-param-syms '[a & [b c :as d]]))))

  (testing "destructuring maps"
    (is (= '[a b] (#'core/find-fn-param-syms '[{a :a b :b}])))
    (is (= '[a b] (#'core/find-fn-param-syms '[{:keys [a b]}])))
    (is (= '[a b] (#'core/find-fn-param-syms '[{:keys [a b] :or {a c b d}}])))))
