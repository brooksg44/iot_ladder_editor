(ns iot-ladder-editor.core-tests
  (:require [clojure.test :refer :all]
            [iot-ladder-editor.core :refer :all]
            [iot-ladder-editor.parser :as parser]
            [iot-ladder-editor.converter :as converter]))

(deftest parser-basic-test
  (testing "Basic IL parsing functionality"
    (let [simple-il "LD %I0.0"
          result (parser/parse-il-program simple-il)]
      (is (= :program (:type result)))
      (is (= 1 (count (:instructions result))))
      (is (= "LD" (:operation (first (:instructions result)))))
      (is (= "%I0.0" (:operand (first (:instructions result))))))))

(deftest parser-complex-test
  (testing "Complex IL parsing with multiple instructions"
    (let [complex-il "LD %I0.0\nAND %I0.1\nST %Q0.0"
          result (parser/parse-il-program complex-il)]
      (is (= :program (:type result)))
      (is (= 3 (count (:instructions result))))
      (let [instructions (:instructions result)]
        (is (= "LD" (:operation (nth instructions 0))))
        (is (= "AND" (:operation (nth instructions 1))))
        (is (= "ST" (:operation (nth instructions 2))))))))

(deftest parser-with-comments-test
  (testing "IL parsing with comments"
    (let [il-with-comments "LD %I0.0    ; Load input\nST %Q0.0    ; Store output"
          result (parser/parse-il-program il-with-comments)]
      (is (= :program (:type result)))
      (is (= 2 (count (:instructions result)))))))

(deftest parser-error-handling-test
  (testing "Parser error handling"
    (is (thrown? Exception (parser/parse-il-program "")))
    (is (thrown? Exception (parser/parse-il-program "INVALID_OPERATION %I0.0")))))

(deftest converter-basic-test
  (testing "Basic IL to LD conversion"
    (let [simple-program {:type :program
                          :instructions [{:type :instruction
                                          :operation "LD"
                                          :operand "%I0.0"}]}
          result (converter/convert-il-to-ld simple-program)]
      (is (= :ld-program (:type result)))
      (is (= 1 (count (:rungs result))))
      (let [rung (first (:rungs result))]
        (is (= :contact-load (:type rung)))
        (is (= "%I0.0" (:operand rung)))
        (is (= false (:normally-closed? rung)))))))

(deftest converter-normally-closed-test
  (testing "Normally closed operations conversion"
    (let [program {:type :program
                   :instructions [{:type :instruction
                                   :operation "LDN"
                                   :operand "%I0.0"}
                                  {:type :instruction
                                   :operation "STN"
                                   :operand "%Q0.0"}]}
          result (converter/convert-il-to-ld program)]
      (is (= 2 (count (:rungs result))))
      (is (= true (:normally-closed? (first (:rungs result)))))
      (is (= true (:normally-closed? (second (:rungs result))))))))

(deftest converter-all-operations-test
  (testing "All supported operations conversion"
    (let [operations ["LD" "LDN" "ST" "STN" "AND" "ANDN" "OR" "ORN"]
          instructions (map #(hash-map :type :instruction
                                       :operation %
                                       :operand "%I0.0") operations)
          program {:type :program :instructions instructions}
          result (converter/convert-il-to-ld program)]
      (is (= (count operations) (count (:rungs result))))
      (is (every? #(contains? % :type) (:rungs result)))
      (is (every? #(contains? % :operand) (:rungs result)))
      (is (every? #(contains? % :normally-closed?) (:rungs result))))))

(deftest end-to-end-conversion-test
  (testing "End-to-end IL to LD conversion"
    (let [sample-il "LD %I0.0\nAND %I0.1\nST %Q0.0"
          result (convert-il-program sample-il)]
      (is (:success result))
      (is (= :ld-program (get-in result [:result :type])))
      (is (= 3 (count (get-in result [:result :rungs])))))))

(deftest error-handling-integration-test
  (testing "End-to-end error handling"
    (let [invalid-il "BADOP %I0.0"
          result (convert-il-program invalid-il)]
      (is (not (:success result)))
      (is (string? (:message result)))
      (is (keyword? (:error result))))))

(deftest ld-diagram-generation-test
  (testing "LD diagram text generation"
    (let [sample-program {:type :ld-program
                          :rungs [{:type :contact-load
                                   :operand "%I0.0"
                                   :normally-closed? false}
                                  {:type :coil-store
                                   :operand "%Q0.0"
                                   :normally-closed? false}]}
          diagram (converter/generate-ld-diagram sample-program)]
      (is (string? diagram))
      (is (> (count diagram) 0))
      (is (.contains diagram "Ladder Diagram"))
      (is (.contains diagram "%I0.0"))
      (is (.contains diagram "%Q0.0")))))

(deftest validation-test
  (testing "Program validation"
    (let [valid-program {:type :program
                         :instructions [{:type :instruction
                                         :operation "LD"
                                         :operand "%I0.0"}]}
          invalid-program {:type :invalid
                           :instructions []}]
      (is (= valid-program (parser/validate-program valid-program)))
      (is (thrown? Exception (parser/validate-program invalid-program))))))

(deftest memory-address-parsing-test
  (testing "Different memory address formats"
    (let [addresses ["%I0.0" "%Q1.5" "%M2.3"]
          test-programs (map #(str "LD " %) addresses)]
      (doseq [program test-programs]
        (let [result (parser/parse-il-program program)]
          (is (= :program (:type result)))
          (is (= 1 (count (:instructions result)))))))))

(deftest empty-program-handling-test
  (testing "Empty program handling"
    (let [whitespace-il "   \n  \n  "
          result (convert-il-program whitespace-il)]
      (is (not (:success result)))
      (is (= :validation-error (:error result))))))

(deftest console-mode-test
  (testing "Console mode doesn't crash"
    ;; This test just ensures console mode can be called without exceptions
    ;; In a real environment, you'd capture stdout to test the actual output
    (is (nil? (with-out-str (run-console-mode))))))
