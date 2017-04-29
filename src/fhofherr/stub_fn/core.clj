(ns fhofherr.stub-fn.core)


(defn- find-fn-arg-syms
  "Find all symbols to which function parameter values are bound."
  [fn-args]
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
    (search-syms (seq fn-args) [])))


(defn- make-stub-info
  []
  (ref []))


(defn- assoc-stub-info
  [stubbed-fn stub-info]
  (vary-meta stubbed-fn assoc ::stub-info stub-info))


(defn get-stub-info
  [stubbed-fn]
  @(-> stubbed-fn meta ::stub-info))


(defn- register-stub-invocation
  [stub-info args return-value]
  (let [invocation {::invocation-args args
                    ::return-value return-value}]
    (dosync (alter stub-info conj invocation))))


(defmacro stub-fn
  [fn-name fn-args & fn-body]
  (let [args (or fn-args [])
        arg-syms (#'find-fn-arg-syms args)
        args-collector `(into {} [~@(for [s arg-syms] `['~s ~s])])]
    `(let [stub-info# (#'make-stub-info)
           stubbed-fn# (fn fn-name [~@args]
                         (let [invocation-args# ~args-collector
                               return-value# (do ~@fn-body)]
                           (#'register-stub-invocation stub-info#
                                                       invocation-args#
                                                       return-value#)
                           return-value#))]
       (#'assoc-stub-info stubbed-fn# stub-info#))))



(defn- filter-by-args
  [stub-infos expected-args]
  (filter #(-> % ::invocation-args (= expected-args)) stub-infos))


(defn invoked?
  [stubbed-fn & {:keys [times args] :or {times 1}}]
  (let [invocations (as-> stubbed-fn $
                      (get-stub-info $)
                      (if args (filter-by-args $ args) $))]
    (-> invocations count (= times))))
