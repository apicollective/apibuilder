drop table if exists changes;

create table changes (
  guid                    uuid primary key,
  application_guid        uuid not null references applications,
  from_version_guid       uuid not null references versions,
  to_version_guid         uuid not null references versions,
  constraint changes_from_version_guid_ne_to_version_guid_ck check (from_version_guid != to_version_guid),
  -- enforce application guid is the same for both versions
  constraint changes_application_from_version_fk foreign key (application_guid, from_version_guid) references versions(application_guid, guid),
  constraint changes_application_to_version_fk foreign key (application_guid, to_version_guid) references versions(application_guid, guid),
  type                    text not null check(enum(type)),
  description             text not null check(trim(description) = description),
  changed_at              timestamptz not null,
  changed_by_guid         uuid not null
);

select schema_evolution_manager.create_basic_audit_data('public', 'changes');
alter table changes drop column updated_by_guid; -- we never update

create index on changes(application_guid);

create unique index changes_from_to_lower_description_not_deleted_un_idx on changes(from_version_guid, to_version_guid, lower(description)) where deleted_at is null;

comment on table changes is '
  Records changes made between individual versions of an application
';
