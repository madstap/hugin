(ns hugin.repl
  #?(:cljs (:require-macros [hugin.repl :refer [gen-test p-test]])))

(defmacro gen-test
  "Given a form, returns a basic test case in the shape of:

      (is (= result form))

      This can then be copy-pasted from the repl to the test file.
      Beware of impure functions, and double check that the result is actually correct."
  [form]
  (let [sexp `(~'is (~'= ~(eval form) ~form))]
    `(quote ~sexp)))

(defmacro p-test "Prints the result of (gen-test form)."
  [form] `(prn (gen-test ~form)))
