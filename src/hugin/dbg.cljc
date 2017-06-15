(ns hugin.dbg
  "Debugging helpers.

  < appended to a name means that the value is \"passed through\" the open paren,
  this means that you can drop these in anywhere, and they won't change how the
  rest of the code works.

  This ns should not be required anywhere in a clojurescript prod
  build as requiring pprint adds a lot of non DCEable code."
  (:require
   [clojure.pprint :refer [pprint]]))

(defn println-debugger [print-fn]
  (fn
    ([x] (print-fn x) x)
    ([msg x] (println (str msg \newline (with-out-str (print-fn x)))) x)))

(def ^{:arglists '([x] [msg x])
       :doc
       "Prints x using prn, then returns it.
  Takes an optional message as the first argument."}
  p< (println-debugger prn))

(def ^{:arglists '([x] [msg x])
       :doc
       "Prints x using pprint, then returns it.
  Takes an optional message as the first argument."}
  pp< (println-debugger pprint))

(defonce ^{:doc "An atom containing a map to store debugging stuff in."}
  a (atom {}))

(defn a<
  "Put something in the debugging atom, a map, under k. If no key supplied,
  v is assoced under ::default. The value is returned,
  so that you can wrap this around anything."
  ([v] (a< ::default v))
  ([k v] (swap! a assoc k v) v))

(defn a>
  "Get something from the debugging atom, k defaults to ::default and if nothing
  is found ::nothing is returned."
  ([] (a> ::default))
  ([k] (get @a k ::nothing)))

(defn aconj<
  "Put something in a vector in the debugging atom, under k.
  k defaults to ::default.
  Like a<, v is returned, so you can wrap this around anything.
  Useful for collecting every value as you go
  through a loop, reduce, map or similar."
  ([v] (aconj< ::default v))
  ([k v] (swap! a update-in [k] (fnil conj []) v) v))

(defn a>> "The whole debug atom, aka @a" [] @a)

(defn diss
  "Dissocs the value under k in the debugging atom. Defaults to ::default.
   Returns the value dissoced."
  ([] (diss ::default))
  ([k]
   (let [v (a> k)]
     (swap! a dissoc k)
     v)))

(defn nuke "Resets the dbg atom to an empty map." []
  (reset! a {}))
