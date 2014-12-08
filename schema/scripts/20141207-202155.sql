drop table if exists watches;

create table watches (
  guid                    uuid primary key,
  user_guid               uuid not null references users,
  service_guid            uuid not null references services
);

select schema_evolution_manager.create_basic_audit_data('public', 'watches');
alter table watches drop column updated_by_guid; -- no updates
create index on watches(user_guid);
create index on watches(service_guid);

create unique index watches_user_guid_service_guid_not_deleted_un_idx
           on watches(user_guid, service_guid)
        where deleted_at is null;

comment on table watches is '
  Users can watch individual services which enables features like
  receiving an email notification when there is a new version of
  a service.
';
