(defproject il-to-ld-converter "0.1.0-SNAPSHOT"
  :description "IEC 61131-3 Instruction List (IL) to Ladder Diagram (LD) Converter"
  :url "https://github.com/yourusername/il-to-ld-converter"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [instaparse "1.5.0"]
                 [org.clojure/core.match "1.0.1"]
                 [org.clojure/tools.logging "1.2.4"]
                 [cljfx "1.9.5"]]
  :main ^:skip-aot iot-ladder-editor.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:dependencies [[org.clojure/test.check "1.1.1"]]}})

;; Add alias for running tests
;; lein test-all to run all tests
;; lein test-auto to run tests automatically when files change