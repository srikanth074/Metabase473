import styled from "@emotion/styled";
import type { HTMLAttributes } from "react";
import { PLUGIN_MODERATION } from "metabase/plugins";
import type { BoxProps, TextProps, CenterProps, GroupProps } from "metabase/ui";
import { Box, Text, Center, Group } from "metabase/ui";

const ICON_GRID_AREA = "1 / 1 / 3 / 2";
const TITLE_GRID_AREA = "1 / 2 / 2 / 3";
const INFO_TEXT_GRID_AREA = "2 / 2 / 3 / 3";
const DESCRIPTION_GRID_AREA = "3 / 2 / 4 / 3";
const LOADING_GRID_AREA = "1 / 3 / 3 / 4";

export const IconContainer = styled(Center)<CenterProps>`
  grid-area: ${ICON_GRID_AREA};
`;
export const TitleContainer = styled(Text)<TextProps>`
  grid-area: ${TITLE_GRID_AREA};
`;

export const InfoTextContainer = styled(Box)<GroupProps>`
  grid-area: ${INFO_TEXT_GRID_AREA};

  //  add dot between elements in flex container
  //  & > div:not(:first-child)::before {
  //    content:'•';
  //     color: ${({ theme }) => theme.colors.text[1]};
  //    align-self: stretch;
  //  }
`;
export const DescriptionContainer = styled(Group)<
  GroupProps & HTMLAttributes<HTMLDivElement>
>`
  grid-area: ${DESCRIPTION_GRID_AREA};
`;
export const LoadingContainer = styled(Center)<
  CenterProps & HTMLAttributes<HTMLDivElement>
>`
  grid-area: ${LOADING_GRID_AREA};
`;
export const ModerationIcon = styled(PLUGIN_MODERATION.ModerationStatusIcon)`
  margin-bottom: -0.125rem;
  margin-left: 0.25rem;
`;

export const SearchResultContainer = styled(Box)<
  BoxProps & {
    showDescription: boolean;
    isActive: boolean;
    isLoading: boolean;
    isSelected: boolean;
  }
>`
  display: grid;
  grid-template-columns: auto 1fr auto;
  grid-template-rows: auto auto;
  gap: ${({ theme }) => `0 ${theme.spacing.md}`};

  cursor: ${({ isActive }) => (isActive ? "pointer" : "default")};

  border-radius: ${({ theme }) => theme.radius.sm};

  &:hover {
    background-color: ${({ theme, isActive }) =>
      isActive ? theme.colors.brand[0] : null};

    ${TitleContainer} {
      color: ${({ theme, isActive, isSelected }) =>
        isActive || isSelected ? theme.colors.brand[1] : null};
    }
  }
`;
