;; Copyright (c) Daniel Borchmann. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns conexp.contrib.java
  (:use conexp.base)
  (:use clojure.java.io)
  (:require conexp.main
            clojure.pprint))

;;;

(defn- to-valid-Java-name
  "Convert old-name to a valid Java variable name."
  [old-name]
  (let [old-name (str old-name),
        length   (count old-name)]
    (when (<= 0 (.indexOf old-name "!") (- length 2))
      (illegal-argument "Name has exclamation mark not as last symbol."))
    (when (<= 0 (.indexOf old-name "?") (- length 2))
      (illegal-argument "Name has question mark not as last symbol."))
    (-> old-name
        (.replace "-" "_")
        (.replace " " "_")
        (.replace "!" "_f")             ;assume ! only at end
        (.replace "?" "_p")             ;assume ? only at end
        (.replace "<" "_lt_")
        (.replace ">" "_gt_")
        (.replace "=" "_eq_")
        symbol)))

(defn- conexp-functions
  "Returns a hash-map of function names to vars of ns. The function
  names are converted by to-valid-Java-name."
  [ns]
  (let [public-map (ns-publics ns)]
    (reduce! (fn [map [sym, ^clojure.lang.Var var]]
               (if-not (.isMacro var)
                 (conj! map [(to-valid-Java-name sym) var])
                 map))
             {}
             public-map)))

(defn- dissect-arglist
  [arglist]
  (if (not (vector? arglist))
    arglist
    (let [[a b] (split-with #(not= '& %) arglist)]
      (if (not-empty b)
        (vec (concat (map dissect-arglist a) (list (with-meta (second b) {:tag "[Ljava.lang.Object;"}))))
        (vec (map dissect-arglist arglist))))))

(defn- function-signatures
  "Returns sequence of function signatures, each being suitable for
  gen-class."
  [new-name, ^clojure.lang.Var var]
  (let [arglists (:arglists (meta var)),
        return   (or (:tag (meta var)) 'Object),
        tag      (fn [x]
                   (let [tag (:tag (meta x))]
                     (if-not tag
                       'Object
                       (case tag
                         Lattice conexp.fca.lattices.Lattice
                         Context conexp.fca.contexts.Context
                         tag))))]
    (for [arglist arglists]
      [new-name
       (vec (map tag (dissect-arglist arglist)))
       return])))

(defn- generate-definition
  "Generates function definition with name new-name, calling orig-name
  with supplied arguments. Prepends prefix before new-name"
  [prefix new-name orig-name & arglists]
  (when (not-empty arglists)
    `(defn ~(symbol (str prefix new-name))
       ~@(for [args arglists]
           `(~(dissect-arglist args)
             (apply ~(symbol orig-name) ~(dissect-arglist args)))))))

;;;

(defn generate-java-interface
  "Given a name of a file generates the code for the Java interface in
  that file. After this has been compiled it can be used to call
  conexp-clj functions from Java."
  [orig-ns new-ns file-name]
  (let [methods (mapcat #(apply function-signatures %)
                        (conexp-functions orig-ns)),
        methods (mapcat #(list (symbol "^{:static true}") %) methods)]
    (with-open [out (writer file-name)]
      (binding [*print-meta* true
                *out* out]
        (clojure.pprint/pprint
         `(do
            (ns ~new-ns
              (:require conexp.main)
              (:gen-class
               :prefix ~'conexp-clj-
               :methods ~methods))

            (import 'conexp.fca.contexts.Context)
            (import 'conexp.fca.lattices.Lattice)

            ~@(for [[new-name, ^clojure.lang.Var var] (conexp-functions orig-ns)]
                (let [orig-name (symbol (str (.ns var)) (str (.sym var))),
                      arglists  (:arglists (meta var))]
                  (apply generate-definition
                         "conexp-clj-"
                         new-name
                         orig-name
                         arglists)))))))
    (compile new-ns)))

;;;

(generate-java-interface 'conexp.main
                         'conexp.contrib.java.Main
                         "src/conexp/contrib/java/Main.clj")

;;;

nil
