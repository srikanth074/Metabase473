import { DATE_OPERATORS } from "metabase/query_builder/components/filters/pickers/DatePicker/DatePicker";
import { DATE_SHORTCUT_OPTIONS } from "metabase/query_builder/components/filters/pickers/DatePicker/DatePickerShortcutOptions";
import { SearchFilterDatePickerWrapper } from "metabase/search/components/SearchFilterDatePicker/SearchFilterDatePicker.styled";

const CREATED_AT_FILTERS = DATE_OPERATORS.filter(
  ({ name }) => name !== "exclude",
);

const CREATED_AT_SHORTCUTS = {
  ...DATE_SHORTCUT_OPTIONS,
  MISC_OPTIONS: DATE_SHORTCUT_OPTIONS.MISC_OPTIONS.filter(
    ({ displayName }) => displayName !== "Exclude…",
  ),
};

export const SearchFilterDatePicker = ({
  value,
  onChange,
}: {
  value: string | null;
  onChange: (value: string | null) => void;
}) => (
  <SearchFilterDatePickerWrapper
    value={value}
    setValue={(value: string | null) => onChange(value)}
    operators={CREATED_AT_FILTERS}
    dateShortcutOptions={CREATED_AT_SHORTCUTS}
    withPadding={false}
  />
);
