;; Copyright (c) Daniel Borchmann. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns conexp.fca.more
  (:use conexp.base
        conexp.fca.contexts
        conexp.fca.implications
        conexp.fca.exploration)
  (:require [clojure.contrib.graph :as graph]))

(ns-doc "More on FCA.")


;;; Compatible Subcontexts

(defn compatible-subcontext?
  "Tests whether ctx-1 is a compatible subcontext of ctx-2."
  [ctx-1 ctx-2]
  (and (subcontext? ctx-1 ctx-2)
       (forall [[h m] (up-arrows ctx-2)]
         (=> (contains? (objects ctx-1) h)
             (contains? (attributes ctx-1) m)))
       (forall [[g n] (down-arrows ctx-2)]
         (=> (contains? (attributes ctx-1) n)
             (contains? (objects ctx-1) g)))))

(defn compatible-subcontexts
  "Returns all compatible subcontexts of ctx. ctx has to be reduced."
  [ctx]
  (if (not (reduced? ctx))
    (illegal-argument "Context given to compatible-subcontexts has to be reduced."))
  (let [up-arrows        (up-arrows ctx)
        down-arrows      (down-arrows ctx)
        subcontext-graph (graph/transitive-closure
                          (struct graph/directed-graph
                                  (disjoint-union (objects ctx) (attributes ctx))
                                  (fn [[x idx]]
                                    (condp = idx
                                      0 (for [[g m] up-arrows
                                              :when (= g x)]
                                          [m 1])
                                      1 (for [[g m] down-arrows
                                              :when (= m x)]
                                          [g 0])))))
        down-down        (set-of [g m] [m (attributes ctx)
                                        [g idx] (graph/get-neighbors subcontext-graph [m 1])
                                        :when (= idx 0)])
        compatible-ctx   (make-context (objects ctx)
                                       (attributes ctx)
                                       (fn [g m]
                                         (not (contains? down-down [g m]))))]
    (for [[G-H N] (concepts compatible-ctx)]
      (make-context (difference (objects ctx) G-H) N (incidence ctx)))))

;;; Bonds

