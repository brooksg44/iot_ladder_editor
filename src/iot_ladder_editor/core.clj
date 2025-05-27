(ns iot-ladder-editor.core
  (:require [cljfx.api :as fx]
            [cljfx.prop :as fx.prop]
            [cljfx.mutator :as fx.mutator]
            [cljfx.lifecycle :as fx.lifecycle]
            [clojure.string :as str])
  (:import [javafx.scene.paint Color]
           [javafx.scene.input KeyCode MouseButton TransferMode]
           [javafx.scene.layout Priority]
           [javafx.geometry Pos Insets]
           [javafx.scene.control Alert$AlertType ButtonType]))

;; Application State
(def *state
  (atom {:project-name "Untitled Project"
         :rungs []
         :selected-rung nil
         :selected-instruction nil
         :instruction-palette-visible true
         :properties-panel-visible true
         :variables {:inputs {} :outputs {} :memory {}}
         :pin-mapping {}
         :mqtt-config {:broker "" :port 1883 :topic-sub "" :topic-pub ""}
         :wifi-config {:ssid "" :password ""}
         :build-output ""}))

;; Ladder Logic Instructions
(def instruction-types
  {:normally-open {:name "NO Contact" :symbol "─| |─" :type :input :color Color/BLACK}
   :normally-closed {:name "NC Contact" :symbol "─|/|─" :type :input :color Color/BLACK}
   :output-coil {:name "Output Coil" :symbol "─( )─" :type :output :color Color/RED}
   :set-coil {:name "Set Coil" :symbol "─(S)─" :type :output :color Color/GREEN}
   :reset-coil {:name "Reset Coil" :symbol "─(R)─" :type :output :color Color/BLUE}
   :timer-on {:name "Timer ON" :symbol "TON" :type :function :color Color/PURPLE}
   :timer-off {:name "Timer OFF" :symbol "TOF" :type :function :color Color/PURPLE}
   :counter {:name "Counter" :symbol "CTU" :type :function :color Color/ORANGE}
   :compare {:name "Compare" :symbol "CMP" :type :function :color Color/CYAN}})

;; Instruction Component
(defn draw-instruction [instruction x y selected?]
  (let [inst-type (get instruction-types (:type instruction))
        color (if selected? Color/YELLOW (:color inst-type))]
    {:fx/type :group
     :children
     [{:fx/type :rectangle
       :x x :y y
       :width 80 :height 40
       :fill Color/TRANSPARENT
       :stroke color
       :stroke-width (if selected? 3 2)
       :on-mouse-clicked {:event/type :instruction-selected
                          :instruction instruction}}
      {:fx/type :text
       :x (+ x 40) :y (+ y 25)
       :text (:symbol inst-type)
       :text-alignment :center
       :fill color
       :font {:family "Courier New" :size 12 :weight :bold}}
      {:fx/type :text
       :x (+ x 40) :y (+ y 55)
       :text (or (:address instruction) "")
       :text-alignment :center
       :fill Color/BLACK
       :font {:family "Arial" :size 10}}]}))

;; Rung Component
(defn draw-rung [rung-index rung selected-rung?]
  (let [rung-y (* rung-index 100)
        instructions (:instructions rung)]
    {:fx/type :group
     :children
     (concat
      ;; Power rails
      [{:fx/type :line
        :start-x 50 :start-y (+ rung-y 50)
        :end-x 50 :end-y (+ rung-y 90)
        :stroke Color/BLACK :stroke-width 3}
       {:fx/type :line
        :start-x 650 :start-y (+ rung-y 50)
        :end-x 650 :end-y (+ rung-y 90)
        :stroke Color/BLACK :stroke-width 3}
       ;; Horizontal connection line
       {:fx/type :line
        :start-x 50 :start-y (+ rung-y 70)
        :end-x 650 :end-y (+ rung-y 70)
        :stroke Color/BLACK :stroke-width 2}
       ;; Rung number
       {:fx/type :text
        :x 20 :y (+ rung-y 75)
        :text (str rung-index)
        :fill Color/BLUE
        :font {:family "Arial" :size 14 :weight :bold}}]
      ;; Instructions
      (map-indexed
       (fn [inst-index instruction]
         (draw-instruction instruction
                           (+ 100 (* inst-index 90))
                           (+ rung-y 50)
                           (and selected-rung?
                                (= instruction (:selected-instruction @*state)))))
       instructions))}))

