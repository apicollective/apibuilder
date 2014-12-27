drop table if exists applications;

create table applications (
  guid                    uuid primary key,
  organization_guid       uuid not null references organizations,
  name                    text not null check (trim(name) = name),
  key                     text not null constraint applications_key_ck check (lower(trim(key)) = key),
  visibility              text not null check (visibility in ('public', 'organization')),
  description             text
);

select schema_evolution_manager.create_basic_audit_data('public', 'applications');

create unique index applications_organization_guid_key_not_deleted_un_idx on applications(organization_guid, key) where deleted_at is null;
create index on applications(organization_guid);

comment on table applications is '
  A application is the core entity - an application has some basic attributes
  but is really a container for multiple versions of an API.
';

comment on column applications.key is '
  Used to uniquely identify this application. URL friendly.
';
