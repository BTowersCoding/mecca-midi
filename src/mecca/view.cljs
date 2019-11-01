(ns ^:figwheel-hooks mecca.view
  (:require [mecca.music :as music :refer [audiocontext]]
            [mecca.subs :as subs]
            [reagent.core :as r]
            [re-frame.core :as rf :refer [subscribe dispatch]]
            [mecca.events :as events]
            [mecca.notation :as notation]
            [mecca.castle :as castle]
            [mecca.transport :as transport]
            [mecca.editor :as editor :refer [svg-paths]]
            [mecca.xml :as xml]
            [mecca.upload :as upload]
            [mecca.mario :as mario :refer [mario]]
            [cljs.reader :refer [read-string]]))

#_(defn bar-number [n x y scale]
  [:path {:transform (str "scale(" scale ") translate(" x "," y ")")
          :stroke "#000000"
          :d (case n
               0 "M3 0h6M3 1h6M3 2h6M0 3h3M9 3h3M0 4h3M9 4h3M0 5h3M9 5h3M0 6h3M9 6h3M0 7h3M9 7h3M0 8h3M9 8h3M0 9h3M9 9h3M0 10h3M9 10h3M0 11h3M9 11h3M0 12h3M9 12h3M0 13h3M9 13h3M0 14h3M9 14h3M0 15h3M9 15h3M0 16h3M9 16h3M0 17h3M9 17h3M3 18h6M3 19h6M3 20h6"
               1 "M3 0h3M3 1h3M3 2h3M0 3h6M0 4h6M0 5h6M3 6h3M3 7h3M3 8h3M3 9h3M3 10h3M3 11h3M3 12h3M3 13h3M3 14h3M3 15h3M3 16h3M3 17h3M0 18h9M0 19h9M0 20h9"
               2 "M3 0h6M3 1h6M3 2h6M0 3h3M9 3h3M0 4h3M9 4h3M0 5h3M9 5h3M0 6h3M9 6h3M0 7h3M9 7h3M0 8h3M9 8h3M6 9h3M6 10h3M6 11h3M3 12h3M3 13h3M3 14h3M0 15h3M0 16h3M0 17h3M0 18h12M0 19h12M0 20h12"
               3 "M3 0h6M3 1h6M3 2h6M0 3h3M9 3h3M0 4h3M9 4h3M0 5h3M9 5h3M9 6h3M9 7h3M9 8h3M3 9h6M3 10h6M3 11h6M9 12h3M9 13h3M9 14h3M0 15h3M9 15h3M0 16h3M9 16h3M0 17h3M9 17h3M3 18h6M3 19h6M3 20h6"
               4 "M9 0h3M9 1h3M9 2h3M6 3h6M6 4h6M6 5h6M3 6h3M9 6h3M3 7h3M9 7h3M3 8h3M9 8h3M0 9h3M9 9h3M0 10h3M9 10h3M0 11h3M9 11h3M0 12h15M0 13h15M0 14h15M9 15h3M9 16h3M9 17h3M9 18h3M9 19h3M9 20h3"
               5 "M0 0h12M0 1h12M0 2h12M0 3h3M0 4h3M0 5h3M0 6h9M0 7h9M0 8h9M9 9h3M9 10h3M9 11h3M9 12h3M9 13h3M9 14h3M0 15h3M9 15h3M0 16h3M9 16h3M0 17h3M9 17h3M3 18h6M3 19h6M3 20h6"
               6 "M3 0h6M3 1h6M3 2h6M0 3h3M9 3h3M0 4h3M9 4h3M0 5h3M9 5h3M0 6h3M0 7h3M0 8h3M0 9h9M0 10h9M0 11h9M0 12h3M9 12h3M0 13h3M9 13h3M0 14h3M9 14h3M0 15h3M9 15h3M0 16h3M9 16h3M0 17h3M9 17h3M3 18h6M3 19h6M3 20h6"
               7 "M0 0h12M0 1h12M0 2h12M0 3h3M9 3h3M0 4h3M9 4h3M0 5h3M9 5h3M9 6h3M9 7h3M9 8h3M6 9h3M6 10h3M6 11h3M6 12h3M6 13h3M6 14h3M3 15h3M3 16h3M3 17h3M3 18h3M3 19h3M3 20h3"
               8 "M3 0h6M3 1h6M3 2h6M0 3h3M9 3h3M0 4h3M9 4h3M0 5h3M9 5h3M0 6h3M9 6h3M0 7h3M9 7h3M0 8h3M9 8h3M3 9h6M3 10h6M3 11h6M0 12h3M9 12h3M0 13h3M9 13h3M0 14h3M9 14h3M0 15h3M9 15h3M0 16h3M9 16h3M0 17h3M9 17h3M3 18h6M3 19h6M3 20h6"
               9 "M3 0h6M3 1h6M3 2h6M0 3h3M9 3h3M0 4h3M9 4h3M0 5h3M9 5h3M0 6h3M9 6h3M0 7h3M9 7h3M0 8h3M9 8h3M3 9h9M3 10h9M3 11h9M9 12h3M9 13h3M9 14h3M0 15h3M9 15h3M0 16h3M9 16h3M0 17h3M9 17h3M3 18h6M3 19h6M3 20h6")}])

