(ns com.tylerkindy.game-releases.main
  (:require [reagent.dom :as rdom]))

(defn simple-component []
  [:div
   [:p "I am a component!"]
   [:p.someclass
    "I have " [:strong "bold"]
    [:span {:style {:color "red"}} " and red "] "text."]])

(defn main []
  (rdom/render [simple-component]
               (.getElementById js/document "root")))
