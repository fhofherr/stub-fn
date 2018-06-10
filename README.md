# stub-fn

A macro to stub arbitrary Clojure functions.

## Usage

### Dependency

`stub-fn` `v0.1.0 `is available on [Clojars](https://clojars.org). To
use it in your project add the following to your `project.clj`
dependency vector:

```clojure
[fhofherr/stub-fn "0.1.0"]
```

Starting with version `0.2.0-SNASPSHOT` `stub-fn` has a `deps.edn`. To use
the latest version add something similar to the following to your `deps.edn`:

```clojure
:aliases {:test {:extra-paths ["test"]
                 :extra-deps {fhofherr/stub-fn {:git/url "https://github.com/fhofherr/stub-fn.git"
                                                :sha "<SHA YOU WANT TO USE>"}}}}
```

### Without `clojure.test`

If you do not use
[`clojure.test`](https://clojure.github.io/clojure/clojure.test-api.html)
require the `fhofherr.stub-fn.core` namespace. To stub a function use
the `stub-fn` macro. To verify invocations use either
`verify-invocations` or `invoked?`.

### With `clojure.test`

Users of
[`clojure.test`](https://clojure.github.io/clojure/clojure.test-api.html)
should use the `fhofherr.stub-fn.clojure.test` namespace instead. It
re-exports the `stub-fn` macro and extends `clojure.test` to enable
verification of invocations using the
[`is`](https://clojure.github.io/clojure/clojure.test-api.html#clojure.test/is)
macro. Instead of using `fhofherr.stub-fn.core/verify-invocations` users
can do the following:


```clojure
(ns some-test-ns
  (:require [clojure.test :refer :all]
            [fhofherr.stub-fn.clojure.test :refer [stub-fn]]))

(deftest using-test-is
  (let [f (stub-fn f [x])]
      (f 1)
      (is (invoked? f :times 1 :args {'x 1}))))
```

## License

Copyright © 2017, 2018 Ferdinand Hofherr

Distributed under the MIT License.
