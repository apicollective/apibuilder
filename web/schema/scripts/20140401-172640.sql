drop table if exists membership_logs;
drop table if exists membership_requests;
drop table if exists memberships;

drop table if exists tokens;
drop table if exists versions;
drop table if exists services;
drop table if exists organizations;
drop table if exists users;

create table organizations (
  guid                    uuid primary key,
  name                    text not null constraint organization_name_ck check (trim(name) = name),
  key                     text not null constraint organization_key_ck check (lower(trim(key)) = key)
);

select schema_evolution_manager.create_basic_created_audit_data('public', 'organizations');
select schema_evolution_manager.create_basic_deleted_audit_data('public', 'organizations');
select schema_evolution_manager.create_prevent_delete_trigger('public', 'organizations');

create unique index organizations_lower_name_not_deleted_un_idx on organizations(lower(name)) where deleted_at is null;
create unique index organizations_key_not_deleted_un_idx on organizations(key) where deleted_at is null;

comment on table organizations is '
  A organization is the owner of services. Conceptually, gilt will be an organization
  and all of our services will be mapped under the url /gilt/services/...
';

comment on column organizations.key is '
  Used to uniquely identify this organization. URL friendly.
';


create table users (
  guid                    uuid primary key,
  email                   text not null constraint users_email_lower_ck check (lower(trim(email)) = email),
  name                    text,
  image_url               text
);

select schema_evolution_manager.create_basic_audit_data('public', 'users');
create unique index users_email_not_deleted_un_idx on users(email) where deleted_at is null;

comment on table users is '
  Represents a person interacting with the system.
';

create table memberships (
  guid                    uuid primary key,
  user_guid               uuid not null references users,
  organization_guid       uuid not null references organizations,
  role                    text not null
);

select schema_evolution_manager.create_basic_created_audit_data('public', 'memberships');
select schema_evolution_manager.create_basic_deleted_audit_data('public', 'memberships');
select schema_evolution_manager.create_prevent_delete_trigger('public', 'memberships');

create index on memberships(user_guid);
create index on memberships(organization_guid);
create unique index on memberships(organization_guid, user_guid) where deleted_at is null;

comment on table memberships is '
  Maps a user to 1 or more organizations, authorizing that user to see
  information for that organization.
';

comment on column memberships.role is '
  e.g. member, admin
';


create table membership_requests (
  guid                              uuid primary key,
  user_guid                         uuid not null references users,
  organization_guid                 uuid not null references organizations,
  role                              text not null
);

select schema_evolution_manager.create_basic_created_audit_data('public', 'membership_requests');
select schema_evolution_manager.create_basic_deleted_audit_data('public', 'membership_requests');
select schema_evolution_manager.create_prevent_delete_trigger('public', 'membership_requests');

create index on membership_requests(user_guid);
create index on membership_requests(organization_guid);
create unique index on membership_requests(organization_guid, user_guid, role) where deleted_at is null;

comment on table membership_requests is '
  Captures that a user has requested membership in an organization with
  a given role. This table represents a queue of requests. Each request
  can be reviewed by an admin of the organization, resulting in either
  an approval or rejection which are then tracked in the
  membership_logs table.
';

comment on column membership_requests.role is '
  The role that this user is requesting in this organization.
';

create table membership_logs (
  guid                                uuid primary key,
  organization_guid                   uuid not null references organizations,
  message                             text not null
);

select schema_evolution_manager.create_basic_created_audit_data('public', 'membership_logs');
select schema_evolution_manager.create_prevent_update_trigger('public', 'membership_logs');
select schema_evolution_manager.create_prevent_delete_trigger('public', 'membership_logs');

create index on membership_logs(organization_guid);

comment on table membership_logs is '
  An event log related to membership decisions.
';

create table services (
  guid                    uuid primary key,
  organization_guid       uuid not null references organizations,
  name                    text not null,
  key                     text not null constraint services_key_ck check (lower(trim(key)) = key),
  description             text
);

select schema_evolution_manager.create_basic_audit_data('public', 'services');

create unique index services_organization_guid_name_not_deleted_un_idx on services(organization_guid, lower(name)) where deleted_at is null;
create unique index services_organization_guid_key_not_deleted_un_idx on services(organization_guid, key) where deleted_at is null;
create index on services(organization_guid);

comment on table services is '
  A service is the core entity - a service has some basic attributes (name, url) and
  multiple versions.
';

comment on column services.key is '
  Used to uniquely identify this service. URL friendly.
';

create table versions (
  guid                    uuid primary key,
  service_guid            uuid not null references services,
  version                 text not null,
  version_sort_key        text not null,
  json                    json not null
);

select schema_evolution_manager.create_basic_created_audit_data('public', 'versions');
select schema_evolution_manager.create_basic_deleted_audit_data('public', 'versions');
select schema_evolution_manager.create_prevent_delete_trigger('public', 'versions');

create index on versions(service_guid);
create unique index versions_service_guid_version_not_deleted_un_idx on versions(service_guid, version) where deleted_at is null;

comment on table versions is '
  An version has a version (e.g. 1.2.3) and a json document describing its structure.
  Versions are immutable - if an edit is made, we implement that as a soft delete
  followed by a new insert.
';

comment on column versions.version_sort_key is '
  internal lexicographic string to quickly sort by versions. Initial implementation
  only for versions numbers that are all numeric with dots, with no more than
  4 pieces.
';

create table tokens (
  guid                    uuid primary key,
  user_guid               uuid not null references users,
  token                   text not null unique
);

select schema_evolution_manager.create_basic_created_audit_data('public', 'tokens');
select schema_evolution_manager.create_basic_deleted_audit_data('public', 'tokens');
select schema_evolution_manager.create_prevent_delete_trigger('public', 'tokens');

create index on tokens(user_guid);

comment on table tokens is '
  A user can have 1 or more tokens (a random string) to use when interacting with the API.
';
