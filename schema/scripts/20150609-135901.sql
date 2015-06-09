create table application_moves (
  guid                    uuid primary key,
  application_guid        uuid not null references applications,
  from_organization_guid  uuid not null references organizations,
  to_organization_guid    uuid not null references organizations,
  check (from_organization_guid != to_organization_guid)
);

select schema_evolution_manager.create_basic_audit_data('public', 'application_moves');
alter table application_moves drop column updated_by_guid; -- never updated

create index on application_moves(application_guid);
create index on application_moves(from_organization_guid);
create index on application_moves(to_organization_guid);

comment on table application_moves is '
  Records that a user moved an application from one organization
  to another.
';
