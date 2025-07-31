(ns iot-ladder-editor.parser
  (:require [instaparse.core :as insta]
            [instaparse.transform :as insta-transform]
            [clojure.string :as str]))

;; IL Grammar using Instaparse - simplified and focused
(def il-grammar
  "
  <S> = program
  program = (statement | <nl> | <ws>)*
  statement = (labeled-instr | instruction | comment)
  
  instruction = operation <ws-plus> operand (<ws> comment)?
  labeled-instr = label <ws-opt> operation <ws-plus> operand (<ws> comment)?
  
  label = identifier ':'
  operation = 'LD' | 'LDN' | 'ST' | 'STN' | 'S' | 'R' | 'AND' | 'ANDN' | 
              'OR' | 'ORN' | 'XOR' | 'XORN' | 'ADD' | 'SUB' | 'MUL' | 
              'DIV' | 'GT' | 'GE' | 'EQ' | 'NE' | 'LE' | 'LT' | 'JMP'
  
  operand = memory-address | identifier | numeric
  
  identifier = #'[a-zA-Z_][a-zA-Z0-9_]*'
  numeric = #'[+-]?[0-9]+(\\.[0-9]+)?'
  memory-address = #'%[IQM][XBWDL]?[0-9]+(\\.[0-9]+)?'
  
  comment = #';.*'
  <ws> = #'[ \\t]'
  <ws-opt> = #'[ \\t]*'
  <ws-plus> = #'[ \\t]+'
  <nl> = #'\\r?\\n'
  ")

;; Create the parser
(def parse-il
  (insta/parser il-grammar))

;; Parse an IL program 
(defn parse
  "Parse an Instruction List program"
  [il-code]
  (let [parsed (parse-il il-code)]
    (if (insta/failure? parsed)
      (throw (ex-info "IL parsing failed"
                      {:error (insta/get-failure parsed)
                       :input il-code
                       :type :parse-error}))
      parsed)))

;; Transform parsed IL to a more usable structure
(defn transform-il [parsed]
  (insta-transform/transform
   {:program (fn [& statements]
               {:type :program
                :instructions (vec (remove nil? statements))})
    :statement identity
    :instruction (fn [op operand & _rest]
                   {:type :instruction
                    :operation (str op)  ; Ensure operation is a string
                    :operand (str operand)})
    :labeled-instr (fn [label op operand & _rest]
                     {:type :instruction
                      :label (str label)
                      :operation (str op)
                      :operand (str operand)})
    :label (fn [id _colon] (str id)) ; Remove the colon
    :operation str
    :operand str
    :identifier str
    :numeric #(try (Double/parseDouble %) (catch Exception _ (str %)))
    :memory-address str
    :comment (fn [_] nil)}  ; Filter out comments
   parsed))

;; High-level parsing function with better error handling
(defn parse-il-program
  "Parse an IL program and transform it to a usable structure"
  [il-code]
  (when (str/blank? il-code)
    (throw (ex-info "IL code cannot be empty" {:type :validation-error})))
  
  (try
    (-> il-code
        str/trim
        parse
        transform-il
        first)
    (catch Exception e
      (if (= (:type (ex-data e)) :parse-error)
        (throw e)
        (throw (ex-info "Failed to transform parsed IL"
                        {:cause e
                         :input il-code
                         :type :transform-error}))))))

;; Validation functions
(defn validate-instruction
  "Validate a single IL instruction"
  [instruction]
  (let [{operation :operation operand :operand} instruction
        valid-ops #{"LD" "LDN" "ST" "STN" "AND" "ANDN" "OR" "ORN"}]
    (when-not (valid-ops operation)
      (throw (ex-info (str "Unsupported operation: " operation)
                      {:operation operation
                       :operand operand
                       :type :validation-error})))
    instruction))

(defn validate-program
  "Validate an entire IL program"
  [program]
  (when-not (= :program (:type program))
    (throw (ex-info "Invalid program structure" {:type :validation-error})))
  
  (doseq [instruction (:instructions program)]
    (validate-instruction instruction))
  
  program)