(ns hugin.dbg
  "Debugging helpers.

  < appended to a name means that the value is \"passed through\" the open paren,
  meaning that you can drop these in anywhere without changing how the
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
  "Put something in the debugging atom, a map, at k.
  k defaults to :hugin.dbg/default. Returns v."
  ([v] (a< ::default v))
  ([k v] (swap! a assoc k v) v))

(defn a>
  "Get something from the debugging atom, k defaults to :hugin.dbg/default
  If nothing is found then :hugin.dbg/nothing is returned."
  ([] (a> ::default))
  ([k] (get @a k ::nothing)))

(defn aconj<
  "Put something in a vector in the debugging atom, at k.
  k defaults to :hugin.dbg/default.
  Like a<, v is returned, so you can wrap this around anything.
  Useful for collecting each value as you go
  through a loop, reduce, map or similar."
  ([v] (aconj< ::default v))
  ([k v] (swap! a update-in [k] (fnil conj []) v) v))

(defn a>> "The whole debug atom, aka @a" [] @a)

(defn diss
  "Dissocs the value at k in the debugging atom. Defaults to :hugin.dbg/default.
   Returns the value that was dissoced."
  ([] (diss ::default))
  ([k]
   (let [v (a> k)]
     (swap! a dissoc k)
     v)))

(defn nuke "Resets the dbg atom to an empty map." []
  (reset! a {}))

(defn ppre
  "Pretty-print x inside a pre tag (hiccup-style)."
  [x]
  [:pre {:style {:white-space :pre-wrap}} ; Vertical scroll is not cool...
   (with-out-str (pprint x))])
