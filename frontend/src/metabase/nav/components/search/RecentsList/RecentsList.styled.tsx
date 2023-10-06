import styled from "@emotion/styled";
import LoadingSpinner from "metabase/components/LoadingSpinner";
import Text from "metabase/components/type/Text";
import Link from "metabase/core/components/Link";

import { color, lighten } from "metabase/lib/colors";
import { breakpointMinSmall, space } from "metabase/styled-components/theme";

export const Root = styled.div`
  padding-top: 0.5rem;
  padding-bottom: 0.5rem;

  background-color: ${color("bg-white")};
  line-height: 24px;

  box-shadow: 0 20px 20px ${color("shadow")};

  ${breakpointMinSmall} {
    border: 1px solid ${color("border")};
    border-radius: 6px;
    box-shadow: 0 7px 20px ${color("shadow")};
  }
`;

export const EmptyStateContainer = styled.div`
  margin: 3rem 0;
`;

export const Header = styled.h4`
  padding: 0.5rem 1rem;
`;

export const RecentListItemContent = styled.div`
  display: flex;
  align-items: flex-start;
`;

interface ResultStylesProps {
  compact: boolean;
  active: boolean;
  isSelected: boolean;
}

export const TitleWrapper = styled.div`
  display: flex;
  grid-gap: 0.25rem;
  align-items: center;
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
export const ResultSpinner = styled(LoadingSpinner)`
  display: flex;
  flex-grow: 1;
  align-self: center;
  justify-content: flex-end;
  margin-left: ${space(1)};
  color: ${color("brand")};
`;
