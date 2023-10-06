import { Divider } from "@mantine/core";
import { Fragment } from "react";
import { color } from "metabase/lib/colors";
import { isSyncCompleted } from "metabase/lib/syncing";
import { InfoText } from "metabase/search/components/InfoText";

import { ItemIcon } from "metabase/search/components/SearchResult/ItemIcon";
import {
  DescriptionContainer,
  IconContainer,
  InfoTextContainer,
  LoadingContainer,
  ModerationIcon,
  SearchResultContainer,
  TitleContainer,
} from "metabase/search/components/SearchResult/SearchResult.styled";
import { formatDate } from "metabase/search/components/SearchResult/utils/format-date";
import { getUserLabel } from "metabase/search/components/SearchResult/utils/get-user-label";

import type { WrappedResult } from "metabase/search/types";
import * as UI from "metabase/ui";

export function SearchResult({
  result,
  compact = false,
  showDescription = true,
  isSelected = false,
}: {
  result: WrappedResult;
  compact?: boolean;
  showDescription?: boolean;
  onClick?: (result: WrappedResult) => void;
  isSelected?: boolean;
}) {
  const {
    name,
    description,
    moderated_status,
    last_edited_at,
    created_at,
  } = result;

  const shouldDisplayDescription = showDescription && !!description;

  const isActive = isItemActive(result);
  const isLoading = isItemLoading(result);

  const userLabel = getUserLabel(result);
  const dateLabel = formatDate(last_edited_at ?? created_at);

  const descriptionContainer = shouldDisplayDescription ? (
    <DescriptionContainer spacing="sm" mt="sm" noWrap>
      <Divider size="md" py="sm" color="focus.0" orientation="vertical" />
      <UI.Text size="sm" c="text.1" lineClamp={3}>
        {description}
      </UI.Text>
    </DescriptionContainer>
  ) : null;

  const loadingContainer = isLoading ? (
    <LoadingContainer p="lg">
      <UI.Loader data-testid="search-result-loading-spinner" size="md" />
    </LoadingContainer>
  ) : null;

  const infoContainer = [
    <InfoText key="info-text" result={result} />,
    userLabel,
    compact ? dateLabel : null,
  ]
    .filter(Boolean)
    .map((item, index) => (
      <Fragment key={index}>
        {index > 0 && (
          <UI.Text span mx="xs" c="text.1">
            •
          </UI.Text>
        )}
        {item}
      </Fragment>
    ));

  return (
    <SearchResultContainer
      p="sm"
      w="100%"
      showDescription={shouldDisplayDescription}
      isActive={isActive}
      isLoading={isLoading}
      isSelected={isSelected}
    >
      <IconContainer>
        <ItemIcon item={result} type={result.model} active={isActive} />
      </IconContainer>
      <TitleContainer
        lh="unset"
        size="md"
        fw={700}
        c={color(isActive ? "text.2" : "text.1")}
        truncate
      >
        {name}
        <UI.Box pos="relative" top="0.125rem" component="span">
          <ModerationIcon status={moderated_status} size={16} />
        </UI.Box>
      </TitleContainer>
      <InfoTextContainer fz={compact ? "sm" : "md"} spacing="xs">{infoContainer}</InfoTextContainer>
      {descriptionContainer}
      {loadingContainer}
    </SearchResultContainer>
  );
}

const isItemActive = (result: WrappedResult) => {
  if (result.model === "table") {
    return isSyncCompleted(result);
  }
  return true;
};

const isItemLoading = (result: WrappedResult) => {
  if (result.model === "database" || result.model === "table") {
    return !isSyncCompleted(result);
  }
  return false;
};
