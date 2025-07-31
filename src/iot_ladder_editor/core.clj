(ns iot-ladder-editor.core
  (:require [iot-ladder-editor.parser :as parser]
            [iot-ladder-editor.converter :as converter]
            [iot-ladder-editor.ui :as ui]
            [clojure.pprint :as pprint]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn convert-il-program
  "Convert an IL program to LD representation with comprehensive error handling"
  [il-code]
  (try
    (let [parsed-program (parser/parse-il-program il-code)
          validated-program (parser/validate-program parsed-program)
          ld-program (converter/convert-il-to-ld validated-program)]
      {:success true
       :result ld-program
       :message "Conversion successful"})
    (catch Exception e
      (let [error-type (or (:type (ex-data e)) :unknown-error)
            error-msg (.getMessage e)]
        {:success false
         :error error-type
         :message error-msg
         :details (ex-data e)}))))

(defn print-conversion-result
  "Print the conversion result in a formatted way"
  [result]
  (if (:success result)
    (do
      (println "✓ Conversion successful!")
      (println "\nLD Diagram:")
      (println (converter/generate-ld-diagram (:result result))))
    (do
      (println "✗ Conversion failed!")
      (println "Error type:" (:error result))
      (println "Message:" (:message result))
      (when (:details result)
        (println "Details:")
        (pprint/pprint (:details result))))))

(defn run-console-mode
  "Run the application in console mode for testing"
  []
  (let [sample-il "LD %I0.0    ; Load input bit 0\nANDN %I0.1  ; AND with inverted input bit 1\nST %Q0.0    ; Store result to output bit 0"]
    (println "IL to LD Converter - Console Mode")
    (println "=================================")
    (println "\nInput IL Program:")
    (println sample-il)
    (println "\nParsing and converting...")

    (let [result (convert-il-program sample-il)]
      (print-conversion-result result))

    (println "\nTesting error handling with invalid code...")
    (let [invalid-il "INVALID_OP %I0.0"
          result (convert-il-program invalid-il)]
      (print-conversion-result result))))

(defn run-gui-mode
  "Run the application in GUI mode"
  []
  (println "Starting IL to LD Converter GUI...")
  (try
    (ui/start-app)
    (catch Exception e
      (println "Failed to start GUI:" (.getMessage e))
      (println "This might be due to missing JavaFX. Try running in console mode with --console"))))

(defn -main
  "Main entry point for the IL to LD converter"
  [& args]
  (try
    (if (and args (some #{"--console" "-c"} args))
      (run-console-mode)
      (run-gui-mode))
    (catch Exception e
      (println "Application error:" (.getMessage e))
      (System/exit 1))))