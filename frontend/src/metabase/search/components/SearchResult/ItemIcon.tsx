import type { SearchModelType } from "metabase-types/api";
import { CollectionIcon } from "metabase/search/components/SearchResult/CollectionIcon";
import { DefaultIcon } from "metabase/search/components/SearchResult/DefaultIcon";
import { IconWrapper } from "metabase/search/components/SearchResult/ItemIcon.styled";
import { TableIcon } from "metabase/search/components/SearchResult/TableIcon";
import type { WrappedResult } from "metabase/search/types";

const ModelIconComponentMap = {
  table: TableIcon,
  collection: CollectionIcon,
};

export function ItemIcon({
  item,
  type,
  active,
}: {
  item: WrappedResult;
  type: SearchModelType;
  active: boolean;
}) {
  const IconComponent =
    type in Object.keys(ModelIconComponentMap)
      ? ModelIconComponentMap[type as keyof typeof ModelIconComponentMap]
      : DefaultIcon;

  return (
    <IconWrapper type={type} active={active}>
      <IconComponent item={item} />
    </IconWrapper>
  );
}
