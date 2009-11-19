(ns conexp.graphics.draw
  (:use conexp.layout
	conexp.layout.base
	conexp.graphics.base)
  (:import [javax.swing JFrame]))


; extend this to get a full "lattice editor"
(defn draw-lattice
  "Draws given lattice with given layout on a canvas and returns
  it. Uses *standard-layout* if no layout is given."
  ([lattice]
     (draw-lattice lattice *standard-layout*))
  ([lattice layout]
     (doto (JFrame. "conexp-clj Lattice")
       (.add (draw-on-canvas [0.0 0.0] [100.0 100.0]
			     (scale-layout [0.0 0.0] [100.0 100.0] (layout lattice))))
       (.setVisible true))))

nil
