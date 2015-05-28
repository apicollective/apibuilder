set search_path to generators;

drop table if exists generators;
drop table if exists refreshes;
drop table if exists services;

create table services (
  guid                    uuid primary key,
  uri                     text not null check(trim(uri) = uri),
  visibility              text not null check(public.enum(visibility))
);


select schema_evolution_manager.create_basic_audit_data('generators', 'services');
alter table services drop column updated_by_guid; -- never updated
create unique index services_lower_uri_not_deleted_un_idx on services(lower(uri)) where deleted_at is null;

comment on table services is '
  Stores the URI of each generator service.
';

create table refreshes (
  guid                    uuid primary key,
  service_guid             uuid not null references services,
  checked_at              timestamptz not null
);

select schema_evolution_manager.create_basic_audit_data('generators', 'refreshes');
create index on refreshes(service_guid);
create unique index on refreshes(service_guid) where refreshes.deleted_at is null;

comment on table refreshes is '
  For each service, we periodically refresh the generator information
  and store it in the generators table. This table records when we
  last checked the generator information.
';

create table generators (
  guid                    uuid primary key,
  service_guid             uuid not null references services,
  key                     text not null check(trim(key) = key),
  name                    text not null check(trim(name) = name),
  description             text check(trim(description) = description),
  language                text check(trim(language) = language)
);

select schema_evolution_manager.create_basic_audit_data('generators', 'generators');
alter table generators drop column updated_by_guid; -- never updated
create index on generators(service_guid);
create unique index generators_key_not_deleted_un_idx on generators(key) where deleted_at is null;

comment on table generators is '
  This table is a cache of the generator information. It is refreshed
  periodically in the background. The data comes from fetching the service
  URI which returns an instance of the generator model, stored here.
';
