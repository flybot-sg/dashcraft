(ns robertluo.dashcraft.edn-editor
  "An EDN editor backed by Malli schema.
  
  Copy from https://github.com/opqdonut/malli-edn-editor/blob/main/src/editor.cljs
  Many thanks to @opqdonut"
  (:require
   [clojure.edn :as edn]
   [malli.core :as m]
   [malli.transform :as mt]
   [malli.util :as mu]
   [replicant.alias :refer [defalias]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; default-value multimethod for initializing new entries

(defmulti default-value (fn [schema]
                          (when schema
                            (:type (m/ast schema)))))

(defmethod default-value :default    [_] nil)
(defmethod default-value :string     [_] "")
(defmethod default-value :keyword    [_] (->> (rand-int 1000) (str "keyword") keyword))
(defmethod default-value :vector     [_] [])
(defmethod default-value :sequential [_] [])
(defmethod default-value :set        [_] #{})
(defmethod default-value :boolean    [_] (even? (rand-int 2)))
(defmethod default-value :int        [_] (rand-int 1000))
(defmethod default-value :double     [_] (rand))

(defmethod default-value := [schema] (first (m/children schema)))

(defmethod default-value :map [schema]
  (into {}
        (keep (fn [[k properties value]]
                (when-not (:optional properties)
                  [k (default-value value)]))
              (m/children schema))))

(defmethod default-value :map-of [_] {})

(defmethod default-value :and [schema]
  (default-value (first (m/children schema))))

(defmethod default-value :or [schema]
  (default-value (first (m/children schema))))

(defmethod default-value :orn [schema]
  (default-value (last (first (m/children schema)))))

(defmethod default-value :multi [schema]
  (default-value (last (first (m/children schema)))))

(defmethod default-value :enum [schema]
  (first (m/children schema)))

(defmethod default-value :tuple [schema]
  (mapv default-value (m/children schema)))

(defmethod default-value :schema [schema]
  (default-value (m/deref schema)))

(defmethod default-value :malli.core/schema [schema]
  (default-value (m/deref schema)))

(defmethod default-value :ref [schema]
  (default-value (m/deref schema)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; edit multimethod: editor ui per malli schema type

(defmulti edit (fn [schema _value _on-change]
                 (when schema
                   (:type (m/ast schema))))) ;; TODO is there a simpler way to check the type?

(defmethod edit :default [schema value _on-change]
  [:div
   [:p "Default"]
   [:p (pr-str (m/ast schema))]
   [:p (pr-str value)]])

;; TODO validation errors
(defn input-field [schema value on-change]
  (let [valid? (m/validate schema value)]
    [:input {:type :text
             :class (when-not valid? :malli-editor-invalid)
             :default-value (m/encode schema value mt/string-transformer)
             :on {:change #(on-change (m/decode schema (-> % .-target .-value) mt/string-transformer))}}]))

(defmethod edit := [_ value _]
  [:div (pr-str value)])

(defmethod edit :string [schema value on-change]
  [:div.malli-editor-string "\"" (input-field schema value on-change) "\""])

(defmethod edit :int [schema value on-change]
  [:div.malli-editor-int
   (input-field schema value on-change)])

(defmethod edit :double [schema value on-change]
  [:div.malli-editor-double
   (input-field schema value on-change)])

(defmethod edit :keyword [schema value on-change]
  [:div.malli-editor-keyword
   ":" (input-field schema value on-change)])

(defmethod edit :boolean [_schema value on-change]
  [:div.malli-editor-boolean
   [:input {:type :checkbox :checked value :on {:change #(on-change (-> % .-target .-checked))}}]])

(defmethod edit :enum [schema value on-change]
  [:div.malli-editor-enum
   (into [:select {:on {:change #(on-change (-> % .-target .-value edn/read-string))}}]
         (for [v (m/children schema)]
           [:option {:selected (= value v)}
            (pr-str v)]))])

(defn bracket [open close contents]
  [:div.malli-editor-brackets {:style {:display :flex}}
   [:div.malli-editor-bracket-open open]
   [:div.malli-editor-bracket-contents contents]
   [:div.malli-editor-bracket-close {:style {:align-self :flex-end}} close]])

(defn btn [text on-click]
  [:div.malli-editor-btn
   [:span {:on {:click on-click}}
    text]])

(defn btn-plus [on-click]
  (btn "+" on-click))

(defn btn-minus [on-click]
  (btn "–" on-click))

(defmethod edit :maybe [schema value on-change]
  (let [child (first (m/children schema))]
    [:div.malli-editor-maybe {:style {:display :flex}}
     (if (nil? value)
       (btn-plus #(on-change (default-value child)))
       [:div (btn-minus #(on-change nil)) (edit child value on-change)])]))

(defmethod edit :map [schema value on-change]
  (let [map-schema (m/children schema)
        known-keys (->> map-schema
                        ;; mandatory keys first
                        (sort-by #(:optional (second %)))
                        (map first))
        {present-keys true missing-keys false} (group-by #(contains? value %) known-keys)
        extra-value (apply dissoc value known-keys)]
    [:div.malli-editor-map
     (bracket "{" "}"
              [:div
               (for [k present-keys]
                 (let [[_ properties value-schema] (mu/find schema k)]
                   [:div.malli-editor-key-value {:style {:display :flex}}
                    [:div.malli-editor-key (pr-str k)]
                    (when (:optional properties)
                      (btn-minus #(on-change (dissoc value k))))
                    (when-let [[_ v] (find value k)]
                      [:div.malli-editor-value {:style {:margin-left "0.5em"}}
                       (edit value-schema v #(on-change (assoc value k %)))])]))
               [:div.malli-editor-add-keys {:style {:display :flex :flex-flow "row wrap"}}
                (for [k missing-keys]
                  (let [value-schema (mu/get schema k)]
                    (btn (str "<+" (pr-str k) ">") #(on-change (assoc value k (default-value value-schema))))))]
               (when-not (empty? extra-value)
                 [:div.malli-editor-extra-keys
                  [:div.malli-editor-extra-keys-title ";; extra keys:"]
                  (for [[k v] extra-value]
                    [:div {:style {:display :flex}}
                     [:div.malli-editor-key (pr-str k)]
                     (btn-minus #(on-change (dissoc value k)))
                     [:div.malli-editor-value {:style {:margin-left "0.5em"}}
                      (pr-str v)]])])])]))

(defmethod edit :map-of [schema value on-change]
  (let [[key-schema value-schema] (m/children schema)]
    [:div.malli-editor-map-of
     (bracket "{" "}"
              [:div
               (for [[k v] value]
                 [:div.malli-editor-key-value {:style {:display :flex}}
                  (btn-minus #(on-change (dissoc value k)))
                  [:div.malli-editor-key
                   (edit key-schema k #(on-change (-> value (dissoc k) (assoc % v))))]
                  [:div.malli-editor-value {:style {:margin-left "0.5em"}}
                   (edit value-schema v #(on-change (assoc value k %)))]])
               ;; TODO what to do when key is already taken?
               (btn-plus #(on-change (assoc value (default-value key-schema) (default-value value-schema))))])]))

(defn- dissocv [v i]
  (into (subvec v 0 i) (subvec v (inc i))))

(defn sequential-remove-at
  "Removes an item at index `idx` from a sequential collection `coll`,
  preserving the original collection type."
  [coll idx]
  (cond
    (vector? coll)
    (dissocv coll idx)

    (list? coll)
    (keep-indexed #(when-not (= idx %1) %2) coll)))

^:rct/test
(comment
  (sequential-remove-at [1 2 3 4] 1)
  ;=> [1 3 4]

  (sequential-remove-at [1 2 3 4] 0)
  ;=> [2 3 4]

  (sequential-remove-at [1 2 3 4] 3)
  ;=> [1 2 3]

  (sequential-remove-at [1] 0)
  ;=> []

  (sequential-remove-at '(1 2 3 4) 1)
  ;=> '(1 3 4)

  (sequential-remove-at '(1 2 3 4) 0)
  ;=> '(2 3 4)

  (sequential-remove-at '(1 2 3 4) 3)
  ;=> '(1 2 3)

  (sequential-remove-at '(1) 0)
  ;=> '()
  )

(defn sequential-update-at
  "Updates an item at index `idx` with value `val` in a sequential
  collection `coll`, preserving the original collection type."
  [coll idx val]
  (cond
    (vector? coll)
    (assoc coll idx val)

    (list? coll)
    (apply list (assoc (vec coll) idx val))))

^:rct/test
(comment
  (sequential-update-at [1 2 3] 1 :x)
  ;=> [1 :x 3]

  (sequential-update-at [1 2 3] 0 :x)
  ;=> [:x 2 3]

  (sequential-update-at [1 2 3] 2 :x)
  ;=> [1 2 :x]

  (sequential-update-at '(1 2 3) 1 :x)
  ;=> '(1 :x 3)

  (sequential-update-at '(1 2 3) 0 :x)
  ;=> '(:x 2 3)

  (sequential-update-at '(1 2 3) 2 :x)
  ;=> '(1 2 :x)
  )

(defn sequential-add
  "Adds an item `val` to the sequential collection `coll`,
  preserving the original collection type."
  [coll val]
  (cond
    (vector? coll) (conj coll val)
    (list? coll) (concat coll (list val))))

^:rct/test
(comment
  (sequential-add [1 2] 3)
  ;=> [1 2 3]

  (sequential-add [] 1)
  ;=> [1]

  (sequential-add '(1 2) 3)
  ;=> '(1 2 3)

  (sequential-add '() 1)
  ;=> '(1)
  )

(defmethod edit :tuple [schema value on-change]
  [:div.malli-editor-tuple
   (bracket "[" "]"
            (map-indexed (fn [i schema]
                           (edit schema (nth value i) #(on-change (assoc value i %))))
                         (m/children schema)))])

;; TODO: reordering vectors, adding elements in the middle
(defmethod edit :vector [schema value on-change]
  [:div.malli-editor-vector
   (bracket "[" "]"
            [:div
             (map-indexed (fn [i v]
                            [:div {:style {:display :flex}}
                             (btn-minus #(on-change (dissocv value i)))
                             (edit (mu/get schema i) v #(on-change (assoc value i %)))])
                          value)
             (btn-plus #(on-change (conj (or value []) (default-value (mu/get schema 0)))))])])

(defmethod edit :sequential [schema value on-change]
  [:div.malli-editor-sequential
   (bracket (if (list? value) "(" "[")
            (if (list? value) ")" "]")
            [:div
             (map-indexed (fn [i v]
                            [:div {:style {:display :flex}}
                             (btn-minus #(on-change (sequential-remove-at value i)))
                             (edit (mu/get schema i) v #(on-change (sequential-update-at value i %)))])
                          value)
             (btn-plus #(on-change (sequential-add (or value []) (default-value (mu/get schema 0)))))])])

(defmethod edit :set [schema value on-change]
  [:div.malli-editor-set
   (bracket "#{" "}"
            [:div
             (map (fn [v]
                    [:div {:style {:display :flex}}
                     (btn-minus #(on-change (disj value v)))
                     (edit (mu/get schema 0) v #(on-change (-> value (disj v) (conj %))))])
                  value)
             (btn-plus #(on-change (conj (or value #{}) (default-value (mu/get schema 0)))))])])

(defmethod edit :and [schema value on-change]
  (let [primary-schema (first (m/children schema))
        valid? (m/validate schema value)]
    [:div.malli-editor-and
     {:class (when-not valid? :malli-editor-invalid)}
     (edit primary-schema value on-change)]))

(defmethod edit :orn [schema value on-change]
  (let [nom (name (gensym "orn-schema"))
        p (m/parse schema value)
        matched-case (if (= :malli.core/invalid p)
                       ;; just pick the first case if input is invalid
                       ;; TODO figure out what to do here 
                       (->> schema m/entries first key)
                       (:key p))]
    [:div.malli-editor-orn
     (into [:div.malli-editor-orn-choices ";;"]
           (for [[k _p value-schema] (m/children schema)]
             (let [label (pr-str k)
                   id (str nom "--" label)]
               [:div
                [:input {:type :radio :id id :name nom
                         :checked (= k matched-case)
                         :on {:change (if (= k matched-case)
                                        (constantly nil) ;; silence react warning by always having a callback
                                        #(on-change (default-value value-schema)))}}]
                [:label {:for id} label]])))
     (edit (mu/get schema matched-case) value on-change)]))

(defmethod edit :or [schema value on-change]
  (let [nom (name (gensym "or-schema"))
        children (for [c (m/children schema)]
                   {:schema c
                    :valid (m/validate c value)
                    :label (pr-str (:type (m/ast c)))})
        selected (or (first (filter :valid children))
                     (first children))]
    [:div.malli-editor-or
     (into [:div.malli-editor-or-choices ";;"]
           (for [c children]
             (let [id (str nom "--" (:label c))]
               [:div
                [:input {:type :radio :id id :name nom
                         :checked (= selected c)
                         :on {:change (if (= selected c)
                                        (constantly nil) ;; silence reagent warning by always having a callback
                                        #(on-change (default-value (:schema c))))}}]
                [:label {:for id} (:label c)]])))
     (when selected
       (edit (:schema selected) value on-change))]))

(defmethod edit :multi [schema value on-change]
  (let [nom (name (gensym "multi-schema"))
        dispatch-key (:dispatch (m/properties schema))
        children (for [[dispatch _ schema] (m/children schema)]
                   {:dispatch dispatch
                    :schema schema
                    :label (pr-str dispatch)
                    :selected (= dispatch (dispatch-key value))})
        selected (first (filter :selected children))]
    [:div.malli-editor-multi
     (into [:div.malli-editor-multi-choices ";;"]
           (for [c children]
             (let [id (str nom "--" (:label c))]
               [:div
                [:input {:type :radio :id id :name nom
                         :checked (:selected c)
                         :on {:change (if (:selected c)
                                        (constantly nil)
                                        #(on-change (default-value (:schema c))))}}]
                [:label {:for id} (:label c)]])))
     (when selected
       (edit (:schema selected) value on-change))]))

(defmethod edit :schema [schema value on-change]
  (edit (m/deref schema) value on-change))

(defmethod edit :malli.core/schema [schema value on-change]
  (edit (m/deref schema) value on-change))

(defmethod edit :ref [schema value on-change]
  (edit (m/deref schema) value on-change))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main entry points

(defalias
  ^{:doc "
An EDN editor backed by malli `::schema` with `::value`, when its value changed,
triggers `::on-change` event with the value.
   "}
  editor
  [{::keys [schema value on-change] :as attrs}]
  [:div (merge {:class "malli-editor"} attrs)
   (edit schema value on-change)])
