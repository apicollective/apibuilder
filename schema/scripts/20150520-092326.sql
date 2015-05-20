set search_path to search;

drop table if exists items;

create table items (
  guid                  uuid primary key,
  type                  text not null check(public.enum(type)),
  label                 text not null check(trim(label) = label),
  description           text check(trim(description) = description),
  content               text not null check(content = trim(lower(content))),
  created_at            timestamptz not null default now()
);

comment on table items is '
  A denormalization of the data in the items table optimized for search
';


comment on column items.guid is '
  The specific guid of the item that was indexed. For versions, this will
  be the version_guid. For applications, this will be the application_guid,
  etc.
';
comment on column items.label is '
  Label to display in the search results. e.g. apidoc
';

comment on column items.description is '
  Optional short description of this item to display in
  the search results
';
