import { t } from "ttag";
import type { WrappedResult } from "metabase/search/types";
import { Paper, Stack, Text } from "metabase/ui";
import { SearchResult } from "metabase/search/components/SearchResult";

type SearchResultSectionProps = {
  items: WrappedResult[];
  totalResults: number;
};

export const SearchResultSection = ({
  items,
  totalResults,
}: SearchResultSectionProps) => {
  const numResultsText =
    totalResults === 1 ? t`1 result` : t`${totalResults} results`;

  return (
    <Paper p="md">
      <Text mb="sm" ml="xs" tt="uppercase" c="text.1" fw={700}>
        {numResultsText}
      </Text>
      <Stack spacing="xs" justify="center" align="center">
        {items.map(item => (
          <SearchResult key={item.id} result={item} />
        ))}
      </Stack>
    </Paper>
  );
};
