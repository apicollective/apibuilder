create schema cache;
set search_path to cache;

create table services (
  guid                    uuid primary key,
  version_guid            uuid not null references public.versions,
  version_number          integer not null check (version_number >= 1),
  json                    json not null
);

comment on table services is '
  A cache of the service JSON for a particular version. Stored in a
  different table so that it is reasonably straightforward to implement
  an upgrade w/out needing to modify the versions table itself.
';

comment on column services.version_number is '
  This is an interval version number for the service spec. On startup,
  we automatically make sure that every record in the versions table
  has a service with the latest version number, creating the services
  as needed. So the upgrade process is to increment the service
  version number - the service json will automatically get regenerated.
';

select schema_evolution_manager.create_basic_audit_data('cache', 'services');
alter table services drop column updated_by_guid; -- insert/delete only

create index on services(version_guid);
create unique index services_version_guid_version_number_not_deleted_un_idx on services(version_guid, version_number) where deleted_at is null;

