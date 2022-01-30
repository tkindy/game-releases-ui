(ns com.tylerkindy.game-releases.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :as str]
            [clojure.core.match :refer [match]]
            [clojure.set :as set]))

(def nbsp "\u00A0")
(defn use-nbsp [s]
  (str/replace s " " nbsp))

(def platform-map
  (->> {:ps5 "PS5"
        :ps4 "PS4"
        :psvr "PSVR"
        :pc "PC"
        :mac "Mac"
        :linux "Linux"
        :xbox-series "Xbox Series X/S"
        :xbox-one "Xbox One"
        :switch "Switch"
        :stadia "Stadia"
        :ios "iOS"
        :android "Android"}
       (map (fn [[k v]] [k (use-nbsp v)]))
       (into {})))

(def init-filters
  (->> platform-map
       (map key)
       (map (fn [platform] [platform true]))
       (into {})))
(def init-state
  (-> {:sort-dir :asc
       :filters {:open? false}}
      (assoc-in [:filters :platforms] init-filters)))

(defonce state (r/atom init-state))

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
  (let [{:keys [releases sort-dir filters]} @state
        {:keys [platforms]} filters
        selected-platforms (->> platforms
                                keys
                                (filter platforms)
                                (into #{}))]
    (->> releases
         (filter (fn [release]
                   (seq (set/intersection (:platforms release)
                                          selected-platforms))))
         (sort-by :release-date (compare-dates sort-dir)))))

(def releases-url "https://tkindy-public.s3.amazonaws.com/2022-game-releases.json")
(defn fetch-releases []
  (letfn [(clean [releases]
            (map (fn [release]
                   (update release :platforms #(->> %
                                                    (map keyword)
                                                    (into #{}))))
                 releases))]
    (go
      (let [response (<! (http/get releases-url))
            releases (clean (:body response))]
        (swap! state assoc :releases releases)))))

(defn month-str [month]
  (case month
    1  "Jan"
    2  "Feb"
    3  "Mar"
    4  "Apr"
    5  "May"
    6  "Jun"
    7  "Jul"
    8  "Aug"
    9  "Sep"
    10 "Oct"
    11 "Nov"
    12 "Dec"))

(def date-sep #"-")
(defn date-str [date]
  (if date
    (let [[_ month day] (->> (str/split date date-sep)
                             (map js/parseInt))]
      (str (month-str month) nbsp day))
    ""))



(defn build-platforms [platforms]
  (->> platforms
       (map platform-map)
       sort
       (str/join ", ")))

(defn release-row [{:keys [name link release-date platforms]}]
  [:tr
   [:td
    [:a {:href link :target :_blank} name]]
   [:td (build-platforms platforms)]
   [:td
    [:time {:dateTime release-date} (date-str release-date)]]])

(defn cycle-sort []
  (let [dir (:sort-dir @state)
        new-dir (if (= dir :asc)
                  :desc
                  :asc)]
    (swap! state assoc :sort-dir new-dir)))

(defn filter-controls [platforms]
  [:div.filter-region
   [:fieldset.platform-select
    [:legend "Platforms"]
    (for [[key checked] (sort-by key platforms)]
      (let [id (str "platform-" key)
            name (platform-map key)]
        ^{:key key} [:div.platform-option
                     [:input {:id id
                              :type :checkbox
                              :checked checked
                              :on-change #(swap! state
                                                 update-in
                                                 [:filters :platforms key]
                                                 not)}]
                     [:label {:for id} name]]))]])

(defn filter-section []
  (let [{:keys [open? platforms]} (:filters @state)
        content (if open? "Close" "Filter")]
    [:<>
     [:button.filter-btn
      {:on-click #(swap! state update-in [:filters :open?] not)}
      content]
     (when open? [filter-controls platforms])]))

(defn releases-table []
  (let [releases (releases)]
    [:<>
     [filter-section]
     [:table.releases-table
      [:thead
       [:tr
        [:th "Game"]
        [:th "Platforms"]
        [:th.release-date-header
         {:on-click cycle-sort} "Release date"]]]
      [:tbody
       (for [{:keys [name platforms] :as release} releases]
         ^{:key (str name platforms)} [release-row release])]]]))

(defn releases-table-wrapper []
  (let [releases (releases)]
    [:div.releases-table-wrapper
     (if (nil? releases)
       [:i.loading-msg "Loading..."]
       [releases-table])]))

(defn header []
  [:header
   [:h1 "2022 Game Releases"]
   [:p "This site uses data scraped from "
    [:a {:href "https://www.gameinformer.com/2022"} (use-nbsp "Game Informer")]]
   [:p [:i "Made by " [:a {:href "https://tylerkindy.com"}
                       (use-nbsp "Tyler Kindy")]]]])

(defn app []
  [:<>
   [header]
   [releases-table-wrapper]])

(defn mount []
  (rdom/render [app]
               (.getElementById js/document "root")))

(defn main []
  (mount)
  (fetch-releases))
