(ns metabase.lib.drill-thru.zoom-in-timeseries
  (:require
   [medley.core :as m]
   [metabase.lib.breakout :as lib.breakout]
   [metabase.lib.drill-thru.common :as lib.drill-thru.common]
   [metabase.lib.filter :as lib.filter]
   [metabase.lib.join.util :as lib.join.util]
   [metabase.lib.metadata :as lib.metadata]
   [metabase.lib.remove-replace :as lib.remove-replace]
   [metabase.lib.schema :as lib.schema]
   [metabase.lib.schema.common :as lib.schema.common]
   [metabase.lib.schema.drill-thru :as lib.schema.drill-thru]
   [metabase.lib.schema.temporal-bucketing
    :as lib.schema.temporal-bucketing]
   [metabase.lib.temporal-bucket :as lib.temporal-bucket]
   [metabase.lib.util :as lib.util]
   [metabase.shared.util.i18n :as i18n]
   [metabase.util.malli :as mu]))

;;; TODO -- we shouldn't include hour and minute for `:type/Date` columns.
(def ^:private valid-current-units
  [:year :quarter :month :week :day :hour :minute])

(def ^:private unit->next-unit
  (zipmap (drop-last valid-current-units)
          (drop 1 valid-current-units)))

(defn- is-ref-for-source-column? [a-ref column]
  (and (lib.util/clause-of-type? a-ref :field)
       (let [[_field _opts id-or-name] a-ref]
         (if (integer? id-or-name)
           (= id-or-name (:id column))
           (and (if-let [join-alias (lib.join.util/current-join-alias a-ref)]
                  (= join-alias (lib.join.util/current-join-alias column))
                  true)
                (= id-or-name (:lib/source-column-alias column)))))))

(mu/defn ^:private matching-breakout-ref :- [:maybe :mbql.clause/field]
  [query        :- ::lib.schema/query
   stage-number :- :int
   column       :- lib.metadata/ColumnMetadata]
  (let [breakouts (lib.breakout/breakouts query stage-number)]
    (m/find-first (fn [breakout]
                    (and (is-ref-for-source-column? breakout column)
                         (= (lib.temporal-bucket/temporal-bucket breakout)
                            (lib.temporal-bucket/temporal-bucket column))))
                  breakouts)))

(mu/defn ^:private next-breakout-unit :- [:maybe ::lib.schema.temporal-bucketing/unit.date-time.truncate]
  [column :- lib.metadata/ColumnMetadata]
  (when-let [current-unit (lib.temporal-bucket/raw-temporal-bucket column)]
    (when (contains? (set valid-current-units) current-unit)
      (unit->next-unit current-unit))))

(mu/defn ^:private describe-next-unit :- ::lib.schema.common/non-blank-string
  [unit :- ::lib.schema.drill-thru/drill-thru.zoom-in.timeseries.next-unit]
  (case unit
    :quarter (i18n/tru "See this year by quarter")
    :month   (i18n/tru "See this quarter by month")
    :week    (i18n/tru "See this month by week")
    :day     (i18n/tru "See this week by day")
    :hour    (i18n/tru "See this day by hour")
    :minute  (i18n/tru "See this hour by minute")))

(mu/defn zoom-in-timeseries-drill :- [:maybe ::lib.schema.drill-thru/drill-thru.zoom-in.timeseries]
  "Zooms in on some window, showing it in finer detail.

  For example: The month of a year, days or weeks of a quarter, smaller lat/long regions, etc.

  This is different from the `:drill-thru/zoom` type, which is for showing the details of a single object."
  ;; TODO: This naming is confusing. Fix it?
  [query                  :- ::lib.schema/query
   stage-number           :- :int
   {:keys [column value]} :- ::lib.schema.drill-thru/context]
  (when (and (lib.drill-thru.common/mbql-stage? query stage-number)
             column
             (some? value)
             (matching-breakout-ref query stage-number column))
    (when-let [next-unit (next-breakout-unit column)]
      {:lib/type     :metabase.lib.drill-thru/drill-thru
       :display-name (describe-next-unit next-unit)
       :type         :drill-thru/zoom-in.timeseries
       :column       column
       :value        value
       :next-unit    next-unit})))

(mu/defmethod lib.drill-thru.common/drill-thru-method :drill-thru/zoom-in.timeseries
  [query                            :- ::lib.schema/query
   stage-number                     :- :int
   {:keys [column value next-unit]} :- ::lib.schema.drill-thru/drill-thru.zoom-in.timeseries]
  (let [breakout     (matching-breakout-ref query stage-number column)
        new-breakout (lib.temporal-bucket/with-temporal-bucket breakout next-unit)]
    (-> query
      (lib.filter/filter stage-number (lib.filter/= column value))
      (lib.remove-replace/replace-clause stage-number breakout new-breakout))))
