import { Divider } from "@mantine/core";
import { color } from "metabase/lib/colors";
import moment from "moment";
import { isSyncCompleted } from "metabase/lib/syncing";

import { ItemIcon } from "metabase/search/components/SearchResult/ItemIcon";

import type { WrappedResult } from "metabase/search/types";
import * as UI from "metabase/ui";
import {
  ModerationIcon,
  SearchResultContainer,
  SearchResultParentLink,
  TitleText,
} from "./SearchResult.styled";

const formatDate = (inputDate?: string | null) => {
  if (!inputDate) {
    return null;
  }
  const date = moment(inputDate);
  const today = moment().startOf("day");
  const yesterday = moment().subtract(1, "days").startOf("day");
  const lastWeek = moment().subtract(7, "days").startOf("day");

  if (date.isSame(today, "day")) {
    return `Today, ${date.format("h:mmA")}`;
  } else if (date.isSame(yesterday, "day")) {
    return `Yesterday, ${date.format("h:mmA")}`;
  } else if (date.isAfter(lastWeek)) {
    return `${date.format("dddd, h:mmA")}`;
  } else {
    return date.format("MMMM D, YYYY");
  }
};

export function SearchResult({
  result,
  hasDescription = true,
  isSelected = false,
}: {
  result: WrappedResult;
  compact?: boolean;
  hasDescription?: boolean;
  onClick?: (result: WrappedResult) => void;
  isSelected?: boolean;
}) {
  const {
    name,
    description,
    moderated_status,
    last_edited_at,
    last_editor_common_name,
    creator_common_name,
    created_at,
  } = result;

  const isActive = isItemActive(result);
  const isLoading = isItemLoading(result);

  const getUserLabel = () => {
    if (last_editor_common_name) {
      return `Last edited by ${last_editor_common_name}`;
    }

    if (creator_common_name) {
      return `Created by ${creator_common_name}`;
    }

    return null;
  };

  const dateLabel = last_edited_at ?? created_at;

  return (
    <SearchResultContainer
      p="sm"
      w="100%"
      justify="flex-start"
      align="center"
      gap="0.5rem 0.875rem"
      hasDescription={hasDescription && !!description}
      isActive={isActive}
      isLoading={isLoading}
      isSelected={isSelected}
    >
      <UI.Center>
        <ItemIcon item={result} type={result.model} active={isActive} />
      </UI.Center>
      <UI.Stack style={{ flex: 1 }} spacing="xs">
        <TitleText
          lh="unset"
          size="md"
          fw={700}
          c={color(isActive ? "text.2" : "text.1")}
        >
          {name}
          <ModerationIcon status={moderated_status} size={14} />
        </TitleText>
        <UI.Group spacing="xs">
          <SearchResultParentLink result={result} />
          {getUserLabel() && (
            <>
              <UI.Text lh="unset" size="sm" c="text.1">
                •
              </UI.Text>
              <UI.Text lh="unset" size="sm" c="text.1">
                {getUserLabel()}
              </UI.Text>
            </>
          )}

          <UI.Text lh="unset" size="sm" c="text.1">
            •
          </UI.Text>
          <UI.Text lh="unset" size="sm" c="text.1">
            {formatDate(dateLabel)}
          </UI.Text>
        </UI.Group>
      </UI.Stack>
      {/*{hasDescription && description && (*/}
      {/*  <>*/}
      {/*    <div></div>*/}
      {/*    <UI.Group spacing="sm" noWrap>*/}
      {/*      <Divider*/}
      {/*        size="md"*/}
      {/*        py="sm"*/}
      {/*        color="focus.0"*/}
      {/*        orientation="vertical"*/}
      {/*        style={{ borderRadius: "4px" }}*/}
      {/*      />*/}
      {/*      <UI.Text size="sm" c="text.1">*/}
      {/*        {description}*/}
      {/*      </UI.Text>*/}
      {/*    </UI.Group>*/}
      {/*  </>*/}
      {/*)}*/}

      {isLoading && (
        // SearchApp also uses `loading-spinner`, using a different test ID
        // to not confuse unit tests waiting for loading-spinner to disappear
        <UI.Center>
          <UI.Loader data-testid="search-result-loading-spinner" size="md" />
        </UI.Center>
      )}
    </SearchResultContainer>
  );
}

const isItemActive = (result: WrappedResult) => {
  switch (result.model) {
    case "table":
      return isSyncCompleted(result);
    default:
      return true;
  }
};

const isItemLoading = (result: WrappedResult) => {
  switch (result.model) {
    case "database":
    case "table":
      return !isSyncCompleted(result);
    default:
      return false;
  }
};
