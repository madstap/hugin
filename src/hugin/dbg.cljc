(ns hugin.dbg
  "Debugging helpers.

  < appended to a name means that the value is \"passed through\" the open paren,
  meaning that you can drop these in anywhere without changing how the
  rest of the code works."
  (:require
   [clojure.pprint :refer [pprint]]))

(defn- rev-args
  "Takes a function and returns it with the arguments reversed."
  [f]
  (fn [& args]
    (apply f (reverse args))))

;;;;;;;;;;;;;;;;;;;;
;; Printing

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

(def ^{:arglists '([x] [x msg])
       :doc "Like p<, but with the arguments reversed. For use inside a ->"}
  p<- (rev-args p<))

(def ^{:arglists '([x] [x msg])
       :doc "Like pp<, but with the arguments reversed. For use inside a ->"}
  pp<- (rev-args pp<))

;;;;;;;;;;;;;;;;;;;;
;; Debug atom

(defonce ^{:doc "An atom containing a map to store debugging stuff in."}
  a (atom {}))

(defn a<
  "Put something in the debugging atom, a map, at k.
  k defaults to :hugin.dbg/default. Returns v.
  Can take multiple key-value pairs, in which case the first value is returned."
  ([v] (a< ::default v))
  ([k v] (swap! a assoc k v) v)
  ([k v & kvs] (apply swap! a assoc k v kvs) v))

(defn a>
  "Get something from the debugging atom, k defaults to :hugin.dbg/default
  If nothing is found then :hugin.dbg/nothing is returned."
  ([] (a> ::default))
  ([k] (get @a k ::nothing)))

(defn aconj<
  "Put a succession of values in a vector in the debugging atom, at k.
  k defaults to :hugin.dbg/default.
  Like a<, v is returned, so you can wrap this around anything.
  Useful for collecting each value as you go
  through a loop, reduce, map or similar."
  ([v] (aconj< ::default v))
  ([k v] (swap! a update k (fnil conj []) v) v))

(def ^{:arglists '([v] [v k])
       :doc "Like a<, but with the arguments reversed. For use inside a ->"}
  a<- (rev-args a<))

(def ^{:arglists '([v] [v k])
       :doc "Like aconj<, but with the arguments reversed. For use inside a ->"}
  aconj<- (rev-args aconj<))

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

;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc

(defn ppre
  "Pretty-print x inside a pre tag (hiccup-style)."
  [x]
  [:pre {:style {:white-space :pre-wrap}} ; Vertical scroll is not cool...
   (with-out-str (pprint x))])

(defmacro def<
  {:arglists '([name] [name expr])}
  ([n]
   {:pre [(symbol? n)]}
   (let [name-sym (symbol (name n))]
     `(do (def ~name-sym ~n)
          ~n)))
  ([n expr]
   `(let [x# ~expr]
      (def ~n x#)
      x#)))

;; TODO: defconj< ?

(defmacro def<-
  {:arglists '([expr name])}
  [expr n]
  `(let [x# ~expr]
     (def ~n x#)
     x#))
