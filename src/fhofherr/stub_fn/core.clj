(ns fhofherr.stub-fn.core)


(defn- find-fn-param-syms
  "Find all symbols to which function parameter values are bound."
  [fn-params]
  (letfn [(search-syms [[x & xs] found-syms]
            (cond
              (= x '&) (recur xs found-syms)
              (keyword? x) (recur xs found-syms)
              (symbol? x) (recur xs (conj found-syms x))
              (map? x) (let [ys (as-> x $
                                    (get $ :keys (keys $))
                                    (apply conj $ xs))]
                         (recur ys found-syms))
              (sequential? x) (let [ys (apply conj x xs)]
                                (recur ys found-syms))
              :else found-syms))]
    (search-syms (seq fn-params) [])))


(defmacro stub-fn
  [fn-params & fn-body]
  (let [param-syms (#'find-fn-param-syms fn-params)]
    `(let [stub-invocations# (ref [])
           stubbed-fn# (fn [~@fn-params]
                         (let [invocation-args# (into {}
                                                      [~@(for [s param-syms]
                                                           `['~s ~s])])
                               return-value# (do ~@fn-body)
                               invocation# {::invocation-args invocation-args#
                                            ::return-value return-value#}]
                           (dosync
                             (alter stub-invocations# conj invocation#))
                           return-value#))]
       (vary-meta stubbed-fn# assoc ::stub-fn stub-invocations#))))

(defn stub-info
  [stubbed-fn]
  @(-> stubbed-fn meta ::stub-fn))


(defn- filter-by-args
  [stub-infos expected-args]
  (filter #(-> % ::invocation-args (= expected-args)) stub-infos))


(defn invoked?
  [stubbed-fn & {:keys [times args] :or {times 1}}]
  (let [invocations (as-> stubbed-fn $
                      (stub-info $)
                      (if args (filter-by-args $ args) $))]
    (-> invocations count (= times))))
