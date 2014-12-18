drop table if exists password_resets;

create table password_resets (
  guid                    uuid not null primary key,
  user_guid               uuid not null references users,
  token                   text not null unique check (trim(token) = token),
  expires_at              timestamptz not null
);

select schema_evolution_manager.create_basic_audit_data('public', 'password_resets');
alter table password_resets drop column updated_by_guid; -- never updated

create index on password_resets(user_guid);

comment on table password_resets is '
  one time use tokens to reset a password for a user. Tokens expire
  automatically after a configurable number of hours.
';
