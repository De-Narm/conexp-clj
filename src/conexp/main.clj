;; Copyright (c) Daniel Borchmann. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns conexp.main
  (:require conexp.base
            conexp.fca
            conexp.io
            conexp.layouts))

(conexp.base/ns-doc
 "Main namespace for conexp-clj. Immigrates all needed namespaces.")

;;;

(def conexp-namespaces '[conexp.base
                         conexp.fca
                         conexp.io
                         conexp.layouts])

(dorun (map conexp.base/immigrate conexp-namespaces))

;;;

(defvar- internal-version-string
  (.trim #=(slurp "VERSION")))

(defvar- conexp-version-map
  (let [[_ major minor patch qualifier] (re-find #"(\d+)\.(\d+)\.(\d+)-(.+)" internal-version-string)]
    {:major (Integer/parseInt major),
     :minor (Integer/parseInt minor),
     :patch (Integer/parseInt patch),
     :qualifier qualifier}))

(defn- conexp-built-version
  "Returns the date of the conexp build, retrieved from the name of
  the conexp-clj jar file. Returns \"source\" if there is none in the
  classpath."
  []
  (if-let [[_ date time] (re-find #"conexp-clj-.*?-(\d+)\.(\d+)\.jar"
                                  (System/getProperty "java.class.path"))]
    (str date "." time)
    "source"))

(defn conexp-version
  "Returns the version of conexp as a string."
  []
  (let [{:keys [major minor patch qualifier]} conexp-version-map]
    (str major "." minor "." patch "-" qualifier "-" (conexp-built-version))))

(defn has-version?
  "Compares given version of conexp and returns true if and only if
  the current version of conexp is higher or equal than the given one"
  [{my-major :major, my-minor :minor, my-patch :patch}]
  (assert (and my-major my-minor my-patch))
  (let [{:keys [major, minor, patch]} conexp-version-map]
    (or (and (< my-major major))
        (and (= my-major major)
             (< my-minor minor))
        (and (= my-major major)
             (= my-minor minor)
             (< my-patch patch)))))

(defn test-conexp
  "Runs tests for conexp. If with-contrib? is given and true, tests
  conexp.contrib.tests too."
  ([] (test-conexp false))
  ([with-contrib?]
     (if with-contrib?
       (do (require 'conexp.tests
                    'conexp.contrib.tests)
           (clojure.test/run-tests 'conexp.tests
                                   'conexp.contrib.tests))
       (do (require 'conexp.tests)
           (clojure.test/run-tests 'conexp.tests)))))

(defn quit
  "Quits conexp-clj."
  []
  (System/exit 0))

;;;


nil

