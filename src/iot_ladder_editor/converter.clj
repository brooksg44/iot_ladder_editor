(ns iot-ladder-editor.converter
  (:require [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [il-to-ld-converter.ld-visualizer :as viz]))

;; Convert IL operations to LD elements
(defn il-to-ld-element
  "Convert an IL instruction to an LD element"
  [instruction]
  (let [op (:operation instruction)
        operand (:operand instruction)]
    (match [op]
      ["LD"]   {:type :contact-load
                :operand operand
                :normally-closed? false
                :description "Load contact (normally open)"}
      ["LDN"]  {:type :contact-load
                :operand operand
                :normally-closed? true
                :description "Load contact (normally closed)"}
      ["ST"]   {:type :coil-store
                :operand operand
                :normally-closed? false
                :description "Store to output coil"}
      ["STN"]  {:type :coil-store
                :operand operand
                :normally-closed? true
                :description "Store to output coil (normally closed)"}
      ["AND"]  {:type :contact-series
                :operand operand
                :normally-closed? false
                :description "Series contact (normally open)"}
      ["ANDN"] {:type :contact-series
                :operand operand
                :normally-closed? true
                :description "Series contact (normally closed)"}
      ["OR"]   {:type :contact-parallel
                :operand operand
                :normally-closed? false
                :description "Parallel contact (normally open)"}
      ["ORN"]  {:type :contact-parallel
                :operand operand
                :normally-closed? true
                :description "Parallel contact (normally closed)"}
      :else    {:type :unsupported-operation
                :operation op
                :operand operand
                :description (str "Unsupported operation: " op)})))

(defn analyze-program-structure
  "Analyze the structure of the IL program for better LD representation"
  [instructions]
  (let [rungs (group-by #(or (some #{(:operation %)} ["LD" "LDN"])
                             (= "ST" (:operation %))
                             (= "STN" (:operation %))) instructions)]
    {:total-instructions (count instructions)
     :load-instructions (filter #(some #{(:operation %)} ["LD" "LDN"]) instructions)
     :store-instructions (filter #(some #{(:operation %)} ["ST" "STN"]) instructions)
     :logic-instructions (filter #(some #{(:operation %)} ["AND" "ANDN" "OR" "ORN"]) instructions)}))

(defn convert-il-to-ld
  "Convert an entire IL program to LD representation"
  [parsed-program]
  (if-not (= :program (:type parsed-program))
    (throw (ex-info "Invalid program structure for conversion"
                    {:received-type (:type parsed-program)
                     :expected-type :program})))

  (let [instructions (:instructions parsed-program)
        analysis (analyze-program-structure instructions)]
    (if (empty? instructions)
      {:type :ld-program
       :rungs []
       :analysis analysis
       :warning "No instructions found in program"}
      {:type :ld-program
       :rungs (mapv il-to-ld-element instructions)
       :analysis analysis})))

(defn generate-ld-diagram
  "Generate a visual representation of the Ladder Diagram"
  [ld-program]
  (str (viz/generate-ascii-ld ld-program)
       "\n\n"
       (viz/generate-detailed-ld ld-program)))

(defn validate-ld-program
  "Validate the structure of an LD program"
  [ld-program]
  (when-not (= :ld-program (:type ld-program))
    (throw (ex-info "Invalid LD program type" {:type (:type ld-program)})))

  (when-not (vector? (:rungs ld-program))
    (throw (ex-info "LD program rungs must be a vector" {:rungs (:rungs ld-program)})))

  ;; Validate each rung
  (doseq [rung (:rungs ld-program)]
    (when-not (contains? rung :type)
      (throw (ex-info "Each rung must have a type" {:rung rung})))
    (when-not (contains? rung :operand)
      (throw (ex-info "Each rung must have an operand" {:rung rung}))))

  ld-program)