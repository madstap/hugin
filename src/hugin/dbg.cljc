(ns hugin.dbg
  "Debugging helpers.

  < appended to a name means that the value is \"passed through\" the open paren,
  meaning that you can drop these in anywhere without changing how the
  rest of the code works."
  (:require
   [clojure.pprint :refer [pprint]]
   #?@(:clj
       [[clojure.edn :as edn]
        [clojure.data.json :as json]]))
  #?(:clj
     (:import
      (java.awt Toolkit HeadlessException)
      (java.awt.datatransfer StringSelection
                             DataFlavor
                             UnsupportedFlavorException)
      (java.io.IOException))))

(defn- rev-args
  "Takes a function or arity one and two and
  returns it with the arity two reversed."
  [f]
  (fn
    ([x] (f x))
    ([k x] (f x k))))


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
  ([k v & kvs]
   {:pre [(even? (count kvs))]}
   (apply swap! a assoc k v kvs)
   v))

(defn a>
  "Get something from the debugging atom, k defaults to :hugin.dbg/default
  If nothing is found then :hugin.dbg/nothing is returned."
  ([] (a> ::default))
  ([k] (get @a k ::nothing))
  ([k & ks] (mapv a> (cons k ks))))

(defn aconj<
  "Put a succession of values in a vector in the debugging atom, at k.
  k defaults to :hugin.dbg/default.
  Like a<, v is returned, so you can wrap this around anything.
  Useful for collecting each value as you go
  through a loop, reduce, map or similar.
  Can take multiple key-value pairs, in which case the first value is returned."
  ([v] (aconj< ::default v))
  ([k v] (swap! a update k (fnil conj []) v) v)
  ([k v & kvs]
   {:pre [(even? (count kvs))]}
   (doseq [[k' v'] (partition 2 kvs)] (aconj< k' v'))
   (aconj< k v)))

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


(defn pp-str [x]
  (with-out-str (pprint x)))

#?(:clj
   (defn clip* [s]
     (-> (Toolkit/getDefaultToolkit)
         (.getSystemClipboard)
         (.setContents (StringSelection. s) nil))))

#?(:clj
   (defn clip< [x]
     (clip* (str x))
     x))

#?(:clj
   (defn clip>
     "Get contents of clipboard."
     []
     (-> (Toolkit/getDefaultToolkit)
         .getSystemClipboard
         (.getData DataFlavor/stringFlavor))))

#?(:clj
   (defn clipr>
     "Read contents of clipboard."
     []
     (edn/read-string (clip>))))

#?(:clj
   (defn clipe>
     "Eval contents of clipboard."
     []
     (eval (clipr>))))

#?(:clj
   (defn clipj>

     []
     (json/read-str (clip>) :key-fn keyword)))

#?(:clj
   (defn clipj< [x]
     (clip* (json/write-str x))
     x))

#?(:clj
   (defn clipjp< [x]
     (clip* (with-out-str (json/pprint x)))
     x))

#?(:clj
   (defn clipr< [x]
     (clip* (pr-str x))
     x))

#?(:clj
   (defn cliprn< [x]
     (clip* (prn-str x))
     x))

#?(:clj
   (defn clipp< [x]
     (clip* (pp-str x))
     x))

(comment

  (require '[clojure.test :refer [deftest is]])

  (let [x {:foo 12}]
    (clipj< x)
    (is (= x (json/read-str (clip>) :key-fn keyword)))
    (clipjp< x)
    (is (= x (clipj>)))
    (let [x-str (json/write-str x)]
      (clip< x-str)
      (is (= x-str (clip>)))
      (is (= x (clipj>)))))

  (clip>)

  (clip>)
  (clipj>)

  (clipr>)

  (clipjp< (repeat 20 {:foo 12}))

  (clip< "foo")

  (clip< "(def foo 12)")

  (clipr< '(def foo 12))

  (clip>)

  (clipr>)

  (clipe>)

  )
