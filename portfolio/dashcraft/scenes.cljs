(ns dashcraft.scenes
  (:require
   [portfolio.replicant :refer [defscene]]
   [portfolio.ui :as portfolio]
   [replicant.dom :as replicant]
   [robertluo.dashcraft.chart :as ch]
   [robertluo.dashcraft.data-table :as dt]
   [robertluo.dashcraft.edn-editor :as ee]
   [robertluo.dashcraft.form :as form]
   [robertluo.dashcraft.loading :as loading]
   [robertluo.dashcraft.error-aware :as error-aware]))

(def bar-chart-data
  {:columns [:product "2015" "2016"]
   :rows [{:product "Shirts"    "2015" 15.3 "2016" 5}
          {:product "Cardigans" "2015" 9.1  "2016" 20}
          {:product "Socks"     "2015" 4.8  "2016" 25}]
   :xAxis {:type :category}
   :yAxis {}
   :series [{:type :bar} {:type :bar}]
   :legend {}
   :tooltip {}})

(def ^:private sun-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg" :width "20" :height "20" :viewBox "0 0 256 256" :fill "currentColor"}
   [:path {:d "M128,60a68,68,0,1,0,68,68A68,68,0,0,0,128,60Zm0,120a52,52,0,1,1,52-52A52,52,0,0,1,128,180ZM120,36V16a8,8,0,0,1,16,0V36a8,8,0,0,1-16,0Zm0,204V220a8,8,0,0,1,16,0v20a8,8,0,0,1-16,0ZM60.46,66.27,46.63,52.46A8,8,0,0,1,57.94,41.14L71.77,55a8,8,0,0,1-11.31,11.31ZM240,128a8,8,0,0,1-8,8H212a8,8,0,0,1,0-16h20A8,8,0,0,1,240,128ZM52,128a8,8,0,0,1-8,8H24a8,8,0,0,1,0-16H44A8,8,0,0,1,52,128Zm143.54,61.73a8,8,0,0,1,0,11.31l-13.83,13.82a8,8,0,0,1-11.31-11.31L184.23,190A8,8,0,0,1,195.54,189.73ZM71.77,201a8,8,0,0,1-11.31,11.31L46.63,198.46a8,8,0,0,1,11.31-11.31ZM209.37,57.54a8,8,0,0,1,0,11.31L195.54,82.68a8,8,0,0,1-11.31-11.31l13.83-13.83A8,8,0,0,1,209.37,57.54Z"}]])

(def ^:private moon-icon
  [:svg {:xmlns "http://www.w3.org/2000/svg" :width "20" :height "20" :viewBox "0 0 256 256" :fill "currentColor"}
   [:path {:d "M240,96a8,8,0,0,1-8,8H216v16a8,8,0,0,1-16,0V104H184a8,8,0,0,1,0-16h16V72a8,8,0,0,1,16,0V88h16A8,8,0,0,1,240,96ZM144,56h8v8a8,8,0,0,0,16,0V56h8a8,8,0,0,0,0-16h-8V32a8,8,0,0,0-16,0v8h-8a8,8,0,0,0,0,16Zm72.77,97a8,8,0,0,1,1.43,8A96.06,96.06,0,1,1,95.07,37.8a8,8,0,0,1,10.6,9.06A88.07,88.07,0,0,0,209.14,150.33,8,8,0,0,1,216.77,153Z"}]])

