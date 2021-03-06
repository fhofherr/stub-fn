(ns fhofherr.stub-fn.core
  "Implements the core functionality of `fhofherr/stub-fn`.

  Users of `clojure.test` should not use this namespace directly. Instead
  they should use [[fhofherr.stub-fn.clojure.test]].

  The core functionality is implemented by the [[stub-fn]] macro. It creates
  an anonymous function which can be used in place of any other function. The
  stubbed function tracks information about its invocations under the
  `::stub-info` key in its meta data.

  Starting with version `v0.2.0` `fhofherr/stub-fn` provides a `stub-protocol`
  macro which reifys a protocol using stub functions."
  (:require [clojure.pprint :refer [pprint]]))

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
  [fn-name]
  {::fn-name fn-name
   ::invocations (ref [])})

(defn- assoc-stub-info
  [stubbed-fn stub-info]
  (vary-meta stubbed-fn assoc ::stub-info stub-info))

(defn get-stub-info
  "Get all available information about the `stubbed-fn`.

  Returns `nil` if the `stubbed-function` is not a stub.

  Arguments:

  *   `stubbed-fn`: the stubbed function"
  [stubbed-fn]
  (-> stubbed-fn meta ::stub-info))

(defn get-stub-fn-name
  "Get `stubbed-fn`'s name.

  Returns `nil` if the `stubbed-function` is not a stub.

  Arguments:

  *   `stubbed-fn`: the stubbed function"
  [stubbed-fn]
  (some-> stubbed-fn
          get-stub-info
          ::fn-name
          name))

(defn get-stub-invocations
  "Get all available information about `stubbed-fn`'s invocations.

  Returns `nil` if the `stubbed-function` is not a stub.

  Arguments:

  *   `stubbed-fn`: the stubbed function"
  [stubbed-fn]
  (some-> stubbed-fn
          get-stub-info
          ::invocations
          deref))

(defn stub-fn?
  "Check if the function `f` is a stub.

  Return `true` if `f` is a stub, or `false` otherwise.

  Arguments:

  *   `f`: a function that may be a stub"
  [f]
  {:pre [(fn? f)]}
  (as-> f $
    (get-stub-info $)
    ((complement nil?) $)))

