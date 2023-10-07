import { t } from "ttag";
import {
  useDatabaseQuery,
  useSchemaListQuery,
  useTableQuery,
} from "metabase/common/hooks";
import type { AnchorProps, TextProps } from "metabase/ui";
import { Box } from "metabase/ui";
import {
  browseSchema,
  tableRowsQuery,
  collection as collectionUrl,
  browseDatabase,
} from "metabase/lib/urls";
import type { Collection } from "metabase-types/api";

import { PLUGIN_COLLECTIONS } from "metabase/plugins";
import { AuthorityLevelIcon } from "metabase/search/components/CollectionBadge.styled";
import { SearchResultLink } from "metabase/search/components/SearchResultLink/SearchResultLink";
import type { WrappedResult } from "metabase/search/types";
import { Icon } from "metabase/core/components/Icon";
import type TableType from "metabase-lib/metadata/Table";

type InfoTextData = {
  link?: string | null;
  icon?: JSX.Element;
  label: string | null;
};

export function InfoText({
  result,
  ...textProps
}: {
  result: WrappedResult;
  textProps?: TextProps | AnchorProps;
}) {
  const infoText: InfoTextData[] = useInfoText(result);

  return (
    <>
      {infoText.map(({ link, icon, label }: InfoTextData, index: number) => (
        <>
          {index > 0 && (
            <Box mt="xs" mx="xs" component="span">
              <Icon name="chevronright" size={10} />
            </Box>
          )}
          <SearchResultLink
            key={label}
            to={link}
            leftIcon={icon}
            {...textProps}
          >
            {label}
          </SearchResultLink>
        </>
      ))}
    </>
  );
}

export const useInfoText = (result: WrappedResult): InfoTextData[] => {
  let infoTextHook;

  switch (result.model) {
    case "table":
      infoTextHook = useTablePath;
      break;
    case "segment":
    case "metric":
      infoTextHook = useTableLink;
      break;
    case "collection":
      infoTextHook = useCollectionInfoText;
      break;
    case "database":
      infoTextHook = useDatabaseInfoText;
      break;
    case "action":
      infoTextHook = useActionInfoText;
      break;
    case "card":
    case "dataset":
    case "indexed-entity":
    default:
      infoTextHook = useCollectionResultLink;
      break;
  }

  return infoTextHook(result);
};

const useActionInfoText = (result: WrappedResult): InfoTextData[] => {
  return [
    {
      label: result.model_name,
    },
  ];
};

const useDatabaseInfoText = (): InfoTextData[] => {
  return [
    {
      label: t`Database`,
    },
  ];
};

const useCollectionInfoText = (result: WrappedResult): InfoTextData[] => {
  const collection: Partial<Collection> = result.getCollection();

  if (
    PLUGIN_COLLECTIONS.isRegularCollection(collection) ||
    !collection.authority_level
  ) {
    return [
      {
        label: t`Collection`,
      },
    ];
  }
  const level = PLUGIN_COLLECTIONS.AUTHORITY_LEVEL[collection.authority_level];
  return [
    {
      label: `${level.name} ${t`Collection`}`,
    },
  ];
};

const useTablePath = (result: WrappedResult): InfoTextData[] => {
  const { data: database } = useDatabaseQuery({
    id: result.database_id,
  });

  const { data: schema = [] } = useSchemaListQuery({
    query: {
      dbId: result.database_id,
    },
  });

  return [
    {
      link: database && browseDatabase(database),
      label: database?.name ?? null,
    },
    {
      link:
        schema?.length > 1
          ? browseSchema({
              db: { id: result.database_id },
              schema_name: result.table_schema,
            } as TableType)
          : null,
      label: result.table_schema,
    },
  ];
};

const useTableLink = (result: WrappedResult): InfoTextData[] => {
  const { data: table } = useTableQuery({
    id: result.table_id,
  });

  return [
    {
      link: tableRowsQuery(result.database_id, result.table_id),
      label: table?.display_name ?? null,
    },
  ];
};

const useCollectionResultLink = (result: WrappedResult): InfoTextData[] => {
  const collection = result.getCollection();
  return [
    {
      icon: <AuthorityLevelIcon collection={collection} />,
      link: collectionUrl(collection),
      label: collection.name,
    },
  ];
};
