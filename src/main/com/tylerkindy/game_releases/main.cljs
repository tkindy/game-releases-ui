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

(defn releases-table []
  (let [releases (:releases @state)]
    [:table
     [:thead
      [:tr
       [:th "Game"]
       [:th "Release date"]]]
     [:tbody
      (for [{:keys [name release-date]} releases]
        ; TODO: name is not unique key, need to include platforms
        ^{:key name} [:tr
                      [:th name]
                      [:th release-date]])]]))

(defn app []
  [:div
   [:h1 "Hello, world!"]
   [releases-table]])

(defn mount []
  (rdom/render [app]
               (.getElementById js/document "root")))

(defn main []
  (mount)
  (fetch-releases))
