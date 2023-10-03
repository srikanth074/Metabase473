import { t } from "ttag";
import type { Series, VisualizationSettings } from "metabase-types/api";
import type { ChartGoal } from "metabase/visualizations/shared/types/settings";
import type { VisualizationSettingsDefinitions } from "metabase/visualizations/types";
import { getStackOffset } from "./stacking";

const getGoalValue = (value: number, isPercent: boolean) =>
  isPercent ? value / 100 : value;

export const getChartGoal = (
  settings: VisualizationSettings,
): ChartGoal | null => {
  if (!settings["graph.show_goal"]) {
    return null;
  }
  const isPercent = getStackOffset(settings) === "expand";

  return {
    value: getGoalValue(settings["graph.goal_value"] ?? 0, isPercent),
    label: settings["graph.goal_label"] ?? t`Goal`,
  };
};

export const GRAPH_GOAL_SETTINGS: VisualizationSettingsDefinitions<Series> = {
  "graph.show_goal": {
    section: t`Display`,
    title: t`Goal line`,
    widget: "toggle",
    default: false,
    inline: true,
    marginBottom: "1rem",
  },
  "graph.goal_value": {
    section: t`Display`,
    title: t`Goal value`,
    widget: "number",
    default: 0,
    getHidden: (_series: unknown, vizSettings: VisualizationSettings) =>
      vizSettings["graph.show_goal"] !== true,
    readDependencies: ["graph.show_goal"],
  },
  "graph.goal_label": {
    section: t`Display`,
    title: t`Goal label`,
    widget: "input",
    default: t`Goal`,
    getHidden: (_series: unknown, vizSettings: VisualizationSettings) =>
      vizSettings["graph.show_goal"] !== true,
    readDependencies: ["graph.show_goal"],
  },
};