(defn note-guides []
  (let [editor-x (subscribe [:editor-beat-start])]
    (fn []
      (into [:g]
            (for [beat (range 0 9 0.5)]
              (if (= 0
                     (mod (+ (dec @editor-x) beat) 4))
                [:g
                 ;[bar-number (/ beat 4) (+ 296 (* 120 (mod beat 8))) 64 0.05]
                 [:line {:x1 (+ 8 (* 6 beat)) :x2 (+ 8 (* 6 beat))
                         :y1 4 :y2 21 :stroke "orange"
                         :stroke-width 0.25
                         :stroke-dasharray 0.5}]]
                [:line {:x1 (+ 8 (* 6 beat)) :x2 (+ 8 (* 6 beat))
                        :y1 4 :y2 21 :stroke "black"
                        :stroke-width 0.075
                        :stroke-dasharray 0.5}]))))))


(defn note-targets []
  (let [instrument (subscribe [:instrument])
        editor-x (subscribe [:editor-beat-start])
        sharp? (subscribe [:sharp?])]
    (fn []
      (into [:g]
            (for [time (range 0 9 0.5)
                  pitch (range 19)]
              ^{:key [time pitch]}
              [:rect {:transform "translate(6.5,4)"
                      :x (* 6 time)
                      :y (- pitch 1)
                      :height 1 :width 3
                      :stroke "black"
                      :stroke-width 0.2
                      :fill "gray"
                      :visibility "hidden"
                      :opacity 0.2
                      :pointer-events "all"
                      :on-mouse-over #(if-not (and 
                                               @sharp?
                                               (or (= pitch 12)
                                                   (= pitch 15)
                                                   (= pitch 8)
                                                   (= pitch 5)
                                                   (= pitch 1)
                                                   (= pitch 0)))
                                        (dispatch [:update-focus-note [time pitch]]))
                      :on-mouse-out #(dispatch [:update-focus-note [nil nil]])
                      :on-click (let [pitches [84 83 81 79 77 76 74 72 71 69 67 65 64 62 60 59 57 55 53]]
                                  (cond
                                    @(subscribe [:eraser?])
                                    #(do (music/play-sample 18 63)
                                       (dispatch [:remove-note (+ time (dec @editor-x))
                                                  (get pitches pitch)]))
                                    @(subscribe [:repeat?])
                                    #(dispatch [:set-loop-end time])
                                    :else
                                    #(dispatch [:add-note @instrument
                                                (+ time (dec @editor-x))
                                                (get pitches pitch)])))}])))))

