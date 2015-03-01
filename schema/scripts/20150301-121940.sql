drop table if exists originals;

create table originals (
  id                      bigserial primary key,
  version_guid            uuid not null references versions,
  type                    text not null check(enum(type)),
  data                    text not null
);

select schema_evolution_manager.create_basic_audit_data('public', 'originals');
alter table originals drop column updated_by_guid; -- never updated

create index on originals(version_guid);

comment on table originals is '
  Tracks the original content uploaded to create versions.
';

comment on column originals.type is '
  The type of original - e.g. api_json, avro_idl, etc.
';
