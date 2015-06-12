;; Copyright (c) Daniel Borchmann. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

;;;

(defproject conexp-clj "1.2.0-SNAPSHOT"
  :min-lein-version "2.0.0"
  :description "A ConExp rewrite in clojure"
  :url "http://github.com/exot/conexp-clj/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["math" {:url "http://www.math.tu-dresden.de/~borch/repos/mvn/"
                          :checksum :ignore}]]
  :dependencies [[org.clojure/clojure             "1.6.0"]
                 [org.clojure/tools.cli           "0.3.1"]
                 [org.clojure/core.incubator      "0.1.3"]
                 [org.clojure/math.combinatorics  "0.0.7"]
                 [org.clojure/math.numeric-tower  "0.0.4"]
                 [org.apache.commons/commons-math "2.2"]
                 [conexp-clj/G                    "0.1.1"]
                 [conexp-clj/LatDraw              "0.0.1"]
                 [seesaw                          "1.4.3"]
                 [jline/jline                     "0.9.93"]]
  :aot [conexp.main
        conexp.contrib.java
        conexp.contrib.gui]
  :keep-non-project-classes true
  :global-vars {*warn-on-reflection* true}
  :test-paths ["tests" "src/test/"]
  :compile-path "lib/classes/"
  :scm {:url "git@github.com:exot/conexp-clj.git"}
  :java-opts ["-Dawt.useSystemAAFontSettings=on"])

;;;

nil
