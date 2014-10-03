drop table if exists generators;

create table generators (
  guid                    uuid primary key,
  organization_guid       uuid not null references organizations,
  name                    text not null,
  uri                     text not null
);

select schema_evolution_manager.create_basic_created_audit_data('public', 'generators');
select schema_evolution_manager.create_basic_deleted_audit_data('public', 'generators');
select schema_evolution_manager.create_prevent_delete_trigger('public', 'generators');

create index on generators(uri);
create unique index on generators(organization_guid, uri) where deleted_at is null;

comment on table generators is '
  A generator represents a remote service that implements the apidoc generator api
';

comment on column generators.uri is '
  A uri pointing to the generator service
';
