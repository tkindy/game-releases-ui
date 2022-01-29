(ns com.tylerkindy.game-releases.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :as str]
            [clojure.core.match :refer [match]]))

(defonce state (r/atom {:sort-dir :asc}))

(defn compare-dates [dir]
  (fn [x y]
    (match [x y]
      [nil nil] false
      [nil _]   false
      [_ nil]   true
      [x y]     ((if (= dir :asc)
                   <
                   >) x y))))

(defn releases []
  (let [{:keys [releases sort-dir]} @state]
    (sort-by :release-date (compare-dates sort-dir) releases)))

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

(defn cycle-sort []
  (let [dir (:sort-dir @state)
        new-dir (if (= dir :asc)
                  :desc
                  :asc)]
    (swap! state assoc :sort-dir new-dir)))

(defn releases-table []
  (let [releases (releases)]
    [:table.releases-table
     [:thead
      [:tr
       [:th "Game"]
       [:th "Platforms"]
       [:th {:on-click cycle-sort} "Release date"]]]
     [:tbody
      (for [{:keys [name platforms] :as release} releases]
        ^{:key (str name platforms)} [release-row release])]]))

(defn releases-table-wrapper []
  (let [releases (releases)]
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
