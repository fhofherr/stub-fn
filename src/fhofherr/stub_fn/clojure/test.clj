(ns fhofherr.stub-fn.clojure.test
  (:require [clojure.test :as t]
            [fhofherr.stub-fn.core :as core]))

;; Re-export the core/stub-fn macro for convenience.
;;
;; This is a little tricky, as stub-fn is a macro. The macro function itself
;; is obtained by de-refing the var that points to it. In order to make the
;; interned var a macro again the :macro key has to be set to true
;; in the new var's meta data. We achieve this by copying all of the original
;; macro var's meta data. Since this is just a re-export we alter the doc.
;;
;; See http://stackoverflow.com/questions/20831029/how-is-it-possible-to-intern-macros-in-clojure
(intern 'fhofherr.stub-fn.clojure.test
        (with-meta 'stub-fn (-> #'core/stub-fn
                                meta
                                (assoc :doc
                                       "Convenience re-export of [[core/stub-fn]].")))
        @#'core/stub-fn)


(defmethod t/assert-expr 'invoked? [msg form]
  `(let [verification-report# (core/verify-invocations ~@(rest form))
         verification-result# (if (core/success? verification-report#)
                                :pass
                                :fail)
         verification-report-str# (core/format-verification-report verification-report#)]
     (t/do-report {:type verification-result#
                   :message verification-report-str#
                   :expected '~form
                   :actual '(~'not ~form)})))