(defn- register-stub-invocation
  [stub-info args return-value]
  (let [invocation {:args args
                    :return-value return-value}]
    (dosync
     (update-in stub-info
                [::invocations]
                #(alter % conj invocation)))))

(defn- compile-invocation-report
  [stubbed-fn]
  {:pre [(stub-fn? stubbed-fn)]}
  (let [stub-info (get-stub-invocations stubbed-fn)
        total-invocations (count stub-info)
        n-invocations-by-args (->> stub-info
                                   (group-by :args)
                                   (map (fn [[k v]] [k (count v)]))
                                   (into {}))]
    {:total-invocations total-invocations
     :n-invocations-by-args n-invocations-by-args}))

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

  *   `fn-name`: symbol identifying the function stub
  *   `fn-args`: argument vector of the stubbed function
  *   `fn-body`: body of the function stub (optional)."
  [fn-name fn-args & fn-body]
  (let [args (or fn-args [])
        arg-syms (#'find-fn-arg-syms args)
        args-collector `(into {} [~@(for [s arg-syms] `['~s ~s])])]
    `(let [stub-info# (#'make-stub-info '~fn-name)
           stubbed-fn# (fn ~fn-name [~@args]
                         (let [invocation-args# ~args-collector
                               return-value# (do ~@fn-body)]
                           (#'register-stub-invocation stub-info#
                                                       invocation-args#
                                                       return-value#)
                           return-value#))]
       (#'assoc-stub-info stubbed-fn# stub-info#))))

(defn- emit-stub-fns
  [fn-sigs]
  (into {}
        (for [[fn-name fn-args & fn-body] fn-sigs]
          `['~fn-name (stub-fn ~fn-name ~fn-args ~@fn-body)])))

(defn emit-proto-fn-impl
  [fn-name fn-args stub-fns-sym]
  `(~fn-name ~fn-args (let [f# (get ~stub-fns-sym '~fn-name)]
                        (f# ~@fn-args))))

(defmacro stub-protocol
  "Stub a protocol using stub functions.

  Uses [[stub-fn]] to stub the listed protocol methods. Protocol
  implementations may be partial. It is therefore not necessary to list
  all protocol methods when stubbing it. Just the ones really necessary
  are enough.

  As with [[stub-fn]] you should never store a stubed protocol in a global
  context. Something like the following is a *bad* idea:

  ```clojure
  (def (stub-protocol Protocol ...))
  ```

  Arguments:

  *   `proto`: the protocol to stub
  *   `sigs`: the method signatures to stub"
  [proto & sigs]
  (let [stub-fns-sym (gensym "stub-fns")]
    `(let [~stub-fns-sym ~(#'emit-stub-fns sigs)
           stub-proto# (reify ~proto
                         ~@(for [[fn-name fn-args] sigs]
                             (#'emit-proto-fn-impl fn-name fn-args stub-fns-sym)))]
       (vary-meta stub-proto# assoc ::stub-fns ~stub-fns-sym))))

(defn- filter-by-args
  [stub-infos expected-args]
  (filter #(-> % :args (= expected-args)) stub-infos))

(defn- verify-fn-invocations
  [stubbed-fn {:keys [times args] :or {times 1}}]
  {:pre [(stub-fn? stubbed-fn)]}
  (let [invocation-report (compile-invocation-report stubbed-fn)
        expected {:times times :args args}
        actual {:times (if args
                         (get-in invocation-report
                                 [:n-invocations-by-args args]
                                 0)
                         (:total-invocations invocation-report 0))
                :args args}
        type (if (= expected actual) ::success ::failure)
        verification-report {::type type
                             ::fn-name (get-stub-fn-name stubbed-fn)
                             ::expected expected
                             ::actual actual
                             ::invocation-report invocation-report}]
    verification-report))

(defn- verify-method-invocations
  [stub {:keys [method] :as opts}]
  (when-not method
    (throw (IllegalArgumentException. "No method specified")))
  (if-let [f (-> stub meta (get-in [::stub-fns method]))]
    (verify-fn-invocations f opts)
    (throw (IllegalArgumentException.
             (format "Method %s is not stubbed" method)))))

(defn verify-invocations
  "Check if the `stubbed` function or protocol has been invoked.

  If the `times` keyword argument is given `verify-invocations` will test if
  a stub has been invoked the expected number of times.

  If the `args` keyword argument is given `verify-invocations` will test if
  a stub has been invoked with the expected arguments.

  If both `args` and `times` are given `verify-invocations` checks if the
  stub has been invoked the expected number of times with the expected
  arguments. This means that the stub may have been additionally invoked
  with different arguments an arbitrary number of times.

  Arguments:

  *   `stub`: a stubbed function or protocol.

  Keyword arguments:

  *   `times`: expected number of invocations. Defaults to 1.
  *   `args`: map of expected arguments to the function invocation.
  *   `method`: name of the protocol method expected to be invoked. Only
  necessary if `stubbed` is not a function. Ignored otherwise."
  [stub & {:as opts}]
  (if (fn? stub)
    (verify-fn-invocations stub opts)
    (verify-method-invocations stub opts)))

(defn- pprint-str
  [x]
  (with-out-str (pprint x)))

(defn- indent-lines
  [n s]
  (let [indent-str (->> " "
                        (repeat n)
                        (apply str))]
    (clojure.string/replace s
                            #"[^\r\n]+(\r|\n|\r\n)"
                            (str indent-str "$0"))))

(defn- format-invocation-report
  [stub-invocations]
  (->> stub-invocations
       :n-invocations-by-args
       (map (fn [[args times]] {:args args :times times}))
       (map pprint-str)
       (apply str)
       (indent-lines 4)))

(defn- format-fn-args
  [fn-args]
  (if fn-args
    (->> fn-args
         pprint-str
         (indent-lines 4)
         (format  " with arguments:\n\n%s"))
    "."))

(defn- format-report-type
  [report-type]
  (if (= report-type ::success)
    "Success!"
    "Failure!"))

(defn format-verification-report
  "Format the verification report returned by [[verify-invocations]].

  Arguments:

  *   `verification-report`: the verification report to format.
  *   `add-type`: whether to add the type of the report, which is either
      'Success!' or 'Failure!'. Default `false`."
  [verification-report & {:keys [add-type] :or {add-type false}}]
  (let [n-total (get-in verification-report
                        [::invocation-report :total-invocations])
        n-expected (get-in verification-report [::expected :times])
        args-expected (get-in verification-report [::expected :args])
        n-actual (get-in verification-report [::actual :times])
        args-actual (get-in verification-report [::actual :args])
        type-str (if add-type
                   (-> verification-report ::type format-report-type (str "\n"))
                   "")
        fn-name-str (-> verification-report ::fn-name name)
        expected-str (format "Expected function '%s' to be called %d times%s"
                             fn-name-str
                             n-expected
                             (format-fn-args args-expected))
        actual-str (format "It has been called %d times%s"
                           n-actual
                           (format-fn-args args-actual))
        invocations-str (if (> n-total 0)
                          (format "\nIn total there were %d invocations of this stub:\n\n%s"
                                  n-total
                                  (-> verification-report
                                      ::invocation-report
                                      format-invocation-report))
                          "\nThere were no interactions with this stub!")]
    (str type-str
         expected-str
         "\n"
         actual-str
         invocations-str)))

(defn success?
  [verification-report]
  (-> verification-report ::type (= ::success)))

(defn invoked?
  "Check if a stubbed function or protocol method has been invoked
  successfully.

  Returns `true` if the stub was invoked as expected.

  See [[verify-invocations]] for details about arguments."
  [stub & {:as opts}]
  (as-> stub $
    (apply verify-invocations $ (flatten (seq opts)))
    (success? $)))
