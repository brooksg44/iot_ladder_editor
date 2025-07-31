(ns il-to-ld-converter.ld-visualizer
  (:require [clojure.string :as str]))

(defn contact-symbol [operand normally-closed?]
  (if normally-closed?
    (format "|/%s|" operand)    ; Normally closed contact
    (format "| %s |" operand))) ; Normally open contact

(defn coil-symbol [operand normally-closed?]
  (if normally-closed?
    (format "(%s/)" operand)    ; Normally closed coil
    (format "( %s )" operand))) ; Normal coil

(defn generate-ascii-ld [ld-program]
  "Generate ASCII art representation of ladder diagram"
  (let [rungs (:rungs ld-program)]
    (str "┌─ Ladder Diagram ─────────────────────────────────────────────┐\n"
         "│                                                              │\n"
         (str/join "\n"
                   (map-indexed
                    (fn [idx rung]
                      (let [operand (:operand rung)
                            normally-closed? (:normally-closed? rung)
                            rung-num (format "%02d" (inc idx))]
                        (case (:type rung)
                          :contact-load
                          (format "│ %s  ├─%s──────────────────────────────────────────┤ │"
                                  rung-num (contact-symbol operand normally-closed?))
                          
                          :contact-series
                          (format "│ %s  ├─%s──────────────────────────────────────────┤ │"
                                  rung-num (contact-symbol operand normally-closed?))
                          
                          :contact-parallel
                          (format "│ %s  ├─┬─%s─┬─────────────────────────────────────┤ │"
                                  rung-num (contact-symbol operand normally-closed?))
                          
                          :coil-store
                          (format "│ %s  ├──────────────────────────────────%s──────┤ │"
                                  rung-num (coil-symbol operand normally-closed?))
                          
                          (format "│ %s  ├─ UNSUPPORTED: %s ─────────────────────────┤ │"
                                  rung-num (:operation rung)))))
                    rungs))
         "\n│                                                              │"
         "\n└──────────────────────────────────────────────────────────────┘")))

(defn generate-detailed-ld [ld-program]
  "Generate detailed textual representation"
  (let [rungs (:rungs ld-program)]
    (str "=== LADDER DIAGRAM ANALYSIS ===\n\n"
         "Total Rungs: " (count rungs) "\n\n"
         (str/join "\n\n"
                   (map-indexed
                    (fn [idx rung]
                      (format "RUNG %d:\n  Operation: %s\n  Operand: %s\n  Type: %s\n  Normally Closed: %s\n  Description: %s"
                              (inc idx)
                              (str/upper-case (name (:type rung)))
                              (:operand rung)
                              (name (:type rung))
                              (:normally-closed? rung)
                              (case (:type rung)
                                :contact-load "Load contact - starts a new logic path"
                                :contact-series "Series contact - ANDs with previous logic"
                                :contact-parallel "Parallel contact - ORs with previous logic"
                                :coil-store "Output coil - stores result to operand"
                                "Unsupported operation")))
                    rungs))
         "\n\n=== END ANALYSIS ===")))
