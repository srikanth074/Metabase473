import { css } from "@emotion/react";
import styled from "@emotion/styled";

import { color, lighten } from "metabase/lib/colors";
import { PLUGIN_MODERATION } from "metabase/plugins";
import { InfoText } from "metabase/search/components/InfoText";
import { space } from "metabase/styled-components/theme";
import Link from "metabase/core/components/Link";
import Text from "metabase/components/type/Text";
import LoadingSpinner from "metabase/components/LoadingSpinner";

import type { SearchModelType } from "metabase-types/api";

import type { FlexProps, TextProps } from "metabase/ui";
import { Flex, Text as MantineText } from "metabase/ui";

type SearchEntity = any;

interface ResultStylesProps {
  compact: boolean;
  active: boolean;
  isSelected: boolean;
}

function getColorForIconWrapper({
  active,
  type,
}: {
  active: boolean;
  type: SearchModelType;
}) {
  if (!active) {
    return color("text-medium");
  }
  if (type === "collection") {
    return lighten("brand", 0.35);
  }
  return color("brand");
}

export const IconWrapper = styled.div<{
  active: boolean;
  type: SearchModelType;
}>`
  border: ${({ theme }) => `1px solid ${theme.colors.border[0]}`};
  border-radius: ${({ theme }) => theme.radius.sm};
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: ${({ active, type }) => getColorForIconWrapper({ active, type })};
  //margin-right: 10px;
  flex-shrink: 0;
`;

export const TitleWrapper = styled.div`
  display: flex;
  grid-gap: 0.25rem;
  align-items: center;
`;

export const ContextText = styled("p")`
  line-height: 1.4em;
  color: ${color("text-medium")};
  margin-top: 0;
`;

export const Title = styled("h3")<{ active: boolean }>`
  margin-bottom: 4px;
  color: ${props => color(props.active ? "text-dark" : "text-medium")};
`;

export const ResultButton = styled.button<ResultStylesProps>`
  ${props => resultStyles(props)}
  padding-right: 0.5rem;
  text-align: left;
  cursor: pointer;
  width: 100%;

  &:hover {
    ${Title} {
      color: ${color("brand")};
    }
  }
`;

export const ResultLink = styled(Link)<ResultStylesProps>`
  ${props => resultStyles(props)}
`;

const resultStyles = ({ compact, active, isSelected }: ResultStylesProps) => `
  display: block;
  background-color: ${isSelected ? lighten("brand", 0.63) : "transparent"};
  min-height: ${compact ? "36px" : "54px"};
  padding-top: ${space(1)};
  padding-bottom: ${space(1)};
  padding-left: 14px;
  padding-right: ${compact ? "20px" : space(3)};
  cursor: ${active ? "pointer" : "default"};

  &:hover {
    background-color: ${active ? lighten("brand", 0.63) : ""};

    h3 {
      color: ${active || isSelected ? color("brand") : ""};
    }
  }

  ${Link.Root} {
    text-underline-position: under;
    text-decoration: underline ${color("text-light")};
    text-decoration-style: dashed;

    &:hover {
      color: ${active ? color("brand") : ""};
      text-decoration-color: ${active ? color("brand") : ""};
    }
  }

  ${Text} {
    margin-top: 0;
    margin-bottom: 0;
    font-size: 13px;
    line-height: 19px;
  }

  h3 {
    font-size: ${compact ? "14px" : "16px"};
    line-height: 1.2em;
    overflow-wrap: anywhere;
    margin-bottom: 0;
    color: ${active && isSelected ? color("brand") : ""};
  }

  .Icon-info {
    color: ${color("text-light")};
  }
`;

export const ResultInner = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

export const ResultLinkContent = styled.div`
  display: flex;
  align-items: start;
  overflow-wrap: anywhere;
`;

export const Description = styled(Text)`
  padding-left: ${space(1)};
  margin-top: ${space(1)} !important;
  border-left: 2px solid ${lighten("brand", 0.45)};
`;

export const ContextContainer = styled.div`
  margin-left: 42px;
  margin-top: 12px;
  max-width: 620px;
`;

export const ResultSpinner = styled(LoadingSpinner)`
  display: flex;
  flex-grow: 1;
  align-self: center;
  justify-content: flex-end;
  margin-left: ${space(1)};
  color: ${color("brand")};
`;

export const TitleText = styled(MantineText)<TextProps>``;

export const SearchResultContainer = styled(Flex)<
  FlexProps & {
    hasDescription: boolean;
    isActive: boolean;
    isLoading: boolean;
    isSelected: boolean;
  }
>`
  transition: background-color 0.2s ease-in-out;
  cursor: ${({isActive}) => (isActive ? "pointer" : "default")};

  border-radius: ${({theme}) => theme.radius.sm};

  &:hover {
    background-color: ${({theme, isActive}) =>
            isActive ? theme.colors.brand[0] : null};

    ${TitleText} {
      color: ${({theme, isActive, isSelected}) =>
              isActive || isSelected ? theme.colors.brand[1] : null};
    }
  }
`;

export const SearchResultParentLink = styled(InfoText)`
  & a {
    font-weight: bold;
  }
`;

export const ModerationIcon = styled(PLUGIN_MODERATION.ModerationStatusIcon)`
  margin-bottom: -0.125rem;
  margin-left: 0.25rem;
`;