(defn smallest-bond
  "Returns the smallest bond between ctx-1 and ctx-2 that has the
  elements of rel as crosses."
  [ctx-1 ctx-2 rel]
  (loop [ctx-rel (make-context-nc (objects ctx-1) (attributes ctx-2) rel)]
    (let [next-rel (union (set-of [g m] [m (attributes ctx-2),
                                         g (attribute-derivation
                                            ctx-1
                                            (object-derivation
                                             ctx-1
                                             (attribute-derivation
                                              ctx-rel
                                              #{m})))])
                          (set-of [g m] [g (objects ctx-1),
                                         m (object-derivation
                                            ctx-2
                                            (attribute-derivation
                                             ctx-2
                                             (object-derivation
                                              ctx-rel
                                              #{g})))]))]
      (if (= next-rel (incidence ctx-rel))
        ctx-rel
        (recur (make-context-nc (objects ctx-1) (attributes ctx-2) next-rel))))))

(defn bond?
  "Checks whether context ctx is a context between ctx-1 and ctx-2."
  [ctx-1 ctx-2 ctx]
  (and (context? ctx)
       (context? ctx-1)
       (context? ctx-2)
       (= (objects ctx-1) (objects ctx))
       (= (attributes ctx-2) (attributes ctx))
       (forall [g (objects ctx-1)]
         (let [g-R (object-derivation ctx #{g})]
           (= g-R (context-attribute-closure ctx g-R))))
       (forall [m (attributes ctx-2)]
         (let [m-R (attribute-derivation ctx #{m})]
           (= m-R (context-object-closure ctx m-R))))))

(defn all-bonds
  "Returns all bonds between ctx-1 and ctx-2."
  [ctx-1 ctx-2]
  (let [G-1     (objects ctx-1),
        M-2     (attributes ctx-2),
        impls-1 (set-of (make-implication (cross-product A #{m})
                                          (cross-product B #{m}))
                        [impl (stem-base (dual-context ctx-1)),
                         :let [A (premise impl),
                               B (conclusion impl)],
                         m M-2]),
        impls-2 (set-of (make-implication (cross-product #{g} A)
                                          (cross-product #{g} B))
                        [impl (stem-base ctx-2),
                         :let [A (premise impl),
                               B (conclusion impl)],
                         g G-1]),
        clop (clop-by-implications (union impls-1 impls-2))]
    (map #(make-context (objects ctx-1) (attributes ctx-2) %)
         (all-closed-sets (cross-product (objects ctx-1) (attributes ctx-2))
                          clop))))

;;; Shared Intents (implemented by Stefan Borgwardt)

(defn- next-shared-intent
  "The smallest shared intent of contexts ctx-1 and ctx-2 containing b."
  [ctx-1 ctx-2 b]
  (loop [shared-attrs b,
         objs-1       (attribute-derivation ctx-1 b),
         objs-2       (attribute-derivation ctx-2 b)]
    (let [new-shared-attrs-1 (set-of m [m (difference (attributes ctx-1) shared-attrs)
                                        :when (forall [g objs-1]
                                                ((incidence ctx-1) [g m]))]),
          new-shared-attrs-2 (set-of m [m (difference (attributes ctx-2) shared-attrs)
                                        :when (forall [g objs-2]
                                                ((incidence ctx-2) [g m]))])]
      (if (and (empty? new-shared-attrs-1) (empty? new-shared-attrs-2))
        shared-attrs
        (recur (union shared-attrs new-shared-attrs-1 new-shared-attrs-2)
               (set-of g [g objs-1
                          :when (forall [m new-shared-attrs-2]
                                  ((incidence ctx-1) [g m]))])
               (set-of g [g objs-2
                          :when (forall [m new-shared-attrs-1]
                                  ((incidence ctx-2) [g m]))]))))))

(defn all-shared-intents
  "All intents shared between contexts ctx-1 and ctx-2. ctx-1 and
  ctx-2 must have the same attribute set."
  [ctx-1 ctx-2]
  (assert (= (attributes ctx-1) (attributes ctx-2)))
  (all-closed-sets (attributes ctx-1) #(next-shared-intent ctx-1 ctx-2 %)))

(defn all-bonds-by-shared-intents
  "All bonds between ctx-1 and ctx-2, computed using shared intents."
  [ctx-1 ctx-2]
  (map #(make-context (objects ctx-1) (attributes ctx-2) %)
       (all-shared-intents (context-product (adiag-context (objects ctx-1)) ctx-2)
                           (dual-context (context-product ctx-1 (adiag-context (attributes ctx-2)))))))

;;; Context for Closure Operators

(defn- maximal-counterexample
  "For a given closure operator c on a set base-set, maximizes the set
  C (i.e. returns a superset of C) that is not a superset of
  B (i.e. there exists an element m in B without C that is also not in
  the result."
  [c base-set B C]
  (let [m (first (difference B C))]
    (loop [C C,
           elts (difference (disj base-set m)
                            C)]
      (if (empty? elts)
        C
        (let [n (first elts),
              new-C (c (conj C n))]
          (if (contains? new-C m)
            (recur C (rest elts))
            (recur new-C (rest elts))))))))

(defn context-from-clop
  "Returns a context whose intents are exactly the closed sets of the
  given closure operator on the given base-set."
  [base-set clop]
  (let [base-set (set base-set)]
    (:context
     (explore-attributes (make-context #{} base-set #{})
                         :handler (fn [_ _ impl]
                                    (let [A (premise impl),
                                          B (conclusion impl),
                                          C (clop A)]
                                      (when-not (subset? B C)
                                        [[(gensym) (maximal-counterexample clop base-set B C)]])))))))

;;; Neighbours in the concept lattice with Lindig's Algorithm

(defn direct-upper-concepts
  "Computes the set of direct upper neighbours of the concept [A B] in
  the concept lattice of ctx. Uses Lindig's Algorithm for that."
  [ctx [A B]]
  (assert (concept? ctx [A B])
          "Given pair must a concept in the given context")
  (loop [objs        (difference (objects ctx) A),
         min-objects (difference (objects ctx) A),
         neighbours  (transient #{})]
    (if (empty? objs)
      (persistent! neighbours)
      (let [g   (first objs),
            M_1 (oprime ctx (conj A g)),
            G_1 (aprime ctx M_1)]
        (if (empty? (intersection min-objects
                                  (difference (disj G_1 g)
                                              A)))
          (recur (rest objs)
                 min-objects
                 (conj! neighbours [G_1 M_1]))
          (recur (rest objs)
                 (disj min-objects g)
                 neighbours))))))

(defn direct-lower-concepts
  "Computes the set of direct upper neighbours of the concept [A B] in
  the concept lattice of ctx. Uses Lindig's Algorithm for that."
  [ctx [A B]]
  (assert (concept? ctx [A B])
          "Given pair must a concept in the given context")
    (loop [atts        (difference (attributes ctx) B),
           min-attrs   (difference (attributes ctx) B),
           neighbours  (transient #{})]
      (if (empty? atts)
        (persistent! neighbours)
        (let [m   (first atts),
              G_1 (aprime ctx (conj B m)),
              M_1 (oprime ctx G_1)]
          (if (empty? (intersection min-attrs
                                    (difference (disj M_1 m)
                                                B)))
            (recur (rest atts)
                   min-attrs
                   (conj! neighbours [G_1 M_1]))
            (recur (rest atts)
                   (disj min-attrs m)
                   neighbours))))))

;;; Certain contexts

(defn implication-context
  "Returns a context for a non-negative number n which has as it's
  extents the closure systems on the set {0 .. (n-1)} and as it's
  intents the corresponding implicational theory."
  [n]
  (assert (and (number? n) (<= 0 n))
          "Must be given a non-negative number.")
  (let [base-set  (set-of-range 0 n)
        base-sets (subsets base-set)]
    (make-context base-sets
                  (set-of (make-implication A #{x}) |
                          A base-sets
                          x (difference base-set base-sets))
                  respects?)))

;;; Context Automorphisms

;; McKay Algorithm, very simplified

(defn context-automorphism?
  "Returns true if and only if the pair [alpha beta] is a context automorphism of ctx."
  [ctx [alpha beta]]
  (and (or (map? alpha) (fn? alpha))
       (or (map? beta) (fn? beta))
       (forall [g (objects ctx)
                m (attributes ctx)]
         (<=> (contains? (incidence ctx) [g m])
              (contains? (incidence ctx) [(alpha g) (beta m)])))))

(defn- refine-partition
  ""
  [ctx, pi]
  (loop [pi pi,
         m  0,
         k  0]
    (cond
     (or (>= m (count pi))
         (every? singleton? pi))
     pi
     ;;
     (>= k (count pi))
     (recur pi (inc m) 0)
     ;;
     :else
     (let [W (pi m),
           V (pi k),
           X (group-by #(count (filter (fn [z]
                                         (or (incident? ctx % z)
                                             (incident? ctx z %)))
                                       W))
                       V),
           X (remove empty?
                     (map #(set (X %))
                          (range (inc (apply max -1 (keys X))))))]
       (recur (if (singleton? X)
                pi
                (vec (concat (subvec pi 0 k)
                             X
                             (subvec pi (inc k)))))
              m                        ;correct?
              (inc k))))))             ;correct?

(defn- split-partition-at
  ""
  [pi u]
  (let [[before-u after-u] (split-with #(not (contains? % u)) pi)]
    (vec (concat before-u [#{u} (disj (first after-u) u)] (rest after-u)))))

(defn- terminal-nodes
  ""
  [ctx pi]
  (if (every? singleton? pi)
    (list pi)
    (mapcat #(terminal-nodes ctx (refine-partition ctx (split-partition-at pi %)))
            (first (remove singleton? pi)))))

(defn context-automorphisms
  "Computes the context automorphisms of ctx as pairs of bijective mappings acting on the objects
  and the attributes of ctx, respectively.  Returns its result as a lazy sequence."
  [ctx]
  (unsupported-operation))

(defn- terminal-nodes/pruning
  [ctx partition]
  )

(defn canonical-isomorph
  "Returns the canonical isomorph of ctx, as defined by McKay's algorithm using lexicographic
  ordering."
  [ctx]
  )

;;

(defn isomorphic-contexts?
  "Tests whether ctx-1 and ctx-2 are isomorphic."
  [ctx-1 ctx-2]
  (and (= (count (objects ctx-1)) (count (objects ctx-2)))
       (= (count (attributes ctx-1)) (count (attributes ctx-2)))
       (= (count (incidence ctx-1)) (count (incidence ctx-2)))
       (= (canonical-isomorph ctx-1)
          (canonical-isomorph ctx-2))))

(defn context-object-automorphisms
  "Computes the parts of the context automorphisms of ctx which act on the objects of ctx.  Returns
  its result as a lasy sequence."
  [ctx]
  (map first (context-automorphisms ctx)))

(defn context-attribute-automorphisms
  "Computes the parts of the context automorphisms of ctx which act on the attributes of ctx.
  Returns its result as a lazy sequence."
  [ctx]
  (map second (context-automorphisms ctx)))

(defn rigid?
  "Returns true if and only if ctx does not have any other automorphisms than the identity."
  [ctx]
  (nil? (second (context-object-automorphisms ctx))))

(defn induced-object-automorphism
  "Returns the automorphism on the objects of ctx that is induced by beta, which is part of an
  context automorphism of ctx and acts on the attributes of ctx.  Note that ctx must be clarified."
  [ctx beta]
  (assert (clarified? ctx)
          "Given context must be clarified to uniquely determine induced object automorphism.")
  (let [pairs (set-of [g h] | g (objects ctx)
                              h (objects ctx) ;this choice can be made finer
                              :when (forall [m (attributes ctx)]
                                      (<=> (contains? (incidence ctx) [g m])
                                           (contains? (incidence ctx) [h (beta m)]))))]
    (into {} pairs)))

(defn induced-attribute-automorphism
  "Returns the automorphism on the attributes of ctx that is induced by alpha, which is part of an
  context automorphism of ctx and acts on the objects of ctx.  Note that ctx must be clarified."
  [ctx alpha]
  (assert (clarified? ctx)
          "Given context must be clarified to uniquely determine induced attribute automorphism.")
  (let [pairs (set-of [m n] | m (attributes ctx)
                              n (attributes ctx) ;see above
                              :when (forall [g (objects ctx)]
                                      (<=> (contains? (incidence ctx) [g m])
                                           (contains? (incidence ctx) [(alpha g) n]))))]
    (into {} pairs)))

;;;

nil