;; Instruction Palette
(defn instruction-palette []
  {:fx/type :v-box
   :pref-width 200
   :spacing 5
   :padding 10
   :style "-fx-background-color: #f0f0f0; -fx-border-color: #cccccc;"
   :children
   (concat
    [{:fx/type :label
      :text "Instruction Palette"
      :font {:family "Arial" :size 14 :weight :bold}}]
    (map (fn [[type-key type-info]]
           {:fx/type :button
            :text (:name type-info)
            :pref-width 180
            :style (str "-fx-background-color: "
                        (cond
                          (= (:type type-info) :input) "#e6f3ff"
                          (= (:type type-info) :output) "#ffe6e6"
                          (= (:type type-info) :function) "#f0e6ff"
                          :else "#f0f0f0"))
            :on-action {:event/type :add-instruction
                        :instruction-type type-key}})
         instruction-types))})

;; Properties Panel
(defn properties-panel []
  (let [selected-inst (:selected-instruction @*state)]
    {:fx/type :v-box
     :pref-width 250
     :spacing 10
     :padding 10
     :style "-fx-background-color: #f8f8f8; -fx-border-color: #cccccc;"
     :children
     [{:fx/type :label
       :text "Properties"
       :font {:family "Arial" :size 14 :weight :bold}}
      {:fx/type :separator}
      (if selected-inst
        {:fx/type :v-box
         :spacing 5
         :children
         [{:fx/type :label
           :text "Instruction Properties"}
          {:fx/type :h-box
           :spacing 5
           :alignment :center-left
           :children
           [{:fx/type :label
             :text "Address:"
             :pref-width 60}
            {:fx/type :text-field
             :text (or (:address selected-inst) "")
             :pref-width 100
             :on-text-changed {:event/type :update-instruction-address}}]}
          {:fx/type :h-box
           :spacing 5
           :alignment :center-left
           :children
           [{:fx/type :label
             :text "Comment:"
             :pref-width 60}
            {:fx/type :text-field
             :text (or (:comment selected-inst) "")
             :pref-width 150
             :on-text-changed {:event/type :update-instruction-comment}}]}]}
        {:fx/type :label
         :text "No instruction selected"
         :text-fill Color/GRAY})]}))

;; Menu Bar
(defn menu-bar []
  {:fx/type :menu-bar
   :menus
   [{:fx/type :menu
     :text "File"
     :items
     [{:fx/type :menu-item
       :text "New Project"
       :on-action {:event/type :new-project}}
      {:fx/type :menu-item
       :text "Open Project"
       :on-action {:event/type :open-project}}
      {:fx/type :menu-item
       :text "Save Project"
       :on-action {:event/type :save-project}}
      {:fx/type :separator-menu-item}
      {:fx/type :menu-item
       :text "Exit"
       :on-action {:event/type :exit-app}}]}
    {:fx/type :menu
     :text "Edit"
     :items
     [{:fx/type :menu-item
       :text "Add Rung"
       :on-action {:event/type :add-rung}}
      {:fx/type :menu-item
       :text "Delete Rung"
       :on-action {:event/type :delete-rung}}]}
    {:fx/type :menu
     :text "Project"
     :items
     [{:fx/type :menu-item
       :text "Properties"
       :on-action {:event/type :show-project-properties}}
      {:fx/type :menu-item
       :text "Variables"
       :on-action {:event/type :show-variables}}
      {:fx/type :menu-item
       :text "Build"
       :on-action {:event/type :build-project}}]}
    {:fx/type :menu
     :text "View"
     :items
     [{:fx/type :check-menu-item
       :text "Instruction Palette"
       :selected (:instruction-palette-visible @*state)
       :on-action {:event/type :toggle-instruction-palette}}
      {:fx/type :check-menu-item
       :text "Properties Panel"
       :selected (:properties-panel-visible @*state)
       :on-action {:event/type :toggle-properties-panel}}]}]})

;; Toolbar
(defn toolbar []
  {:fx/type :tool-bar
   :items
   [{:fx/type :button
     :text "New"
     :on-action {:event/type :new-project}}
    {:fx/type :button
     :text "Open"
     :on-action {:event/type :open-project}}
    {:fx/type :button
     :text "Save"
     :on-action {:event/type :save-project}}
    {:fx/type :separator}
    {:fx/type :button
     :text "Add Rung"
     :on-action {:event/type :add-rung}}
    {:fx/type :button
     :text "Delete Rung"
     :on-action {:event/type :delete-rung}}
    {:fx/type :separator}
    {:fx/type :button
     :text "Build"
     :on-action {:event/type :build-project}}]})