(defn note-cursor []
  (let [focused (subscribe [:focused-note-pos])
        instrument (subscribe [:instrument])
        sharp? (subscribe [:sharp?])]
       (when-not (= @focused [nil nil])
         (let [x (first @focused) y (last @focused)]
           [:g
            (if @sharp? (svg-paths [["black" "M15 46C15 47 14 47 13 47 13 47 12 47 12 46V37L7 39V49C7 49 6 50 6 50 5 50 5 49 5 49V40L3 40C3 40 2 40 2 40 2 40 1 40 1 39V35C1 35 1 34 2 34L5 33V23L3 24C3 24 2 24 2 24 2 24 1 23 1 23V19C1 19 1 18 2 18L5 17V7C5 6 5 6 6 6 6 6 7 6 7 7V16L12 14V4C12 4 13 3 13 3 14 3 15 4 15 4V13L17 13C17 12 17 12 17 12 18 12 18 13 18 14V17C18 18 18 18 17 19L15 20V30L17 29C17 29 17 29 17 29 18 29 18 29 18 30V34C18 34 18 35 17 35L15 36V46ZM7 22V32L12 31V21Z"]]
                                   (+ 68 (* 86 x)) (+ 18 (* 15 y)) 0.07))
            (cond
              @(subscribe [:eraser?])
              [editor/eraser-cursor (+ 36 (* 30 x)) (+ (* 5 y) 20) 0.2]
              @(subscribe [:repeat?])
              [editor/repeat-sign (+ 7 (* 6 x)) 8 0.13]
              :else
              (case @instrument
                1 [mario/mario-icon (+ 2 (* 30 x)) (+ (* 5 y) 9) 0.2]
                2 [mario/shroom (+ 32 (* 30 x)) (+ (* 5 y) 12) 0.2]
                3 [mario/yoshi (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                4 [mario/star (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                5 [mario/flower (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                6 [mario/gb (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                7 [mario/dog (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                8 [mario/kitty (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                9 [mario/pig (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                10 [mario/swan (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                11 [mario/face (+ 32 (* 30 x)) (+ (* 5 y) 10) 0.2]
                12 [mario/plane (+ 32 (* 30 x)) (+ (* 5 y) 15) 0.2]
                13 [mario/boat (+ 32  (* 30 x)) (+ (* 5 y) 12) 0.2]
                14 [mario/car (+ 32 (* 30 x)) (+ (* 5 y) 12) 0.2]
                15 [mario/heart (+ 32 (* 30 x)) (+ (* 5 y) 15) 0.2]))]))))

(defn score-notes []
   (let [notes (subscribe [:notes])
         editor-x (subscribe [:editor-beat-start])]
     (into [:g]
           (for [{:keys [time instrument pitch]} @notes
                 :when (<= (dec @editor-x) time (+ 16 (dec @editor-x)))]
                    ^{:key [instrument time pitch]}
                    (let [x (- time (dec @editor-x))
                          pitch-map (zipmap [84 83 81 79 77 76 74 72 71 69 67 65 64 62 60 59 57 55 53]
                                            (range 19))]
                      [:g
                       (if-not (get pitch-map pitch)
                        (svg-paths [["black" "M15 46C15 47 14 47 13 47 13 47 12 47 12 46V37L7 39V49C7 49 6 50 6 50 5 50 5 49 5 49V40L3 40C3 40 2 40 2 40 2 40 1 40 1 39V35C1 35 1 34 2 34L5 33V23L3 24C3 24 2 24 2 24 2 24 1 23 1 23V19C1 19 1 18 2 18L5 17V7C5 6 5 6 6 6 6 6 7 6 7 7V16L12 14V4C12 4 13 3 13 3 14 3 15 4 15 4V13L17 13C17 12 17 12 17 12 18 12 18 13 18 14V17C18 18 18 18 17 19L15 20V30L17 29C17 29 17 29 17 29 18 29 18 29 18 30V34C18 34 18 35 17 35L15 36V46ZM7 22V32L12 31V21Z"]]
                                   (+ 68 (* 86 x)) (+ 18 (* 15 (get pitch-map (dec pitch)))) 0.07))
                      (case instrument
                        1 [mario/mario-note (+ 2 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 9) 0.2]
                        2 [mario/shroom (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 12) 0.2]
                        3 [mario/yoshi (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        4 [mario/star (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        5 [mario/flower (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        6 [mario/gb (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        7 [mario/dog (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        8 [mario/kitty (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        9 [mario/pig (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        10 [mario/swan (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        11 [mario/face (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 10) 0.2]
                        12 [mario/plane (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 15) 0.2]
                        13 [mario/boat (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 12) 0.2]
                        14 [mario/car (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 12) 0.2]
                        15 [mario/heart (+ 32 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 15) 0.2]
                        [mario/mario-note (+ 2 (* 30 x)) (+ (* 5 (or (get pitch-map pitch) (get pitch-map (dec pitch)))) 9) 0.2])])))))

(defn editor []
  (let [notes (subscribe [:notes])
        focused (subscribe [:focused-note-pos])
        current-position (subscribe [:current-position])
        editor-x (subscribe [:editor-beat-start])
        play-start (subscribe [:play-start])
        mario-run (subscribe [:mario-run])
        mario-jump (subscribe [:mario-jump])
        instrument (subscribe [:instrument])]
    (fn []
      (if (= 20 @mario-run)
        (dispatch [:jump-reset]))
      [:svg {:width "100%"
             :view-box "0 0 64 36"
             :style {:cursor "url(/images/hand.png),crosshair"}}
       [mario/cloud 1 1]
       [mario/hill 40]
       [castle/brick-face 363 18 6]
       [castle/brick-face 348 48 10]
       [mario/mario-sm]
       [editor/current-note-display 47 0 0.22]
       [editor/note-blocks]
       [mario/floor-tile 16]
       [:rect#editorframe
          {:stroke "black"
           :stroke-width 0.2
           :fill "none"
           :height 20 :width 63.5 :x 0.25 :y 14.5}]
       [:g.staff {:transform "translate(0,13.5) scale(1)"}
        [notation/staff-lines]
        [editor/retract-editor 2]
         [notation/treble-clef
          (- 0.8 (* 6 (dec @editor-x)))
          6.3]
        [editor/advance-editor]
        [editor/advance-measure]
        [editor/advance-end]
        [note-targets]
        [note-guides]
        [note-cursor]
        [score-notes]
        (if @(subscribe [:loop-end])
          [editor/repeat-sign (+ 7 (* 6 @(subscribe [:loop-end]))) 8 0.13])]])))

(defn debug-info []
  [:div
    [:p (str "Play start: " @(subscribe [:play-start]))]
   [:p (str "Song time: " 
 (- (.-currentTime @audiocontext) @(subscribe [:play-start])))]
[:p "Song data: "]
(into [:p]
  (for [note @(subscribe [:notes])]
    (pr-str note)))
   ;[:p (str "Mario run: " @(subscribe [:mario-run]))]
   [:p 
    (let [mouse-pos (subscribe [:focused-note-pos])]
      (str "Beat: " (first @mouse-pos) " Pitch: " (last @mouse-pos)))]
   [:p (str "Tempo: " @(subscribe [:tempo]))]
   [:p (str "Current position: " @(subscribe [:current-position]))]
   [:p (str "Editor beat start: " @(subscribe [:editor-beat-start]))]])

(defn mecca []
  [:div
   [editor]
   [transport/transport 140 0 0.5]
   [editor/toolbar 71 0]
   [:div
    [:label {:for "edn"} "Load song data:"
     [:p]
     [:form
      {:on-submit
       (fn [e]
         (.preventDefault e)
         (dispatch [:set-notes (read-string (.. e -target -elements -edn -value))]))}
      [:textarea {:id "edn" :name "edn"
                  :rows 8 :cols 38}]
      [:p]
      [:input {:type "submit" :value "Import"}]]]]
    [:div
     [:input#input {:type "file"
                    :on-change
                    upload/put-upload}]]
   [debug-info]
   #_[:svg
    (svg-paths [["#000000" "M0 0h14M0 1h1M13 1h1M0 2h1M4 2h1M13 2h1M0 3h1M3 3h2M13 3h1M0 4h1M2 4h1M4 4h1M9 4h1M13 4h1M0 5h1M2 5h4M8 5h1M13 5h1M0 6h1M4 6h1M7 6h1M13 6h1M0 7h1M6 7h1M9 7h1M13 7h1M0 8h1M5 8h1M8 8h2M13 8h1M0 9h1M4 9h1M7 9h1M9 9h1M13 9h1M0 10h1M7 10h4M13 10h1M0 11h1M9 11h1M13 11h1M0 12h1M13 12h1M0 13h1M13 13h1M0 14h14"]
                ["#d6944a" "M1 1h1M12 1h1M1 13h1"]
                ["#ffffff" "M2 1h10M1 2h1M1 3h1M1 4h1M1 5h1M1 6h1M1 7h1M1 8h1M1 9h1M1 10h1M1 11h1M1 12h1"]
                ["#ffff00" "M2 2h2M5 2h7M2 3h1M5 3h7M3 4h1M5 4h4M10 4h2M6 5h2M9 5h3M2 6h2M5 6h2M8 6h4M2 7h4M7 7h2M10 7h2M2 8h3M6 8h2M10 8h2M2 9h2M5 9h2M8 9h1M10 9h2M2 10h5M11 10h1M2 11h7M10 11h2M2 12h10"]
                ["#b54a00" "M12 2h1M12 3h1M12 4h1M12 5h1M12 6h1M12 7h1M12 8h1M12 9h1M12 10h1M12 11h1M12 12h1M2 13h11"]]
               0 0 5)]])
