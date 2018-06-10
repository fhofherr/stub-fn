# Change Log

All notable changes to this project will be documented in this file. This
change log follows the conventions of
[keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

### Added

*   `stub-protocol` macro that allows to stub protocol methods.

## [v0.1.0] - 2018-06-10

### Added

*   `stub-fn` macro that can be used almost like `fn` and defines an
    anonymous function that keeps track of its invocations. In contrast to
    `fn` `stub-fn` requires a function name to be passed. This makes
    creation of meaningful error messages and failure reports easier.
*   `verify-invocations` function that verifys a stub's invcations against
    expectations.
*   `format-verification-report` function that creates a pretty prints
    a verification report.
*   `fhofherr.stub-fn.clojure.test` namespace providing integration into
    `clojure.test`.

[Unreleased]: https://github.com/fhofherr/stub-fn/compare/v0.1.0...HEAD
[v0.1.0]: https://github.com/fhofherr/stub-fn/tree/v0.1.0