;; Ladder Canvas
(defn ladder-canvas []
  (let [rungs (:rungs @*state)]
    {:fx/type :scroll-pane
     :fit-to-width true
     :content
     {:fx/type :pane
      :pref-width 800
      :pref-height (max 400 (* (count rungs) 100))
      :style "-fx-background-color: white;"
      :children
      (map-indexed
       (fn [index rung]
         (draw-rung index rung (= index (:selected-rung @*state))))
       rungs)}}))

;; Status Bar
(defn status-bar []
  {:fx/type :h-box
   :alignment :center-left
   :spacing 10
   :padding 5
   :style "-fx-background-color: #f0f0f0; -fx-border-color: #cccccc;"
   :children
   [{:fx/type :label
     :text (str "Project: " (:project-name @*state))}
    {:fx/type :separator}
    {:fx/type :label
     :text (str "Rungs: " (count (:rungs @*state)))}]})

;; Build Output Panel
(defn build-output-panel []
  {:fx/type :v-box
   :pref-height 150
   :children
   [{:fx/type :label
     :text "Build Output"
     :font {:family "Arial" :size 12 :weight :bold}}
    {:fx/type :text-area
     :text (:build-output @*state)
     :editable false
     :font {:family "Courier New" :size 10}
     :v-box/vgrow Priority/ALWAYS}]})

;; Main Layout
(defn main-view [{:keys [instruction-palette-visible properties-panel-visible]}]
  {:fx/type :stage
   :showing true
   :title "IoT Ladder Editor - Clojure"
   :width 1200
   :height 800
   :scene
   {:fx/type :scene
    :root
    {:fx/type :border-pane
     :top {:fx/type :v-box
           :children [(menu-bar) (toolbar)]}
     :left (when instruction-palette-visible
             (instruction-palette))
     :center {:fx/type :split-pane
              :orientation :vertical
              :items [(ladder-canvas)
                      (build-output-panel)]}
     :right (when properties-panel-visible
              (properties-panel))
     :bottom (status-bar)}}})

