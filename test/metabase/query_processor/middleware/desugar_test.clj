(ns metabase.query-processor.middleware.desugar-test
  (:require [clojure.test :refer :all]
            [metabase.query-processor.middleware.desugar :as desugar]
            [metabase.query-processor.timezone :as qp.timezone]))

;; actual desugaring logic and tests are in [[metabase.mbql.util-test]]
(deftest ^:parallel e2e-test
  (binding [qp.timezone/*results-timezone-id-override* "Asia/Ho_Chi_Minh"]
    (is (= {:database 1
            :type     :query
            :query    {:source-table 1
                       :filter       [:and
                                      [:= [:field 1 nil] "Run Query"]
                                      [:between
                                       [:field 2 {:temporal-unit :day}]
                                       [:relative-datetime -30 :day]
                                       [:relative-datetime -1 :day]]
                                      [:!= [:field 3 nil] "(not set)"]
                                      [:!= [:field 3 nil] "url"]
                                      [:> [:temporal-extract [:field 4 nil] :year-of-era] 2004]]
                       :expressions  {"year" [:temporal-extract [:field 4 nil] :year-of-era]
                                      "zoned" [:convert-timezone [:field 4 nil] "Asia/Ho_Chi_Minh" "UTC"]}
                       :aggregation  [[:share [:and
                                               [:= [:field 1 nil] "Run Query"]
                                               [:between
                                                [:field 2 {:temporal-unit :day}]
                                                [:relative-datetime -30 :day]
                                                [:relative-datetime -1 :day]]
                                               [:!= [:field 3 nil] "(not set)"]
                                               [:!= [:field 3 nil] "url"]]]]}}
           (desugar/desugar
            {:database 1
             :type     :query
             :query    {:source-table 1
                        :filter       [:and
                                       [:= [:field 1 nil] "Run Query"]
                                       [:time-interval [:field 2 nil] -30 :day]
                                       [:!= [:field 3 nil] "(not set)" "url"]
                                       [:> [:get-year [:field 4 nil]] 2004]]
                        :expressions  {"year" [:get-year [:field 4 nil]]
                                       "zoned" [:convert-timezone [:field 4 nil] "instance" "UTC"]}
                        :aggregation  [[:share [:and
                                                [:= [:field 1 nil] "Run Query"]
                                                [:time-interval [:field 2 nil] -30 :day]
                                                [:!= [:field 3 nil] "(not set)" "url"]]]]}})))))
