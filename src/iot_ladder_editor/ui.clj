(ns iot-ladder-editor.ui
  (:require [cljfx.api :as fx]
            [iot-ladder-editor.parser :as parser]
            [iot-ladder-editor.converter :as converter]
            [il-to-ld-converter.ld-visualizer :as viz]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [javafx.stage FileChooser FileChooser$ExtensionFilter]))

;; Application state
(defonce *state
  (atom {:il-code ""
         :ld-output ""
         :status "Ready"
         :status-type :info
         :output-format :combined
         :last-conversion-success false}))

;; File operations
(defn load-example-il []
  "LD %I0.0    ; Load input bit 0 (start button)\nANDN %I0.1  ; AND with inverted input bit 1 (stop button)\nOR %Q0.0    ; OR with previous output (seal-in)\nST %Q0.0    ; Store result to output bit 0 (motor)\n\nLD %I0.2    ; Load input bit 2 (emergency stop)\nSTN %Q0.1   ; Store inverted to alarm output")

(defn save-to-file [content filename]
  (try
    (spit filename content)
    {:success true :message (str "Saved to " filename)}
    (catch Exception e
      {:success false :message (str "Failed to save: " (.getMessage e))})))

(defn load-from-file [filename]
  (try
    {:success true :content (slurp filename)}
    (catch Exception e
      {:success false :message (str "Failed to load: " (.getMessage e))})))

;; Event handlers
(defn on-il-code-change [event]
  (swap! *state assoc :il-code event))

(defn on-convert-click [_event]
  (let [il-code (:il-code @*state)
        output-format (:output-format @*state)]
    (if (str/blank? il-code)
      (swap! *state assoc
             :status "Please enter some IL code"
             :status-type :warning
             :last-conversion-success false)
      (let [result (try
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
                          :details (ex-data e)})))]
        (if (:success result)
          (let [ld-program (:result result)
                ld-output (case output-format
                            :ascii (viz/generate-ascii-ld ld-program)
                            :detailed (viz/generate-detailed-ld ld-program)
                            :combined (converter/generate-ld-diagram ld-program))]
            (swap! *state assoc
                   :ld-output ld-output
                   :status (:message result)
                   :status-type :success
                   :last-conversion-success true))
          (swap! *state assoc
                 :ld-output ""
                 :status (:message result)
                 :status-type :error
                 :last-conversion-success false))))))

(defn on-clear-click [_event]
  (swap! *state assoc
         :il-code ""
         :ld-output ""
         :status "Cleared"
         :status-type :info
         :last-conversion-success false))

(defn on-load-example-click [_event]
  (swap! *state assoc
         :il-code (load-example-il)
         :status "Example loaded - Click Convert to see result"
         :status-type :info
         :last-conversion-success false))

(defn on-output-format-change [event]
  (swap! *state assoc :output-format event)
  (when (:last-conversion-success @*state)
    (on-convert-click nil))) ; Auto-reconvert if last conversion was successful

(defn on-save-il-click [_event]
  (let [file-chooser (FileChooser.)]
    (.setTitle file-chooser "Save IL Code")
    (.getExtensionFilters file-chooser)
    (.add (.getExtensionFilters file-chooser)
          (FileChooser$ExtensionFilter. "IL Files" (into-array ["*.il"])))
    (.add (.getExtensionFilters file-chooser)
          (FileChooser$ExtensionFilter. "Text Files" (into-array ["*.txt"])))
    (when-let [file (.showSaveDialog file-chooser nil)]
      (let [result (save-to-file (:il-code @*state) (.getAbsolutePath file))]
        (swap! *state assoc
               :status (:message result)
               :status-type (if (:success result) :success :error))))))

(defn on-load-il-click [_event]
  (let [file-chooser (FileChooser.)]
    (.setTitle file-chooser "Load IL Code")
    (.getExtensionFilters file-chooser)
    (.add (.getExtensionFilters file-chooser)
          (FileChooser$ExtensionFilter. "IL Files" (into-array ["*.il"])))
    (.add (.getExtensionFilters file-chooser)
          (FileChooser$ExtensionFilter. "Text Files" (into-array ["*.txt"])))
    (.add (.getExtensionFilters file-chooser)
          (FileChooser$ExtensionFilter. "All Files" (into-array ["*.*"])))
    (when-let [file (.showOpenDialog file-chooser nil)]
      (let [result (load-from-file (.getAbsolutePath file))]
        (if (:success result)
          (swap! *state assoc
                 :il-code (:content result)
                 :status (str "Loaded from " (.getName file))
                 :status-type :success
                 :last-conversion-success false)
          (swap! *state assoc
                 :status (:message result)
                 :status-type :error))))))

(defn on-save-ld-click [_event]
  (if (str/blank? (:ld-output @*state))
    (swap! *state assoc
           :status "No LD output to save. Convert IL code first."
           :status-type :warning)
    (let [file-chooser (FileChooser.)]
      (.setTitle file-chooser "Save LD Diagram")
      (.getExtensionFilters file-chooser)
      (.add (.getExtensionFilters file-chooser)
            (FileChooser$ExtensionFilter. "Text Files" (into-array ["*.txt"])))
      (when-let [file (.showSaveDialog file-chooser nil)]
        (let [result (save-to-file (:ld-output @*state) (.getAbsolutePath file))]
          (swap! *state assoc
                 :status (:message result)
                 :status-type (if (:success result) :success :error)))))))