(defn- theme-toggle [state]
  (let [dark? (= :dark (:theme @state))]
    [:button {:on {:click (fn [_] (swap! state update :theme #(if (= :dark %) :light :dark)))}
              :style {:background "none" :border "1px solid #ccc" :border-radius "4px"
                      :cursor "pointer" :padding "4px 8px" :margin-bottom "8px"}}
     (if dark?
       (assoc-in sun-icon [1 :style] {:color "#f0c040"})
       moon-icon)]))

(defn- chart-wrapper [state chart]
  (let [dark? (= :dark (:theme @state))]
    [:div {:style {:background (if dark? "#333" "#fff") :padding "8px"}}
     (theme-toggle state)
     chart]))

(defscene simple-chart
  :params (atom (assoc bar-chart-data :theme :light))
  [state]
  (chart-wrapper state
                 [ch/chart
                  {::ch/data (dissoc @state :theme)
                   ::ch/theme (:theme @state)
                   ::ch/notify {:toggle [:click {:seriesName "2015"}]}
                   :on {:toggle (fn [_]
                                  (swap! state update-in [:series 1 :type]
                                         #(if (= :line %) :bar :line)))}}]))

(defscene drill-down-chart
  :params (atom {:columns [:product "2015" "2016"],
                 :rows [{:product "Shirts" "2015" 15.3 "2016" 5
                         :children [{:product "Cotton"  "2015" 8.2 "2016" 3}
                                    {:product "Silk"    "2015" 4.1 "2016" 1.2}
                                    {:product "Linen"   "2015" 3.0 "2016" 0.8}]}
                        {:product "Cardigans" "2015" 9.1 "2016" 20
                         :children [{:product "English" "2015" 1.5 "2016" 5
                                     :children [{:product "London"    "2015" 0.1}
                                                {:product "Liverpool" "2015" 1.4}]}
                                    {:product "Scottish" "2015" 4.8 "2016" 12}
                                    {:product "Irish"    "2015" 2.8 "2016" 3}]}
                        {:product "Socks" "2015" 4.8 "2016" 25
                         :children [{:product "Ankle"  "2015" 2.1 "2016" 15}
                                    {:product "Crew"   "2015" 1.9 "2016" 8}
                                    {:product "Knee"   "2015" 0.8 "2016" 2}]}]
                 :drill-down :children
                 :xAxis {:type :category :show false}
                 :yAxis {}
                 :series [{:type :pie} {:type :pie}]
                 :legend {}
                 :tooltip {}
                 :theme :light})
  [state]
  (chart-wrapper state
                 [ch/drill-down #::ch {:data (dissoc @state :theme)
                                       :theme (:theme @state)
                                       :on-drill (fn [idx] (prn (swap! state assoc :path idx)))
                                       :label-of (fn [item] (:product item))}]))

(def table-data
  {:columns [:name :balance :sex :age]
   :rows
   [{:name "Robert" :sex :male :age 23 :balance 1323442}
    {:name "Jane" :sex :female :age 15 :balance 61923}
    {:name "John" :sex :male :balance -456 :age 45}]
   :sorting {:sortable #{:balance :age}}}) ;specify which columns can be sorted

(defscene simple-data-table
  [dt/table {::dt/data table-data}])

(defscene grouping-data-table
  :params (atom (dt/grouping-data table-data {:column :sex :aggregations [[:balance (fnil + 0)] [:age]]}))
  [state]
  [dt/table
   {::dt/data @state
    ::dt/drill-down :children}
   [dt/th {::dt/label-of (fn [v] (case v ::ch/group "" (name v)))}
    [dt/sort-button {::dt/sorting (:sorting @state)
                     ::dt/on-sort (fn [st] (swap! state #(-> % (assoc :sorting st) (update :rows dt/sort-rows st :children))))}]]
   [dt/td {::dt/class-of (fn [column _] (cond-> [] (= column :balance) (conj "number-cell")))
           ::dt/label-of (fn [column v] (if (= column :balance) (cljs.pprint/cl-format nil "~:d" v) (str v)))}]])

(defscene simple-edn-editor
  :params (atom {:name "Old Gaffer"
                 :address {:street "Bagshot Row"
                           :number 1
                           :additional-key "hello"}
                 :items [{:item :spade
                          :price 3.2
                          :in-stock true}
                         {:item :pipe
                          :price 2.7
                          :in-stock false}]
                 :instructions ["please" "send" "help"]
                 :foo [1 "hello" false]})
  [state]
  [ee/editor
   {::ee/schema
    [:map
     [:name :string]
     [:address
      [:orn
       [:structured [:map
                     [:street [:string {:min 1}]]
                     [:number {:optional true} :int]]]
       [:raw :string]]]
     [:items
      [:vector
       [:map
        [:item [:enum :fork :spade :pipe]]
        [:price {:optional true} :double]
        [:in-stock {:optional true} :boolean]]]]
     [:instructions [:or
                     :string
                     [:vector :string]]]
     [:metadata [:map-of :keyword :string]]
     [:foo [:tuple :int :string :boolean]]]
    ::ee/value @state
    ::ee/on-change (fn [v] (prn (reset! state v)))}])

(defscene edn-editor
  :params (atom {:type :human
                 :name "bob"
                 :pet '("dog" "cat")
                 :favourite-color #{"red" "green" "blue"}})
  [state]
  [ee/editor
   {::ee/schema
    [:multi {:dispatch :type}
     [:malli.core/default [:map [:type [:enum :sized :human]]]]
     [:sized [:map
              [:type [:= :sized]]
              [:size [:and :int [:fn pos-int?]]]]]
     [:human [:map
              [:type [:= :human]]
              [:name :string]
              [:pet [:sequential :string]]
              [:favourite-color [:set :string]]]]]
    ::ee/value @state
    ::ee/on-change (fn [v] (prn (reset! state v)))}])

(defscene Simple-form
  :params (atom {:username "whoever" :balance "xxx"})
  [state]
  (let [schema [:map
                [:username {:placeholder "some@example.com" :description "Your username"} :string]
                [:password {:input-type :password} :string]
                [:balance {:optional true} :int]]]
    [form/form
     {::form/schema schema
      ::form/data @state
      ::form/button-label "Login"
      :on {:submit (fn [evt]
                     (let [[data _errors] (form/data&errors schema (form/form-data evt))]
                       (prn (reset! state data)))
                     (.preventDefault evt))}}
     [:h2 (str "Simple form for: " (:username @state))]]))

(defscene loading-container-demo
  :params (atom {:loading? true})
  [state]
  [loading/loading-container
   {::loading/loading? (:loading? @state)}
   [dt/table {::dt/data table-data}]])

(defscene error-aware-container-demo
  :params (atom {:error "Something went wrong! This is an example error message."})
  [state]
  [error-aware/error-aware-container
   {::error-aware/error (:error @state)
    :on {:dismiss (fn [_] (swap! state dissoc :error))}}
   [:div
    [:h3 "Content Behind Error"]
    [:p "This content is visible behind the error overlay."]
    [:button {:on {:click #(swap! state assoc :error "New error occurred!")}}
     "Trigger Error"]]])

(defn main []
  (replicant/set-dispatch! (fn [_ _ action] (prn action)))
  (portfolio/start!
   {:config
    {:css-paths ["/css/chart.css" "/css/data_table.css"
                 "/css/edn_editor.css" "/css/form.css"
                 "/css/loading.css" "/css/error_aware.css"]
     :viewport/defaults
     {:background/background-color "#fdeddd"}}}))

