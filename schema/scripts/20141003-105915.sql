drop table if exists generator_users;
drop table if exists generator_organizations;
drop table if exists generators;

create table generators (
  guid                    uuid not null primary key,
  key                     text not null check (lower(btrim(key)) = key),
  user_guid               uuid not null references users,
  uri                     text,
  name                    text not null,
  description             text,
  language                text,
  visibility              text not null check (visibility in ('public', 'organization', 'user'))
);

select schema_evolution_manager.create_basic_audit_data('public', 'generators');
alter table generators drop column updated_by_guid;

create unique index on generators(key) where deleted_at is null;
create index on generators(user_guid);
create index on generators(uri);
create index on generators(visibility);

comment on table generators is '
  A generator represents a remote service that implements the apidoc generator api
';

comment on column generators.uri is '
  A uri pointing to the generator service
';


alter table services drop constraint services_visibility_check;
alter table services add constraint services_visibility_check check (visibility in ('public', 'organization', 'user'));

alter table organization_metadata drop constraint organization_metadata_visibility_check;
alter table organization_metadata add constraint organization_metadata_visibility_check check (visibility in ('public', 'organization', 'user'));



create table generator_users (
  guid                    uuid not null primary key,
  generator_guid          uuid not null references generators,
  user_guid               uuid not null references users,
  enabled                 boolean not null
);

select schema_evolution_manager.create_basic_audit_data('public', 'generator_users');
alter table generator_users drop column updated_by_guid;

create index on generator_users(generator_guid);
create index on generator_users(user_guid);
create unique index on generator_users(generator_guid, user_guid) where deleted_at is null;



create table generator_organizations (
  guid                    uuid not null primary key,
  generator_guid          uuid not null references generators,
  organization_guid       uuid not null references organizations
);

select schema_evolution_manager.create_basic_audit_data('public', 'generator_organizations');
alter table generator_organizations drop column updated_by_guid;

create index on generator_organizations(generator_guid);
create index on generator_organizations(organization_guid);
create unique index on generator_organizations(generator_guid, organization_guid) where deleted_at is null;
