(ns metabase.lib.drill-thru.zoom-in-timeseries-test
  (:require
   [clojure.test :refer [deftest is]]
   [medley.core :as m]
   [metabase.lib.core :as lib]
   [metabase.lib.drill-thru.zoom-in-timeseries
    :as lib.drill-thru.zoom-in-timeseries]
   [metabase.lib.temporal-bucket :as lib.temporal-bucket]
   [metabase.lib.test-metadata :as meta]
   #?@(:cljs ([metabase.test-runner.assert-exprs.approximately-equal]))))

#?(:cljs (comment metabase.test-runner.assert-exprs.approximately-equal/keep-me))

(deftest ^:parallel zoom-in-timeseries-e2e-test
  (let [query (-> (lib/query meta/metadata-provider (meta/table-metadata :orders))
                  (lib/aggregate (lib/count))
                  (lib/breakout (meta/field-metadata :products :category))
                  (lib/breakout (lib/with-temporal-bucket (meta/field-metadata :orders :created-at) :year)))]
    (is (=? {:stages [{:aggregation [[:count {}]]
                       :breakout    [[:field {} (meta/id :products :category)]
                                     [:field {:temporal-unit :year} (meta/id :orders :created-at)]]}]}
            query))
    (let [columns        (lib/returned-columns query)
          created-at    (m/find-first #(= (:id %) (meta/id :orders :created-at))
                                      columns)
          _              (assert created-at)
          category      (m/find-first #(= (:id %) (meta/id :products :category))
                                      columns)
          _              (assert category)
          count-col      (m/find-first #(= (:name %) "count") columns)
          _              (assert count-col)
          drill          (lib.drill-thru.zoom-in-timeseries/zoom-in-timeseries-drill
                           query -1
                           {:column     count-col
                            :column-ref (lib/ref count-col)
                            :value      200
                            :dimensions [{:column     created-at
                                          :column-ref (lib/ref created-at)
                                          :value      2022}
                                         {:column     category
                                          :column-ref (lib/ref category)
                                          :value      "Doohickey"}]})]
      (is (=? {:type         :drill-thru/zoom-in.timeseries
               :dimension    {:column {:id                               (meta/id :orders :created-at)
                                       :metabase.lib.field/temporal-unit :year}
                              :value  2022}
               :next-unit    :quarter
               :display-name "See this year by quarter"}
              drill))
      (is (=? {:display-name "See this year by quarter"}
              (lib/display-info query -1 drill)))
      (let [query' (lib/drill-thru query drill)]
        (is (=? {:stages [{:aggregation [[:count {}]]
                           :breakout    [[:field {} (meta/id :products :category)]
                                         [:field {:temporal-unit :quarter} (meta/id :orders :created-at)]]
                           :filters     [[:=
                                          {}
                                          [:field {:temporal-unit :year} (meta/id :orders :created-at)]
                                          2022]]}]}
                query'))
        (let [quarters (m/find-first #(and (= (:id %) (meta/id :orders :created-at))
                                           (= (lib.temporal-bucket/raw-temporal-bucket %) :quarter))
                                     (lib/returned-columns query'))
              _          (assert quarters)
              drill      (lib.drill-thru.zoom-in-timeseries/zoom-in-timeseries-drill
                           query' -1
                           {:column     count-col
                            :column-ref (lib/ref count-col)
                            :value      19
                            :dimensions [{:column     quarters
                                          :column-ref (lib/ref quarters)
                                          :value      "2022-04-01T00:00:00"}
                                         {:column     category
                                          :column-ref (lib/ref category)
                                          :value      "Doohickey"}]})]
          (is (=? {:type         :drill-thru/zoom-in.timeseries
                   :dimension    {:column {:id                               (meta/id :orders :created-at)
                                           :metabase.lib.field/temporal-unit :quarter}
                                  :value  "2022-04-01T00:00:00"}
                   :next-unit    :month
                   :display-name "See this quarter by month"}
                  drill))
          (is (=? {:display-name "See this quarter by month"}
                  (lib/display-info query' -1 drill)))
          (let [query'' (lib/drill-thru query' drill)]
            (is (=? {:stages [{:aggregation [[:count {}]]
                               :breakout    [[:field {} (meta/id :products :category)]
                                             [:field {:temporal-unit :month} (meta/id :orders :created-at)]]
                               ;; if we were SMART we could remove the first filter clause since it's not adding any
                               ;; value, but it won't hurt anything other than performance to keep it there. QP can
                               ;; generate optimal filters anyway
                               :filters     [[:=
                                              {}
                                              [:field {:temporal-unit :year} (meta/id :orders :created-at)]
                                              2022]
                                             [:=
                                              {}
                                              [:field {:temporal-unit :quarter} (meta/id :orders :created-at)]
                                              "2022-04-01T00:00:00"]]}]}
                    query''))))))))
