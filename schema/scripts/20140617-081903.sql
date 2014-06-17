drop table if exists user_passwords;

create table user_passwords (
  guid                    uuid not null primary key,
  user_guid               uuid not null references users,
  algorithm_key           text not null,
  hash                    text not null
);

select schema_evolution_manager.create_basic_audit_data('public', 'user_passwords');

create index on user_passwords(user_guid);
create unique index user_passwords_user_guid_not_deleted_un_idx on user_passwords(user_guid) where deleted_at is null;

comment on table user_passwords is '
  An immutable store of hashed user passwords. When a new password is created,
  we soft delete the old password and create a new one. A user can have at most
  1 active password at any time (enforced by a unique constraint)
';

comment on column user_passwords.algorithm_key is '
  A key identifying the algorithm_key / version of hashing we used
  for this particular password.
';

comment on column user_passwords.hash is '
  Base64 encoded hash of the password
';
