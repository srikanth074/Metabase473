import { ResultLink } from "metabase/search/components/SearchResultLink/SearchResultLink.styled";
import type { TextProps, AnchorProps } from "metabase/ui";
import { Anchor, Group, Text } from "metabase/ui";

export const SearchResultLink = ({
  children,
  leftIcon = null,
  to,
  ...textProps
}: {
  children: JSX.Element | string | null;
  leftIcon?: JSX.Element | null;
  to?: string;
  textProps?: TextProps | AnchorProps;
}) => {
  const Component = to ? Anchor : Text;

  return (
    <Group align="center">
      {leftIcon}
      <ResultLink fz="sm" lh="unset" as={Component} href={to} {...textProps}>
        {children}
      </ResultLink>
    </Group>
  );
};
