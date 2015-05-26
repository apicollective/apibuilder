set search_path to search;

drop table if exists items;

create table items (
  guid                  uuid primary key,
  organization_guid     uuid not null references public.organizations,
  application_guid      uuid references public.applications,
  detail                json not null,
  label                 text not null check(trim(label) = label),
  description           text check(trim(description) = description),
  content               text not null check(content = trim(lower(content))),
  created_at            timestamptz not null default now()
);

create index items_organization_guid_idx on items(organization_guid);
create index items_application_guid_idx on items(application_guid);
create index items_organization_content_idx on items(content);

comment on table items is '
  A denormalization of the data in the items table optimized for search
';

comment on column items.detail is '
  Actual information about this particular item. The contents will vary
  based on the type - in the API the data here are identified by a union
  type.
';

comment on column items.organization_guid is '
  We denormalize the organization guid into the search items table to enable
  filtering all of the content easily by organization and to enable authorization
  checks for the user/organization.
';

comment on column items.application_guid is '
  We denormalize the organization guid into the search items table to authorization
  checks for the user/application.
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
