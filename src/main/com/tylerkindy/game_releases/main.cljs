(ns com.tylerkindy.game-releases.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defonce state (r/atom {}))

(def releases-url "https://tkindy-public.s3.amazonaws.com/2022-game-releases.json")
(defn fetch-releases []
  (go
    (let [response (<! (http/get releases-url))]
      (swap! state assoc :releases (get-in response [:body])))))

(defn simple-component []
  [:div
   [:p "I am a component!"]
   [:p.someclass
    "I have " [:strong "bold"]
    [:span {:style {:color "red"}} " and red "] "text."]])

(defn mount []
  (rdom/render [simple-component]
               (.getElementById js/document "root")))

(defn main []
  (mount)
  (fetch-releases))
