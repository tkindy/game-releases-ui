(ns com.tylerkindy.game-releases.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :as str]))

(defonce state (r/atom {}))

(def releases-url "https://tkindy-public.s3.amazonaws.com/2022-game-releases.json")
(defn fetch-releases []
  (go
    (let [response (<! (http/get releases-url))]
      (swap! state assoc :releases (get-in response [:body])))))

(defn platform-str [platform]
  (case platform
    "ps5" "PlayStation 5"
    "ps4" "PlayStation 4"
    "psvr" "PlayStation VR"
    "pc" "PC"
    "mac" "Mac"
    "linux" "Linux"
    "xbox-series" "Xbox Series X/S"
    "xbox-one" "Xbox One"
    "switch" "Switch"
    "stadia" "Stadia"
    "ios" "iOS"
    "android" "Android"))

(defn build-platforms [platforms]
  (->> platforms
       (map platform-str)
       (str/join ", ")))

(defn release-row [{:keys [name link release-date platforms]}]
  [:tr
   [:td
    [:a {:href link :target :_blank} name]]
   [:td (build-platforms platforms)]
   [:td
    [:time {:dateTime release-date} release-date]]])

(defn releases-table []
  (let [releases (:releases @state)]
    [:table.releases-table
     [:thead
      [:tr
       [:th "Game"]
       [:th "Platforms"]
       [:th "Release date"]]]
     [:tbody
      (for [{:keys [name platforms] :as release} releases]
        ; TODO: name is not unique key, need to include platforms
        ^{:key (str name platforms)} [release-row release])]]))

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
