import type TableType from "metabase-lib/metadata/Table";

import type { Collection } from "metabase-types/api";

import { Icon } from "metabase/core/components/Icon";
import Database from "metabase/entities/databases";

import Schema from "metabase/entities/schemas";
import Table from "metabase/entities/tables";

import * as Urls from "metabase/lib/urls";
import { PLUGIN_COLLECTIONS } from "metabase/plugins";
import { AuthorityLevelIcon } from "metabase/search/components/CollectionBadge.styled";
import { SearchResultLink } from "metabase/search/components/SearchResultLink/SearchResultLink";
import type { WrappedResult } from "metabase/search/types";
import { jt, t } from "ttag";

const getCollectionResultLink = (result: WrappedResult) => {
  const collection = result.getCollection();
  return (
    <SearchResultLink
      leftIcon={<AuthorityLevelIcon collection={collection} />}
      to={Urls.collection(collection)}
    >
      {collection.name}
    </SearchResultLink>
  );
};

export function InfoText({ result }: { result: WrappedResult }) {
  let textContent: string | string[] | JSX.Element | null;

  switch (result.model) {
    case "card":
    case "dataset":
    case "indexed-entity":
    default:
      return getCollectionResultLink(result);
    case "table":
      return <TablePath result={result} />;
    case "segment":
    case "metric":
      return <TableLink result={result} />;
    case "collection":
      textContent = getCollectionInfoText(result.collection);
      break;
    case "database":
      textContent = t`Database`;
      break;
    case "action":
      textContent = result.model_name;
      break;
  }

  return <SearchResultLink>{textContent}</SearchResultLink>;
}

function getCollectionInfoText(collection: Partial<Collection>) {
  if (
    PLUGIN_COLLECTIONS.isRegularCollection(collection) ||
    !collection.authority_level
  ) {
    return t`Collection`;
  }
  const level = PLUGIN_COLLECTIONS.AUTHORITY_LEVEL[collection.authority_level];
  return `${level.name} ${t`Collection`}`;
}

function TablePath({ result }: { result: WrappedResult }) {
  return (
    <>
      {jt`${(
        <>
          <Database.Link
            LinkComponent={SearchResultLink}
            id={result.database_id}
          />{" "}
          {result.table_schema && (
            <Schema.ListLoader
              query={{ dbId: result.database_id }}
              loadingAndErrorWrapper={false}
            >
              {({ list }: { list: typeof Schema[] }) =>
                list?.length > 1 ? (
                  <SearchResultLink
                    leftIcon={<Icon name="chevronright" size={10} />}
                    to={Urls.browseSchema({
                      db: { id: result.database_id },
                      schema_name: result.table_schema,
                    } as TableType)}
                  >
                    {result.table_schema}
                  </SearchResultLink>
                ) : null
              }
            </Schema.ListLoader>
          )}
        </>
      )}`}
    </>
  );
}

function TableLink({ result }: { result: WrappedResult }) {
  return (
    <SearchResultLink
      to={Urls.tableRowsQuery(result.database_id, result.table_id)}
    >
      <Table.Loader id={result.table_id} loadingAndErrorWrapper={false}>
        {({ table }: { table: TableType }) =>
          table ? <span>{table.display_name}</span> : null
        }
      </Table.Loader>
    </SearchResultLink>
  );
}
