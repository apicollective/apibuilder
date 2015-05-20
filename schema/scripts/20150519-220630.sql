set search_path to search;

create table versions (
  guid                  uuid primary key references public.versions on delete cascade,
  text                  text not null check(text = trim(lower(text))),
  created_at            timestamptz not null default now()
);

comment on table versions is '
  A denormalization of the data in the versions table optimized for search
';
