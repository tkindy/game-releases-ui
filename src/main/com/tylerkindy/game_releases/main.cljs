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

(defn release-row [{:keys [name link release-date]}]
  [:tr
   [:td
    [:a {:href link :target :_blank} name]]
   [:td
    [:time {:datetime release-date} release-date]]])

(defn releases-table []
  (let [releases (:releases @state)]
    [:table.releases-table
     [:thead
      [:tr
       [:th "Game"]
       [:th "Release date"]]]
     [:tbody
      (for [release releases]
        ; TODO: name is not unique key, need to include platforms
        ^{:key name} [release-row release])]]))

(defn releases-table-wrapper []
  (let [releases (:releases @state)]
    (if releases
      [releases-table]
      [:i "Loading..."])))

(defn header []
  [:header
   [:h1 "2022 Game Releases"]
   [:p "This site runs off data scraped from "
    [:a {:href "https://www.gameinformer.com/2022"} "Game Informer"]
    "."]
   [:p [:i "Made by " [:a {:href "https://tylerkindy.com"} "Tyler Kindy"] "."]]])

(defn app []
  [:div
   [header]
   [releases-table-wrapper]])

(defn mount []
  (rdom/render [app]
               (.getElementById js/document "root")))

(defn main []
  (mount)
  (fetch-releases))
