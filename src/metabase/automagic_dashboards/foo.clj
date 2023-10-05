(ns metabase.automagic-dashboards.foo
  (:require
    [clojure.math.combinatorics :as math.combo]
    [clojure.walk :as walk]
    [metabase.automagic-dashboards.dashboard-templates :as dashboard-templates]
    [metabase.automagic-dashboards.foo-dashboard-generator :as dash-gen]
    [metabase.automagic-dashboards.util :as magic.util]
    [metabase.models.interface :as mi]
    [metabase.util :as u]
    [toucan2.core :as t2]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Global affinity definitions

(defn affinity-set-interestingness [affinity-set]
  (reduce + (map (fn [a] (count (ancestors a))) affinity-set)))

(def affinity-specs
  (mapv
    (fn [{:keys [affinity-set] :as m}]
      (assoc m :affinity-interestingness (affinity-set-interestingness affinity-set)))
    [{:affinity-set #{:type/Longitude :type/Latitude} :charts [:map :binned-map]}
     {:affinity-set #{:type/Country} :charts [:map]}
     ;; How does this look?
     {:affinity-set #{:type/State} :charts [:map]}
     ;{:affinity-set #{:type/ZipCode} :charts [:map]}
     {:affinity-set #{:type/Source} :charts [:bar]}
     {:affinity-set #{:type/Category} :charts [:bar]}
     {:affinity-set #{:type/CreationTimestamp} :charts [:line]}
     {:affinity-set #{:type/Quantity} :charts [:line]}
     {:affinity-set #{:type/Discount} :charts [:line]}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Code for creation of instantiated affinities

(defn find-field-ids
  "A utility function for pulling field definitions from mbql queries and return their IDs.
   Does something like this already exist in our utils? I was unable to find anything like it."
  [m]
  (let [fields (atom #{})]
    (walk/prewalk
      (fn [v]
        (when (vector? v)
          (let [[f id] v]
            (when (and id (= :field f))
              (swap! fields conj id))))
        v)
      m)
    @fields))

(defn semantic-groups
  "From a :model/Metric, construct a mapping of semantic types of linked fields to
   sets of fields that can satisfy that type. A linked field is one that is in the
   source table for the metric contribute to the metric itself, is not a PK, and
   has a semantic_type (we assume nil semantic_type fields are boring)."
  [{:keys [table_id definition]}]
  (let [field-ids            (find-field-ids definition)
        potential-dimensions (t2/select :model/Field
                               :id [:not-in field-ids]
                               :table_id table_id
                               :semantic_type [:not-in [:type/PK]])]
    (update-vals
      (->> potential-dimensions
           (group-by :semantic_type))
      set)))

(defn instantiate-affinities
  "For a given metric, determine adjacent fields and return a map of them by
  semantic type grouping."
  [metric]
  (let [semantic-groups (semantic-groups metric)]
    (for [{:keys [affinity-set] :as affinity-spec} affinity-specs
          :when (every? semantic-groups affinity-set)
          :let [bindings (map semantic-groups affinity-set)]
          dimensions (apply math.combo/cartesian-product bindings)]
      (assoc affinity-spec
        :metric metric
        :dimensions dimensions))))

;; This is SUPER important
;; It can be sourced internally or from other data
(defn find-metrics [thing]
  ;;Can come from linked entities or templates
  ;; Maybe will be returned as queries, as is done with linked metrics above
  )

;; Here we add in breakouts. Or should this be identify dimensions.
(defn associate-affinities [thing]
  )

;; Make the cards or whatever
(defn do-stuff [thing])

(comment

  (let [{table-id :id :as table} (t2/select-one :model/Table :name "ORDERS")]
    (map (juxt :id :name :semantic_type) (t2/select :model/Field :table_id table-id)))

  (map (juxt :name :id) (t2/select :model/Metric))
  ;(["Churn" 785] ["gmail" 909] ["Weird Thing" 910] ["Multitable Metric" 920])
  (->> (t2/select :model/Metric)
       (map (fn [{:keys [name id] :as metric}]
              [name id (keys (semantic-groups metric))])))

  (semantic-groups
    (t2/select-one :model/Metric :name "Churn"))

  (let [{metric-name :name :as metric} (t2/select-one :model/Metric :name "Churn")]
    (->> metric
         instantiate-affinities
         (dash-gen/create-dashboard {:dashboard-name metric-name})))

  (->> (t2/select-one :model/Metric :name "Churn")
       instantiate-affinities
       (mapv (fn [affinity]
               (-> affinity
                   (update :dimensions #(mapv :name %))
                   (update :metric :name)))))

  (->> (t2/select-one :model/Metric :name "Weird Thing")
       instantiate-affinities)


  (->> (instantiate-affinities (t2/select-one :model/Metric :name "Churn"))
       (map :card)
       dash-gen/do-layout)
  )

(defmulti linked-metrics mi/model)

(defmethod linked-metrics :model/Metric [{metric-name :name :keys [definition] :as metric}]
  (let [field-ids (set (find-field-ids definition))]
    [{:metric-name            metric-name
      :metric-title           metric-name
      :metric-definition      definition
      :grounded-metric-fields (t2/select :model/Field :id [:in field-ids])}]))

(defmethod linked-metrics :model/Table [{table-id :id :keys [definition] :as table}]
  (mapcat
    linked-metrics
    (t2/select :model/Metric :table_id table-id)))

(defmethod linked-metrics :default [_] [])

(defn transform-metric-aggregate
  "Map a metric aggregate definition from nominal types to semantic types."
  [m decoder]
  (walk/prewalk
    (fn [v]
      (if (vector? v)
        (let [[d n] v]
          (if (= "dimension" d)
            (decoder n)
            v))
        v))
    m))

(defn ground-metric
  "Generate \"grounded\" metrics from the mapped dimensions (dimension name -> field matches).
   Since there may be multiple matches to a dimension, this will produce a sequence of potential matches."
  [{metric-name :metric-name metric-definition :metric} ground-dimensions]
  (let [named-dimensions (dashboard-templates/collect-dimensions metric-definition)]
    (->> (map (comp :matches ground-dimensions) named-dimensions)
         (apply math.combo/cartesian-product)
         (map (partial zipmap named-dimensions))
         (map (fn [nm->field]
                (let [xform (update-vals nm->field (fn [{field-id :id}]
                                                     [:field field-id nil]))]
                  {:metric-name            metric-name
                   :metric-title           metric-name
                   :metric-definition      {:aggregation
                                            (transform-metric-aggregate metric-definition xform)}
                   :grounded-metric-fields (vals nm->field)}))))))

(defn grounded-metrics
  "Given a set of metric definitions and grounded (assigned) dimensions, produce a sequence of grounded metrics."
  [metric-templates ground-dimensions]
  (mapcat #(ground-metric % ground-dimensions) metric-templates))

(defn normalize-metrics
  "Utility function to convert a dashboard template into a sequence of metric templates that are easier to work with."
  [metrics]
  (->> metrics
       (map first)
       (map (fn [[metric-name metric-definition]]
              (assoc metric-definition :metric-name metric-name)))))

;;; dimensions

(defn- fieldspec-matcher
  "Generate a predicate of the form (f field) -> truthy value based on a fieldspec."
  [fieldspec]
  (if (and (string? fieldspec)
           (dashboard-templates/ga-dimension? fieldspec))
    (comp #{fieldspec} :name)
    (fn [{:keys [semantic_type target] :as field}]
      (cond
        ;; This case is mostly relevant for native queries
        (#{:type/PK :type/FK} fieldspec) (isa? semantic_type fieldspec)
        target (recur target)
        :else (and (not (magic.util/key-col? field)) (magic.util/field-isa? field fieldspec))))))

(defn- name-regex-matcher
  "Generate a truthy predicate of the form (f field) -> truthy value based on a regex applied to the field name."
  [name-pattern]
  (comp (->> name-pattern
             u/lower-case-en
             re-pattern
             (partial re-find))
        u/lower-case-en
        :name))

(defn- max-cardinality-matcher
  "Generate a predicate of the form (f field) -> true | false based on the provided cardinality.
  Returns true if the distinct count of fingerprint values is less than or equal to the cardinality."
  [cardinality]
  (fn [field]
    (some-> field
            (get-in [:fingerprint :global :distinct-count])
            (<= cardinality))))

(def ^:private field-filters
  {:fieldspec       fieldspec-matcher
   :named           name-regex-matcher
   :max-cardinality max-cardinality-matcher})

(defn- filter-fields
  "Find all fields belonging to table `table` for which all predicates in
   `preds` are true. `preds` is a map with keys :fieldspec, :named, and :max-cardinality."
  [preds fields]
  (filter (->> preds
               (keep (fn [[k v]]
                       (when-let [pred (field-filters k)]
                         (some-> v pred))))
               (apply every-pred))
          fields))

(defn- matching-fields
  "Given a context and a dimension definition, find all fields from the context
   that match the definition of this dimension."
  [{{:keys [fields]} :source :keys [tables] :as context}
   {:keys [field_type links_to named max_cardinality] :as constraints}]
  (if links_to
    (filter (comp (->> (magic.util/filter-tables links_to tables)
                       (keep :link)
                       set)
                  u/the-id)
            (matching-fields context (dissoc constraints :links_to)))
    (let [[tablespec fieldspec] field_type]
      (if fieldspec
        (mapcat (fn [table]
                  (some->> table
                           :fields
                           (filter-fields {:fieldspec       fieldspec
                                           :named           named
                                           :max-cardinality max_cardinality})
                           (map #(assoc % :link (:link table)))))
                (magic.util/filter-tables tablespec tables))
        (filter-fields {:fieldspec       tablespec
                        :named           named
                        :max-cardinality max_cardinality}
                       fields)))))

;; util candidate
(def ^:private ^{:arglists '([field])} id-or-name
  (some-fn :id :name))

(defn- candidate-bindings
  "For every field in a given context determine all potential dimensions each field may map to.
  This will return a map of field id (or name) to collection of potential matching dimensions."
  [context dimension-specs]
  ;; TODO - Fix this so that the intermediate representations aren't so crazy.
  ;; all-bindings a map of binding dim identifier to binding def which contains
  ;; field matches which are all the same field except they are merged with the binding.
  ;; What we want instead is just a map of field to potential bindings.
  ;; Just rack and stack the bindings then return that with the field or something.
  (let [all-bindings (for [dimension      dimension-specs
                           :let [[identifier definition] (first dimension)]
                           matching-field (matching-fields context definition)]
                       {(name identifier)
                        (assoc definition :matches [(merge matching-field definition)])})]
    (group-by (comp id-or-name first :matches val first) all-bindings)))

(defn- score-bindings
  "Assign a value to each potential binding.
  Takes a seq of potential bindings and returns a seq of vectors in the shape
  of [score binding], where score is a 3 element vector. This is computed as:
     1) Number of ancestors `field_type` has (if field_type has a table prefix,
        ancestors for both table and field are counted);
     2) Number of fields in the definition, which would include additional filters
        (`named`, `max_cardinality`, `links_to`, ...) etc.;
     3) The manually assigned score for the binding definition
  "
  [candidate-binding-values]
  (letfn [(score [a]
            (let [[_ definition] a]
              [(reduce + (map (comp count ancestors) (:field_type definition)))
               (count definition)
               (:score definition)]))]
    (map (juxt (comp score first) identity) candidate-binding-values)))

(defn- most-specific-matched-dimension
  "Return the most specific dimension from one or more dimensions that all
   match the same field. Specificity is determined based on:
   1) how many ancestors `field_type` has (if field_type has a table prefix,
      ancestors for both table and field are counted);
   2) if there is a tie, how many additional filters (`named`, `max_cardinality`,
      `links_to`, ...) are used;
   3) if there is still a tie, `score`.

   candidate-binding-values is a sequence of maps. Each map is a has a key
   of dimension spec name to potential dimension binding spec along with a
   collection of matches, all of which are merges of this spec with the same
   column.

   Note that it would make a lot more sense to refactor this to return a
   map of column to potential binding dimensions. This return value is kind of
   the opposite of what makes sense.

   Here's an example input with :matches updated as just the names of the
   columns in the matches. IRL, matches are the entire field n times, with
   each field a merge of the spec with the field.

   ({\"Timestamp\" {:field_type [:type/DateTime],
                    :score 60,
                    :matches [\"CREATED_AT\"]}}
    {\"CreateTimestamp\" {:field_type [:type/CreationTimestamp],
                          :score 80
                          :matches [\"CREATED_AT\"]}})
   "
  [candidate-binding-values]
  (let [scored-bindings (score-bindings candidate-binding-values)]
    (second (last (sort-by first scored-bindings)))))

(defn find-dimensions
  "Bind fields to dimensions from the dashboard template and resolve overloaded cases in which multiple fields match the
  dimension specification.

   Each field will be bound to only one dimension. If multiple dimension definitions match a single field, the field
  is bound to the most specific definition used
   (see `most-specific-definition` for details).

  The context is passed in, but it only needs tables and fields in `candidate-bindings`. It is not extensively used."
  [context dimension-specs]
  (->> (candidate-bindings context dimension-specs)
       (map (comp most-specific-matched-dimension val))
       (apply merge-with (fn [a b]
                           (case (compare (:score a) (:score b))
                             1 a
                             0 (update a :matches concat (:matches b))
                             -1 b))
              {})))

(defn add-breakouts
  "Add breakouts to a query based on the breakout fields"
  [query breakout-fields]
  (assoc query
    :breakout (mapv (fn [{field-id :id base-type :base_type}]
                      [:field field-id {:base-type base-type}])
                    breakout-fields)))

(defn make-metric-combinations
  "From a grounded metric, produce additional metrics that have potential dimensions 
   mixed in based on the provided ground dimensions and semantic affinity sets."
  [{grounded-metric-fields :grounded-metric-fields :as metric}
   ground-dimensions
   semantic-affinity-sets]
  (let [grounded-field-ids   (set (map :id grounded-metric-fields))
        ;; We won't add dimensions to a metric where the dimension is already
        ;; contributing to the fields already grounded in the metric definition itself.
        groundable-fields    (->> (vals ground-dimensions)
                                  (mapcat :matches)
                                  (remove (comp grounded-field-ids :id))
                                  (group-by (some-fn
                                              :semantic_type
                                              :effective_type)))
        grounded-field-types (map (some-fn
                                    :semantic_type
                                    :effective_type) grounded-metric-fields)]
    (distinct
      (for [affinity-set            semantic-affinity-sets
            dimset                  (math.combo/permutations affinity-set)
            :when (and
                    (>= (count dimset)
                        (count grounded-metric-fields))
                    (->> (map
                           (fn [a b] (isa? a b))
                           grounded-field-types dimset)
                         (every? true?)))
            :let [unsatisfied-semantic-dims (vec (drop (count grounded-field-types) dimset))]
            ground-dimension-fields (->> (map groundable-fields unsatisfied-semantic-dims)
                                         (apply math.combo/cartesian-product)
                                         (map (partial zipmap unsatisfied-semantic-dims)))]
        (-> metric
            (assoc
              :dimensions ground-dimension-fields
              :affinity-set affinity-set)
            (update :metric-definition add-breakouts (vals ground-dimension-fields)))))))

(defn make-combinations
  "Expand simple ground metrics in to ground metrics with dimensions
   mixed in based on potential semantic affinity sets."
  [grounded-metrics ground-dimensions semantic-affinity-sets]
  (mapcat #(make-metric-combinations % ground-dimensions semantic-affinity-sets) grounded-metrics))

(comment
  (require '[metabase.automagic-dashboards.core :as magic])
  (let [template-name     "GenericTable"
        entity            (t2/select-one :model/Metric :name "Churn")
        {template-dimensions :dimensions
         template-metrics    :metrics} (dashboard-templates/get-dashboard-template ["table" template-name])
        base-context      (#'magic/make-base-context (magic/->root entity))
        ground-dimensions (find-dimensions base-context template-dimensions)
        metric-templates  (normalize-metrics template-metrics)]
    (concat
      (grounded-metrics metric-templates ground-dimensions)
      (linked-metrics entity)))

  (require '[metabase.query-processor :as qp])
  (let [template-name          "GenericTable"
        {:keys [id db_id] :as entity} (t2/select-one :model/Table :name "ACCOUNTS")
        template               (dashboard-templates/get-dashboard-template ["table" template-name])
        {template-dimensions :dimensions
         template-metrics    :metrics} template
        semantic-affinity-sets (keys (magic/semantic-affinities template))
        base-context           (#'magic/make-base-context (magic/->root entity))
        ground-dimensions      (find-dimensions base-context template-dimensions)
        metric-templates       (normalize-metrics template-metrics)
        grounded-metrics       (concat
                                 (grounded-metrics metric-templates ground-dimensions)
                                 (linked-metrics entity))]
    (->> (make-combinations
           grounded-metrics
           ground-dimensions
           semantic-affinity-sets)
         (mapv (fn [ground-metric-with-dimensions]
                 (-> ground-metric-with-dimensions
                     (update :grounded-metric-fields (partial map (juxt :id :name)))
                     (update :dimensions #(update-vals % (juxt :id :name))))))
         ;; Stop above here to see the produced grounded metrics
         ;; Below is just showing we can make queries
         (mapv (fn [{:keys [metric-definition]}]
                 {:database db_id
                  :type     :query
                  :query    (assoc metric-definition
                              :source-table id)}))
         (take 2)
         (map qp/process-query)
         ))
  )
