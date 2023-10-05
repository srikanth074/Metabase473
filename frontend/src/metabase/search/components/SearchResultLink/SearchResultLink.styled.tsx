/*
* type ContainerProps = {
    children: React.ReactNode,
    type?: 'section' | 'div' | 'article',
}

const Container = styled.div`
  color: ${props => props.theme.primaryColor};
`;

const Icon = styled.div<ContainerProps>`
    background-color: red;
`;

const container: React.FunctionComponent<ContainerProps['children']> = ({ children, type = 'section' }: ContainerProps) => {
    return (
        <Container as={type}>
            { children }
        </Container>
    );
};

*
* */

import styled from "@emotion/styled";
import type { TextProps, AnchorProps } from "metabase/ui";

type ResultLinkProps = AnchorProps | TextProps;

export const ResultLink = styled.a<ResultLinkProps>`
`;
