drop table if exists organization_metadata;

create table organization_metadata (
  guid                    uuid primary key,
  organization_guid       uuid not null references organizations,
  package_name            text
);

select schema_evolution_manager.create_basic_created_audit_data('public', 'organization_metadata');
select schema_evolution_manager.create_basic_deleted_audit_data('public', 'organization_metadata');

create index on organization_metadata(organization_guid);
create unique index organization_metadata_organization_guid_not_deleted_un_idx on organization_metadata(organization_guid) where deleted_at is null;

comment on table organization_metadata is '
  Stores supplementary information for a given organization. One example is the package name
  to use for code generators.
';
