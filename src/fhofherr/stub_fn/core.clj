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
  "Get all available information about the `stubbed-fn`.

  Returns `nil` if the `stubbed-function` is not a stub.

  Arguments:

  * `stubbed-fn`: the stubbed function"
  [stubbed-fn]
  (when-let [stub-info (-> stubbed-fn meta ::stub-info)]
    @stub-info))

(defn stub?
  "Check if the function `f` is a stub.

  Return `true` if `f` is a stub, or `false` otherwise.

  Arguments:

  * `f`: a function that may be a stub"
  [f]
  {:pre [(fn? f)]}
  (as-> f $
       (get-stub-info $)
       ((complement nil?) $)))


(defn- register-stub-invocation
  [stub-info args return-value]
  (let [invocation {::invocation-args args
                    ::return-value return-value}]
    (dosync (alter stub-info conj invocation))))


(defmacro stub-fn
  "Define an anonymous function that can be used as a stub for another function.

  Can be used almost like `fn`. The only difference is, that a function name
  is required. This makes it easier to produce meaningful stack traces and
  failure reports.

  Internally `stub-fn` uses `fn` to create an anonymous function. Various
  information about the function's invocations is stored in a `ref` in the
  function's meta data.

  **Warning**: storing information about function invocations in a `ref` makes
  any stubbed function *stateful*. Avoid to store it in the global context.
  You should *never* need to do something like the following:

  ```clojure
  (def (stub-fn stubbed [] ...))
  ```

  Arguments:

  * `fn-name`: symbol identifying the function stub
  * `fn-args`: argument vector of the stubbed function
  * `fn-body`: body of the function stub (optional).
  "
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
  "Check if the function `stubbed-fn` has been invoked.

  If the `times` keyword argument is given `invoked?` will test if `stubbed-fn`
  has been invoked the expected number of times.

  If the `args` keyword argument is given `invoked?` will test if `stubbed-fn`
  has been invoked with the expected arguments.

  If both `args` and `times` are given `invoked?` checks if the function has
  been invoked the expected number of times with the expected arguments.
  This means that the function may have been additionally invoked with
  different arguments an arbitrary number of times.

  Arguments:

  * `stubbed-fn`: the stubbed function.

  Keyword arguments:

  * `times`: expected number of invocations. Defaults to 1.
  * `args`: map of expected arguments to the function invocation."
  [stubbed-fn & {:keys [times args] :or {times 1}}]
  {:pre [(stub? stubbed-fn)]}
  (let [invocations (as-> stubbed-fn $
                      (get-stub-info $)
                      (if args (filter-by-args $ args) $))]
    (-> invocations count (= times))))
