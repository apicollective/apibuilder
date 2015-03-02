create schema cache;
set search_path to cache;

create table services (
  guid                    uuid primary key,
  version_guid            uuid not null references public.versions,
  version                 text not null check (lower(btrim(version)) = version),
  json                    json not null
);

comment on table services is '
  A cache of the service JSON for a particular version. Stored in a
  different table so that it is reasonably straightforward to implement
  an upgrade w/out needing to modify the versions table itself.
';

comment on column services.version is '
  References the version of the service spec. Allows us to
  automatically generate new versions of the service whenever a new
  version of the spec is available.
';

select schema_evolution_manager.create_basic_audit_data('cache', 'services');
alter table services drop column updated_by_guid; -- insert/delete only

create index on services(version_guid);
create unique index services_version_guid_version_not_deleted_un_idx on services(version_guid, version) where deleted_at is null;
