drop table if exists public.migrations;

set search_path to public;

create table migrations (
  id                          text primary key check(util.non_empty_trimmed_string(id)),
  version_guid                uuid not null,
  num_attempts                bigint not null check(num_attempts >= 0),
  errors                      json,
  created_at                  timestamptz not null default timezone('utc'::text, now())::timestamptz,
  updated_at                  timestamptz not null default timezone('utc'::text, now())::timestamptz,
  updated_by_guid             text not null check(util.non_empty_trimmed_string(updated_by_guid)),
  hash_code                   bigint not null
);

create index migrations_version_guid_idx on migrations(version_guid);
create index migrations_num_attempts_created_at_idx on migrations(num_attempts, created_at);

alter table migrations
  add constraint migrations_version_guid_fk
  foreign key(version_guid)
  references versions;

select schema_evolution_manager.create_updated_at_trigger('public', 'migrations');