(defn on-quit [_event]
  (System/exit 0))

(defn on-show-about [_event]
  (swap! *state assoc
         :status "IL to LD Converter v0.1.0 - Converts IEC 61131-3 Instruction List to Ladder Diagram"
         :status-type :info))

;; UI Components
(defn status-style [status-type]
  (case status-type
    :success {:-fx-text-fill "green" :-fx-font-weight "bold"}
    :error {:-fx-text-fill "red" :-fx-font-weight "bold"}
    :warning {:-fx-text-fill "orange" :-fx-font-weight "bold"}
    :info {:-fx-text-fill "blue"}))

(defn create-toolbar []
  {:fx/type :tool-bar
   :items [{:fx/type :button
            :text "Convert"
            :on-action on-convert-click
            :style {:-fx-font-size "12px"}}
           {:fx/type :separator}
           {:fx/type :label
            :text "Format:"}
           {:fx/type :choice-box
            :value (:output-format @*state)
            :items [:ascii :detailed :combined]
            :on-value-changed on-output-format-change}
           {:fx/type :separator}
           {:fx/type :button
            :text "Clear All"
            :on-action on-clear-click}]})

(defn root-view [{:keys [il-code ld-output status status-type output-format]}]
  {:fx/type :stage
   :showing true
   :title "IL to LD Converter - IEC 61131-3"
   :width 1200
   :height 800
   :on-close-request on-quit
   :scene {:fx/type :scene
           :root {:fx/type :border-pane
                  :top {:fx/type :v-box
                        :children [{:fx/type :menu-bar
                                    :menus [{:fx/type :menu
                                             :text "File"
                                             :items [{:fx/type :menu-item
                                                      :text "Load IL File..."
                                                      :on-action on-load-il-click}
                                                     {:fx/type :menu-item
                                                      :text "Save IL File..."
                                                      :on-action on-save-il-click}
                                                     {:fx/type :separator-menu-item}
                                                     {:fx/type :menu-item
                                                      :text "Save LD Output..."
                                                      :on-action on-save-ld-click}
                                                     {:fx/type :separator-menu-item}
                                                     {:fx/type :menu-item
                                                      :text "Load Example"
                                                      :on-action on-load-example-click}
                                                     {:fx/type :menu-item
                                                      :text "Clear All"
                                                      :on-action on-clear-click}
                                                     {:fx/type :separator-menu-item}
                                                     {:fx/type :menu-item
                                                      :text "Exit"
                                                      :on-action on-quit}]}
                                            {:fx/type :menu
                                             :text "Help"
                                             :items [{:fx/type :menu-item
                                                      :text "About"
                                                      :on-action on-show-about}]}]}
                                   (create-toolbar)]}
                  :center {:fx/type :split-pane
                           :divider-positions [0.5]
                           :items [{:fx/type :v-box
                                    :children [{:fx/type :label
                                                :text "IL Code Input:"
                                                :style {:-fx-font-weight "bold"
                                                        :-fx-font-size "14px"
                                                        :-fx-padding "5"}}
                                               {:fx/type :text-area
                                                :v-box/vgrow :always
                                                :text il-code
                                                :on-text-changed on-il-code-change
                                                :style {:-fx-font-family "monospace"
                                                        :-fx-font-size "12px"}
                                                :prompt-text "Enter IL code here...\nExample:\nLD %I0.0\nAND %I0.1\nST %Q0.0"}]}
                                   {:fx/type :v-box
                                    :children [{:fx/type :label
                                                :text "LD Diagram Output:"
                                                :style {:-fx-font-weight "bold"
                                                        :-fx-font-size "14px"
                                                        :-fx-padding "5"}}
                                               {:fx/type :text-area
                                                :v-box/vgrow :always
                                                :text ld-output
                                                :editable false
                                                :style {:-fx-font-family "monospace"
                                                        :-fx-font-size "11px"
                                                        :-fx-background-color "#f8f8f8"}
                                                :prompt-text "LD diagram will appear here after conversion..."}]}]}
                  :bottom {:fx/type :v-box
                           :children [{:fx/type :separator}
                                      {:fx/type :h-box
                                       :alignment :center-left
                                       :padding 10
                                       :children [{:fx/type :label
                                                   :text "Status: "}
                                                  {:fx/type :label
                                                   :text status
                                                   :style (status-style status-type)}]}]}}}})

;; App definition using simple renderer
(def app
  (fx/create-app *state
                 :desc-fn (fn [state]
                            (root-view state))))

(defn start-app []
  (fx/mount-renderer *state #'app)
  (println "IL to LD Converter GUI started successfully"))

(defn stop-app []
  (fx/unmount-renderer *state #'app)
  (println "IL to LD Converter GUI stopped"))