;; Arduino Code Generation
(defn generate-rung-code [instructions]
  (if (empty? instructions)
    "// Empty rung"
    (let [inputs (filter #(#{:normally-open :normally-closed} (:type %)) instructions)
          outputs (filter #(#{:output-coil :set-coil :reset-coil} (:type %)) instructions)]
      (str "bool rung_result = "
           (if (empty? inputs)
             "true"
             (str/join " && "
                       (map #(case (:type %)
                               :normally-open (str "digitalRead(" (:address % "INPUT_PIN_1") ")")
                               :normally-closed (str "!digitalRead(" (:address % "INPUT_PIN_1") ")"))
                            inputs)))
           ";\n  "
           (str/join "\n  "
                     (map #(case (:type %)
                             :output-coil (str "digitalWrite(" (:address % "OUTPUT_PIN_1") ", rung_result);")
                             :set-coil (str "if (rung_result) digitalWrite(" (:address % "OUTPUT_PIN_1") ", HIGH);")
                             :reset-coil (str "if (rung_result) digitalWrite(" (:address % "OUTPUT_PIN_1") ", LOW);"))
                          outputs))))))

(defn generate-ladder-logic-code [rungs]
  (str/join "\n  "
            (map-indexed
             (fn [rung-index rung]
               (str "// Rung " rung-index "\n  "
                    (generate-rung-code (:instructions rung))))
             rungs)))

(defn generate-arduino-code [rungs]
  (str "// Generated Arduino Code for ESP32\n"
       "#include <WiFi.h>\n"
       "#include <PubSubClient.h>\n\n"
       "// Pin definitions\n"
       "#define INPUT_PIN_1 2\n"
       "#define OUTPUT_PIN_1 4\n\n"
       "// WiFi and MQTT configuration\n"
       "const char* ssid = \"YOUR_WIFI_SSID\";\n"
       "const char* password = \"YOUR_WIFI_PASSWORD\";\n"
       "const char* mqtt_server = \"YOUR_MQTT_BROKER\";\n\n"
       "WiFiClient espClient;\n"
       "PubSubClient client(espClient);\n\n"
       "void setup() {\n"
       "  Serial.begin(115200);\n"
       "  pinMode(INPUT_PIN_1, INPUT);\n"
       "  pinMode(OUTPUT_PIN_1, OUTPUT);\n"
       "  \n"
       "  // WiFi connection\n"
       "  WiFi.begin(ssid, password);\n"
       "  while (WiFi.status() != WL_CONNECTED) {\n"
       "    delay(500);\n"
       "    Serial.print(\".\");\n"
       "  }\n"
       "  Serial.println(\"WiFi connected\");\n"
       "  \n"
       "  // MQTT setup\n"
       "  client.setServer(mqtt_server, 1883);\n"
       "}\n\n"
       "void loop() {\n"
       "  if (!client.connected()) {\n"
       "    reconnect();\n"
       "  }\n"
       "  client.loop();\n"
       "  \n"
       "  // Ladder Logic Execution\n"
       (generate-ladder-logic-code rungs)
       "  \n"
       "  delay(100);\n"
       "}\n\n"
       "void reconnect() {\n"
       "  while (!client.connected()) {\n"
       "    if (client.connect(\"ESP32Client\")) {\n"
       "      Serial.println(\"MQTT connected\");\n"
       "    } else {\n"
       "      delay(5000);\n"
       "    }\n"
       "  }\n"
       "}\n"))

;; Event Handlers
(defmulti handle-event (fn [event state] (:event/type event)))

(defmethod handle-event :new-project [event state]
  (assoc state
         :project-name "Untitled Project"
         :rungs []
         :selected-rung nil
         :selected-instruction nil
         :build-output ""))

(defmethod handle-event :add-rung [event state]
  (update state :rungs conj {:instructions []}))

(defmethod handle-event :delete-rung [event state]
  (if-let [selected (:selected-rung state)]
    (-> state
        (update :rungs (fn [rungs] (vec (concat (take selected rungs)
                                                (drop (inc selected) rungs)))))
        (assoc :selected-rung nil :selected-instruction nil))
    state))

(defmethod handle-event :add-instruction [event state]
  (if-let [selected-rung (:selected-rung state)]
    (let [new-instruction {:type (:instruction-type event)
                           :address ""
                           :comment ""}]
      (update-in state [:rungs selected-rung :instructions] conj new-instruction))
    ;; If no rung selected, add to first rung or create new rung
    (if (empty? (:rungs state))
      (-> state
          (update :rungs conj {:instructions [{:type (:instruction-type event)
                                               :address ""
                                               :comment ""}]})
          (assoc :selected-rung 0))
      (update-in state [:rungs 0 :instructions]
                 conj {:type (:instruction-type event)
                       :address ""
                       :comment ""}))))

(defmethod handle-event :instruction-selected [event state]
  (assoc state :selected-instruction (:instruction event)))

(defmethod handle-event :update-instruction-address [event state]
  ;; This would update the selected instruction's address
  ;; Implementation depends on how instructions are stored and referenced
  state)

(defmethod handle-event :toggle-instruction-palette [event state]
  (update state :instruction-palette-visible not))

(defmethod handle-event :toggle-properties-panel [event state]
  (update state :properties-panel-visible not))

(defmethod handle-event :build-project [event state]
  (let [arduino-code (generate-arduino-code (:rungs state))]
    (assoc state :build-output arduino-code)))

(defmethod handle-event :default [event state]
  (println "Unhandled event:" (:event/type event))
  state)


;; Main Application
(defn -main []
  (fx/mount-renderer
   *state
   (fx/create-renderer
    :middleware (fx/wrap-map-desc
                 (fn [state]
                   (merge state {:fx/type main-view})))
    :opts {:fx.opt/map-event-handler
           (fn [event]
             (swap! *state handle-event event))})))

;; For REPL development
(comment
  (-main)

  ;; Reset state
  (reset! *state {:project-name "Test Project"
                  :rungs [{:instructions [{:type :normally-open :address "I1" :comment "Start"}
                                          {:type :output-coil :address "Q1" :comment "Motor"}]}]
                  :selected-rung 0
                  :selected-instruction nil
                  :instruction-palette-visible true
                  :properties-panel-visible true
                  :variables {:inputs {} :outputs {} :memory {}}
                  :pin-mapping {}
                  :mqtt-config {:broker "" :port 1883 :topic-sub "" :topic-pub ""}
                  :wifi-config {:ssid "" :password ""}
                  :build-output ""})

  ;; Test code generation
  (println (generate-arduino-code (:rungs @*state))))