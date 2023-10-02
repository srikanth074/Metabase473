(ns metabase.lib.drill-thru.underlying-records-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [medley.core :as m]
   [metabase.lib.core :as lib]
   [metabase.lib.drill-thru :as lib.drill-thru]
   [metabase.lib.metadata ]
   [metabase.lib.options :as lib.options]
   [metabase.lib.test-metadata :as meta]
   [metabase.lib.test-util.metadata-providers.mock :as providers.mock]
   #?@(:cljs ([metabase.test-runner.assert-exprs.approximately-equal])
       :clj  ([java-time.api :as jt]))))

#?(:cljs (comment metabase.test-runner.assert-exprs.approximately-equal/keep-me))

(def last-month
  #?(:cljs (let [now    (js/Date.)
                 year   (.getFullYear now)
                 month  (.getMonth now)]
             (-> (js/Date.UTC year (dec month))
                 (js/Date.)
                 (.toISOString)))
     :clj  (let [last-month (-> (jt/zoned-date-time (jt/year) (jt/month))
                                (jt/minus (jt/months 1)))]
             (jt/format :iso-offset-date-time last-month))))

(defn- underlying-state [query agg-value breakout-value exp-filters-fn]
  (let [columns                         (lib/returned-columns query)
        {aggs      :source/aggregations
         breakouts :source/breakouts}   (group-by :lib/source columns)
        agg-dim                         {:column     (first aggs)
                                         :column-ref (lib/ref (first aggs))
                                         :value      agg-value}
        breakout-dim                    {:column     (first breakouts)
                                         :column-ref (lib/ref (first breakouts))
                                         :value      breakout-value}
        context                         (merge agg-dim
                                               {:row   [breakout-dim agg-dim]
                                                :dimensions [breakout-dim]})]
      (is (=? {:lib/type :mbql/query
               :stages [{:filters     (exp-filters-fn agg-dim breakout-dim)
                         :aggregation (symbol "nil #_\"key is not present.\"")
                         :breakout    (symbol "nil #_\"key is not present.\"")
                         :fields      (symbol "nil #_\"key is not present.\"")}]}
              (->> (lib.drill-thru/available-drill-thrus query context)
                   (m/find-first #(= (:type %) :drill-thru/underlying-records))
                   (lib.drill-thru/drill-thru query -1))))))

(deftest ^:parallel underlying-records-apply-test
  (testing "sum(subtotal) over time"
    (underlying-state (-> (lib/query meta/metadata-provider (meta/table-metadata :orders))
                          (lib/aggregate (lib/sum (meta/field-metadata :orders :subtotal)))
                          (lib/breakout (lib/with-temporal-bucket
                                          (meta/field-metadata :orders :created-at)
                                          :month)))
                      42295.12
                      last-month
                      (fn [_agg-dim breakout-dim]
                        [[:= {}
                          (-> (:column-ref breakout-dim)
                              (lib.options/with-options {:temporal-unit :month}))
                          last-month]])))
  (testing "sum_where(subtotal, products.category = \"Doohickey\") over time"
    (underlying-state (-> (lib/query meta/metadata-provider (meta/table-metadata :orders))
                          (lib/aggregate (lib/sum-where
                                           (meta/field-metadata :orders :subtotal)
                                           (lib/= (meta/field-metadata :products :category)
                                                  "Doohickey")))
                          (lib/breakout (lib/with-temporal-bucket
                                          (meta/field-metadata :orders :created-at)
                                          :month)))
                      6572.12
                      last-month
                      (fn [_agg-dim breakout-dim]
                        [[:= {}
                          (-> (:column-ref breakout-dim)
                              (lib.options/with-options {:temporal-unit :month}))
                          last-month]
                         [:= {} (-> (meta/field-metadata :products :category)
                                    lib/ref
                                    (lib.options/with-options {}))
                          "Doohickey"]])))

  (testing "metric over time"
    (let [metric   {:description "Orders with a subtotal of $100 or more."
                    :archived false
                    :updated-at "2023-10-04T20:11:34.029582"
                    :lib/type :metadata/metric
                    :definition
                    {"source-table" (meta/id :orders)
                     "aggregation" [["count"]]
                     "filter" [">=" ["field" (meta/id :orders :subtotal) nil] 100]}
                    :table-id (meta/id :orders)
                    :name "Large orders"
                    :caveats nil
                    :entity-id "NWMNcv_yhhZIT7winoIdi"
                    :how-is-this-calculated nil
                    :show-in-getting-started false
                    :id 1
                    :database (meta/id)
                    :points-of-interest nil
                    :creator-id 1
                    :created-at "2023-10-04T20:11:34.029582"}
          provider (lib/composed-metadata-provider
                     meta/metadata-provider
                     (providers.mock/mock-metadata-provider {:metrics [metric]}))]
      (underlying-state (-> (lib/query provider (meta/table-metadata :orders))
                            (lib/aggregate metric)
                            (lib/breakout (lib/with-temporal-bucket
                                            (meta/field-metadata :orders :created-at)
                                            :month)))
                        6572.12
                        last-month
                        (fn [_agg-dim breakout-dim]
                          (let [monthly-breakout (-> (:column-ref breakout-dim)
                                                     (lib.options/with-options {:temporal-unit :month}))
                                subtotal         (-> (meta/field-metadata :orders :subtotal)
                                                     lib/ref
                                                     (lib.options/with-options {}))]
                            [[:=  {} monthly-breakout last-month]
                             [:>= {} subtotal 100]]))))))
